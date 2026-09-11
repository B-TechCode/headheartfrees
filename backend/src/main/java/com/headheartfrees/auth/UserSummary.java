package com.headheartfrees.auth;

import java.time.Instant;
import java.util.UUID;

/**
 * What the API is willing to say about a person.
 *
 * <p>This is the only user-shaped type that ever reaches a response body. It
 * has no password hash, no Google id and no token, and the omissions are the
 * design: serialising {@link UserAccount} directly would leak the hash, and the
 * way that mistake normally happens is a controller returning the entity
 * "temporarily". {@code AuthResponseLeakageIT} fails the build if a hash or a
 * token ever appears in an auth response.
 *
 * <p>{@code googleId} is absent too. It is an identifier for a third-party
 * account and the client has no use for it.
 *
 * <h2>The three second-factor fields</h2>
 *
 * {@code totpEnabled}, {@code totpRequired} and {@code backupCodesRemaining}
 * are state <em>about</em> a credential and never the credential: no secret, no
 * ciphertext, no code, no hash. They are here because the client cannot render
 * the account page or the enrolment prompt without them, and the alternative -
 * a second endpoint - would be a second thing to keep in step with this one.
 *
 * <p>{@code backupCodesRemaining} is a count, and a count is the honest way to
 * answer "how many do I have left?" for somebody who cannot be shown the codes
 * again. It tells an attacker who has already taken over a session how much
 * recovery material exists, which is not a secret worth protecting from
 * somebody who is already inside the account.
 */
public record UserSummary(
        UUID id,
        String email,
        String displayName,
        UserRole role,
        boolean emailVerified,
        Instant createdAt,
        boolean totpEnabled,
        boolean totpRequired,
        long backupCodesRemaining) {
}
