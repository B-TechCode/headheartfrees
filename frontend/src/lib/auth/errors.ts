import { ApiError } from "@/lib/api";

/**
 * Turning a failed auth call into a sentence.
 *
 * ===========================================================================
 * Why this mostly just passes `message` through
 * ===========================================================================
 *
 * The backend's auth messages are already written to be shown to a person and
 * already say as little as they can. `InvalidCredentialsException` and
 * `RefreshTokenReuseException` produce byte-identical responses on purpose,
 * and registration answers a duplicate address exactly as it answers a new
 * one — the whole point being that nobody can use this site to test whether a
 * given person has an account here.
 *
 * Rewriting those messages in the client is therefore the one thing this must
 * not do. A friendlier "we couldn't find that account" would undo the property
 * the server went to some trouble to have, and it would do it in the layer
 * nobody thinks to audit.
 *
 * So this handles the two cases the server cannot phrase for us — a rate limit
 * whose wait is a number, and a connection that never arrived — and otherwise
 * gets out of the way.
 */
export function describeAuthError(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 429) {
      const wait = error.retryAfterSeconds;
      // `retryAfterSeconds` is only ever populated because SecurityConfig
      // exposes `Retry-After` across origins. If that line is removed this
      // silently degrades to the vaguer sentence rather than breaking, which
      // is exactly why the backend has a test asserting the header is exposed.
      return wait !== null && wait > 0
        ? `Too many attempts. Try again in ${describeWait(wait)}.`
        : "Too many attempts. Try again in a minute.";
    }
    return error.message;
  }

  // Not an ApiError: `fetch` rejected, so nothing reached the server. Saying
  // "incorrect password" here would be a guess, and the wrong one.
  return "We could not reach the server. Check your connection and try again.";
}

function describeWait(seconds: number): string {
  if (seconds < 60) {
    return `${seconds} second${seconds === 1 ? "" : "s"}`;
  }
  const minutes = Math.ceil(seconds / 60);
  return `${minutes} minute${minutes === 1 ? "" : "s"}`;
}

/**
 * Per-field messages from a 400, keyed by field name.
 *
 * Empty for every other status, so a caller can apply it unconditionally.
 */
export function fieldErrorsOf(error: unknown): Record<string, string> {
  return error instanceof ApiError ? error.fieldErrors : {};
}
