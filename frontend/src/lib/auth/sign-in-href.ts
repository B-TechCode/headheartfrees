import { sanitiseReturnTo } from "./return-to";

/**
 * The link to `/login`, carrying where the person was.
 *
 * A query parameter here rather than the cookie, because this is an ordinary
 * same-origin navigation and a URL that says where it will send you is easier
 * to reason about than one that reads a cookie you cannot see. The cookie
 * exists for the case a URL cannot cover — Google sign-in, which leaves this
 * origin entirely (`return-to.ts`).
 *
 * `/login` sanitises whatever arrives with `sanitiseReturnTo` regardless of
 * which route it came by, so this being user-visible and editable is fine: an
 * edited value is either a same-origin path, or it is dropped.
 */
export function signInHref(currentPath: string | null | undefined): string {
  // Home is the default destination anyway, so `?next=/` is noise in the URL
  // bar for no gain. `/login` and `/register` are refused by the sanitiser.
  const next = currentPath === "/" ? null : sanitiseReturnTo(currentPath);
  return next === null ? "/login" : `/login?next=${encodeURIComponent(next)}`;
}
