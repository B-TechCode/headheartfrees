package com.headheartfrees.auth;

/**
 * The body returned by login and refresh.
 *
 * <p>Carries the access token and nothing else that is secret. <strong>The
 * refresh token is not here and must never be added</strong> - it travels only
 * in the httpOnly cookie, and a field for it on this record is exactly how that
 * separation would be lost. {@code AuthResponseLeakageIT} asserts no auth
 * response body ever contains one.
 *
 * @param accessToken short-lived HS256 JWT for the Authorization header
 * @param tokenType   always {@code Bearer}; present so clients need not hardcode it
 * @param expiresIn   seconds until {@code accessToken} expires
 * @param user        who the caller now is, saving an immediate call to /me
 */
public record AccessTokenResponse(
        String accessToken, String tokenType, long expiresIn, UserSummary user) {

    static AccessTokenResponse of(String accessToken, long expiresInSeconds, UserSummary user) {
        return new AccessTokenResponse(accessToken, "Bearer", expiresInSeconds, user);
    }
}
