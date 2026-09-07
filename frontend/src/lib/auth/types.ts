/**
 * The auth shapes the API actually returns.
 *
 * Mirrors `UserSummary`, `AccessTokenResponse` and `RegistrationResponse` on
 * the backend. Three omissions are load-bearing and match deliberate decisions
 * there, so they are restated rather than left to be rediscovered:
 *
 *   - **No refresh token.** It exists only in the httpOnly `hhf_refresh`
 *     cookie. A field for it here is exactly how that separation would be lost,
 *     and `AuthResponseLeakageIT` fails the backend build if one ever appears
 *     in a response body.
 *   - **No password hash**, and no `googleId`. `UserSummary` is the only
 *     user-shaped type the API will serialise, precisely so neither can escape.
 *   - **No email or display name in the access token itself.** They change; a
 *     stale name rendered in a navbar is tedious to trace. They come with the
 *     login and refresh responses instead, which are always current.
 */

export type UserRole = "USER" | "ADMIN";

export interface UserSummary {
  id: string;
  email: string;
  displayName: string | null;
  role: UserRole;
  emailVerified: boolean;
  /** ISO-8601, UTC. */
  createdAt: string;
}

export interface AccessTokenResponse {
  accessToken: string;
  tokenType: string;
  /** Seconds until `accessToken` expires. */
  expiresIn: number;
  user: UserSummary;
}

export interface RegistrationResponse {
  /**
   * The same sentence whether or not an account was created — see
   * `RegistrationResponse.SHARED_MESSAGE` on the backend and the reasoning in
   * `/register`. Render it; do not replace it with a congratulatory one.
   */
  message: string;
}

/**
 * What the UI is allowed to know about the session.
 *
 * `restoring` is a real state and not a loading spinner: it means "we have a
 * hint that this browser has a session and have not yet finished asking". No
 * component may render a signed-out affordance while it holds, because showing
 * "Sign in" to someone who is signed in reads as having been logged out — see
 * the note in `Navbar`.
 */
export type SessionStatus = "restoring" | "authenticated" | "anonymous";
