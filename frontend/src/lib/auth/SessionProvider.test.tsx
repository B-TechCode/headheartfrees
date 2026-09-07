import { act, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("next/navigation", async () => (await import("@/test/navigation")).navigationModule());

import { SessionProvider, useSession } from "@/lib/auth/SessionProvider";
import { SESSION_HINT_COOKIE } from "@/lib/auth/session-hint";
import {
  aSession,
  aUser,
  jsonResponse,
  requestedUrls,
  stubFetch,
  tooManyRequests,
  unauthorized,
} from "@/test/http";
import { resetNavigation } from "@/test/navigation";

/**
 * What a failed refresh is allowed to conclude.
 *
 * The distinction these tests exist to hold is narrow and easy to lose:
 *
 *   **401** is the server saying "this person is not signed in". Believe it.
 *   **429** is the server saying "not now". It says nothing about the session.
 *   **A network error** says nothing about anything.
 *
 * Collapsing the last two into the first is the natural simplification — one
 * `catch`, sign out, done — and it means a rate limiter having a busy minute,
 * or a phone going through a tunnel, silently ends somebody's session. On a
 * shared-IP deployment the busy minute is not hypothetical: `/refresh` draws
 * on the same 5/min bucket as login.
 *
 * The hint cookie is checked alongside the status in every case, because the
 * two together are what decides whether the *next* page load tries again.
 */

function Probe() {
  const { status, user } = useSession();
  return (
    <>
      <span data-testid="status">{status}</span>
      <span data-testid="who">{user?.email ?? "nobody"}</span>
    </>
  );
}

function renderSession() {
  return render(
    <SessionProvider>
      <Probe />
    </SessionProvider>,
  );
}

function status(): string {
  return screen.getByTestId("status").textContent ?? "";
}

function hintIsSet(): boolean {
  return document.cookie.includes(`${SESSION_HINT_COOKIE}=1`);
}

/** Runs the pending retry timer and lets the promises it releases settle. */
async function letTheRetryHappen(ms: number) {
  await act(async () => {
    await vi.advanceTimersByTimeAsync(ms);
  });
}

describe("restoring a session", () => {
  beforeEach(() => {
    resetNavigation();
    vi.useFakeTimers();
    document.cookie = `${SESSION_HINT_COOKIE}=1; Path=/`;
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("adopts the session the server hands back", async () => {
    stubFetch(() => jsonResponse(200, aSession()));

    renderSession();
    await act(async () => {});

    expect(status()).toBe("authenticated");
    expect(screen.getByTestId("who")).toHaveTextContent("person@example.com");
    expect(hintIsSet()).toBe(true);
  });

  it("signs out and forgets the hint on a 401", async () => {
    stubFetch(() => unauthorized());

    renderSession();
    await act(async () => {});

    expect(status()).toBe("anonymous");
    // The hint is cleared here and only here. The server has actually said the
    // session is gone, so a further page load has nothing to try.
    expect(hintIsSet()).toBe(false);
  });

  it("retries a 429 once, and keeps the session when the retry succeeds", async () => {
    let call = 0;
    const fetchMock = stubFetch(() => {
      call += 1;
      return call === 1 ? tooManyRequests(1) : jsonResponse(200, aSession());
    });

    renderSession();
    await act(async () => {});

    // Still nothing decided: a 429 must not settle the status either way.
    expect(status()).toBe("restoring");

    await letTheRetryHappen(1000);

    expect(status()).toBe("authenticated");
    expect(requestedUrls(fetchMock)).toHaveLength(2);
  });

  it("waits as long as Retry-After asks, and no less", async () => {
    let call = 0;
    const fetchMock = stubFetch(() => {
      call += 1;
      return call === 1 ? tooManyRequests(30) : jsonResponse(200, aSession());
    });

    renderSession();
    await act(async () => {});

    await letTheRetryHappen(29_000);
    // The header said thirty seconds. Retrying at twenty-nine spends a token
    // the bucket has not refilled and gets another 429 for the trouble.
    expect(requestedUrls(fetchMock)).toHaveLength(1);

    await letTheRetryHappen(1_000);
    expect(requestedUrls(fetchMock)).toHaveLength(2);
    expect(status()).toBe("authenticated");
  });

  it("keeps the hint when it never got an answer, so the next load tries again", async () => {
    stubFetch(() => tooManyRequests(1));

    renderSession();
    await act(async () => {});
    await letTheRetryHappen(1000);

    // Anonymous, because there is no session to show and the navbar needs to
    // offer a way in - but the hint stays, because nothing ever established
    // that the session was over.
    expect(status()).toBe("anonymous");
    expect(hintIsSet()).toBe(true);
  });

  it("treats a dropped connection like a 429, not like a 401", async () => {
    stubFetch(() => {
      throw new TypeError("Failed to fetch");
    });

    renderSession();
    await act(async () => {});
    expect(status()).toBe("restoring");

    // No Retry-After to read, so the two-second fallback applies.
    await letTheRetryHappen(2000);

    expect(status()).toBe("anonymous");
    expect(hintIsSet()).toBe(true);
  });

  it("gives up on a 401 even when the first failure was a 429", async () => {
    let call = 0;
    stubFetch(() => {
      call += 1;
      return call === 1 ? tooManyRequests(1) : unauthorized();
    });

    renderSession();
    await act(async () => {});
    await letTheRetryHappen(1000);

    expect(status()).toBe("anonymous");
    expect(hintIsSet()).toBe(false);
  });
});

describe("a browser with no hint", () => {
  beforeEach(() => {
    resetNavigation();
  });

  it("settles to anonymous without asking the server anything", async () => {
    const fetchMock = stubFetch(() => {
      throw new Error("the provider must not call the API here");
    });

    renderSession();
    await act(async () => {});

    expect(status()).toBe("anonymous");
    expect(requestedUrls(fetchMock)).toHaveLength(0);
  });
});

describe("calling the API as the signed-in user", () => {
  beforeEach(() => {
    resetNavigation();
    document.cookie = `${SESSION_HINT_COOKIE}=1; Path=/`;
  });

  function Caller() {
    const { status: sessionStatus, authFetch } = useSession();
    return (
      <>
        <span data-testid="status">{sessionStatus}</span>
        <button
          type="button"
          onClick={() => {
            void authFetch<unknown>("/api/v1/auth/me").catch(() => {});
          }}
        >
          load me
        </button>
      </>
    );
  }

  it("sends the access token, and refreshes once when it has expired", async () => {
    const calls: Array<{ url: string; auth: string | null }> = [];
    let meCall = 0;
    let refreshCall = 0;

    const fetchMock = stubFetch((url, init) => {
      const headers = (init.headers ?? {}) as Record<string, string>;
      calls.push({ url, auth: headers.Authorization ?? null });

      if (url.includes("/auth/refresh")) {
        refreshCall += 1;
        // Rotation: every refresh mints a different access token, so the two
        // /me calls below can be told apart by which one they carried.
        return jsonResponse(200, aSession({ accessToken: `token.${refreshCall}` }));
      }
      meCall += 1;
      // The ordinary case this exists for: the token expired between the page
      // rendering and the person clicking.
      return meCall === 1 ? unauthorized("/api/v1/auth/me") : jsonResponse(200, aUser());
    });

    render(
      <SessionProvider>
        <Caller />
      </SessionProvider>,
    );
    await act(async () => {});
    expect(status()).toBe("authenticated");

    await act(async () => {
      screen.getByRole("button", { name: "load me" }).click();
    });

    const meCalls = calls.filter((call) => call.url.includes("/auth/me"));
    expect(meCalls).toHaveLength(2);
    expect(meCalls[0]?.auth).toBe("Bearer token.1");
    // The retry carries the *new* token, not the one that just failed.
    expect(meCalls[1]?.auth).toBe("Bearer token.2");

    // One refresh on mount, one for the 401. Not two for the 401.
    expect(requestedUrls(fetchMock).filter((url) => url.includes("/auth/refresh"))).toHaveLength(2);
  });
});
