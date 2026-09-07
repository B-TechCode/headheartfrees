import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("next/navigation", async () => (await import("@/test/navigation")).navigationModule());

import { Navbar } from "@/components/layout/Navbar";
import { SessionProvider } from "@/lib/auth/SessionProvider";
import { SESSION_HINT_COOKIE } from "@/lib/auth/session-hint";
import { aSession, jsonResponse, requestedUrls, stubFetch } from "@/test/http";
import { resetNavigation } from "@/test/navigation";

/**
 * The navbar's one hard rule: **never show "Sign in" to someone who is signed
 * in**, not even for a frame.
 *
 * It is worth being precise about why this is not cosmetic. On most sites a
 * navbar that flickers from signed-out to signed-in is a rendering wart. Here
 * the signed-out state is a claim about somebody's account, made on a site
 * whose entire proposition is that it is careful with them — and "you have been
 * logged out" is a frightening thing to read on a page you came to because you
 * were already having a bad day. The neutral state says nothing, which is the
 * only honest thing to say before the answer arrives.
 */
describe("the navbar while the session is being restored", () => {
  // Assigned when the provider calls fetch, which happens inside `render`.
  // Initialised so TypeScript does not have to take that on trust.
  let resolveRefresh: (response: Response) => void = () => {};

  beforeEach(() => {
    resetNavigation();
    document.cookie = `${SESSION_HINT_COOKIE}=1; Path=/`;

    stubFetch((url) => {
      if (url.includes("/api/v1/auth/refresh")) {
        return new Promise<Response>((resolve) => {
          resolveRefresh = resolve;
        });
      }
      throw new Error(`Unexpected request to ${url}`);
    });
  });

  function renderNavbar() {
    return render(
      <SessionProvider>
        <Navbar />
      </SessionProvider>,
    );
  }

  it("offers no sign-in affordance while the answer is unknown", () => {
    renderNavbar();

    // Both the desktop corner and the mobile panel. `queryAllByText` rather
    // than a role query because the failure being guarded against is the text
    // appearing at all, in any element, anywhere in the header.
    expect(screen.queryAllByText(/sign in/i)).toHaveLength(0);
  });

  it("goes from neutral straight to the account menu, never through Sign in", async () => {
    renderNavbar();

    expect(screen.queryAllByText(/sign in/i)).toHaveLength(0);

    resolveRefresh(jsonResponse(200, aSession()));

    expect(await screen.findByRole("button", { name: /your account/i })).toBeInTheDocument();
    expect(screen.queryAllByText(/^sign in$/i)).toHaveLength(0);
  });

  it("names the person in the menu once it opens", async () => {
    renderNavbar();
    resolveRefresh(jsonResponse(200, aSession()));

    const trigger = await screen.findByRole("button", { name: /your account/i });
    expect(trigger).toHaveAttribute("aria-expanded", "false");

    await userEvent.setup().click(trigger);

    await waitFor(() =>
      expect(screen.getByRole("menuitem", { name: "Your account" })).toBeInTheDocument(),
    );
    // `getAllByText`: the mobile panel renders the address too. It is behind
    // the `hidden` attribute, so role queries skip it, but text queries do not.
    expect(screen.getAllByText("person@example.com").length).toBeGreaterThan(0);
    expect(screen.getByRole("menuitem", { name: "Sign out" })).toBeInTheDocument();
    // A USER must not be shown the moderation queue.
    expect(screen.queryByRole("menuitem", { name: /moderation/i })).not.toBeInTheDocument();
  });
});

describe("the navbar for a browser that has never signed in", () => {
  beforeEach(() => {
    resetNavigation();
  });

  it("shows Sign in and asks the server nothing", async () => {
    const fetchMock = stubFetch((url) => {
      throw new Error(`Unexpected request to ${url}`);
    });

    render(
      <SessionProvider>
        <Navbar />
      </SessionProvider>,
    );

    // Desktop corner and mobile panel both carry one.
    await waitFor(() => expect(screen.getAllByRole("link", { name: "Sign in" })).not.toHaveLength(0));
    expect(requestedUrls(fetchMock)).toHaveLength(0);
  });

  it("carries the current page along so sign-in returns there", async () => {
    stubFetch((url) => {
      throw new Error(`Unexpected request to ${url}`);
    });

    const { location } = await import("@/test/navigation");
    location.pathname = "/about";

    render(
      <SessionProvider>
        <Navbar />
      </SessionProvider>,
    );

    const links = await screen.findAllByRole("link", { name: "Sign in" });
    for (const link of links) {
      expect(link).toHaveAttribute("href", "/login?next=%2Fabout");
    }
  });
});
