"use client";

import { useCallback, useEffect, useState } from "react";
import { Button } from "@/components/ui/Button";
import { Callout } from "@/components/ui/Callout";
import { cn } from "@/lib/cn";
import { focusRing } from "@/components/ui/styles";
import { describeAuthError } from "@/lib/auth/errors";
import { useSession } from "@/lib/auth/SessionProvider";
import type { Decision, FeedbackStatus, Page, QueuedFeedback } from "@/lib/feedback";

type Filter = "PENDING" | "DECIDED";

/**
 * The moderation queue.
 *
 * ===========================================================================
 * This screen is read by a person, and that shapes it
 * ===========================================================================
 *
 * It shows unfiltered writing from strangers on a mental-health site. Some of
 * it will be distressing, and whoever is reviewing may read a lot of it in one
 * sitting. Three consequences, all deliberate:
 *
 * 1. **A compact list, collapsed by default.** Twenty open items is twenty
 *    things read at once whether or not the reader was ready for any of them.
 *    Each row shows a rating and a one-line preview; the full note appears
 *    when it is opened.
 * 2. **Nothing animates, nothing auto-plays, nothing arrives on its own.** No
 *    polling, no toast, no "3 new items" appearing mid-read.
 * 3. **The helplines are on this page, worded for the moderator.** See
 *    `ModeratorSupport` below.
 *
 * ===========================================================================
 * What a moderator cannot do
 * ===========================================================================
 *
 * There is no reply, and there cannot be. Submissions carry no contact detail
 * of any kind — no email, no IP, nothing — and that is by design rather than
 * an omission to fix. So if a note reads as someone in trouble, the honest
 * position is that there is nobody to reach, and the page says so instead of
 * implying an action that does not exist.
 */
