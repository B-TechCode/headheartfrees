import type { Metadata } from "next";
import { Suspense } from "react";
import { CallbackHandler } from "@/components/auth/CallbackHandler";

export const metadata: Metadata = {
  title: "Signing you in",
  robots: { index: false, follow: false },
};

/*
 * /auth/callback — where Google sign-in lands.
 *
 * This URL is configuration, not navigation: it is the value of
 * `APP_OAUTH2_SUCCESS_REDIRECT` on the backend, it must match what is set
 * there, and nothing on this site links to it. Until this phase it 404d, which
 * is what the first real Google sign-in hit after everything else had worked.
 *
 * The page itself is deliberately almost empty. It exists for two or three
 * hundred milliseconds and then replaces itself, so anything more elaborate
 * would be a flash of design nobody asked to see. The one sentence is a live
 * region rather than decoration, because on a slow connection this is all a
 * screen reader has to go on.
 */
export default function AuthCallbackPage() {
  return (
    <div className="mx-auto max-w-2xl px-4 py-20 sm:px-6 lg:py-28">
      <span aria-hidden="true" className="block h-px w-12 bg-accent" />
      <h1 className="mt-6 font-display text-h2 text-ink">One moment.</h1>

      <Suspense fallback={null}>
        <CallbackHandler />
      </Suspense>
    </div>
  );
}
