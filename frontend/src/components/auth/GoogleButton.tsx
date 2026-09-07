"use client";

import { cn } from "@/lib/cn";
import { focusRing } from "@/components/ui/styles";
import { GOOGLE_SIGN_IN_URL } from "@/lib/auth/google";
import { rememberReturnTo } from "@/lib/auth/return-to";

/**
 * Sign in with Google.
 *
 * An anchor, not a `Button`. This is a top-level navigation off this origin —
 * `fetch` cannot follow the redirect chain to Google and back — so it has to
 * be a real link, and the `Button` token classes are applied to the anchor
 * rather than nesting a `<button>` inside a link, which is invalid HTML. The
 * 404 page does the same thing for the same reason.
 *
 * `rememberReturnTo` runs on the click, before the browser leaves. It is a
 * synchronous cookie write, so there is no race with the navigation.
 */
export function GoogleButton({ returnTo }: { returnTo?: string | null }) {
  return (
    <a
      href={GOOGLE_SIGN_IN_URL}
      onClick={() => rememberReturnTo(returnTo)}
      className={cn(
        "inline-flex h-12 w-full items-center justify-center gap-3 rounded-md px-5",
        "border border-ink-faint bg-surface-raised",
        "font-sans text-body font-medium tracking-[0.005em] text-ink",
        "transition-[background-color,border-color] duration-150 ease-out",
        "hover:bg-surface-sunk active:translate-y-[0.5px]",
        focusRing,
      )}
    >
      <GoogleMark />
      Continue with Google
    </a>
  );
}

/**
 * Google's G.
 *
 * The four hex values are hardcoded, which is the one place in this codebase
 * that is correct: they are another organisation's trademark and Google's brand
 * guidelines require the mark be reproduced in its own colours. Putting them in
 * `globals.css` would file someone else's logo in our palette, where the next
 * person to adjust the accent would quite reasonably change them.
 */
function GoogleMark() {
  return (
    <svg viewBox="0 0 18 18" aria-hidden="true" focusable="false" className="h-[18px] w-[18px]">
      <path
        fill="#4285F4"
        d="M17.64 9.2c0-.64-.06-1.25-.16-1.84H9v3.48h4.84a4.14 4.14 0 0 1-1.8 2.72v2.26h2.92c1.7-1.57 2.68-3.88 2.68-6.62Z"
      />
      <path
        fill="#34A853"
        d="M9 18c2.43 0 4.47-.8 5.96-2.18l-2.92-2.26c-.8.54-1.84.86-3.04.86-2.34 0-4.32-1.58-5.03-3.7H.96v2.33A9 9 0 0 0 9 18Z"
      />
      <path
        fill="#FBBC05"
        d="M3.97 10.72a5.4 5.4 0 0 1 0-3.44V4.95H.96a9 9 0 0 0 0 8.1l3.01-2.33Z"
      />
      <path
        fill="#EA4335"
        d="M9 3.58c1.32 0 2.5.45 3.44 1.35l2.58-2.58C13.46.89 11.43 0 9 0A9 9 0 0 0 .96 4.95l3.01 2.33C4.68 5.16 6.66 3.58 9 3.58Z"
      />
    </svg>
  );
}
