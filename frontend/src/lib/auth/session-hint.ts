import { clearCookie, readCookie, writeCookie } from "@/lib/cookies";

/**
 * The session hint: one boolean, and **not a credential**.
 *
 * ===========================================================================
 * Read this before treating it as one
 * ===========================================================================
 *
 * The value is the literal string "1". It is not a token, not a user id, not a
 * signature, and not derived from anything secret. Forging it grants exactly
 * one thing: the right to make a `POST /api/v1/auth/refresh` that 401s, because
 * the real credential is the httpOnly `hhf_refresh` cookie the backend sets and
 * script cannot read. Nothing anywhere trusts this value to decide who someone
 * is — it decides only whether a network call is worth making.
 *
 * If a later change starts reading this to authorise anything, that change is
 * wrong.
 *
 * ===========================================================================
 * Why it exists
 * ===========================================================================
 *
 * Without it, every page load by every visitor has to call `/refresh` to find
 * out whether there is a session to restore. That endpoint is rate limited at
 * 5/min per IP alongside register and login (PROJECT_BRIEF.md §6) and, behind
 * Docker or any reverse proxy that is not yet passing the real client address,
 * every visitor shares one bucket. Five anonymous page loads a minute would
 * then exhaust the bucket that real sign-ins need — a denial of service the
 * site would be doing to itself, and one that gets worse the more traffic it
 * gets.
 *
 * With it, a visitor who has never signed in makes no auth request at all.
 *
 * ===========================================================================
 * Lifetime
 * ===========================================================================
 *
 * Thirty days, to match the default `APP_JWT_REFRESH_TOKEN_TTL`. The frontend
 * cannot read that setting — it is a server value — so the two can drift, and
 * the drift is deliberately harmless in both directions:
 *
 *   - hint outlives the refresh token → one wasted call, which 401s, which
 *     clears the hint. Self-correcting on the first page load after expiry.
 *   - refresh token outlives the hint → the person is signed out in the UI
 *     while a valid token sits in a cookie the browser will discard by itself.
 *     Signing in again is one form.
 *
 * Neither is a security property, which is the point of the whole design.
 */
export const SESSION_HINT_COOKIE = "hhf_session_hint";

const LIFETIME_SECONDS = 30 * 24 * 60 * 60;

/** True if this browser has signed in and has not since been signed out. */
export function hasSessionHint(): boolean {
  return readCookie(SESSION_HINT_COOKIE) === "1";
}

export function setSessionHint(): void {
  writeCookie(SESSION_HINT_COOKIE, "1", LIFETIME_SECONDS);
}

export function clearSessionHint(): void {
  clearCookie(SESSION_HINT_COOKIE);
}
