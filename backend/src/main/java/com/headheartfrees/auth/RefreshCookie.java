package com.headheartfrees.auth;

import java.time.Duration;
import org.springframework.http.ResponseCookie;

/**
 * The one place the refresh cookie is built, and the one place it is named.
 *
 * <h2>The split that must not blur</h2>
 *
 * The access token goes in the JSON body and never in a cookie. The refresh
 * token goes in this cookie and never in a body. Each is exposed to a different
 * threat and the placement is what mitigates it: the access token is short-lived
 * and may be read by JavaScript, so XSS costs an attacker fifteen minutes; the
 * refresh token is long-lived and must not be readable by JavaScript at all,
 * which is what {@code httpOnly} buys.
 *
 * <h2>Attributes</h2>
 *
 * <ul>
 *   <li>{@code HttpOnly} - script cannot read it. The whole point.
 *   <li>{@code Secure} - configurable only because a LAN-IP deployment over
 *       plain HTTP would otherwise silently drop it. Browsers treat
 *       {@code localhost} as a secure context, so the default of {@code true}
 *       works for local development over HTTP.
 *   <li>{@code SameSite=Strict} - the cookie is only ever needed on a
 *       same-site XHR to {@code /refresh}, never on a cross-site top-level
 *       navigation, so {@code Lax} would buy nothing. <strong>If the frontend
 *       and backend ever move to different registrable domains this must become
 *       {@code SameSite=None; Secure}</strong>, because port and subdomain do
 *       not affect same-site but the registrable domain does.
 *   <li>{@code Path=/api/v1/auth} - not sent on vent, feedback or donation
 *       requests. A cookie scoped to {@code /} would ride along on every
 *       request to the API including the anonymous ones, which is both
 *       unnecessary exposure and, for {@code /api/v1/vent/**}, precisely the
 *       kind of ambient identifier rule 2.2 is written against.
 * </ul>
 */
final class RefreshCookie {

    static final String NAME = "hhf_refresh";

    /** Scoped so the cookie never accompanies a vent request. */
    private static final String PATH = "/api/v1/auth";

    private RefreshCookie() {
    }

    static ResponseCookie issue(String token, Duration ttl, boolean secure) {
        return base(secure).value(token).maxAge(ttl).build();
    }

    /**
     * An immediate expiry with an empty value. Every attribute except
     * {@code Max-Age} must match the issued cookie or the browser treats it as
     * a different cookie and leaves the original in place - which is the usual
     * reason a logout appears to do nothing.
     */
    static ResponseCookie expired(boolean secure) {
        return base(secure).value("").maxAge(Duration.ZERO).build();
    }

    private static ResponseCookie.ResponseCookieBuilder base(boolean secure) {
        return ResponseCookie.from(NAME)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(PATH);
    }
}
