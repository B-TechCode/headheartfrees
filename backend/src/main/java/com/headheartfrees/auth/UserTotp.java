package com.headheartfrees.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * One person's second factor: the encrypted secret, and the state that stops it
 * being brute-forced or replayed.
 *
 * <p>Package-private, like {@link UserAccount} and for the same reason
 * (PROJECT_BRIEF.md section 4). <strong>This class is never serialised to a
 * response.</strong> {@link #secretCiphertext} would go with it, and unlike a
 * password hash a TOTP secret is not a one-way transform of anything - it is
 * the credential itself.
 *
 * <h2>The three states</h2>
 *
 * <ul>
 *   <li><strong>No row</strong> - not enrolled.
 *   <li><strong>Row with {@code confirmedAt == null}</strong> - a secret has
 *       been generated and shown, and no code derived from it has been proven
 *       yet. Grants nothing; sign-in treats this account as not enrolled.
 *       Calling setup again simply overwrites it.
 *   <li><strong>Row with {@code confirmedAt != null}</strong> - enrolled.
 * </ul>
 *
 * <p>The middle state is the whole reason enabling requires a verified code.
 * Without it, a mistyped manual key or a QR that never actually scanned would
 * be recorded as a working second factor, and the person would discover it the
 * next time they tried to sign in - locked out by their own enrolment.
 */
@Entity
@Table(name = "user_totp")
class UserTotp {

    /**
     * The failure count at which the second factor locks, and the ladder of
     * lock durations.
     *
     * <h2>The arithmetic, since it is the whole defence</h2>
     *
     * Six digits is 10^6 combinations. With a drift window of one step either
     * side, three codes are live at once, so a single guess succeeds with
     * probability 3x10^-6.
     *
     * <p>A flat "five attempts per fifteen minutes" sounds strict and is not:
     * it permits 480 guesses a day, which is roughly a 40% chance of a hit
     * within one year. That is a speed bump, not a limit.
     *
     * <p>So the counter is <strong>not</strong> reset when a lock expires -
     * only by a success. The lock therefore compounds: 15 minutes, then 30,
     * then 60, then 24 hours from the fourth lockout onward. The sustained rate
     * settles at five guesses per day, which puts even odds of a hit at roughly
     * 180 years.
     *
     * <h2>Why this is not a denial of service worth worrying about</h2>
     *
     * This counter is only reachable by someone who has <em>already passed the
     * password check</em> - the challenge ticket is issued nowhere else.
     * Anyone able to lock the admin out for 24 hours already knows the admin's
     * password, at which point the lock is the least of the problems. Someone
     * who knows only the email address cannot reach this counter at all.
     */
    static final int ATTEMPTS_PER_LOCKOUT = 5;

    private static final Duration[] LOCK_LADDER = {
        Duration.ofMinutes(15), Duration.ofMinutes(30), Duration.ofMinutes(60), Duration.ofHours(24)
    };

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "secret_ciphertext", nullable = false)
    private String secretCiphertext;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "last_used_step")
    private Long lastUsedStep;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Required by JPA. */
    protected UserTotp() {
    }

    UserTotp(UUID userId, String secretCiphertext, Instant now) {
        this.userId = userId;
        this.secretCiphertext = secretCiphertext;
        this.confirmedAt = null;
        this.failedAttempts = 0;
        this.createdAt = now;
        this.updatedAt = now;
    }

    UUID getUserId() {
        return userId;
    }

    String getSecretCiphertext() {
        return secretCiphertext;
    }

    boolean isConfirmed() {
        return confirmedAt != null;
    }

    /** Replaces an unverified secret when someone restarts enrolment. */
    void replaceSecret(String newCiphertext, Instant now) {
        this.secretCiphertext = newCiphertext;
        this.confirmedAt = null;
        this.lastUsedStep = null;
        this.failedAttempts = 0;
        this.lockedUntil = null;
        this.updatedAt = now;
    }

    void confirm(Instant now) {
        this.confirmedAt = now;
        this.updatedAt = now;
    }

    boolean isLockedAt(Instant now) {
        return lockedUntil != null && now.isBefore(lockedUntil);
    }

    Duration lockRemainingAt(Instant now) {
        return isLockedAt(now) ? Duration.between(now, lockedUntil) : Duration.ZERO;
    }

    /**
     * Whether {@code step} has already been spent.
     *
     * <p>Deliberately {@code <=} and not {@code ==}. "The same code cannot be
     * used twice" is the requirement; refusing every step at or below the last
     * accepted one is strictly stronger and is what RFC 6238 section 5.2
     * recommends. It closes the real attack, which is not reuse of the code you
     * just watched somebody type - it is replay of a code from the step before,
     * still live because of the drift window. A shoulder-surfer's window is 90
     * seconds, not 30.
     *
     * <p>The cost: verifying with a code from the next step makes the current
     * step's code unusable for the following minute. Nobody will ever notice.
     */
    boolean hasSpent(long step) {
        return lastUsedStep != null && step <= lastUsedStep;
    }

    /** A code was accepted. Records the step and clears the failure ladder. */
    void recordSuccess(long step, Instant now) {
        this.lastUsedStep = step;
        this.failedAttempts = 0;
        this.lockedUntil = null;
        this.updatedAt = now;
    }

    /**
     * A backup code was accepted.
     *
     * <p>No step to record - a backup code is not time-based - but the failure
     * ladder is cleared, because a successful recovery is exactly as good a
     * proof of possession as a successful code.
     */
    void recordBackupCodeSuccess(Instant now) {
        this.failedAttempts = 0;
        this.lockedUntil = null;
        this.updatedAt = now;
    }

    /**
     * A code was refused. Advances the counter and, on every fifth failure,
     * sets the next lock from the ladder.
     */
    void recordFailure(Instant now) {
        this.failedAttempts++;
        this.updatedAt = now;
        if (failedAttempts % ATTEMPTS_PER_LOCKOUT != 0) {
            return;
        }
        int lockoutNumber = failedAttempts / ATTEMPTS_PER_LOCKOUT;
        Duration duration = LOCK_LADDER[Math.min(lockoutNumber - 1, LOCK_LADDER.length - 1)];
        this.lockedUntil = now.plus(duration);
    }
}
