"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useRef } from "react";
import { useSession } from "@/lib/auth/SessionProvider";
import { takeReturnTo } from "@/lib/auth/return-to";

/**
 * The far side of Google sign-in.
 *
 * ===========================================================================
 * What has already happened by the time this renders
 * ===========================================================================
 *
 * The browser has been to Google and back through the backend, and
 * `GoogleSignInHandler` has found or created the local account and set the
 * `hhf_refresh` cookie on its redirect. **No token is in this URL.** That is
 * deliberate on the backend's side: a token in a query parameter ends up in
 * browser history, in the referrer of the next request, and in every proxy log
 * on the way. So this page holds nothing but a cookie it cannot read, and its
 * entire job is to spend it — one `POST /api/v1/auth/refresh` — and move on.
 *
 * This is also the first time in the project's life that the refresh cookie is
 * sent back by a real browser. Phase 5 set it and nothing ever replayed it;
 * whether a `SameSite=Strict` cookie set on `localhost:8080` is returned on an
 * XHR from `localhost:3000` was reasoned from the spec and not observed. Port
 * does not affect same-site, so it should be. This page is where that stops
 * being an argument.
 *
 * ===========================================================================
 * Why the guard ref
 * ===========================================================================
 *
 * Refresh tokens rotate, and presenting a spent one is how the backend detects
 * theft: it revokes the whole family and ends every session from that sign-in.
 * React StrictMode runs effects twice in development on the same instance, so
 * without this ref the second run would replay the cookie the first run had
 * just spent and sign the person straight back out — a bug that would appear
 * only in development and look exactly like Google sign-in being broken.
 * `SessionProvider` deduplicates concurrent refreshes for the same reason; this
 * covers the sequential case that guard cannot see.
 */
export function CallbackHandler() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { adoptSessionFromCookie } = useSession();
  const startedRef = useRef(false);

  useEffect(() => {
    if (startedRef.current) return;
    startedRef.current = true;

    const failure = searchParams.get("error");
    if (failure !== null) {
      // The explanation belongs on a page that offers a way forward, not on
      // this one, whose only content is a line saying to wait.
      router.replace(`/login?error=${encodeURIComponent(failure)}`);
      return;
    }

    void adoptSessionFromCookie().then((signedIn) => {
      if (signedIn) {
        // Read and cleared in one go, so a destination is used at most once.
        router.replace(takeReturnTo() ?? "/");
      } else {
        router.replace("/login?error=oauth2");
      }
    });
  }, [adoptSessionFromCookie, router, searchParams]);

  return (
    <p className="mt-6 text-body-lg text-ink-soft" role="status" aria-live="polite">
      Finishing your sign-in. This takes a moment.
    </p>
  );
}
