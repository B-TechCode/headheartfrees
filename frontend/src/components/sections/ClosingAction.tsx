import Link from "next/link";
import { cn } from "@/lib/cn";
import { focusRing } from "@/components/ui/styles";

/**
 * The quiet close.
 *
 * One line and one link. Not a second hero, not a repeated call-to-action
 * block with its own background and heading — the page has already made its
 * case by this point, and pressing again would undercut the tone the rest of
 * the site is trying to hold.
 */
export function ClosingAction() {
  return (
    /*
      `surface`, which is the last step in the home page's alternation and the
      one that keeps the crisis strip below it legible. The strip is
      `surface-raised`; if this section were raised too, the strip would open
      against an identical surface and lose the boundary it depends on.

      No heading and so no section rule. This is one line and one link — §8's
      "not a second hero" — and announcing it would make it the thing it was
      written not to be.
    */
    <section className="bg-surface">
      <div className="mx-auto max-w-6xl px-4 py-16 sm:px-6 lg:px-8 lg:py-20">
        <p className="max-w-xl font-display text-h3 text-ink">
          Whenever you are ready, the box is there.
        </p>

        <Link
          href="/vent"
          className={cn(
            "mt-5 inline-flex min-h-11 items-center rounded-sm text-body-lg",
            "text-(--color-text-accent) underline decoration-clay underline-offset-4",
            "transition-colors duration-150 ease-out hover:text-ink hover:decoration-ink-faint",
            focusRing,
          )}
        >
          Go to the vent
        </Link>
      </div>
    </section>
  );
}
