"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import type { ReactNode } from "react";
import { ApiError, apiFetch, type ApiRequestOptions } from "@/lib/api";
import { clearSessionHint, hasSessionHint, setSessionHint } from "./session-hint";
import type { AccessTokenResponse, SessionStatus, UserSummary } from "./types";

/**
 * The session, for the whole app.
 *
 * ===========================================================================
 * 1. This provider must never gate what it wraps
 * ===========================================================================
 *
 * It renders `children` immediately and unconditionally. There is no branch in
 * which it returns a spinner, a skeleton, or null while it works out who is
 * signed in.
 *
 * That is not a performance preference. `/vent` is the product and rule 2.2
 * says it requires no account; a provider that withheld its children for
 * 300ms while a refresh was in flight would stall the one page that must
 * always work, and would do it invisibly, on a slow connection, for the person
 * least able to wait. `VentComposer.session.test.tsx` types into the composer
 * and releases while a refresh is deliberately left unresolved, and fails if
 * either is blocked.
 *
 * Only components that show session-dependent UI read `status`, and only the
 * navbar changes shape because of it.
 *
 * ===========================================================================
 * 2. Where the tokens live
 * ===========================================================================
 *
 * The access token is held in a ref — memory only, for the life of the tab. It
 * is **not** in localStorage, sessionStorage, or a cookie: a token script can
 * read is a token an XSS can steal, and the fifteen-minute lifetime is the
 * mitigation only if the theft has to happen while the tab is open.
 *
 * The refresh token is never touched here at all. It is the backend's
 * `hhf_refresh` cookie, `HttpOnly` and `SameSite=Strict`, and this file could
 * not read it if it tried. Every call that needs it simply sets
 * `credentials: "include"` and lets the browser attach it.
 *
 * ===========================================================================
 * 3. The hint, and why a page load is usually silent
 * ===========================================================================
 *
 * On mount this asks `session-hint.ts` whether this browser has ever signed
 * in. If not, there is no network call — the status settles to `anonymous` and
 * the auth rate-limit bucket is untouched. See that file for why that matters
 * more than it looks: `/refresh` shares a 5/min per-IP bucket with login, and
 * behind a proxy that is not yet forwarding the client address, every visitor
 * is one IP.
 *
 * ===========================================================================
 * 4. What each failure means
 * ===========================================================================
 *
 * | Response          | Meaning                          | What happens          |
 * |-------------------|----------------------------------|-----------------------|
 * | 401               | the session is genuinely gone    | sign out, clear hint  |
 * | 429               | we were not allowed to ask       | keep state, retry once|
 * | network error     | we could not ask                 | keep state, retry once|
 *
 * The distinction is the whole point. Only a 401 is the server saying "this
 * person is not signed in". A 429 and a dropped connection say nothing about
 * the session, and treating them as a sign-out would throw someone out of
 * their account because a rate limiter was busy — which is both wrong and,
 * on a bad network, wrong repeatedly.
 *
 * The retry delay comes from `Retry-After` when the server sent one. That
 * header is readable here only because `SecurityConfig` names it in
 * `Access-Control-Expose-Headers`; without that, `ApiError.retryAfterSeconds`
 * is null in a browser no matter what is on the wire.
 */

interface SessionContextValue {
  status: SessionStatus;
  user: UserSummary | null;
  /** @throws {ApiError} on bad credentials, a weak-password rejection, or 429. */
  signIn(email: string, password: string): Promise<void>;
  /**
   * Ends the session on the server, then locally.
   *
   * @throws on any failure that leaves the server-side token possibly live -
   *         offline, 5xx, 429. **The local session is NOT cleared in that
   *         case**, so a caller must surface the failure and offer a retry
   *         rather than navigating away. Resolving quietly here would show a
   *         signed-out interface over a live session, which on a shared
   *         computer is the worst outcome this flow has. A 401 is not a
   *         failure: the server already has no session, so it resolves.
   */
  signOut(): Promise<void>;
  /**
   * Calls the API as the signed-in user.
   *
   * On a 401 it refreshes once and retries once, because an access token that
   * expired between render and click is the ordinary case rather than an
   * error. If the refresh also 401s, the session really is over and this
   * signs out.
   */
  authFetch<T>(path: string, options?: ApiRequestOptions): Promise<T>;
  /**
   * Exchanges the refresh cookie for a session, regardless of the hint.
   *
   * Only `/auth/callback` needs this: it has just come back from Google
   * holding a cookie that was set on a different origin's response, so there
   * is nothing for the hint to have recorded yet.
   */
  adoptSessionFromCookie(): Promise<boolean>;
}

const SessionContext = createContext<SessionContextValue | null>(null);

/** Refresh this long before the access token expires. */
const RENEWAL_LEAD_SECONDS = 60;

/** When a renewal fails for a reason that is not a 401, try again this much later. */
const RENEWAL_BACKOFF_SECONDS = 120;

/** Fallback wait before the single retry, when the server named no `Retry-After`. */
const DEFAULT_RETRY_SECONDS = 2;

