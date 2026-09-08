import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("next/navigation", async () => (await import("@/test/navigation")).navigationModule());

import { ModerationQueue } from "@/components/sections/ModerationQueue";
import { SessionProvider } from "@/lib/auth/SessionProvider";
import { jsonResponse, stubFetch } from "@/test/http";
import { resetNavigation } from "@/test/navigation";

/**
 * The two things a moderator needs the queue to tell them.
 *
 * ===========================================================================
 * Why these are worth a test
 * ===========================================================================
 *
 * Both were shipped broken and both were found by a person using the screen,
 * not by the suite — which had asserted that the PATCH went out and that the
 * status came back, and was satisfied. Neither of those is what a moderator
 * looks at.
 *
 * **1. A decision has to be visible.** Approve and Reject previously stayed
 * side by side after a decision, identical apart from a word, so the only
 * change was a small badge. It read as a no-op: the same item was rejected
 * four times before anyone realised the first click had worked.
 *
 * **2. The audit trail has to be on screen.** `moderated_at` and
 * `moderated_by` were stored from the day the table existed and rendered
 * nowhere, which is the same as not having them — the columns exist so a
 * moderator can answer "did I publish this, and when" when somebody asks for
 * their words to be taken down.
 *
 * These assert what the person sees. A test that only checked the request
 * would have passed against both bugs.
 */
