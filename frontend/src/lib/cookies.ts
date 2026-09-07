/**
 * Reading and writing the two cookies this frontend owns.
 *
 * ===========================================================================
 * What may and may not go through here
 * ===========================================================================
 *
 * These are cookies JavaScript can read, which means they are cookies an XSS
 * can read. Only two values are ever stored with them, and neither is worth
 * stealing:
 *
 *   - `hhf_session_hint` — a boolean. See lib/auth/session-hint.ts.
 *   - `hhf_return_to`    — a same-origin path, for a few minutes. See
 *                          lib/auth/return-to.ts.
 *
 * The refresh token is **not** in this file's reach and must never be. It is
 * set by the backend as `HttpOnly`, which is exactly the property that keeps it
 * out of `document.cookie` — a helper here that could read or write it would
 * have thrown that away.
 *
 * And, rule 2.1: nothing a person typed in the vent box goes in a cookie. Not a
 * draft, not a hash of one, not a length.
 */

/** `Secure` on HTTPS, omitted otherwise. */
function secureAttribute(): string {
  // Hardcoding `Secure` would be right in production and would silently drop
  // both cookies on a LAN-IP deployment served over plain HTTP — the same
  // deployment APP_COOKIE_SECURE=false exists for on the backend. Browsers
  // treat localhost as a secure context either way, so development is
  // unaffected by the check.
  return typeof location !== "undefined" && location.protocol === "https:" ? "; Secure" : "";
}

export function readCookie(name: string): string | null {
  if (typeof document === "undefined") {
    return null;
  }

  // Split on "; " rather than parsing: a cookie value cannot contain ";" and
  // both of ours are written by writeCookie below, so there is no exotic
  // quoting to handle.
  const prefix = `${name}=`;
  for (const part of document.cookie.split("; ")) {
    if (part.startsWith(prefix)) {
      return decodeURIComponent(part.slice(prefix.length));
    }
  }
  return null;
}

export function writeCookie(name: string, value: string, maxAgeSeconds: number): void {
  if (typeof document === "undefined") {
    return;
  }

  // SameSite=Lax, not Strict. Both cookies must survive the return leg of
  // Google sign-in, which arrives as a top-level cross-site navigation from
  // accounts.google.com by way of the backend. Strict would withhold them on
  // exactly that request and /auth/callback would lose the destination it was
  // meant to return the person to. Neither value is a credential, so Lax costs
  // nothing here — the refresh token, which is one, stays Strict on the
  // backend's own cookie.
  document.cookie =
    `${name}=${encodeURIComponent(value)}` +
    `; Max-Age=${Math.max(0, Math.floor(maxAgeSeconds))}` +
    "; Path=/" +
    "; SameSite=Lax" +
    secureAttribute();
}

export function clearCookie(name: string): void {
  if (typeof document === "undefined") {
    return;
  }
  // Every attribute except Max-Age has to match what was written or the browser
  // treats this as a different cookie and leaves the original in place.
  document.cookie =
    `${name}=; Max-Age=0; Path=/; SameSite=Lax${secureAttribute()}`;
}
