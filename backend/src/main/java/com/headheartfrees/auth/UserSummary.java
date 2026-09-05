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
 */
public record UserSummary(
        UUID id,
        String email,
        String displayName,
        UserRole role,
        boolean emailVerified,
        Instant createdAt) {
}
