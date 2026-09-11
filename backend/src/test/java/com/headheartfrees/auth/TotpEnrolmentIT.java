package com.headheartfrees.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Enrolment: proving a code before the factor turns on, and the admin who is
 * sent here instead of being signed in.
 *
 * <p>The second group of tests is the one that makes this feature deployable.
 * An install with an existing admin and no second factor must not become an
 * install with no reachable admin, and the only way to be sure of that is to
 * set it up in a test and walk through it.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TotpEnrolmentIT extends TotpTestSupport {

    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String USER_EMAIL = "optional@example.com";

    @Test
    @DisplayName("setup returns a QR code and a manual key, and turns nothing on")
    void setupDoesNotEnable() throws Exception {
        MvcResult login = registerAndLogin(USER_EMAIL);
        String accessToken = accessTokenOf(login);

        MvcResult setup = mockMvc.perform(post("/api/v1/auth/totp/setup")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.manualKey").isNotEmpty())
                .andExpect(jsonPath("$.otpauthUri").value(
                        org.hamcrest.Matchers.startsWith("otpauth://totp/")))
                .andExpect(jsonPath("$.qrDataUri").value(
                        org.hamcrest.Matchers.startsWith("data:image/svg+xml;base64,")))
                .andReturn();

        assertThat(setup.getResponse().getContentAsString()).isNotEmpty();

        // The account is NOT enrolled. A setup screen opened and abandoned must
        // leave sign-in exactly as it was, or opening it by accident would be a
        // lockout.
        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totpEnabled").value(false));

        resetRateLimiter();
        login(USER_EMAIL, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AUTHENTICATED"));
    }

    @Test
    @DisplayName("enabling requires a verified code first")
    void enablingRequiresAVerifiedCode() throws Exception {
        MvcResult login = registerAndLogin(USER_EMAIL);
        String accessToken = accessTokenOf(login);
        beginSetup(accessToken);

        mockMvc.perform(post("/api/v1/auth/totp/enable")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("code", "000000")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOTP_CODE"))
                // No codes handed out for a failed enrolment.
                .andExpect(jsonPath("$.backupCodes").doesNotExist());

        // And the factor is still off, so a mistyped manual key leaves a
        // working account rather than a locked one.
        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(jsonPath("$.totpEnabled").value(false));
    }

    @Test
    @DisplayName("enabling with a valid code turns it on and returns ten codes, once")
    void enablingWithAValidCodeSucceeds() throws Exception {
        MvcResult login = registerAndLogin(USER_EMAIL);
        String accessToken = accessTokenOf(login);
        String manualKey = beginSetup(accessToken);

        MvcResult enabled = mockMvc.perform(post("/api/v1/auth/totp/enable")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("code", codeFor(manualKey))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.backupCodes").isArray())
                .andExpect(jsonPath("$.backupCodes.length()").value(10))
                // Already signed in, so no second session is issued.
                .andExpect(jsonPath("$.session").doesNotExist())
                .andReturn();

        JsonNode codes = objectMapper.readTree(enabled.getResponse().getContentAsString())
                .get("backupCodes");
        assertThat(codes).allSatisfy(node ->
                assertThat(node.asText()).matches("[23456789ABCDEFGHJKMNPQRSTVWXYZ]{5}-[23456789ABCDEFGHJKMNPQRSTVWXYZ]{5}"));

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(jsonPath("$.totpEnabled").value(true))
                .andExpect(jsonPath("$.backupCodesRemaining").value(10));
    }

    // -----------------------------------------------------------------
    // The deploy-day case: an admin that already exists and has no factor.
    // -----------------------------------------------------------------

    @Test
    @DisplayName("an existing admin with no second factor is not locked out - it is sent to enrol")
    void anExistingAdminIsNotLockedOut() throws Exception {
        register(ADMIN_EMAIL, PASSWORD, "The Admin");
        promoteToAdmin(ADMIN_EMAIL);

        // Exactly what happens on the first sign-in after this ships.
        MvcResult stopped = login(ADMIN_EMAIL, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TOTP_ENROLMENT_REQUIRED"))
                .andExpect(jsonPath("$.ticket").isNotEmpty())
                .andExpect(jsonPath("$.session").doesNotExist())
                .andReturn();

        assertThat(stopped.getResponse().getCookie(RefreshCookie.NAME)).isNull();

        String ticket = ticketOf(stopped);

        // The ticket reaches a screen with a QR code on it, holding no session.
        String manualKey = beginSetupWithTicket(ticket);

        // And finishing enrolment issues the session they came for, so this is
        // one step rather than "enrol, now sign in again".
        MvcResult enrolled = mockMvc.perform(post("/api/v1/auth/totp/enable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("ticket", ticket, "code", codeFor(manualKey))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.backupCodes.length()").value(10))
                .andExpect(jsonPath("$.session.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.session.user.role").value("ADMIN"))
                .andReturn();

        assertThat(enrolled.getResponse().getCookie(RefreshCookie.NAME)).isNotNull();

        // The session is real: the moderation queue is reachable.
        mockMvc.perform(get("/api/v1/admin/feedback")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenOf(enrolled)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("an enrolment ticket cannot reach the moderation queue")
    void anEnrolmentTicketIsNotAnAdminSession() throws Exception {
        register(ADMIN_EMAIL, PASSWORD, "The Admin");
        promoteToAdmin(ADMIN_EMAIL);

        String ticket = ticketOf(login(ADMIN_EMAIL, PASSWORD).andReturn());

        // The whole feature in one assertion: the thing a correct admin
        // password now buys must not open the thing the password used to open.
        mockMvc.perform(get("/api/v1/admin/feedback")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ticket))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("an admin signs in with password then code, and the queue opens")
    void anEnrolledAdminSignsInWithACode() throws Exception {
        register(ADMIN_EMAIL, PASSWORD, "The Admin");
        promoteToAdmin(ADMIN_EMAIL);

        String enrolTicket = ticketOf(login(ADMIN_EMAIL, PASSWORD).andReturn());
        String manualKey = beginSetupWithTicket(enrolTicket);
        mockMvc.perform(post("/api/v1/auth/totp/enable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("ticket", enrolTicket, "code", codeFor(manualKey))))
                .andExpect(status().isOk());

        resetRateLimiter();

        // Now the ordinary admin sign-in, from scratch.
        MvcResult challenge = login(ADMIN_EMAIL, PASSWORD)
                .andExpect(jsonPath("$.status").value("TOTP_REQUIRED"))
                .andReturn();

        MvcResult signedIn = completeSignIn(
                        ticketOf(challenge),
                        // Enrolment spent the current step; the next one is the
                        // first this account has not used.
                        codeFor(manualKey, oneStepAfter(java.time.Instant.now())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.session.user.totpRequired").value(true))
                .andReturn();

        mockMvc.perform(get("/api/v1/admin/feedback")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenOf(signedIn)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("an admin cannot turn the second factor off")
    void anAdminCannotDisableIt() throws Exception {
        register(ADMIN_EMAIL, PASSWORD, "The Admin");
        promoteToAdmin(ADMIN_EMAIL);

        String enrolTicket = ticketOf(login(ADMIN_EMAIL, PASSWORD).andReturn());
        String manualKey = beginSetupWithTicket(enrolTicket);
        MvcResult enrolled = mockMvc.perform(post("/api/v1/auth/totp/enable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("ticket", enrolTicket, "code", codeFor(manualKey))))
                .andExpect(status().isOk())
                .andReturn();

        resetRateLimiter();

        mockMvc.perform(post("/api/v1/auth/totp/disable")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenOf(enrolled))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("password", PASSWORD,
                                "code", codeFor(manualKey, oneStepAfter(java.time.Instant.now())))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("TOTP_NOT_PERMITTED"));
    }

    @Test
    @DisplayName("a USER can turn it off with a password and a code")
    void aUserCanDisableIt() throws Exception {
        Enrolled enrolled = enrolUser(USER_EMAIL);

        // Signed in with a BACKUP code, which spends no time step - leaving the
        // next step free for the disable call. Enrolment already spent the
        // current one, and a TOTP sign-in here would spend the only remaining
        // candidate inside the drift window. See TotpBackupCodeIT for the same
        // manoeuvre and the same reason.
        MvcResult challenge = login(USER_EMAIL, PASSWORD).andReturn();
        MvcResult signedIn = completeSignIn(
                        ticketOf(challenge), enrolled.backupCodes().get(9))
                .andExpect(status().isOk())
                .andReturn();

        resetRateLimiter();

        String code = signInCodeFor(enrolled);

        mockMvc.perform(post("/api/v1/auth/totp/disable")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenOf(signedIn))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("password", PASSWORD, "code", code)))
                .andExpect(status().isNoContent());

        resetRateLimiter();
        login(USER_EMAIL, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AUTHENTICATED"));
    }

    @Test
    @DisplayName("turning it off needs the password, not just a session")
    void disablingNeedsThePassword() throws Exception {
        Enrolled enrolled = enrolUser(USER_EMAIL);
        MvcResult challenge = login(USER_EMAIL, PASSWORD).andReturn();
        MvcResult signedIn = completeSignIn(
                        ticketOf(challenge), enrolled.backupCodes().get(9))
                .andExpect(status().isOk())
                .andReturn();

        resetRateLimiter();

        // The threat is somebody else holding the session, and a session is
        // exactly what they have. Without the password, this would be the one
        // move a hijacked session could make to render itself permanent.
        mockMvc.perform(post("/api/v1/auth/totp/disable")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenOf(signedIn))
                        .contentType(MediaType.APPLICATION_JSON)
                        // A genuinely VALID code, so the 401 is attributable to
                        // the password and nothing else. A wrong code here would
                        // make this test pass without proving anything about
                        // whether the password is checked at all.
                        .content(json("password", "not the password",
                                "code", signInCodeFor(enrolled))))
                .andExpect(status().isUnauthorized());
    }
}
