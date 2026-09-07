"use client";

import { usePathname, useRouter } from "next/navigation";
import { useEffect } from "react";
import type { ReactNode } from "react";
import { useSession } from "@/lib/auth/SessionProvider";
import { signInHref } from "@/lib/auth/sign-in-href";
import type { UserSummary } from "@/lib/auth/types";

/**
 * A page that needs an account.
 *
 * ===========================================================================
 * What this is, and what it is not
 * ===========================================================================
 *
 * This is a convenience, not a control. Everything it protects is data the API
 * will refuse to hand over anyway: `/api/v1/auth/me` is the one auth endpoint
 * deliberately absent from `SecurityConfig`'s public list, and the moderation
 * endpoints check the role on the server. A client-side check that could be
 * defeated by editing a variable in a debugger is exactly as strong as it
 * sounds, which is why nothing here is the reason anything is safe. It is here
 * so that someone who follows a bookmark to a page they cannot use gets a
 * sign-in form instead of an empty screen and a 401.
 *
 * ===========================================================================
 * The waiting state
 * ===========================================================================
 *
 * It must not redirect while `status` is "restoring". Doing so would bounce a
 * signed-in person to /login every time they opened a page directly, because
 * the refresh has not come back yet — and they would arrive at a form asking
 * them to sign in to an account they are already in. The same rule the navbar
 * follows: while the answer is unknown, say nothing.
 *
 * ===========================================================================
 * The rule that governs which pages may use this
 * ===========================================================================
 *
 * **Never `/vent`, and never anything on the path to it.** Rule 2.2 is that
 * venting requires no account. A future task that wraps the composer in this
 * component to "save drafts" or "track streaks" is not a feature request, it is
 * a violation, and `VentRemainsAnonymousIT` on the backend will not catch it
 * because the breach would be entirely in the browser.
 */
export function RequireAuth({
  children,
}: {
  children: (user: UserSummary) => ReactNode;
}) {
  const { status, user } = useSession();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (status === "anonymous") {
      // `replace`, so the back button does not land on the page they were
      // just turned away from and bounce them forward again.
      router.replace(signInHref(pathname));
    }
  }, [status, router, pathname]);

  if (status !== "authenticated" || user === null) {
    // Nothing, in both the restoring and the about-to-redirect cases. A
    // "you must sign in" message would be shown for one frame to someone who
    // is signed in, which is the mistake this whole phase is written against.
    return null;
  }

  return <>{children(user)}</>;
}
