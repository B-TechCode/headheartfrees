import { API_BASE_URL } from "@/lib/api";

/**
 * Whether to offer Google sign-in.
 *
 * ===========================================================================
 * Why this is a build-time flag and not something the API tells us
 * ===========================================================================
 *
 * The backend treats Google as optional: with `GOOGLE_CLIENT_ID` and
 * `GOOGLE_CLIENT_SECRET` unset it starts normally, registers no client, and
 * `/oauth2/authorization/google` simply does not exist. Password sign-in is
 * unaffected. That is the right server behaviour, but it leaves the frontend
 * with no way to know which install it is talking to — and a Google button
 * that leads to a 404 is worse than no button.
 *
 * The clean fix is one unauthenticated endpoint saying which providers are
 * configured, and phase 7 should add it. This phase was scoped to a single
 * backend change, so the setting is duplicated as a build-time flag instead,
 * and the duplication is recorded here rather than left to be discovered:
 *
 *   **If `GOOGLE_CLIENT_ID` is set on the backend, set
 *   `NEXT_PUBLIC_GOOGLE_SIGN_IN=true` on the frontend, and rebuild it.**
 *
 * `NEXT_PUBLIC_*` values are inlined into the bundle at build time, so this
 * needs `docker compose build frontend`, not a restart. Getting it wrong is
 * visible rather than silent: set when the backend is not configured, the
 * button 404s; unset when it is, the button is absent and password sign-in
 * still works.
 */
export const GOOGLE_SIGN_IN_ENABLED = process.env.NEXT_PUBLIC_GOOGLE_SIGN_IN === "true";

/**
 * Where the Google button goes.
 *
 * A full page navigation to the **backend**, not a `fetch`. The whole flow is
 * a chain of top-level redirects — this origin, the backend, Google, the
 * backend again, and finally `/auth/callback` — and an XHR cannot participate
 * in one. It is also why the destination someone was heading for has to be
 * parked in a cookie first (`return-to.ts`): nothing in React state survives
 * leaving the origin.
 */
export const GOOGLE_SIGN_IN_URL = `${API_BASE_URL}/oauth2/authorization/google`;

/**
 * The `?error=` values `GoogleSignInHandler` can redirect back with, rendered
 * as something a person can act on.
 *
 * `unverified_email` is the one that needs explaining. The backend refuses to
 * match an unverified Google address onto an account here, because an address
 * Google has not verified could be anyone's and linking on it would be an
 * account-takeover primitive. Saying only "sign-in failed" would send someone
 * to retry the thing that cannot work.
 */
export function describeCallbackError(code: string | null): string | null {
  switch (code) {
    case null:
      return null;
    case "unverified_email":
      return (
        "Google has not verified the email address on that account, so we cannot use it " +
        "to sign you in. Verify the address with Google and try again, or use a password " +
        "instead."
      );
    case "oauth2":
      return "Google sign-in did not complete. You can try again, or use a password instead.";
    default:
      return "Google sign-in did not complete. You can try again, or use a password instead.";
  }
}
