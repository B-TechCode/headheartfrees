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
  /** Whether a confirmed second factor exists on this account. */
  totpEnabled: boolean;
  /**
   * Whether this account *must* hold one. True for ADMIN and nothing else.
   *
   * Read from the server rather than derived from `role` in the client: the
   * policy lives in one place on the backend (`TotpService.isRequiredFor`), and
   * a second copy here is the kind of thing that goes stale silently.
   */
  totpRequired: boolean;
  /**
   * Unused recovery codes.
   *
   * A count, not the codes — those are shown exactly once, at enrolment, and
   * the API has no endpoint that returns them again.
   */
  backupCodesRemaining: number;
}

export interface AccessTokenResponse {
  accessToken: string;
  tokenType: string;
  /** Seconds until `accessToken` expires. */
  expiresIn: number;
  user: UserSummary;
}

/**
 * What `POST /auth/login` and `POST /auth/login/totp` return.
 *
 * A correct password no longer always means a session, so the body is a
 * discriminated union rather than one shape with optional fields. All three
 * arrive as 200: the credential presented was right in every case, so none of
 * them is an error.
 *
 * The two challenge shapes carry **no** `session`. Not an empty one — the key
 * is absent, because the backend serialises with `non_null` inclusion. A
 * client cannot accidentally treat a challenge as a sign-in, and no refresh
 * cookie is set either.
 */
export type LoginResponse =
  | { status: "AUTHENTICATED"; session: AccessTokenResponse }
  | {
      /** The account has a confirmed second factor. Ask for a code. */
      status: "TOTP_REQUIRED";
      ticket: string;
      /** Seconds. The ticket is a half-finished sign-in, not a session. */
      ticketExpiresIn: number;
    }
  | {
      /**
       * An ADMIN with no second factor yet.
       *
       * This is what stops the requirement locking out an admin who existed
       * before it shipped: the password still works and still reaches a screen
       * with a QR code on it. The only thing it no longer reaches is the
       * moderation queue.
       */
      status: "TOTP_ENROLMENT_REQUIRED";
      ticket: string;
      ticketExpiresIn: number;
    };

/** What `signIn` hands back, once the response above has been narrowed. */
export type SignInOutcome =
  | { kind: "signed-in" }
  | { kind: "code-required"; ticket: string }
  | { kind: "enrolment-required"; ticket: string };

/** `POST /auth/totp/setup` — the same secret in three forms. */
export interface TotpSetupResponse {
  /** base32, grouped in fours, for typing by hand. */
  manualKey: string;
  /** `otpauth://totp/...`, for tapping through on the phone that holds the app. */
  otpauthUri: string;
  /** An SVG `data:` URI for an `<img>`. Rendered server-side. */
  qrDataUri: string;
}

/** `POST /auth/totp/enable`. */
export interface TotpEnableResponse {
  /**
   * Ten single-use codes, in existence for exactly this response.
   *
   * They are Argon2id hashes on the server and there is no endpoint that
   * returns them again. If this response is lost, the codes are gone — which
   * is why the screen rendering them has to say so before the person can
   * navigate away.
   */
  backupCodes: string[];
  /**
   * Present only when enrolment was reached on an enrolment ticket — the admin
   * who was stopped at sign-in. Absent for somebody who enrolled from an
   * account page they were already signed in to.
   */
  session?: AccessTokenResponse;
}

export interface BackupCodesResponse {
  backupCodes: string[];
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
