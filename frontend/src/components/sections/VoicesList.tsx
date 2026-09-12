"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/Button";
import { Callout } from "@/components/ui/Callout";
import { cn } from "@/lib/cn";
import { focusRing, secondaryAction } from "@/components/ui/styles";
import { describeAuthError } from "@/lib/auth/errors";
import { fetchPublishedFeedback, type PublishedFeedback } from "@/lib/feedback";

/**
 * The published notes.
 *
 * ===========================================================================
 * The empty state is the real state
 * ===========================================================================
 *
 * This page will be empty for a while, and it is built for that rather than
 * decorated to hide it. No skeleton rows, no sample cards, no "coming soon",
 * no fabricated testimonials — which is the whole reason this page exists, as
 * the honest replacement for the original design's invented quotes.
 *
 * An empty wall on a site this young is not a failure to be papered over. The
 * copy says so plainly and points back at the thing the site is actually for.
 *
 * ===========================================================================
 * Rendering
 * ===========================================================================
 *
 * Every value goes through JSX text interpolation, which escapes. There is no
 * `dangerouslySetInnerHTML` anywhere in this file or in this project, and the
 * backend refuses markup at the boundary as well — two independent reasons a
 * tag cannot execute here.
 *
 * Paginated rather than fetched whole: a "load more" that appends a page at a
 * time, so the first render is twelve rows regardless of how many exist.
 */
export function VoicesList() {
  const [items, setItems] = useState<PublishedFeedback[]>([]);
  const [page, setPage] = useState(0);
  const [hasNext, setHasNext] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async (which: number, signal?: AbortSignal) => {
    setLoading(true);
    setError(null);
    try {
      const result = await fetchPublishedFeedback(which, signal);
      // Append rather than replace, so "load more" accumulates.
      setItems((existing) => (which === 0 ? result.items : [...existing, ...result.items]));
      setHasNext(result.hasNext);
      setPage(which);
    } catch (caught) {
      // An abort is the component going away, not a failure to report.
      if (signal?.aborted) return;
      setError(describeAuthError(caught));
    } finally {
      if (!signal?.aborted) setLoading(false);
    }
  }, []);

  useEffect(() => {
    // Abortable, unlike submission: this is a read the component owns, and
    // dropping it when someone navigates away is correct.
    const controller = new AbortController();
    void load(0, controller.signal);
    return () => controller.abort();
  }, [load]);

  if (error !== null) {
    return (
      <Callout tone="caution" title="These could not be loaded">
        <p>{error}</p>
        <button
          type="button"
          onClick={() => void load(0)}
          className={cn(
            "mt-3 inline-flex min-h-11 items-center text-body",
            secondaryAction,
          )}
        >
          Try again
        </button>
      </Callout>
    );
  }

  if (loading && items.length === 0) {
    // Reserved space, not a skeleton. §8 rules out the pulsing-grey imitation,
    // and this resolves in one round trip.
    return <div className="h-64" aria-live="polite" aria-busy="true" />;
  }

  if (items.length === 0) {
    return <EmptyWall />;
  }

  return (
    <>
      <ul className="mt-12 flex flex-col gap-px border-y border-rule bg-rule">
        {items.map((entry) => (
          <li key={entry.id} className="bg-surface px-1 py-8 sm:px-2">
            <Entry entry={entry} />
          </li>
        ))}
      </ul>

      {hasNext ? (
        <div className="mt-10">
          <Button
            variant="secondary"
            loading={loading}
            loadingLabel="Loading"
            onClick={() => void load(page + 1)}
          >
            Read more
          </Button>
        </div>
      ) : null}
    </>
  );
}

/**
 * Nobody has written yet.
 *
 * Written as a real answer to a real question, not as a placeholder waiting to
 * be replaced. If this page still says this in a year, it is still true and
 * still fine.
 */
function EmptyWall() {
  return (
    <div className="mt-12 border-t border-rule pt-10">
      <p className="font-display text-h3 text-ink">Nobody has written here yet.</p>

      <p className="mt-4 max-w-[58ch] text-body text-ink-soft">
        That is not a problem to be fixed. This page fills slowly, or not at all, and either is
        fine — nothing here is invented to make it look busier than it is.
      </p>

      <Link
        href="/vent"
        className={cn(
          "mt-6 inline-flex min-h-11 items-center rounded-sm text-body-lg",
          "text-(--color-text-accent) underline decoration-accent underline-offset-4",
          "transition-colors duration-150 ease-out hover:text-ink hover:decoration-ink-faint",
          focusRing,
        )}
      >
        Go to the vent box
      </Link>
    </div>
  );
}

function Entry({ entry }: { entry: PublishedFeedback }) {
  const attribution = [entry.displayName, entry.location].filter(Boolean).join(" · ");

  return (
    <figure>
      <Rating value={entry.rating} />

      {/*
        `whitespace-pre-line` so the paragraph breaks somebody typed survive,
        without any HTML being involved. React escapes the content; the backend
        refused markup before it was stored.
      */}
      <blockquote className="mt-4 max-w-[62ch] font-display text-body-lg whitespace-pre-line text-ink">
        {entry.message}
      </blockquote>

      {attribution ? (
        <figcaption className="mt-4 font-sans text-body-sm text-ink-soft">
          {attribution}
        </figcaption>
      ) : (
        // Said out loud rather than left blank: an unattributed note with no
        // caption reads like a missing name, and anonymity here was a choice.
        <figcaption className="mt-4 font-sans text-body-sm text-ink-faint">Anonymous</figcaption>
      )}
    </figure>
  );
}

/**
 * The rating, as text first.
 *
 * The stars are `aria-hidden` and the real content is the sentence beside
 * them, because "★★★☆☆" announces as nothing useful and five separate images
 * announce as five separate images.
 */
function Rating({ value }: { value: number }) {
  return (
    <p className="flex items-center gap-2">
      <span aria-hidden="true" className="flex gap-0.5">
        {[1, 2, 3, 4, 5].map((star) => (
          <svg
            key={star}
            viewBox="0 0 24 24"
            className={cn("h-4 w-4", star <= value ? "text-accent" : "text-ink-faint")}
            fill={star <= value ? "currentColor" : "none"}
            stroke="currentColor"
            strokeWidth={star <= value ? 1 : 1.5}
            strokeLinejoin="round"
          >
            <path d="M12 3.6l2.6 5.5 5.9.8-4.3 4.2 1.1 6-5.3-2.9-5.3 2.9 1.1-6L3.5 9.9l5.9-.8z" />
          </svg>
        ))}
      </span>
      <span className="font-sans text-caption text-ink-soft">{value} out of 5</span>
    </p>
  );
}
