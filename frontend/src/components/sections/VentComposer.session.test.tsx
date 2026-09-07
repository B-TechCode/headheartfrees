import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("next/navigation", async () => (await import("@/test/navigation")).navigationModule());

import { SessionProvider } from "@/lib/auth/SessionProvider";
import { VentComposer } from "@/components/sections/VentComposer";
import { SESSION_HINT_COOKIE } from "@/lib/auth/session-hint";
import { noContent, requestedUrls, stubFetch } from "@/test/http";
import { resetNavigation, routerPush } from "@/test/navigation";

/**
 * ===========================================================================
 * The test this phase exists to pass
 * ===========================================================================
 *
 * The realistic failure is not that somebody deliberately puts a gate in front
 * of `/vent`. Rule 2.2 is written down in three places and would be caught in
 * review. The realistic failure is that a provider added for the navbar holds
 * the render for three hundred milliseconds while it asks the server who is
 * signed in — and the one page that must always work stalls, invisibly, for
 * exactly the person least able to wait for it.
 *
 * So the refresh here never resolves. Not "resolves slowly" and not "fails":
 * the promise is left permanently pending, which is what a dropped connection
 * on a train actually looks like. Everything below then has to work anyway.
 *
 * If a future change makes `SessionProvider` withhold its children while
 * `status === "restoring"`, every test in this file fails, and that is the
 * intended outcome rather than an inconvenience.
 */
describe("the vent composer during a session restore", () => {
  /** A refresh call that is made and never answered. */
  let refreshWasCalled = false;

  beforeEach(() => {
    resetNavigation();
    refreshWasCalled = false;

    // A signed-in browser. Without this the provider settles to anonymous
    // without a network call and there is no in-flight restore to test through.
    document.cookie = `${SESSION_HINT_COOKIE}=1; Path=/`;
  });

  function stubASlowRestore() {
    return stubFetch((url) => {
      if (url.includes("/api/v1/auth/refresh")) {
        refreshWasCalled = true;
        return new Promise<Response>(() => {
          // Never settles. This is the point of the test.
        });
      }
      if (url.includes("/api/v1/vent/release")) {
        return noContent();
      }
      throw new Error(`Unexpected request to ${url}`);
    });
  }

  function renderComposer() {
    return render(
      <SessionProvider>
        <VentComposer />
      </SessionProvider>,
    );
  }

  it("renders the writing box immediately, before the session is known", () => {
    stubASlowRestore();
    renderComposer();

    // Synchronous, with nothing awaited: the box has to be there on the first
    // paint, not after a microtask.
    expect(screen.getByLabelText("What is weighing on you?")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /release/i })).toBeInTheDocument();
  });

  it("accepts typing, a mood and a release while the refresh is still pending", async () => {
    const user = userEvent.setup();
    const fetchMock = stubASlowRestore();
    renderComposer();

    await user.click(screen.getByRole("button", { name: "Anxious" }));
    await user.type(
      screen.getByLabelText("What is weighing on you?"),
      "today was long and I have nowhere to put it",
    );

    const release = screen.getByRole("button", { name: /release/i });
    expect(release).toBeEnabled();
    await user.click(release);

    await waitFor(() => expect(routerPush).toHaveBeenCalledWith("/vent/released"));

    // The restore really was in flight throughout — otherwise this test would
    // be passing for the trivial reason that nothing was ever pending.
    expect(refreshWasCalled).toBe(true);
    expect(
      requestedUrls(fetchMock).some((url) => url.includes("/api/v1/vent/release")),
    ).toBe(true);
  });

  it("sends the mood and nothing else", async () => {
    const user = userEvent.setup();
    const fetchMock = stubASlowRestore();
    renderComposer();

    const secret = "the specific sentence nobody else may ever see";
    await user.click(screen.getByRole("button", { name: "Heavy" }));
    await user.type(screen.getByLabelText("What is weighing on you?"), secret);
    await user.click(screen.getByRole("button", { name: /release/i }));

    await waitFor(() => expect(routerPush).toHaveBeenCalled());

    const releaseCall = fetchMock.mock.calls.find((call) =>
      String(call[0]).includes("/api/v1/vent/release"),
    );
    expect(releaseCall).toBeDefined();
    expect(JSON.parse(String(releaseCall?.[1]?.body))).toEqual({ mood: "HEAVY" });

    // Rule 2.1, asserted across every request the page made rather than only
    // the one we expect to carry a body. A leak would most plausibly arrive as
    // an analytics call or an error report, not as an extra field on release.
    for (const [url, init] of fetchMock.mock.calls) {
      expect(String(url)).not.toContain("specific sentence");
      expect(String(init?.body ?? "")).not.toContain("specific sentence");
    }
  });

  it("still surfaces the crisis panel while the session is unknown", async () => {
    const user = userEvent.setup();
    stubASlowRestore();
    renderComposer();

    await user.type(
      screen.getByLabelText("What is weighing on you?"),
      "some days I just want to die",
    );

    // The safety path runs entirely in this browser precisely so it cannot
    // depend on the network. A restore that never completes must not be able
    // to take the helplines away.
    expect(await screen.findByText(/Tele-MANAS/i)).toBeInTheDocument();
  });
});

describe("the vent composer with no session at all", () => {
  beforeEach(() => {
    resetNavigation();
  });

  it("makes no auth request whatsoever", async () => {
    const user = userEvent.setup();
    const fetchMock = stubFetch((url) => {
      if (url.includes("/api/v1/vent/release")) return noContent();
      throw new Error(`Unexpected request to ${url}`);
    });

    render(
      <SessionProvider>
        <VentComposer />
      </SessionProvider>,
    );

    await user.type(screen.getByLabelText("What is weighing on you?"), "nothing much");
    await user.click(screen.getByRole("button", { name: /release/i }));
    await waitFor(() => expect(routerPush).toHaveBeenCalled());

    // The whole reason the session hint exists. An anonymous visitor must not
    // spend a token from the 5/min auth bucket that real sign-ins need.
    expect(requestedUrls(fetchMock).some((url) => url.includes("/auth/"))).toBe(false);
  });
});
