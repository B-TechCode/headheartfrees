package com.headheartfrees.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Recovery codes: each works exactly once, and then never again.
 *
 * <p>This is the path that runs when the phone is gone, so it is the path least
 * likely to be exercised before it is needed and most expensive to have wrong.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TotpBackupCodeIT extends TotpTestSupport {

    private static final String EMAIL = "recovery@example.com";

    @Test
    @DisplayName("a backup code signs in once, and then does not")
    void aBackupCodeWorksOnceAndThenDoesNot() throws Exception {
        Enrolled enrolled = enrolUser(EMAIL);
        String code = enrolled.backupCodes().get(0);

        String ticket = ticketOf(login(EMAIL, PASSWORD).andReturn());
        MvcResult first = completeSignIn(ticket, code)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AUTHENTICATED"))
                .andReturn();
        assertThat(first.getResponse().getCookie(RefreshCookie.NAME)).isNotNull();

        resetRateLimiter();

        String secondTicket = ticketOf(login(EMAIL, PASSWORD).andReturn());
        MvcResult second = completeSignIn(secondTicket, code)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOTP_CODE"))
                .andReturn();
        assertThat(second.getResponse().getCookie(RefreshCookie.NAME)).isNull();
    }

    @Test
    @DisplayName("every one of the ten works, and the remaining count follows")
    void allTenWorkOnceEach() throws Exception {
        Enrolled enrolled = enrolUser(EMAIL);
        List<String> used = new ArrayList<>();

        for (String code : enrolled.backupCodes()) {
            resetRateLimiter();
            String ticket = ticketOf(login(EMAIL, PASSWORD).andReturn());
            MvcResult result = completeSignIn(ticket, code)
                    .andExpect(status().isOk())
                    .andReturn();

            used.add(code);
            long expectedRemaining = enrolled.backupCodes().size() - used.size();

            mockMvc.perform(get("/api/v1/auth/me")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenOf(result)))
                    .andExpect(jsonPath("$.backupCodesRemaining").value((int) expectedRemaining));
        }

        // And now there are none. The account is not locked - the authenticator
        // still works - but the recovery material is genuinely spent, which is
        // why /account shows the count and offers to regenerate.
        resetRateLimiter();
        String ticket = ticketOf(login(EMAIL, PASSWORD).andReturn());
        completeSignIn(ticket, used.get(0)).andExpect(status().isUnauthorized());

        resetRateLimiter();
        String stillWorks = ticketOf(login(EMAIL, PASSWORD).andReturn());
        completeSignIn(stillWorks, signInCodeFor(enrolled)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("a backup code is accepted with or without its dash, in any case")
    void formattingIsForgiving() throws Exception {
        Enrolled enrolled = enrolUser(EMAIL);
        String code = enrolled.backupCodes().get(0);

        // Somebody reading this off a printed sheet in a hurry. The dash is
        // presentation, and refusing a correct code over punctuation would be a
        // cruel place to be strict.
        String retyped = code.replace("-", "").toLowerCase(java.util.Locale.ROOT);

        String ticket = ticketOf(login(EMAIL, PASSWORD).andReturn());
        completeSignIn(ticket, retyped).andExpect(status().isOk());
    }

    @Test
    @DisplayName("regenerating replaces every code, and the old ones stop working")
    void regeneratingReplacesThemAll() throws Exception {
        Enrolled enrolled = enrolUser(EMAIL);

        // Signed in with a BACKUP code rather than a TOTP code, deliberately.
        //
        // Enrolment has already spent the current time step, and a backup code
        // spends no step at all - so this leaves the next step free for the
        // regeneration call below to use. Signing in with a TOTP code here
        // would spend that step too, and the only remaining candidate would be
        // two steps out, which the drift window correctly refuses. The
        // alternative is a test that sleeps for thirty seconds.
        String ticket = ticketOf(login(EMAIL, PASSWORD).andReturn());
        MvcResult signedIn = completeSignIn(ticket, enrolled.backupCodes().get(9))
                .andExpect(status().isOk())
                .andReturn();
        String accessToken = accessTokenOf(signedIn);

        resetRateLimiter();

        String code = signInCodeFor(enrolled);
        MvcResult regenerated = mockMvc.perform(post("/api/v1/auth/totp/backup-codes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("code", code)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.backupCodes.length()").value(10))
                .andReturn();

        List<String> fresh = new ArrayList<>();
        objectMapper.readTree(regenerated.getResponse().getContentAsString())
                .get("backupCodes")
                .forEach(node -> fresh.add(node.asText()));

        assertThat(fresh).doesNotContainAnyElementsOf(enrolled.backupCodes());

        // An old code is now worthless, which is what makes regeneration
        // meaningful: the sheet somebody just threw away is not still a way in.
        resetRateLimiter();
        String oldTicket = ticketOf(login(EMAIL, PASSWORD).andReturn());
        completeSignIn(oldTicket, enrolled.backupCodes().get(1))
                .andExpect(status().isUnauthorized());

        resetRateLimiter();
        String newTicket = ticketOf(login(EMAIL, PASSWORD).andReturn());
        completeSignIn(newTicket, fresh.get(0)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("regenerating needs a current code, not just a session")
    void regeneratingNeedsACode() throws Exception {
        Enrolled enrolled = enrolUser(EMAIL);
        String ticket = ticketOf(login(EMAIL, PASSWORD).andReturn());
        MvcResult signedIn = completeSignIn(ticket, enrolled.backupCodes().get(9))
                .andExpect(status().isOk())
                .andReturn();

        resetRateLimiter();

        mockMvc.perform(post("/api/v1/auth/totp/backup-codes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenOf(signedIn))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("code", "000000")))
                .andExpect(status().isUnauthorized());
    }
}
