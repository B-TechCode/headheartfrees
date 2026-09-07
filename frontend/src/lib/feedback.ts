import { apiFetch, type ApiRequestOptions } from "@/lib/api";

/**
 * The feedback API, from the client's side.
 *
 * What is deliberately absent from every type here: anything that could carry
 * vent text. A submission is a rating, a message the person wrote *in this
 * form*, and two optional labels. Rule 2.1 is about the vent textarea, whose
 * contents never reach this module or any other.
 */

/** One published note, as `/voices` receives it. */
export interface PublishedFeedback {
  id: string;
  rating: number;
  message: string;
  /** Null when the person chose not to be named. */
  displayName: string | null;
  location: string | null;
}

/**
 * One submission in the moderation queue.
 *
 * A separate type from {@link PublishedFeedback} because it is a separate
 * response from a separate endpoint. Modelling the public one as a partial of
 * this would invite a component to render an admin field on `/voices`.
 */
export interface QueuedFeedback extends PublishedFeedback {
  status: FeedbackStatus;
  userId: string | null;
  moderatedAt: string | null;
  moderatedBy: string | null;
  createdAt: string;
}

export type FeedbackStatus = "PENDING" | "APPROVED" | "REJECTED";

/** A moderator's decision. `PENDING` is not one: it is where things start. */
export type Decision = "APPROVED" | "REJECTED";

export interface Page<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
  hasNext: boolean;
}

export interface FeedbackDraft {
  rating: number;
  message: string;
  displayName?: string;
  location?: string;
}

/** How many notes a page of `/voices` asks for. */
export const VOICES_PAGE_SIZE = 12;

/**
 * Submits a note.
 *
 * `abortable: false` because the form unmounts as a direct result of this
 * succeeding. A submission that is cancelled partway would tell someone their
 * words were sent and leave them nowhere — and this form sits seconds after
 * the moment they let something go, which is the worst possible place to lose
 * something silently.
 */
export function submitFeedback(
  draft: FeedbackDraft,
  fetcher: Fetcher = apiFetch,
): Promise<PublishedFeedback> {
  return fetcher<PublishedFeedback>("/api/v1/feedback", {
    method: "POST",
    body: draft,
    abortable: false,
  });
}

/**
 * Either `apiFetch` or the session's `authFetch`.
 *
 * The caller picks: signed in, it passes `authFetch` so the request carries a
 * Bearer token and the row records `user_id`; anonymous, it passes nothing.
 * Both go through `apiFetch` underneath, so `abortable: false` above holds on
 * either path — which is the reason this is one function taking a fetcher
 * rather than two call sites that could drift.
 */
export type Fetcher = <T>(path: string, options?: ApiRequestOptions) => Promise<T>;

/** Approved notes only. Public; no token needed or sent. */
export function fetchPublishedFeedback(
  page: number,
  signal?: AbortSignal,
): Promise<Page<PublishedFeedback>> {
  // Abortable, unlike submission: this is a read, the component owns it, and
  // abandoning it when someone navigates away is the correct behaviour.
  return apiFetch<Page<PublishedFeedback>>(
    `/api/v1/feedback?page=${page}&size=${VOICES_PAGE_SIZE}`,
    { signal },
  );
}
