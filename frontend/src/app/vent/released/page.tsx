import type { Metadata } from "next";
import Link from "next/link";
import { cn } from "@/lib/cn";
import { focusRing } from "@/components/ui/styles";

export const metadata: Metadata = {
  title: "Released",
  description: "It is gone. Nothing was stored, and nothing was sent.",
};

/*
 * /vent/released
 *
 * What is deliberately NOT here:
 *
 * - **The release count.** `/api/v1/vent/stats` exists and returns a real
 *   number, but nothing displays it. Early on that number is small, and "4
 *   releases" shown to someone who has just let go of something reads as
 *   "nobody else is here" — the opposite of what this page is for. Revisited
 *   when the number means something.
 * - **The feedback form.** It posts to `/api/v1/feedback`, which is phase 7.
 *   A form that posts nowhere would be worse than no form, particularly on this
 *   page, where someone submitting into a void would reasonably conclude the
 *   site had swallowed their words. The slot below is where it lands, and the
 *   surrounding layout will not need redesigning for it.
 * - **Anything they wrote.** There is nothing to show. It was never sent, and
 *   the component holding it has unmounted.
 *
 * A quiet page on purpose. The moment after letting something go is not the
 * moment to ask for a rating, an account, or money.
 */
export default function ReleasedPage() {
  return (
    <div className="mx-auto max-w-2xl px-4 py-16 sm:px-6 lg:py-24">
      <span aria-hidden="true" className="block h-px w-12 bg-clay" />

      <h1 className="mt-6 font-display text-h1 text-ink">It&rsquo;s gone.</h1>

      <p className="mt-5 max-w-[60ch] text-body-lg text-ink-soft">
        What you wrote was never sent to us and was never stored. It was only ever in your
        browser, and now it is not there either.
      </p>

      <p className="mt-4 max-w-[60ch] text-body text-ink-soft">
        You do not have to feel better. Sometimes putting a thing down is just putting it
        down.
      </p>

      <div className="mt-10 flex flex-col gap-3 sm:flex-row sm:items-center sm:gap-4">
        <Link
          href="/vent"
          className={cn(
            "inline-flex h-12 items-center justify-center rounded-md px-5",
            "font-sans text-body font-medium tracking-[0.005em]",
            "border border-transparent bg-clay text-on-clay",
            "transition-[background-color,border-color] duration-150 ease-out",
            "hover:bg-clay-hover active:bg-clay-active active:translate-y-[0.5px]",
            focusRing,
          )}
        >
          Write something else
        </Link>

        <Link
          href="/"
          className={cn(
            "inline-flex min-h-11 items-center rounded-sm text-body text-ink-soft",
            "underline decoration-ink-faint underline-offset-4",
            "transition-colors duration-150 ease-out",
            "hover:text-(--color-text-accent) hover:decoration-clay",
            focusRing,
          )}
        >
          Back to home
        </Link>
      </div>

      {/*
        Phase 7 drops the feedback form in here: heading, rating, message, and
        the optional name and location fields. The spacing and the rule above it
        are already right for it, so adding it is an insertion rather than a
        redesign.
      */}

      <p className="mt-16 border-t border-rule pt-6 max-w-[60ch] text-body-sm text-ink-soft">
        This runs on a small server and stays free.{" "}
        <Link
          href="/support"
          className={cn(
            "rounded-sm underline decoration-ink-faint underline-offset-4",
            "hover:text-(--color-text-accent) hover:decoration-clay",
            focusRing,
          )}
        >
          You can help pay for it
        </Link>{" "}
        if you want to, and nothing changes if you do not.
      </p>
    </div>
  );
}
