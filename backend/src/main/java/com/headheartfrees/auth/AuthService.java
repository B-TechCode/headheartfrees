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
     * @throws InvalidCredentialsException for a wrong password, an unknown
     *         email, or an account with no password set - one exception, one
     *         message, no way to tell which
     */
    TokenPair login(String email, String password);

    /**
     * Rotates a refresh token.
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