describe("the moderation queue", () => {
  const MODERATOR_ID = "9f1d1f2a-0000-4000-8000-000000000001";
  const OTHER_MODERATOR = "0f8fad5b-d9cb-469f-a165-70867728950e";

  beforeEach(() => {
    resetNavigation();
    // Without the hint cookie SessionProvider makes no network call at all and
    // settles to anonymous - so `user` stays null and the "by you" branch can
    // never be reached. See session-hint.ts for why a page load is otherwise
    // silent.
    document.cookie = "hhf_session_hint=1; Path=/";
  });

  function anItem(overrides: Record<string, unknown> = {}) {
    return {
      id: "11111111-1111-4111-8111-111111111111",
      rating: 4,
      message: "A perfectly ordinary note about the site.",
      displayName: "Ada",
      location: "Mumbai",
      status: "PENDING",
      userId: null,
      moderatedAt: null,
      moderatedBy: null,
      createdAt: "2026-09-07T10:00:00Z",
      ...overrides,
    };
  }

  /**
   * Renders the queue with a session that reports the given user.
   *
   * `SessionProvider` refreshes on mount; the stub answers that call with the
   * signed-in moderator so `user.id` is populated and the "by you" branch can
   * be exercised.
   */
  function renderQueue(
    items: Record<string, unknown>[],
    onPatch?: (body: unknown) => Record<string, unknown>,
  ) {
    const fetchMock = stubFetch(async (url, init) => {
      if (url.includes("/auth/refresh")) {
        return jsonResponse(200, {
          accessToken: "header.payload.signature",
          tokenType: "Bearer",
          expiresIn: 900,
          user: {
            id: MODERATOR_ID,
            email: "mod@example.com",
            displayName: "Mod",
            role: "ADMIN",
            emailVerified: false,
            createdAt: "2026-01-05T09:30:00Z",
          },
        });
      }
      if (init.method === "PATCH") {
        return jsonResponse(200, onPatch?.(JSON.parse(String(init.body))) ?? {});
      }
      if (url.includes("/admin/feedback")) {
        return jsonResponse(200, {
          items,
          page: 0,
          size: 100,
          totalItems: items.length,
          totalPages: 1,
          hasNext: false,
        });
      }
      return jsonResponse(404, {});
    });

    render(
      <SessionProvider>
        <ModerationQueue />
      </SessionProvider>,
    );
    return fetchMock;
  }

  /** As `renderQueue`, but hands back the render result for DOM queries. */
  function renderQueueReturningView(items: Record<string, unknown>[]) {
    stubFetch(async (url) => {
      if (url.includes("/auth/refresh")) {
        return jsonResponse(200, {
          accessToken: "header.payload.signature",
          tokenType: "Bearer",
          expiresIn: 900,
          user: {
            id: MODERATOR_ID,
            email: "mod@example.com",
            displayName: "Mod",
            role: "ADMIN",
            emailVerified: false,
            createdAt: "2026-01-05T09:30:00Z",
          },
        });
      }
      return jsonResponse(200, {
        items,
        page: 0,
        size: 100,
        totalItems: items.length,
        totalPages: 1,
        hasNext: false,
      });
    });

    return render(
      <SessionProvider>
        <ModerationQueue />
      </SessionProvider>,
    );
  }

  it("offers both actions while an item is waiting", async () => {
    renderQueue([anItem()]);

    await screen.findByRole("button", { name: /approve/i });
    expect(screen.getByRole("button", { name: /^reject$/i })).toBeInTheDocument();
  });

  it("a decided item leaves the waiting list", async () => {
    const user = userEvent.setup();
    renderQueue([anItem()], () =>
      anItem({
        status: "REJECTED",
        moderatedAt: "2026-09-07T11:30:00Z",
        moderatedBy: MODERATOR_ID,
      }),
    );

    await user.click(await screen.findByRole("button", { name: /^reject$/i }));

    // On the Waiting tab the row simply goes. That is clear on its own - which
    // is why the reported bug was on the OTHER tab, where a decided item stays
    // on screen and used to keep offering the decision it already had.
    await waitFor(() => {
      expect(screen.queryByRole("button", { name: /^reject$/i })).not.toBeInTheDocument();
    });
    expect(screen.getByRole("tab", { name: /waiting \(0\)/i })).toBeInTheDocument();
  });

  it("a rejected item does not offer Reject again, but can still be published", async () => {
    renderQueue([
      anItem({
        status: "REJECTED",
        moderatedAt: "2026-09-07T11:30:00Z",
        moderatedBy: MODERATOR_ID,
      }),
    ]);

    await switchToDecided();

    // The exact reported bug: clicking Reject on an already-rejected item did
    // nothing visible, so it was clicked four times.
    expect(screen.queryByRole("button", { name: /^reject$/i })).not.toBeInTheDocument();
    // Reversal stays available - the removal promise runs both ways.
    expect(screen.getByRole("button", { name: /publish it after all/i })).toBeInTheDocument();
  });

  it("an approved item does not offer Approve again, but can be taken down", async () => {
    renderQueue([
      anItem({
        status: "APPROVED",
        moderatedAt: "2026-09-07T11:30:00Z",
        moderatedBy: MODERATOR_ID,
      }),
    ]);

    await switchToDecided();

    expect(screen.queryByRole("button", { name: /^approve$/i })).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: /take it down/i })).toBeInTheDocument();
  });

  it("announces the outcome for people who cannot see the buttons change", async () => {
    const user = userEvent.setup();
    renderQueue([anItem()], () =>
      anItem({ status: "APPROVED", moderatedAt: "2026-09-07T11:30:00Z", moderatedBy: MODERATOR_ID }),
    );

    await user.click(await screen.findByRole("button", { name: /^approve$/i }));

    await waitFor(() => {
      expect(screen.getByRole("status")).toHaveTextContent(/published/i);
    });
  });

  it("shows when a decision was made, and that it was you", async () => {
    const { container } = renderQueueReturningView([
      anItem({
        status: "APPROVED",
        moderatedAt: "2026-09-07T11:30:00Z",
        moderatedBy: MODERATOR_ID,
      }),
    ]);

    await switchToDecided();

    const time = container.querySelector("time");
    expect(time, "no <time> element - the audit trail is invisible again").not.toBeNull();
    // The machine-readable value, so the record is unambiguous whatever the
    // rendered locale format happens to be on the machine running this.
    expect(time).toHaveAttribute("datetime", "2026-09-07T11:30:00Z");
    expect(time?.closest("p")).toHaveTextContent(/by you/i);
  });

  it("shows the moderator's id when it was somebody else", async () => {
    renderQueue([
      anItem({
        status: "REJECTED",
        moderatedAt: "2026-09-07T11:30:00Z",
        moderatedBy: OTHER_MODERATOR,
      }),
    ]);

    await switchToDecided();

    // Not "by you", and not blank. The API returns a bare UUID because the
    // feedback module may not join to users, so the id is the honest answer.
    expect(await screen.findByText(/0f8fad5b/)).toBeInTheDocument();
  });

  it("shows no decision line on an item still waiting", async () => {
    renderQueue([anItem()]);

    await screen.findByRole("button", { name: /^approve$/i });
    expect(screen.queryByText(/by you/i)).not.toBeInTheDocument();
    expect(screen.queryByRole("time")).not.toBeInTheDocument();
  });

  /** Decided items live behind the second tab. */
  async function switchToDecided() {
    const user = userEvent.setup();
    await user.click(await screen.findByRole("tab", { name: /already decided/i }));
  }
});
