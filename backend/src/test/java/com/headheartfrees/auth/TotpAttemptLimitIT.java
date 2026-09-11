package com.headheartfrees.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;

/**
 * The limit, and the part that matters: that it actually stops the sequence.
 *
 * <h2>Why this tests the per-account lock and not the per-IP one</h2>
 *
 * Both exist. The per-IP bucket is the outer bound and is the weaker half -
 * behind Docker every visitor shares one bucket today (HANDOVER 10.3), so it is
 * either evadable from a second address or a nuisance to everybody. The limit
 * that holds is on the account row, and it is what these tests exercise:
 * {@link #resetRateLimiter()} is called between attempts precisely so that the
 * IP bucket cannot be what stops the sequence. If the account lock were
 * removed, these tests would keep making requests and keep getting 401s
 * forever, which is the failure they exist to catch.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TotpAttemptLimitIT extends TotpTestSupport {

    private static final String EMAIL = "guessed@example.com";

    @Test
    @DisplayName("five wrong codes lock the sequence, and the sixth is refused with Retry-After")
    void theSequenceStops() throws Exception {
        Enrolled enrolled = enrolUser(EMAIL);
        String ticket = ticketOf(login(EMAIL, PASSWORD).andReturn());

        for (int attempt = 1; attempt <= UserTotp.ATTEMPTS_PER_LOCKOUT; attempt++) {
            resetRateLimiter();
            completeSignIn(ticket, "000000")
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_TOTP_CODE"));
        }

        resetRateLimiter();
        MvcResult locked = completeSignIn(ticket, "000000")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TOTP_LOCKED"))
                .andReturn();

        assertThat(locked.getResponse().getHeader(HttpHeaders.RETRY_AFTER))
                .as("Without Retry-After an admin has no idea whether to wait a minute or an "
                        + "hour, and will keep trying - which extends the lock")
                .isNotNull();
        assertThat(Integer.parseInt(locked.getResponse().getHeader(HttpHeaders.RETRY_AFTER)))
                .isPositive();
    }

    @Test
    @DisplayName("the lock stops a CORRECT code too, which is the whole point")
    void aCorrectCodeIsRefusedWhileLocked() throws Exception {
        Enrolled enrolled = enrolUser(EMAIL);
        String ticket = ticketOf(login(EMAIL, PASSWORD).andReturn());

        for (int attempt = 1; attempt <= UserTotp.ATTEMPTS_PER_LOCKOUT; attempt++) {
            resetRateLimiter();
            completeSignIn(ticket, "000000").andExpect(status().isUnauthorized());
        }

        resetRateLimiter();

        // A limit that let the right answer through would be no limit at all:
        // the attacker's whole plan is to eventually submit the right answer.
        MvcResult result = completeSignIn(ticket, signInCodeFor(enrolled))
                .andExpect(status().isTooManyRequests())
                .andReturn();

        assertThat(result.getResponse().getCookie(RefreshCookie.NAME)).isNull();
    }

    @Test
    @DisplayName("a backup code is refused while locked, and is not consumed")
    void aBackupCodeIsRefusedWhileLocked() throws Exception {
        Enrolled enrolled = enrolUser(EMAIL);
        String ticket = ticketOf(login(EMAIL, PASSWORD).andReturn());

        for (int attempt = 1; attempt <= UserTotp.ATTEMPTS_PER_LOCKOUT; attempt++) {
            resetRateLimiter();
            completeSignIn(ticket, "000000").andExpect(status().isUnauthorized());
        }

        resetRateLimiter();
        // Sharing the counter is deliberate - otherwise the backup codes are an
        // unlimited second guessing surface next to a limited one.
        completeSignIn(ticket, enrolled.backupCodes().get(0))
                .andExpect(status().isTooManyRequests());

        // And it must not have been spent by the refusal. Burning somebody's
        // recovery codes while refusing to accept them is the worst of both
        // outcomes: they wait out the lock and find their way back in is gone.
        //
        // Read from the table rather than from /me, because there is no session
        // to ask with - which is the situation this whole test is about.
        assertThat(unusedBackupCodesFor(EMAIL))
                .as("A code refused by the lock must still be usable once the lock lifts")
                .isEqualTo(enrolled.backupCodes().size());
    }

    @Test
    @DisplayName("a successful code clears the counter")
    void successResetsTheCounter() throws Exception {
        Enrolled enrolled = enrolUser(EMAIL);
        String ticket = ticketOf(login(EMAIL, PASSWORD).andReturn());

        // Four failures - one short of the lock.
        for (int attempt = 1; attempt < UserTotp.ATTEMPTS_PER_LOCKOUT; attempt++) {
            resetRateLimiter();
            completeSignIn(ticket, "000000").andExpect(status().isUnauthorized());
        }

        resetRateLimiter();
        completeSignIn(ticket, signInCodeFor(enrolled)).andExpect(status().isOk());

        resetRateLimiter();
        String next = ticketOf(login(EMAIL, PASSWORD).andReturn());

        // Four more failures must not lock, because the counter went back to
        // zero. Somebody who fumbles occasionally over months should never
        // accumulate their way into a 24-hour lock.
        for (int attempt = 1; attempt < UserTotp.ATTEMPTS_PER_LOCKOUT; attempt++) {
            resetRateLimiter();
            completeSignIn(next, "000000").andExpect(status().isUnauthorized());
        }
    }

    @Test
    @DisplayName("the per-IP limit exists as well, and is its own bucket")
    void theIpLimitIsSeparateFromTheAuthBucket() throws Exception {
        Enrolled enrolled = enrolUser(EMAIL);
        String ticket = ticketOf(login(EMAIL, PASSWORD).andReturn());

        // Exhaust the TOTP bucket without touching the account lock, by
        // stopping short of five failures and letting the limiter do the rest.
        // The limiter is NOT reset in this test - that is the point.
        int refusedByTheLimiter = 0;
        for (int i = 0; i < 30; i++) {
            int statusCode = completeSignIn(ticket, "000000").andReturn().getResponse().getStatus();
            if (statusCode == 429) {
                refusedByTheLimiter++;
            }
        }
        assertThat(refusedByTheLimiter)
                .as("The TOTP policy should have cut the sequence off well inside 30 attempts")
                .isPositive();

        // And the auth bucket is untouched, so somebody who fumbled a code can
        // still sign in. Sharing one bucket would mean a mistyped code costs
        // you the ability to try again at all.
        resetRateLimiter();
        login(EMAIL, PASSWORD).andExpect(status().isOk());
    }
}
