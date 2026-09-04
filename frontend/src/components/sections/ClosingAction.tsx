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
    <section className="mx-auto max-w-6xl px-4 py-16 sm:px-6 lg:px-8 lg:py-20">
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
    </section>
  );
}
