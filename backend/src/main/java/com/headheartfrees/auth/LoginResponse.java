package com.headheartfrees.auth;

/**
 * The body of {@code POST /auth/login} and {@code POST /auth/login/totp}.
 *
 * <h2>Why one endpoint returns three shapes</h2>
 *
 * A correct password no longer always means a session. It can also mean "now
 * show me a code" or "now enrol", and none of the three is an error - the
 * credential presented was right in every case, so none of them is a 4xx.
 *
 * <p>{@code status} is the discriminator and is always present. With Jackson's
 * {@code non_null} inclusion (see {@code application.yml}) each response
 * carries only the fields its status uses:
 *
 * <pre>
 * {"status":"AUTHENTICATED","session":{"accessToken":"...","user":{...}}}
 * {"status":"TOTP_REQUIRED","ticket":"...","ticketExpiresIn":300}
 * {"status":"TOTP_ENROLMENT_REQUIRED","ticket":"...","ticketExpiresIn":300}
 * </pre>
 *
 * <p><strong>The two ticket shapes carry no session and no user.</strong> Not a
 * partial one, not a null-filled one: the field is absent. A client cannot
 * accidentally treat a challenge as a sign-in because there is nothing there to
 * treat as one, and a {@code Set-Cookie} is not sent either.
 *
 * <p>{@code session} nests {@link AccessTokenResponse} rather than repeating
 * its fields, so the shape lock in {@code AuthResponseLeakageIT} keeps covering
 * the one type that carries a token.
 *
 * @param ticketExpiresIn seconds. The client shows a real deadline instead of
 *                        letting somebody type a code into a ticket that died
 *                        four minutes ago.
 */
public record LoginResponse(
        String status, AccessTokenResponse session, String ticket, Long ticketExpiresIn) {

    /** Signed in. The only status that comes with a refresh cookie. */
    public static final String AUTHENTICATED = "AUTHENTICATED";

    /** Password accepted; the account has a confirmed second factor. */
    public static final String TOTP_REQUIRED = "TOTP_REQUIRED";

    /** Password accepted; the account is an ADMIN and has not enrolled yet. */
    public static final String TOTP_ENROLMENT_REQUIRED = "TOTP_ENROLMENT_REQUIRED";

    static LoginResponse authenticated(AccessTokenResponse session) {
        return new LoginResponse(AUTHENTICATED, session, null, null);
    }

    static LoginResponse challenge(String status, String ticket, long expiresInSeconds) {
        return new LoginResponse(status, null, ticket, expiresInSeconds);
    }
}