export function ModerationQueue() {
  const { authFetch, user } = useSession();

  const [items, setItems] = useState<QueuedFeedback[]>([]);
  const [filter, setFilter] = useState<Filter>("PENDING");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [announcement, setAnnouncement] = useState("");

  const load = useCallback(
    async (signal?: AbortSignal) => {
      setLoading(true);
      setError(null);
      try {
        // The endpoint returns every status; the split between "waiting" and
        // "decided" is made here so a moderator can find something they
        // already acted on in order to reverse it.
        const result = await authFetch<Page<QueuedFeedback>>(
          "/api/v1/admin/feedback?page=0&size=100",
          { signal },
        );
        setItems(result.items);
      } catch (caught) {
        if (signal?.aborted) return;
        setError(describeAuthError(caught));
      } finally {
        if (!signal?.aborted) setLoading(false);
      }
    },
    [authFetch],
  );

  useEffect(() => {
    const controller = new AbortController();
    void load(controller.signal);
    return () => controller.abort();
  }, [load]);

  async function decide(id: string, status: Decision) {
    setBusyId(id);
    setError(null);
    try {
      const updated = await authFetch<QueuedFeedback>(`/api/v1/admin/feedback/${id}`, {
        method: "PATCH",
        body: { status },
      });
      // Replace in place rather than refetching: the list does not jump, and
      // the moderator keeps their position in it.
      setItems((existing) => existing.map((item) => (item.id === id ? updated : item)));
      // The buttons changing is the visible confirmation; this is the same
      // information for someone who cannot see it happen.
      setAnnouncement(
        status === "APPROVED" ? "Published. It is now on Voices." : "Rejected. It is not public.",
      );
    } catch (caught) {
      setError(describeAuthError(caught));
    } finally {
      setBusyId(null);
    }
  }

  const waiting = items.filter((item) => item.status === "PENDING");
  const decided = items.filter((item) => item.status !== "PENDING");
  const shown = filter === "PENDING" ? waiting : decided;

  return (
    <div className="mt-10">
      {/*
        Announced, not drawn. The visible confirmation is the decision line
        appearing and the matching button going away; neither of those is
        something a screen reader announces on its own.
      */}
      <p role="status" aria-live="polite" className="sr-only">
        {announcement}
      </p>

      <ModeratorSupport />

      {error !== null ? (
        <Callout tone="caution" title="That did not work" className="mt-8">
          <p>{error}</p>
        </Callout>
      ) : null}

      <div role="tablist" aria-label="Which submissions" className="mt-10 flex gap-1">
        <FilterTab
          active={filter === "PENDING"}
          onSelect={() => setFilter("PENDING")}
          label={`Waiting (${waiting.length})`}
        />
        <FilterTab
          active={filter === "DECIDED"}
          onSelect={() => setFilter("DECIDED")}
          label={`Already decided (${decided.length})`}
        />
      </div>

      {loading ? (
        <div className="h-40" aria-live="polite" aria-busy="true" />
      ) : shown.length === 0 ? (
        <p className="mt-8 text-body text-ink-soft">
          {filter === "PENDING"
            ? "Nothing is waiting. "
            : "Nothing has been decided yet."}
          {filter === "PENDING" ? "That is the normal state of this page." : null}
        </p>
      ) : (
        <ul className="mt-8 flex flex-col gap-px border-y border-rule bg-rule">
          {shown.map((item) => (
            <li key={item.id} className="bg-surface">
              <QueueRow
                item={item}
                busy={busyId === item.id}
                viewerId={user?.id ?? null}
                onDecide={(status) => void decide(item.id, status)}
              />
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

/**
 * The helplines, addressed to the moderator.
 *
 * ===========================================================================
 * Why the wording is different here
 * ===========================================================================
 *
 * Everywhere else on this site the helplines are offered to the person
 * writing. Here the person reading is the one who might need them, and
 * repeating the visitor-facing framing — "resources for this person" — would
 * be worse than useless: it implies the moderator could pass them on, and they
 * cannot. Submissions carry no way to reach anybody.
 *
 * So this says the true thing instead. Reading difficult writing is part of
 * the job, some of it stays with you, and these numbers are open to the reader
 * as much as to anyone who wrote in.
 */
function ModeratorSupport() {
  return (
    <aside className="rounded-lg border border-rule bg-surface-sunk p-5 sm:p-6">
      <h2 className="font-display text-h3 text-ink">Before you start</h2>

      <p className="mt-3 max-w-[62ch] text-body text-ink-soft">
        Some of what is below will be hard to read, and you cannot reply to any of it —
        submissions carry no contact details at all, on purpose. If something here stays with
        you afterwards, that is an ordinary response to doing this, not a failure of it. These
        lines are open to you too.
      </p>

      <ul className="mt-4 flex flex-col gap-1.5">
        {MODERATOR_LINES.map((line) => (
          <li key={line.name} className="font-sans text-body-sm text-ink">
            <span className="font-medium">{line.name}</span>{" "}
            <span className="text-ink-soft">{line.number}</span>
            {line.hours ? <span className="text-ink-faint"> · {line.hours}</span> : null}
          </li>
        ))}
      </ul>
    </aside>
  );
}

/**
 * Kept as a small local list rather than reusing the visitor-facing component.
 *
 * `HelplineList` renders the strip the footer and the crisis panel share, and
 * its markup carries the visitor framing. Borrowing it here would either drag
 * that wording in or force a `variant` prop onto a component that does one
 * thing well.
 */
const MODERATOR_LINES = [
  { name: "Tele-MANAS", number: "14416", hours: "24/7" },
  { name: "Vandrevala Foundation", number: "9999666555", hours: "24/7" },
  { name: "iCall", number: "9152987821", hours: "Mon–Sat, 10am–8pm" },
] as const;

function FilterTab({
  active,
  onSelect,
  label,
}: {
  active: boolean;
  onSelect: () => void;
  label: string;
}) {
  return (
    <button
      type="button"
      role="tab"
      aria-selected={active}
      onClick={onSelect}
      className={cn(
        "inline-flex min-h-11 items-center rounded-md px-4",
        "font-sans text-body-sm transition-colors duration-150 ease-out",
        active ? "bg-clay-wash font-medium text-ink" : "text-ink-soft hover:bg-clay-wash",
        focusRing,
      )}
    >
      {label}
    </button>
  );
}

/**
 * One submission, collapsed to a preview until opened.
 *
 * A native `<details>`: it is a disclosure, the element exists, and it is
 * keyboard-operable and announced correctly with no code. The preview is
 * clamped to one line so a difficult note cannot be read by accident while
 * scanning the list.
 */
function QueueRow({
  item,
  busy,
  viewerId,
  onDecide,
}: {
  item: QueuedFeedback;
  busy: boolean;
  viewerId: string | null;
  onDecide: (status: Decision) => void;
}) {
  return (
    <details className="group">
      <summary
        className={cn(
          "flex cursor-pointer list-none items-center gap-4 px-1 py-4 sm:px-2",
          "transition-colors duration-150 ease-out hover:bg-clay-wash",
          focusRing,
        )}
      >
        <StatusBadge status={item.status} />
        <span className="font-sans text-body-sm text-ink-soft">{item.rating}/5</span>
        <span className="min-w-0 flex-1 truncate font-sans text-body-sm text-ink">
          {item.message}
        </span>
        <span aria-hidden="true" className="text-ink-faint transition-transform group-open:rotate-180">
          ▾
        </span>
      </summary>

      <div className="px-1 pb-6 sm:px-2">
        <blockquote className="max-w-[62ch] whitespace-pre-line font-display text-body-lg text-ink">
          {item.message}
        </blockquote>

        <dl className="mt-4 flex flex-wrap gap-x-8 gap-y-1 font-sans text-caption text-ink-soft">
          <div>
            <dt className="inline">Name: </dt>
            <dd className="inline">{item.displayName ?? "not given"}</dd>
          </div>
          <div>
            <dt className="inline">Where: </dt>
            <dd className="inline">{item.location ?? "not given"}</dd>
          </div>
          <div>
            <dt className="inline">Account: </dt>
            {/*
              Whether it is attributed to an account, not who. This page does
              not join to users - the feedback module may not read an auth
              type - and a moderator needs to know a removal request can be
              matched, not the person's address.
            */}
            <dd className="inline">{item.userId ? "signed in" : "anonymous"}</dd>
          </div>
        </dl>

        <DecisionRecord item={item} viewerId={viewerId} />

        {/*
          ===================================================================
          Only the action that would CHANGE something is offered
          ===================================================================

          Previously both buttons stayed put after a decision, identical apart
          from a word. The only feedback was the badge, and a moderator
          clicking "Reject" on an already-rejected item saw a request go out,
          the same two buttons come back, and nothing else move — which reads
          as broken, and led to the same item being rejected four times before
          anyone realised the first click had worked.

          So the button matching the current status is removed rather than
          disabled. A disabled control still occupies the spot and still has to
          be read to find out it is inert; an absent one says "done" by being
          absent, and its disappearance is the confirmation the click landed.

          The opposite action always remains. Reversal is deliberate — the
          Community Guidelines promise that someone can ask for their words to
          come down, and an approval that could not be undone would make that
          promise unkeepable.
        */}
        <div className="mt-5 flex flex-wrap items-center gap-3">
          {item.status !== "APPROVED" ? (
            <Button
              variant="secondary"
              loading={busy}
              loadingLabel="Saving"
              disabled={busy}
              onClick={() => onDecide("APPROVED")}
            >
              {/* "Publish it" once rejected: it is a reversal, not a first pass. */}
              {item.status === "REJECTED" ? "Publish it after all" : "Approve"}
            </Button>
          ) : null}

          {item.status !== "REJECTED" ? (
            <Button
              variant="secondary"
              loading={busy}
              loadingLabel="Saving"
              disabled={busy}
              onClick={() => onDecide("REJECTED")}
            >
              {/*
                "Take it down" rather than "Reject" once something is live: the
                action is different in kind, and so is how it feels to do.
              */}
              {item.status === "APPROVED" ? "Take it down" : "Reject"}
            </Button>
          ) : null}
        </div>
      </div>
    </details>
  );
}

/**
 * Who decided this, and when.
 *
 * ===========================================================================
 * Why this is on the screen at all
 * ===========================================================================
 *
 * `moderated_at` and `moderated_by` have been stored since the table was
 * created, and until now nothing rendered them — an audit trail that only
 * exists in the database is an audit trail nobody can use. The reason the
 * columns exist is so a moderator can answer "did I publish this, and when",
 * which they will need when somebody writes in asking for their words to be
 * taken down.
 *
 * ===========================================================================
 * Why the moderator is an id, and sometimes "you"
 * ===========================================================================
 *
 * The API returns `moderatedBy` as a bare UUID, and deliberately so: the
 * feedback module may not read an auth type (PROJECT_BRIEF.md §4), so the
 * endpoint does not join to `users` and there is no name to send.
 *
 * A raw UUID is close to useless to a person, so the one comparison that can
 * be made in the browser is made here: if it matches the signed-in viewer, it
 * says "you". That covers the common case — a single moderator asking what
 * they did last week — without inventing a lookup the boundary forbids. With
 * more than one moderator the id is still shown, which is honest about what is
 * known rather than blank.
 *
 * The full timestamp is shown, not a relative one. "3 days ago" is the wrong
 * shape for an audit record, and unlike `createdAt` this column keeps full
 * precision: it records a moderator acting on their own queue, so there is
 * nothing for it to be correlated against.
 */
function DecisionRecord({
  item,
  viewerId,
}: {
  item: QueuedFeedback;
  viewerId: string | null;
}) {
  if (item.status === "PENDING" || item.moderatedAt === null) {
    // Nothing has been decided, so there is nothing to record. An empty
    // "Decided by: —" row would be noise on every item in the main queue.
    return null;
  }

  const decidedByYou = viewerId !== null && item.moderatedBy === viewerId;

  return (
    <p className="mt-4 border-t border-rule pt-3 font-sans text-caption text-ink-soft">
      <span className="font-medium text-ink">
        {item.status === "APPROVED" ? "Published" : "Rejected"}
      </span>{" "}
      <time dateTime={item.moderatedAt}>{formatDecisionTime(item.moderatedAt)}</time>
      {" · "}
      {decidedByYou ? (
        "by you"
      ) : (
        <>
          by moderator{" "}
          <span className="font-mono" title={item.moderatedBy ?? undefined}>
            {shortId(item.moderatedBy)}
          </span>
        </>
      )}
    </p>
  );
}

/**
 * Date and time, in the reader's own locale.
 *
 * `undefined` as the locale rather than a hardcoded one: this only ever
 * renders in a browser, behind `RequireAuth`, so there is no server render to
 * disagree with and the moderator's own formatting is the right one.
 */
function formatDecisionTime(iso: string): string {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return "at an unknown time";
  return date.toLocaleString(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

/**
 * The first segment of a UUID, with the whole thing in a `title`.
 *
 * Enough to tell two moderators apart at a glance without a 36-character
 * string running through a caption line.
 */
function shortId(id: string | null): string {
  if (id === null) return "unknown";
  return id.split("-")[0] ?? id;
}

function StatusBadge({ status }: { status: FeedbackStatus }) {
  const label = status === "PENDING" ? "Waiting" : status === "APPROVED" ? "Published" : "Rejected";
  return (
    <span
      className={cn(
        "inline-flex shrink-0 items-center rounded-sm px-2 py-0.5",
        "font-sans text-caption",
        status === "APPROVED" && "bg-clay-wash text-(--color-text-accent)",
        status === "PENDING" && "bg-surface-sunk text-ink-soft",
        status === "REJECTED" && "bg-surface-sunk text-ink-faint",
      )}
    >
      {label}
    </span>
  );
}
