package com.headheartfrees.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;

/**
 * The sign-in half of the second factor.
 *
 * <p>The first test in this class is the requirement the whole phase exists
 * for, and it asserts an absence: a correct password, on an enrolled account,
 * with no code, produces nothing that can authenticate anything. Not a
 * short-lived token, not a cookie, not a partially populated user. An absence
 * is easy to break accidentally and easy to miss in review, so it is checked
 * three ways - the status, the missing body fields, and the missing
 * {@code Set-Cookie}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TotpSignInIT extends TotpTestSupport {

    private static final String EMAIL = "twostep@example.com";

    @Test
    @DisplayName("a correct password and no code issues no session at all")
    void correctPasswordAloneIssuesNoSession() throws Exception {
        Enrolled enrolled = enrolUser(EMAIL);

        MvcResult result = login(enrolled.email(), PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TOTP_REQUIRED"))
                // Absences, checked one at a time so a failure says which of
                // them came back.
                .andExpect(jsonPath("$.session").doesNotExist())
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.user").doesNotExist())
                .andExpect(jsonPath("$.ticket").isNotEmpty())
                .andReturn();

        assertThat(result.getResponse().getCookie(RefreshCookie.NAME))
                .as("A refresh cookie here would be a full session handed out for a password "
                        + "alone, which is the exact thing this phase exists to prevent")
                .isNull();
    }

    @Test
    @DisplayName("the ticket from a password-only login cannot authenticate anything")
    void theTicketIsNotASession() throws Exception {
        Enrolled enrolled = enrolUser(EMAIL);
        String ticket = ticketOf(login(enrolled.email(), PASSWORD).andReturn());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/auth/me")
                        .header(org.springframework.http.HttpHeaders.AUTHORIZATION,
                                "Bearer " + ticket))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("a correct password and a wrong code issues no session")
    void wrongCodeIssuesNoSession() throws Exception {
        Enrolled enrolled = enrolUser(EMAIL);
        String ticket = ticketOf(login(enrolled.email(), PASSWORD).andReturn());

        MvcResult result = completeSignIn(ticket, "000000")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOTP_CODE"))
                .andReturn();

        assertThat(result.getResponse().getCookie(RefreshCookie.NAME)).isNull();
        assertThat(result.getResponse().getContentAsString()).doesNotContain("accessToken");
    }

    @Test
    @DisplayName("a correct password and a correct code signs in")
    void correctCodeSucceeds() throws Exception {
        Enrolled enrolled = enrolUser(EMAIL);
        String ticket = ticketOf(login(enrolled.email(), PASSWORD).andReturn());

        MvcResult result = completeSignIn(ticket, signInCodeFor(enrolled))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AUTHENTICATED"))
                .andExpect(jsonPath("$.session.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.session.user.email").value(EMAIL))
                .andExpect(jsonPath("$.session.user.totpEnabled").value(true))
                .andReturn();

        assertThat(result.getResponse().getCookie(RefreshCookie.NAME)).isNotNull();

        // The token it issued is a real one, not a ticket wearing a different
        // field name.
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/auth/me")
                        .header(org.springframework.http.HttpHeaders.AUTHORIZATION,
                                "Bearer " + accessTokenOf(result)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL));
    }

    @Test
    @DisplayName("the same code cannot be used twice")
    void aCodeCannotBeReplayed() throws Exception {
        Enrolled enrolled = enrolUser(EMAIL);
        String code = signInCodeFor(enrolled);

        String firstTicket = ticketOf(login(enrolled.email(), PASSWORD).andReturn());
        completeSignIn(firstTicket, code).andExpect(status().isOk());

        resetRateLimiter();

        // A second, entirely separate sign-in, with the same code. The code is
        // still inside its time window - that is the point. What refuses it is
        // the spent step recorded on the account, not expiry.
        String secondTicket = ticketOf(login(enrolled.email(), PASSWORD).andReturn());
        MvcResult replayed = completeSignIn(secondTicket, code)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOTP_CODE"))
                .andReturn();

        assertThat(replayed.getResponse().getCookie(RefreshCookie.NAME)).isNull();
    }

    @Test
    @DisplayName("a code from an earlier step inside the drift window is also refused")
    void anOlderCodeInsideTheWindowIsRefused() throws Exception {
        Enrolled enrolled = enrolUser(EMAIL);

        // Enrolment has just accepted the code for the current step, T. The
        // code for T-1 is a different, never-presented code, and the drift
        // window accepts T-1 - so nothing but the spent-step rule can refuse
        // it.
        //
        // That is the distinction being tested. "The same code cannot be used
        // twice" would let this through; the rule actually implemented refuses
        // every step at or below the last accepted one, which is what RFC 6238
        // section 5.2 recommends and what closes the real attack. Somebody who
        // watches a code being typed has a 90-second window, not 30, and the
        // code they saw may well be from the step before the one that lands.
        Instant earlier = oneStepBefore(Instant.now());
        String earlierCode = codeFor(enrolled.manualKey(), earlier);

        org.junit.jupiter.api.Assumptions.assumeTrue(
                !earlierCode.equals(codeFor(enrolled.manualKey())),
                "Two adjacent steps produced the same six digits, which happens about once in "
                        + "a million runs and makes this case untestable");

        String ticket = ticketOf(login(enrolled.email(), PASSWORD).andReturn());
        completeSignIn(ticket, earlierCode)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOTP_CODE"));

        resetRateLimiter();

        // The control: the step AFTER the spent one is accepted, so the
        // refusal above was the spent-step rule and not the account being
        // broken.
        String secondTicket = ticketOf(login(enrolled.email(), PASSWORD).andReturn());
        completeSignIn(secondTicket, signInCodeFor(enrolled)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("a USER with no second factor signs in exactly as before")
    void ordinaryUserIsUntouched() throws Exception {
        register("ordinary@example.com", PASSWORD, "Ordinary Person");

        MvcResult result = login("ordinary@example.com", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AUTHENTICATED"))
                .andExpect(jsonPath("$.session.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.session.user.totpEnabled").value(false))
                .andExpect(jsonPath("$.session.user.totpRequired").value(false))
                .andExpect(jsonPath("$.ticket").doesNotExist())
                .andReturn();

        assertThat(result.getResponse().getCookie(RefreshCookie.NAME))
                .as("Two-step sign-in is optional for a USER. Making it required by accident "
                        + "would be a regression nobody notices until somebody cannot log in")
                .isNotNull();
    }

    @Test
    @DisplayName("a forged or expired ticket is refused, in the same words as a wrong code")
    void aBadTicketIsIndistinguishableFromABadCode() throws Exception {
        Enrolled enrolled = enrolUser(EMAIL);

        MvcResult forged = completeSignIn("not.a.real.ticket", signInCodeFor(enrolled))
                .andExpect(status().isUnauthorized())
                .andReturn();

        String ticket = ticketOf(login(enrolled.email(), PASSWORD).andReturn());
        MvcResult wrongCode = completeSignIn(ticket, "000000")
                .andExpect(status().isUnauthorized())
                .andReturn();

        // Byte-identical but for the timestamp: a caller that could tell a dead
        // ticket from a wrong code could probe which tickets were once live.
        assertThat(bodyWithoutTimestamp(forged))
                .isEqualTo(bodyWithoutTimestamp(wrongCode));
    }

    private String bodyWithoutTimestamp(MvcResult result) throws Exception {
        var node = (com.fasterxml.jackson.databind.node.ObjectNode)
                objectMapper.readTree(result.getResponse().getContentAsString());
        node.remove("timestamp");
        return node.toString();
    }
}
