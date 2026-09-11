package com.headheartfrees.auth;

/**
 * The second factor is locked after repeated failures.
 *
 * <h2>Why this is allowed to be distinguishable, when nothing else is</h2>
 *
 * Every other failure in this flow returns one identical 401. This one returns
 * 429 with {@code Retry-After}, and that is a deliberate exception rather than
 * an oversight.
 *
 * <p>It gives an attacker nothing. The lock counter is only reachable after a
 * correct password, so anyone who can see this response already holds the first
 * factor - the existence of the account, the fact that it is enrolled, and the
 * fact that it is being attacked are all things they already know.
 *
 * <p>And it gives a legitimate person something they badly need. An admin whose
 * correct code is being refused, with no explanation and no indication of when
 * that will stop, has no way to distinguish a lockout from a broken
 * authenticator, a wrong clock, or a bug - and will keep trying, which extends
 * the lock. Saying "not now, try in 14 minutes" is the difference between a
 * wait and a support incident.
 */
class TotpLockedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final long retryAfterSeconds;

    TotpLockedException(long retryAfterSeconds) {
        super("The second factor is locked");
        // Never below 1: a Retry-After of 0 invites an immediate retry that is
        // certain to fail, and some clients treat 0 as absent.
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }

    long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
