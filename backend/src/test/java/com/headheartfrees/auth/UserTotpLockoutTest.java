package com.headheartfrees.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The lockout ladder, tested where it can be tested: on the entity, with time
 * as an argument.
 *
 * <h2>Why this is a unit test and the rest are integration tests</h2>
 *
 * The property that matters here is what happens across <em>hours</em> - that
 * the counter does not reset when a lock expires, so a sustained attack settles
 * to about five guesses a day instead of 480. That cannot be observed through
 * MockMvc without either sleeping or making the application's {@link
 * java.time.Clock} mutable in a way that only tests use.
 *
 * <h2>The arithmetic being defended</h2>
 *
 * Six digits is 10^6. With one drift step either side, three codes are live, so
 * a guess lands with probability 3x10^-6.
 *
 * <ul>
 *   <li>Reset-on-expiry: 5 guesses per 15 minutes = 480/day, which is roughly a
 *       40% chance of a hit inside a year. That is a speed bump.
 *   <li>Compounding: the wait doubles to a 24-hour cap, so the sustained rate
 *       is 5 guesses/day and even odds sit at around 180 years.
 * </ul>
 *
 * <p>The difference between those two is one line - whether {@code
 * failedAttempts} goes back to zero when the lock lifts - and it is invisible in
 * every test that only checks "does the fifth attempt get refused".
 */
class UserTotpLockoutTest {

    private static final Instant START = Instant.parse("2026-09-11T10:00:00Z");

    private UserTotp fresh() {
        return new UserTotp(UUID.randomUUID(), "v1:ciphertext", START);
    }

    @Test
    @DisplayName("four failures do not lock; the fifth does")
    void theFifthFailureLocks() {
        UserTotp totp = fresh();

        for (int i = 0; i < 4; i++) {
            totp.recordFailure(START);
            assertThat(totp.isLockedAt(START)).isFalse();
        }

        totp.recordFailure(START);
        assertThat(totp.isLockedAt(START)).isTrue();
        assertThat(totp.lockRemainingAt(START)).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    @DisplayName("the lock lifts on its own after the interval")
    void theLockLifts() {
        UserTotp totp = fresh();
        for (int i = 0; i < 5; i++) {
            totp.recordFailure(START);
        }

        assertThat(totp.isLockedAt(START.plus(Duration.ofMinutes(14)))).isTrue();
        assertThat(totp.isLockedAt(START.plus(Duration.ofMinutes(16)))).isFalse();
    }

    @Test
    @DisplayName("the interval compounds, because the counter does not reset when a lock expires")
    void theLadderCompounds() {
        UserTotp totp = fresh();
        Instant now = START;

        Duration[] expected = {
            Duration.ofMinutes(15),
            Duration.ofMinutes(30),
            Duration.ofMinutes(60),
            Duration.ofHours(24),
            // The cap. Beyond this the ladder stays at 24 hours rather than
            // running away to a lock nobody can wait out, which would turn a
            // brute-force defence into a permanent denial of service against
            // somebody who mistyped a lot.
            Duration.ofHours(24),
            Duration.ofHours(24),
        };

        for (Duration interval : expected) {
            for (int i = 0; i < 5; i++) {
                totp.recordFailure(now);
            }
            assertThat(totp.lockRemainingAt(now))
                    .as("lockout after %d failures", 5 * (java.util.Arrays.asList(expected)
                            .indexOf(interval) + 1))
                    .isEqualTo(interval);

            // Wait out the lock. This is the step that would reset a naive
            // counter, and the whole point is that it does not.
            now = now.plus(interval).plusSeconds(1);
            assertThat(totp.isLockedAt(now)).isFalse();
        }
    }

    @Test
    @DisplayName("a success clears the ladder completely")
    void successResets() {
        UserTotp totp = fresh();
        for (int i = 0; i < 9; i++) {
            totp.recordFailure(START);
        }

        totp.recordSuccess(1_000L, START);

        assertThat(totp.isLockedAt(START)).isFalse();

        // Back to the bottom of the ladder: four more failures are tolerated,
        // and the fifth locks for fifteen minutes, not thirty. Somebody who
        // fumbles occasionally over months must never accumulate their way to a
        // day-long lock.
        for (int i = 0; i < 4; i++) {
            totp.recordFailure(START);
            assertThat(totp.isLockedAt(START)).isFalse();
        }
        totp.recordFailure(START);
        assertThat(totp.lockRemainingAt(START)).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    @DisplayName("a backup code success clears the ladder too")
    void backupCodeSuccessResets() {
        UserTotp totp = fresh();
        for (int i = 0; i < 5; i++) {
            totp.recordFailure(START);
        }

        // A successful recovery is exactly as good a proof of possession as a
        // successful code, and somebody reaching for a backup code is already
        // having a bad day.
        totp.recordBackupCodeSuccess(START);
        assertThat(totp.isLockedAt(START)).isFalse();
    }

    @Test
    @DisplayName("a step at or below the last accepted one is spent")
    void replayWindowIsClosed() {
        UserTotp totp = fresh();
        totp.recordSuccess(1_000L, START);

        assertThat(totp.hasSpent(1_000L)).as("the same step").isTrue();
        // The one that matters: an OLDER code, still inside the drift window.
        // "Cannot be used twice" would let this through; RFC 6238 section 5.2
        // says not to.
        assertThat(totp.hasSpent(999L)).as("an earlier step inside the window").isTrue();
        assertThat(totp.hasSpent(1_001L)).as("the next step").isFalse();
    }

    @Test
    @DisplayName("a fresh enrolment has spent nothing")
    void nothingIsSpentInitially() {
        assertThat(fresh().hasSpent(0L)).isFalse();
        assertThat(fresh().hasSpent(Long.MAX_VALUE)).isFalse();
    }

    @Test
    @DisplayName("replacing the secret clears every piece of state that belonged to the old one")
    void replacingTheSecretResetsState() {
        UserTotp totp = fresh();
        totp.confirm(START);
        totp.recordSuccess(1_000L, START);
        for (int i = 0; i < 5; i++) {
            totp.recordFailure(START);
        }

        totp.replaceSecret("v1:new", START);

        // A spent step or a lock carried over from a secret that no longer
        // exists would make a fresh enrolment fail for reasons nobody could
        // see.
        assertThat(totp.isConfirmed()).isFalse();
        assertThat(totp.isLockedAt(START)).isFalse();
        assertThat(totp.hasSpent(1_000L)).isFalse();
    }
}
