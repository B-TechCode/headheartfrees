import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("next/navigation", async () => (await import("@/test/navigation")).navigationModule());

import { AccountMenu } from "@/components/auth/AccountMenu";
import { SessionProvider } from "@/lib/auth/SessionProvider";
import { aUser, noContent, stubFetch } from "@/test/http";
import { resetNavigation, routerPush } from "@/test/navigation";
import type { UserSummary } from "@/lib/auth/types";

/**
 * The keyboard behaviour Radix would have supplied, tested because it is
 * hand-rolled.
 *
 * `role="menu"` is a promise to a screen reader that arrow keys will move
 * between items and Escape will close. A menu that announces itself that way
 * and then does not behave that way is worse than an unadorned list of links,
 * because the user is told to expect something that is not there.
 */
describe("the account menu", () => {
  beforeEach(() => {
    resetNavigation();
    stubFetch(() => noContent());
  });

  function renderMenu(user: UserSummary = aUser() as UserSummary) {
    return render(
      <SessionProvider>
        <AccountMenu user={user} />
      </SessionProvider>,
    );
  }

  it("opens on the trigger and puts focus on the first item", async () => {
    const user = userEvent.setup();
    renderMenu();

    const trigger = screen.getByRole("button", { name: /your account/i });
    await user.click(trigger);

    expect(trigger).toHaveAttribute("aria-expanded", "true");
    await waitFor(() =>
      expect(screen.getByRole("menuitem", { name: "Your account" })).toHaveFocus(),
    );
  });

  it("moves between items with the arrow keys, and wraps", async () => {
    const user = userEvent.setup();
    renderMenu();

    await user.click(screen.getByRole("button", { name: /your account/i }));
    const account = screen.getByRole("menuitem", { name: "Your account" });
    const signOut = screen.getByRole("menuitem", { name: "Sign out" });

    await waitFor(() => expect(account).toHaveFocus());

    await user.keyboard("{ArrowDown}");
    expect(signOut).toHaveFocus();

    // Wraps rather than stopping: two items make a dead end obvious.
    await user.keyboard("{ArrowDown}");
    expect(account).toHaveFocus();

    await user.keyboard("{ArrowUp}");
    expect(signOut).toHaveFocus();
  });

  it("closes on Escape and gives focus back to the trigger", async () => {
    const user = userEvent.setup();
    renderMenu();

    const trigger = screen.getByRole("button", { name: /your account/i });
    await user.click(trigger);
    await waitFor(() => expect(trigger).toHaveAttribute("aria-expanded", "true"));

    await user.keyboard("{Escape}");

    expect(trigger).toHaveAttribute("aria-expanded", "false");
    // Returning focus is the half that gets forgotten. Without it, Escape drops
    // the keyboard user at the top of the document.
    expect(trigger).toHaveFocus();
  });

  it("shows the moderation queue to an admin and to nobody else", async () => {
    const user = userEvent.setup();
    renderMenu(aUser({ role: "ADMIN" }) as UserSummary);

    await user.click(screen.getByRole("button", { name: /your account/i }));

    const queue = await screen.findByRole("menuitem", { name: "Moderation queue" });
    expect(queue).toHaveAttribute("href", "/admin/feedback");

    // Three items now, and the arrow keys have to know that. This is the case
    // the index-based version of the focus code got wrong.
    await user.keyboard("{ArrowDown}");
    expect(queue).toHaveFocus();
    await user.keyboard("{ArrowDown}");
    expect(screen.getByRole("menuitem", { name: "Sign out" })).toHaveFocus();
  });

  it("signs out and leaves for the home page", async () => {
    const user = userEvent.setup();
    renderMenu();

    await user.click(screen.getByRole("button", { name: /your account/i }));
    await user.click(await screen.findByRole("menuitem", { name: "Sign out" }));

    // Home, not wherever they were: the current page may be one that requires
    // an account, and RequireAuth would bounce them to /login carrying a return
    // destination they just chose to leave.
    await waitFor(() => expect(routerPush).toHaveBeenCalledWith("/"));
  });
});
