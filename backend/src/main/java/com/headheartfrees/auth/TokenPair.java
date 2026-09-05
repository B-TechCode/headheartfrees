package com.headheartfrees.auth;

import java.time.Duration;

/**
 * The result of a successful authentication, before the controller decides
 * which half goes where.
 *
 * <p>Deliberately not a response DTO. The two tokens travel by different
 * channels and must never be serialised together: the access token goes in the
 * JSON body, the refresh token goes in an httpOnly cookie and never appears in
 * a body. Keeping them in one internal record and splitting them in exactly one
 * place ({@code AuthController}) is what makes that rule checkable.
 *
 * <p>{@code user} rides along because the service already has the account
 * loaded at the moment it mints these. Without it the controller would have to
 * ask who the token belongs to immediately after issuing it - either a second
 * query or, worse, parsing the subject back out of the JWT it just created.
 *
 * @param accessToken  short-lived HS256 JWT, for the Authorization header
 * @param refreshToken opaque 32-byte random value, for the cookie only
 * @param user         the authenticated person, for the response body
 */
record TokenPair(
        String accessToken,
        Duration accessTokenTtl,
        String refreshToken,
        Duration refreshTokenTtl,
        UserSummary user) {
}
