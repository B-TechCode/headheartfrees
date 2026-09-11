package com.headheartfrees.auth;

import java.util.UUID;

/**
 * The auth module's entire public surface, alongside its DTOs.
 *
 * <p>PROJECT_BRIEF.md section 4: no other package may import an entity or
 * repository from here. Callers name a person by {@link UUID}.
 *
 * <p>Nothing in this interface is reachable from the {@code vent} package, and
 * that is enforced rather than trusted - see
 * {@code ModuleBoundaryArchitectureTest}. Venting requires no account
 * (rule 2.2), so the vent domain has no legitimate reason to call any of this.
 */
public interface AuthService {

    /**
     * Registers an account.
     *
     * <p><strong>Returns normally when the email is already taken</strong>, and
     * creates nothing. That is not an oversight: a distinguishable duplicate
     * response is an account-existence oracle. The caller cannot tell the two
     * cases apart and neither can an attacker. See
     * {@code DefaultAuthService.register} for how timing is equalised too.
     */
    void register(String email, String password, String displayName);

    /**
     * Checks the password, and stops there when a second factor is owed.
     *
     * <p>A correct password alone produces no access token, no refresh cookie
     * and no {@link UserSummary} on any account that owes one. See
     * {@link LoginOutcome} for why the return type is a sealed hierarchy rather
     * than a nullable session.
     *
     * @throws InvalidCredentialsException for a wrong password, an unknown
     *         email, or an account with no password set - one exception, one
     *         message, no way to tell which
     */
    LoginOutcome login(String email, String password);

    /**
     * Finishes a sign-in that stopped for a code.
     *
     * @param ticket a challenge ticket from {@link #login}
     * @param code   six digits, or a backup code
     * @throws InvalidTotpCodeException for every failure - a wrong code, an
     *         expired or forged ticket, a replayed code, a spent backup code
     * @throws TotpLockedException when the account has failed too often
     */
    TokenPair completeSecondFactor(String ticket, String code);

    /**
     * Resolves an enrolment ticket to the account it was issued for.
     *
     * @throws InvalidTotpCodeException if it is expired, forged, or a ticket of
     *         a different kind
     */
    UUID resolveEnrolmentTicket(String ticket);

    /**
     * Generates a secret and returns it as a QR code and as text.
     *
     * <p>Grants nothing on its own: the row it writes is unconfirmed until
     * {@link #enableTotp} proves a code derived from it.
     */
    TotpService.TotpSetup beginTotpSetup(UUID userId);

    /**
     * Verifies a code against the pending secret and turns the factor on.
     *
     * @param issueSession true only when the caller arrived on an enrolment
     *        ticket and therefore has no session yet
     */
    TotpEnableResult enableTotp(UUID userId, String code, boolean issueSession);

    /** Replaces every backup code, gated on a current code. */
    java.util.List<String> regenerateBackupCodes(UUID userId, String code);

    /**
     * Turns the second factor off.
     *
     * @throws InvalidCredentialsException on a wrong password
     * @throws TotpNotPermittedException for an ADMIN, which requires it
     */
    void disableTotp(UUID userId, String password, String code);

    /**
     * Rotates a refresh token.
     *
     * <p><strong>An ADMIN who owes a second factor is signed out here.</strong>
     * The family is revoked and this throws, which forces a fresh sign-in and
     * therefore forces enrolment. Without it, an admin holding a refresh cookie
     * issued before this feature shipped would keep minting access tokens for
     * thirty days without ever meeting the requirement - a hole the length of
     * {@code app.auth.refresh-token-ttl} in the middle of a mandatory control.
     *
     * @throws RefreshTokenReuseException when the presented token was already
     *         spent, having first revoked its entire family
     * @throws InvalidCredentialsException when it is unknown or expired
     */
    TokenPair refresh(String refreshToken);

    /** Revokes the presented token's whole family. Silent if unknown. */
    void logout(String refreshToken);

    /** @throws InvalidCredentialsException if the id has no account */
    UserSummary summarise(UUID userId);
}
