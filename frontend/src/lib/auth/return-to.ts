import { clearCookie, readCookie, writeCookie } from "@/lib/cookies";

/**
 * Where to send someone after they sign in.
 *
 * A cookie rather than a query parameter because Google sign-in leaves this
 * origin entirely: the browser goes to the backend, then to Google, then back
 * to the backend, and only then to `/auth/callback`. React state does not
 * survive that, and the backend's redirect URL is fixed configuration that
 * cannot carry a per-person destination, so it has to be parked somewhere the
 * browser carries across.
 *
 * **Short-lived and cleared on use.** Five minutes is longer than any sign-in
 * takes and short enough that a destination cannot sit in a browser for days
 * waiting to surprise someone. `takeReturnTo` deletes it as it reads it, so a
 * second visit to a sign-in page starts from nothing rather than inheriting a
 * stale intention.
 */
export const RETURN_TO_COOKIE = "hhf_return_to";

const LIFETIME_SECONDS = 5 * 60;

/** Long enough for any real path here, short enough to bound what we store. */
const MAX_LENGTH = 512;

/**
 * Pages it is never useful to return to. Returning to `/login` after signing in
 * is a loop, and `/auth/callback` is a machine's page that does nothing without
 * a fresh Google redirect behind it.
 */
const NOT_A_DESTINATION = ["/login", "/register", "/auth/callback"];

/**
 * Control characters, and the backslash that some browsers fold to a slash
 * after this check would otherwise have passed it.
 */
const UNSAFE_CHARACTERS = /[\u0000-\u001f\u007f\\]/;

/**
 * The open-redirect guard.
 *
 * Only a same-origin path is ever accepted, and "starts with a slash" is not
 * enough on its own: `//evil.example` and `/\evil.example` are both read by
 * browsers as protocol-relative URLs to another host, which is the classic way
 * a return-to value becomes a phishing link that begins with your own domain.
 * Anything not clearly safe is rejected rather than repaired.
 *
 * @returns the path, or null if it is not one we will navigate to
 */
export function sanitiseReturnTo(value: string | null | undefined): string | null {
  if (typeof value !== "string" || value.length === 0 || value.length > MAX_LENGTH) {
    return null;
  }
  if (!value.startsWith("/")) {
    return null;
  }
  if (value.startsWith("//") || value.startsWith("/\\")) {
    return null;
  }
  if (UNSAFE_CHARACTERS.test(value)) {
    return null;
  }

  const path = value.split(/[?#]/)[0] ?? "";
  if (NOT_A_DESTINATION.some((page) => path === page || path.startsWith(`${page}/`))) {
    return null;
  }

  return value;
}

/** Parks a destination for the sign-in about to happen. Invalid input is dropped. */
export function rememberReturnTo(path: string | null | undefined): void {
  const safe = sanitiseReturnTo(path);
  if (safe === null) {
    // Not an error: arriving at /login directly is the ordinary case, and it
    // simply means the default destination applies. Clearing rather than
    // leaving whatever was there stops an older intention outliving a newer one.
    clearCookie(RETURN_TO_COOKIE);
    return;
  }
  writeCookie(RETURN_TO_COOKIE, safe, LIFETIME_SECONDS);
}

/**
 * Reads the destination **and clears it**, so it is used at most once.
 *
 * Re-sanitised on the way out. The cookie is writable by script, so what comes
 * back is not necessarily what went in, and validating only on write would put
 * the whole guard on the wrong side of the trust boundary.
 */
export function takeReturnTo(): string | null {
  const raw = readCookie(RETURN_TO_COOKIE);
  clearCookie(RETURN_TO_COOKIE);
  return sanitiseReturnTo(raw);
}