/** However long a `Retry-After` asks for, do not sit on a page load longer than this. */
const MAX_RETRY_SECONDS = 60;

function isUnauthorised(error: unknown): boolean {
  return error instanceof ApiError && error.status === 401;
}

function retryDelayMs(error: unknown): number {
  const asked =
    error instanceof ApiError && error.retryAfterSeconds !== null
      ? error.retryAfterSeconds
      : DEFAULT_RETRY_SECONDS;
  return Math.min(Math.max(asked, 1), MAX_RETRY_SECONDS) * 1000;
}

export function SessionProvider({ children }: { children: ReactNode }) {
  /*
   * `restoring` on both the server render and the first client render.
   *
   * The honest initial value on the client is "anonymous unless the hint says
   * otherwise", but `document.cookie` does not exist during SSR, so seeding
   * from it here would make the two renders disagree and React would discard
   * the server HTML. Instead both start neutral and the effect below settles
   * it on the first commit. A visitor who has never signed in therefore sees
   * the neutral navbar for a single frame rather than "Sign in" — which is the
   * right way round, since the opposite mistake shows "Sign in" to someone who
   * is signed in, and that reads as having been logged out.
   */
  const [status, setStatus] = useState<SessionStatus>("restoring");
  const [user, setUser] = useState<UserSummary | null>(null);

  const accessTokenRef = useRef<string | null>(null);
  const expiresAtRef = useRef<number | null>(null);
  const renewalTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const mountedRef = useRef(true);

  const cancelRenewal = useCallback(() => {
    if (renewalTimerRef.current !== null) {
      clearTimeout(renewalTimerRef.current);
      renewalTimerRef.current = null;
    }
  }, []);

  /*
   * Declared as a ref because `scheduleRenewal` and `attemptRefresh` call each
   * other. A plain `const` pair cannot express that without one of them being
   * used before it is defined.
   */
  const attemptRefreshRef = useRef<(mode: "restore" | "renew") => Promise<boolean>>(
    async () => false,
  );

  const scheduleRenewal = useCallback(
    (inSeconds: number) => {
      cancelRenewal();
      // Never zero: a timer that fires immediately on a clock skew turns into a
      // refresh loop against a rate-limited endpoint.
      const delay = Math.max(5, inSeconds) * 1000;
      renewalTimerRef.current = setTimeout(() => {
        void attemptRefreshRef.current("renew");
      }, delay);
    },
    [cancelRenewal],
  );

  const adopt = useCallback(
    (response: AccessTokenResponse) => {
      if (!mountedRef.current) return;
      accessTokenRef.current = response.accessToken;
      expiresAtRef.current = Date.now() + response.expiresIn * 1000;
      setUser(response.user);
      setStatus("authenticated");
      setSessionHint();
      scheduleRenewal(response.expiresIn - RENEWAL_LEAD_SECONDS);
    },
    [scheduleRenewal],
  );

  const endSession = useCallback(
    ({ keepHint }: { keepHint: boolean }) => {
      cancelRenewal();
      accessTokenRef.current = null;
      expiresAtRef.current = null;
      if (!keepHint) {
        clearSessionHint();
      }
      if (!mountedRef.current) return;
      setUser(null);
      setStatus("anonymous");
    },
    [cancelRenewal],
  );

  const exchangeCookie = useCallback(
    () => apiFetch<AccessTokenResponse>("/api/v1/auth/refresh", { method: "POST" }),
    [],
  );

  /**
   * The refresh currently in flight, if any.
   *
   * Concurrency here is not theoretical and not benign. Rotation means a
   * refresh token is spent by the call that uses it, and presenting a spent
   * token is how the backend detects theft: `RefreshTokenService` revokes the
   * entire family and every session from that sign-in ends. Two overlapping
   * refreshes - the scheduled renewal firing as a 401 retry starts, or a tab
   * becoming visible mid-restore - both send the same cookie, and the second
   * one looks exactly like an attacker replaying a stolen token.
   *
   * So callers share a single attempt rather than starting their own. The
   * first caller's `mode` decides what happens on failure, which is right:
   * whoever started it is the one with something to lose.
   */
  const inFlightRef = useRef<Promise<boolean> | null>(null);

  const attemptRefresh = useCallback(
    (mode: "restore" | "renew"): Promise<boolean> => {
      const existing = inFlightRef.current;
      if (existing !== null) {
        return existing;
      }
      const run = runRefresh(mode).finally(() => {
        inFlightRef.current = null;
      });
      inFlightRef.current = run;
      return run;
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [adopt, endSession, exchangeCookie, scheduleRenewal],
  );

  /**
   * One attempt, then one retry, with the 401 / not-401 split that decides
   * whether a failure means "signed out" or only "we could not tell".
   *
   * A plain function rather than another `useCallback`: it is only ever
   * reached through `attemptRefresh`, which owns the deduplication above and
   * carries the dependency list for both.
   */
  async function runRefresh(mode: "restore" | "renew"): Promise<boolean> {
    try {
      adopt(await exchangeCookie());
      return true;
    } catch (first) {
      if (isUnauthorised(first)) {
        endSession({ keepHint: false });
        return false;
      }

      // 429 or a network failure. Neither says anything about whether the
      // session exists, so nothing is torn down here: we wait and ask again.
      await new Promise((resolve) => setTimeout(resolve, retryDelayMs(first)));
      if (!mountedRef.current) return false;

      try {
        adopt(await exchangeCookie());
        return true;
      } catch (second) {
        if (isUnauthorised(second)) {
          endSession({ keepHint: false });
          return false;
        }

        if (mode === "restore") {
          // Still no answer, and there is no session to preserve — nothing
          // was ever restored. Settle to anonymous so the navbar offers a way
          // in, but **keep the hint**: we never established that the session
          // is gone, and keeping it means the next page load tries again
          // instead of writing the person off.
          endSession({ keepHint: true });
        } else {
          // A live session. Keep it exactly as it is — the access token may
          // well still be valid, and if it is not, `authFetch` will find out
          // on the next call and refresh then. Try again later regardless.
          scheduleRenewal(RENEWAL_BACKOFF_SECONDS);
        }
        return false;
      }
    }
  }

  useEffect(() => {
    attemptRefreshRef.current = attemptRefresh;
  }, [attemptRefresh]);

  // ---- Restore on mount ------------------------------------------------
  useEffect(() => {
    mountedRef.current = true;

    if (!hasSessionHint()) {
      // The silent path, and the common one. No request is made at all.
      setStatus("anonymous");
    } else {
      void attemptRefresh("restore");
    }

    return () => {
      mountedRef.current = false;
      cancelRenewal();
    };
    // Deliberately once. `attemptRefresh` is stable across renders in practice
    // and re-running this on every identity change would restore repeatedly.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // ---- Catch up after the tab was in the background ---------------------
  useEffect(() => {
    if (status !== "authenticated") return;

    function onVisible() {
      if (document.visibilityState !== "visible") return;
      const expiresAt = expiresAtRef.current;
      // Background tabs have their timers throttled, so the scheduled renewal
      // may simply not have run. Without this, coming back to a tab left open
      // overnight means the first click fails, refreshes, and retries — which
      // works, but shows a delay for no reason.
      if (expiresAt !== null && expiresAt - Date.now() < RENEWAL_LEAD_SECONDS * 1000) {
        void attemptRefreshRef.current("renew");
      }
    }

    document.addEventListener("visibilitychange", onVisible);
    return () => document.removeEventListener("visibilitychange", onVisible);
  }, [status]);

  const signIn = useCallback(
    async (email: string, password: string) => {
      const response = await apiFetch<AccessTokenResponse>("/api/v1/auth/login", {
        method: "POST",
        body: { email, password },
      });
      adopt(response);
    },
    [adopt],
  );

  const signOut = useCallback(async () => {
    try {
      await apiFetch<void>("/api/v1/auth/logout", { method: "POST" });
    } catch (error) {
      // A 401 means the server has no session for this cookie - already
      // expired, already revoked, or never valid. There is nothing left on the
      // server to revoke, so ending it locally is the correct and complete
      // outcome, not a failure.
      if (!isUnauthorised(error)) {
        // Everything else - offline, 500, 429, a refused preflight - leaves
        // the refresh token possibly still live on the server. This used to
        // swallow the error and clear the session anyway, on the reasoning
        // that a sign-out must always end the local session. That reasoning is
        // wrong, and dangerously so: it renders a signed-out interface over a
        // session that still exists. Someone on a shared computer who is shown
        // "Sign in" walks away believing they are out, and the refresh cookie
        // in that browser still buys a full session for thirty days.
        //
        // Failing loudly is worse UX and better security, and the caller is
        // expected to say so and offer a retry. The local session is left
        // intact deliberately: it is the honest description of the server's
        // state, and it keeps the retry able to work.
        throw error;
      }
    }
    endSession({ keepHint: false });
  }, [endSession]);

  const adoptSessionFromCookie = useCallback(
    () => attemptRefresh("restore"),
    [attemptRefresh],
  );

  const authFetch = useCallback(
    async <T,>(path: string, options: ApiRequestOptions = {}): Promise<T> => {
      const call = () =>
        apiFetch<T>(path, { ...options, accessToken: accessTokenRef.current });

      try {
        return await call();
      } catch (error) {
        if (!isUnauthorised(error)) throw error;

        const renewed = await attemptRefresh("renew");
        if (!renewed) throw error;
        return await call();
      }
    },
    [attemptRefresh],
  );

  const value = useMemo<SessionContextValue>(
    () => ({ status, user, signIn, signOut, authFetch, adoptSessionFromCookie }),
    [status, user, signIn, signOut, authFetch, adoptSessionFromCookie],
  );

  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>;
}

export function useSession(): SessionContextValue {
  const value = useContext(SessionContext);
  if (value === null) {
    throw new Error("useSession must be used inside <SessionProvider>.");
  }
  return value;
}
