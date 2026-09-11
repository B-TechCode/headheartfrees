package com.headheartfrees.auth;

import java.time.Duration;

/**
 * What a correct password gets you, which is no longer always a session.
 *
 * <h2>Why this is a type and not a nullable field</h2>
 *
 * The rule this phase exists to enforce is that a correct password with no code
 * must not produce a usable token. A {@code TokenPair} with null tokens, or a
 * {@code TokenPair} plus a {@code totpRequired} boolean, would both express
 * that rule as a convention somebody has to remember at every call site. A
 * sealed hierarchy makes the compiler ask: there is no way to reach the
 * {@link Authenticated} tokens without having matched on it, and no way to add
 * a fourth case without every {@code switch} over this type failing to compile.
 *
 * <p>Note what {@link SecondFactorRequired} and {@link EnrolmentRequired} do
 * <em>not</em> carry: no access token, no refresh token, no {@link UserSummary}.
 * Not one field on them could be mistaken for a session, and the email address
 * is absent too - the client already knows what it just typed, and echoing it
 * back would put an account identifier into a response that is issued before
 * authentication is complete.
 *
 * <p>Package-private, and referenced from the public {@link AuthService} the
 * same way {@link TokenPair} already is: the module's collaborators are inside
 * this package, and nothing outside it needs to name these.
 */
sealed interface LoginOutcome {

    /** The password was right and nothing else was needed. */
    record Authenticated(TokenPair tokens) implements LoginOutcome {
    }

    /**
     * The password was right and the account has a confirmed second factor.
     *
     * @param ticket    a {@link JwtService#TYPE_TOTP_CHALLENGE} token, good for
     *                  one endpoint and nothing else
     * @param expiresIn how long that ticket lasts
     */
    record SecondFactorRequired(String ticket, Duration expiresIn) implements LoginOutcome {
    }

    /**
     * The password was right, the account is an ADMIN, and it has no second
     * factor yet.
     *
     * <p>This is the case that makes the requirement deployable. Without it,
     * turning TOTP on for admins would lock out every admin who had not already
     * enrolled - which, on the day it ships, is all of them. The password still
     * works and still reaches a screen with a QR code on it; the only thing it
     * no longer reaches is the moderation queue.
     *
     * @param ticket a {@link JwtService#TYPE_TOTP_ENROLMENT} token, good for
     *               setup and enable and nothing else
     */
    record EnrolmentRequired(String ticket, Duration expiresIn) implements LoginOutcome {
    }
}
