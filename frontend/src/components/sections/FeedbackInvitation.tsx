"use client";

import { useState } from "react";
import { cn } from "@/lib/cn";
import { focusRing, secondaryAction } from "@/components/ui/styles";
import { FeedbackForm } from "@/components/sections/FeedbackForm";

/**
 * The entry point to the feedback form on `/vent/released`.
 *
 * ===========================================================================
 * Why this is a link and not a form
 * ===========================================================================
 *
 * This sits seconds after someone let something go. A form already open here,
 * with stars showing and a cursor waiting, reads as the price of using the
 * site — you have had your turn, now rate us. That is the specific risk the
 * brief names about this page's proximity to the release, and it is why the
 * default state is one sentence and one link.
 *
 * The copy does the work. "You don't have to" is the clause that matters: it
 * says declining is the expected answer rather than a permitted one, which is
 * the difference between an invitation and a toll. Everything else here is
 * arranged so that sentence is the one being read.
 *
 * It is placed **after** "Write something else" deliberately. The primary
 * action on this page is going back to the box; anything that could compete
 * with it belongs below it, and nothing here blocks it.
 */
export function FeedbackInvitation() {
  const [open, setOpen] = useState(false);
  const [submitted, setSubmitted] = useState(false);

  if (submitted) {
    return (
      <section className="mt-16 border-t border-rule pt-8">
        {/*
          `role="status"` rather than `alert`: this is the good outcome, and a
          polite announcement is right for it. It replaces the form entirely,
          so there is no lingering half-filled thing to wonder about.
        */}
        <div role="status">
          <p className="font-display text-h3 text-ink">Thank you — that reached us.</p>
          <p className="mt-3 max-w-[58ch] text-body text-ink-soft">
            Someone will read it before it appears anywhere. If it is published, it will show
            up on{" "}
            <a
              href="/voices"
              className={cn(
                "rounded-sm underline decoration-ink-faint underline-offset-4",
                "text-(--color-text-accent) transition-colors duration-150 ease-out",
                "hover:decoration-accent",
                focusRing,
              )}
            >
              Voices
            </a>
            .
          </p>
        </div>
      </section>
    );
  }

  return (
    <section className="mt-16 border-t border-rule pt-8">
      <p className="max-w-[58ch] text-body text-ink-soft">
        If you want to say something about this place, there is a box for it. You don&rsquo;t
        have to — most people don&rsquo;t, and that is the usual answer.
      </p>

      {open ? (
        <FeedbackForm onSubmitted={() => setSubmitted(true)} onCancel={() => setOpen(false)} />
      ) : (
        <button
          type="button"
          onClick={() => setOpen(true)}
          className={cn(
            "mt-4 inline-flex min-h-11 items-center text-body",
            secondaryAction,
          )}
        >
          Leave a note
        </button>
      )}
    </section>
  );
}
