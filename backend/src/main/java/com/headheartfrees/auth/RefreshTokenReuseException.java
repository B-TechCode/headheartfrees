package com.headheartfrees.auth;

/**
 * A refresh token was presented that had already been spent.
 *
 * <p>This is a security event, not a user error. It means two parties hold
 * tokens descended from the same login, so by the time this is thrown the whole
 * token family has already been revoked and both are logged out.
 *
 * <p>Rendered as 401, identically to {@link InvalidCredentialsException}: the
 * client learns nothing about why, because a client that can tell "reused" from
 * "unknown" can probe for valid tokens.
 */
public class RefreshTokenReuseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RefreshTokenReuseException() {
        super("Refresh token reuse detected");
    }
}
