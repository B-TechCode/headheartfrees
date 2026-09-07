import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("next/navigation", async () => (await import("@/test/navigation")).navigationModule());

import { AccountMenu } from "@/components/auth/AccountMenu";
import { SessionProvider } from "@/lib/auth/SessionProvider";
import { aUser, jsonResponse, noContent, stubFetch } from "@/test/http";
import { resetNavigation } from "@/test/navigation";
import type { UserSummary } from "@/lib/auth/types";

/**
 * The logout request must outlive the component that started it.
 *
 * ===========================================================================
 * What this pins, and what it does not
 * ===========================================================================
 *
 * Signing out unmounts its own trigger. `endSession` flips the session to
 * anonymous, `Navbar` stops rendering `AccountMenu`, and the component whose
 * click handler is still on the stack is gone. If that unmount could cut the
 * request short, the client would end up looking signed out while the refresh
 * token stayed live on the server — someone on a shared computer walks away
 * believing they are out, and the cookie in that browser is still good for
 * thirty days.
 *
 * **This is a property pin, not a reproduction.** Nothing in this codebase
 * currently aborts anything: `apiFetch` accepts a `signal`, and after an
 * exhaustive search no caller anywhere passes one, so there is no
 * effect-scoped `AbortController` for an unmount to fire. These tests
 * therefore passed the day they were written, which is normally a reason to
 * distrust a test. They are here because the failure they describe is one
 * `useEffect` cleanup away at all times, and because it would be invisible:
 * the UI would look exactly like a successful sign-out.
 *
 * The assertion is deliberately about the mechanism rather than the outcome —
 * it checks that no aborted signal reached `fetch`. A test that only asserted
 * "logout was called" would keep passing after someone wired an
 * `AbortController` into the provider, because the call still happens; it is
 * the completion that would stop.
 */
describe("signing out", () => {
  beforeEach(() => {
    resetNavigation();
  });

  const user = aUser() as UserSummary;

  it("does not hand fetch an aborted signal, even after the trigger unmounts", async () => {
    const person = userEvent.setup();

    // The logout response is held open so the unmount happens while the
    // request is genuinely in flight, rather than after it has settled.
    // Definite-assignment: the executor runs synchronously, but TypeScript's
    // control flow does not model that and would narrow this to `null`.
    let releaseLogout!: () => void;
    const logoutInFlight = new Promise<void>((resolve) => {
      releaseLogout = resolve;
    });

    const fetchMock = stubFetch(async (url) => {
      if (url.includes("/auth/logout")) {
        await logoutInFlight;
        return noContent();
      }
      // SessionProvider refreshes on mount; nothing here is about that.
      return jsonResponse(401, { code: "UNAUTHORIZED", message: "no session" });
    });

    const view = render(
      <SessionProvider>
        <AccountMenu user={user} />
      </SessionProvider>,
    );

    await person.click(screen.getByRole("button", { name: /your account/i }));
    await person.click(screen.getByRole("menuitem", { name: /sign out/i }));

    await waitFor(() => {
      expect(
        fetchMock.mock.calls.some(([url]) => String(url).includes("/auth/logout")),
      ).toBe(true);
    });

    // The real event: the component that owns the handler goes away while the
    // request is still open.
    view.unmount();

    const logoutCall = fetchMock.mock.calls.find(([url]) =>
      String(url).includes("/auth/logout"),
    );
    const init = (logoutCall?.[1] ?? {}) as RequestInit;

    expect(
      init.signal === undefined || init.signal === null,
      "logout was given an AbortSignal. If that signal is tied to a component "
        + "lifetime, unmounting mid-flight cancels the revocation and the client "
        + "shows a signed-out UI over a live session.",
    ).toBe(true);

    // And if one is ever introduced, it must not be aborted by the unmount.
    releaseLogout();
    await logoutInFlight;
    expect(init.signal?.aborted ?? false).toBe(false);
  });

  it("a failed logout leaves the person signed in rather than pretending", async () => {
    const person = userEvent.setup();

    stubFetch(async (url) => {
      if (url.includes("/auth/logout")) {
        // A 500: the server may or may not have revoked anything, so the
        // client cannot claim the session is over.
        return jsonResponse(500, {
          status: 500,
          code: "INTERNAL_ERROR",
          message: "Something went wrong.",
          path: "/api/v1/auth/logout",
        });
      }
      return jsonResponse(401, { code: "UNAUTHORIZED", message: "no session" });
    });

    render(
      <SessionProvider>
        <AccountMenu user={user} />
      </SessionProvider>,
    );

    await person.click(screen.getByRole("button", { name: /your account/i }));
    await person.click(screen.getByRole("menuitem", { name: /sign out/i }));

    // The state is stated, not implied by an absence.
    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent(/still signed in/i);

    // And the control is usable again, so the retry the message implies exists.
    await waitFor(() => {
      expect(screen.getByRole("menuitem", { name: /sign out/i })).toBeEnabled();
    });
  });

  it("a 401 from logout is a completed sign-out, not a failure", async () => {
    const person = userEvent.setup();

    stubFetch(async (url) => {
      if (url.includes("/auth/logout")) {
        // The server has no session for this cookie. There is nothing left to
        // revoke, so ending it locally is correct and complete - showing an
        // error here would strand someone who is already signed out.
        return jsonResponse(401, {
          status: 401,
          code: "UNAUTHORIZED",
          message: "Invalid email or password.",
          path: "/api/v1/auth/logout",
        });
      }
      return jsonResponse(401, { code: "UNAUTHORIZED", message: "no session" });
    });

    render(
      <SessionProvider>
        <AccountMenu user={user} />
      </SessionProvider>,
    );

    await person.click(screen.getByRole("button", { name: /your account/i }));
    await person.click(screen.getByRole("menuitem", { name: /sign out/i }));

    await waitFor(() => {
      expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    });
  });
});
