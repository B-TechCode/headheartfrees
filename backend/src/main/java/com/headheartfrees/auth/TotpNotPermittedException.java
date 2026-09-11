package com.headheartfrees.auth;

/**
 * A TOTP management action that is refused on its own terms, rather than for a
 * bad code.
 *
 * <p>Two cases, and both are safe to state plainly because the caller is
 * already authenticated as the account in question and is being told about
 * their own account:
 *
 * <ul>
 *   <li>An ADMIN trying to turn the second factor off. It is required for that
 *       role, so disabling it could only put the account into forced
 *       re-enrolment at the next sign-in - a worse outcome than the one they
 *       were asking for, arrived at by surprise.
 *   <li>Regenerating backup codes, or turning the factor off, on an account
 *       that has not finished enrolling. There is nothing to act on.
 * </ul>
 */
class TotpNotPermittedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    TotpNotPermittedException(String message) {
        super(message);
    }
}
