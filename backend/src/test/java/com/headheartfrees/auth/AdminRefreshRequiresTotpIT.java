package com.headheartfrees.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;

/**
 * The deploy-day hole, closed.
 *
 * <h2>What this is defending</h2>
 *
 * {@code app.auth.refresh-token-ttl} is thirty days. An admin who was signed in
 * when this feature shipped holds a refresh cookie that predates the
 * requirement entirely, and a refresh endpoint that simply rotated it would
 * keep minting admin access tokens for a month without the second factor ever
 * being presented. "Required for admins" would be true of new sign-ins and
 * false of every session that already existed - which, on the day it ships, is
 * the only session that exists.
 *
 * <p>So {@code /refresh} revokes the family and refuses. The admin signs in
 * again, is sent to enrolment, and comes out the other side with a factor. One
 * extra sign-in, once.
 *
 * <p>This test constructs that situation directly: a session obtained while the
 * account was still an ordinary user, and the promotion applied afterwards -
 * which is exactly the sequence {@code APP_ADMIN_BOOTSTRAP_EMAILS} performs at
 * startup, and exactly the sequence a deploy performs.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminRefreshRequiresTotpIT extends TotpTestSupport {

    private static final String EMAIL = "promoted@example.com";

    @Test
    @DisplayName("an admin's pre-existing session is revoked at the first refresh")
    void aPreExistingAdminSessionIsRevoked() throws Exception {
        // Signed in as an ordinary user. This is the cookie that survives the
        // deploy.
        MvcResult login = registerAndLogin(EMAIL);
        Cookie refreshCookie = refreshCookieOf(login);
        String accessToken = accessTokenOf(login);

        // The session works, as it did yesterday.
        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk());

        // Now the account becomes an admin - a restart with
        // APP_ADMIN_BOOTSTRAP_EMAILS set, which is the documented and only way.
        promoteToAdmin(EMAIL);
        resetRateLimiter();

        mockMvc.perform(guardedPost("/api/v1/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isUnauthorized());

        // And the family is genuinely revoked, not merely refused once: the
        // same cookie cannot be retried into a session.
        resetRateLimiter();
        mockMvc.perform(guardedPost("/api/v1/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("after enrolling, the admin refreshes normally again")
    void refreshWorksOnceEnrolled() throws Exception {
        register(EMAIL, PASSWORD, "The Admin");
        promoteToAdmin(EMAIL);

        String enrolTicket = ticketOf(login(EMAIL, PASSWORD).andReturn());
        String manualKey = beginSetupWithTicket(enrolTicket);

        MvcResult enrolled = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/v1/auth/totp/enable")
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content(json("ticket", enrolTicket, "code", codeFor(manualKey))))
                .andExpect(status().isOk())
                .andReturn();

        resetRateLimiter();

        // The revocation is conditional on owing a factor, not on being an
        // admin. An enrolled admin must keep an ordinary session, or they would
        // be signed out every fifteen minutes forever.
        mockMvc.perform(guardedPost("/api/v1/auth/refresh").cookie(refreshCookieOf(enrolled)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.role").value("ADMIN"));
    }

    @Test
    @DisplayName("an ordinary user's refresh is untouched")
    void anOrdinaryUserRefreshesNormally() throws Exception {
        MvcResult login = registerAndLogin("ordinary@example.com");
        resetRateLimiter();

        MvcResult refreshed = mockMvc.perform(
                        guardedPost("/api/v1/auth/refresh").cookie(refreshCookieOf(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        assertThat(refreshed.getResponse().getCookie(RefreshCookie.NAME))
                .as("Two-step sign-in is optional for a USER, so nothing here should change "
                        + "for them at all")
                .isNotNull();
    }
}
