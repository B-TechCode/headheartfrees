import { beforeEach, describe, expect, it, vi } from "vitest";

import { fetchPublishedFeedback, submitFeedback } from "@/lib/feedback";
import { jsonResponse, stubFetch } from "@/test/http";

/**
 * Submission must not be cancellable; the public read must be.
 *
 * The asymmetry is the point, and it is the same judgement made for logout:
 * a request whose failure is *silent* and *unrecoverable* must not be tied to
 * a component lifetime, and a request whose failure is "the list did not
 * load" should be.
 *
 * As with the logout pin, nothing in this codebase currently passes an
 * effect-scoped signal into `submitFeedback`, so these tests pass on the day
 * they are written. They exist because the failure they describe is one
 * `useEffect` cleanup away and would be invisible: the form unmounts on
 * success, so a cancelled submission looks exactly like a delivered one.
 */
describe("feedback requests", () => {
  beforeEach(() => {
    vi.unstubAllGlobals();
  });

  const draft = { rating: 5, message: "A perfectly ordinary note." };

  it("never hands fetch a signal on submission", async () => {
    const fetchMock = stubFetch(async () => jsonResponse(201, { id: "1", rating: 5 }));

    await submitFeedback(draft);

    const init = (fetchMock.mock.calls[0]?.[1] ?? {}) as RequestInit;
    expect(
      init.signal === undefined || init.signal === null,
      "Submission was given an AbortSignal. If it is tied to a component "
        + "lifetime, the form unmounting on success cancels the request and the "
        + "note vanishes while the person is told it was sent.",
    ).toBe(true);
  });

  it("drops a signal that a caller passes to an unabortable request", async () => {
    const fetchMock = stubFetch(async () => jsonResponse(201, { id: "1", rating: 5 }));
    const controller = new AbortController();

    // Reaching past the helper to prove the api client itself enforces this,
    // rather than the helper merely declining to pass one along.
    const { apiFetch } = await import("@/lib/api");
    await apiFetch("/api/v1/feedback", {
      method: "POST",
      body: draft,
      abortable: false,
      signal: controller.signal,
    });

    const init = (fetchMock.mock.calls[0]?.[1] ?? {}) as RequestInit;
    expect(init.signal).toBeUndefined();

    // And aborting afterwards does nothing, because fetch never saw it.
    controller.abort();
    expect(init.signal).toBeUndefined();
  });

  it("passes the signal through on the public read, which should be cancellable", async () => {
    const fetchMock = stubFetch(async () => jsonResponse(200, { items: [], hasNext: false }));
    const controller = new AbortController();

    await fetchPublishedFeedback(0, controller.signal);

    const init = (fetchMock.mock.calls[0]?.[1] ?? {}) as RequestInit;
    expect(init.signal).toBe(controller.signal);
  });

  it("asks for a bounded page rather than everything", async () => {
    const fetchMock = stubFetch(async () => jsonResponse(200, { items: [], hasNext: false }));

    await fetchPublishedFeedback(2);

    // A public list with no size cap on the client and none on the server is
    // a full-table export one query parameter away. The server clamps too.
    expect(String(fetchMock.mock.calls[0]?.[0])).toContain("size=12");
    expect(String(fetchMock.mock.calls[0]?.[0])).toContain("page=2");
  });
});
