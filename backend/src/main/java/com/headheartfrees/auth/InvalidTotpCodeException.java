package com.headheartfrees.auth;

/**
 * Every failure of the code step, without exception.
 *
 * <p>Thrown for a wrong code, an expired or forged challenge ticket, a code
 * already spent, an unknown backup code, an already-used backup code, and a
 * code presented for an account that is not enrolled. One exception, rendered
 * as one message.
 *
 * <p>That uniformity is the point, and it is the same discipline
 * {@link InvalidCredentialsException} and {@link RefreshTokenReuseException}
 * already share: an attacker who is told <em>which</em> part of an attempt was
 * wrong learns whether a ticket is still live, whether an account is enrolled,
 * and whether a guessed backup code was once real. None of that is worth the
 * marginally kinder error message.
 *
 * <p>The one deliberate exception is {@link TotpLockedException}, which is
 * distinguishable. See that class for why that is safe.
 */
class InvalidTotpCodeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    InvalidTotpCodeException() {
        super("Invalid one-time code");
    }
}
