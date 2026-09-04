import Link from "next/link";
import { cn } from "@/lib/cn";
import { Logo } from "@/components/ui/Logo";
import { focusRing } from "@/components/ui/styles";

/**
 * Home hero.
 *
 * Asymmetric per PROJECT_BRIEF.md §8: a 7/5 split on large screens with the
 * text on the left axis and the mark set low and right, not a centred column
 * with a button under it. The mark is oversized and cropped by its container
 * rather than sitting in a tidy box, so the composition has a weight on one
 * side. Below `lg` it stacks and the mark is dropped entirely — at that width
 * it would be decoration pushing the primary action below the fold.
 *
 * There is no sign-up prompt, no modal and no interstitial here, by §7. The
 * only things competing for attention are one link to /vent and one to /about.
 */
export function Hero() {
  return (
    <section className="border-b border-rule">
      <div className="mx-auto max-w-6xl px-4 py-16 sm:px-6 lg:grid lg:grid-cols-12 lg:gap-8 lg:px-8 lg:py-28">
        <div className="lg:col-span-7">
          <span aria-hidden="true" className="block h-px w-12 bg-clay" />

          <h1 className="mt-6 font-display text-display text-ink">Somewhere to put it down.</h1>

          <p className="mt-6 max-w-xl text-body-lg text-ink-soft">
            Write what&rsquo;s weighing on you, then let it go. The words never leave your
            browser. Nothing is sent, nothing is stored, and no one reads them.
          </p>

          <div className="mt-9 flex flex-col gap-3 sm:flex-row sm:items-center sm:gap-4">
            <Link
              href="/vent"
              className={cn(
                "inline-flex h-14 items-center justify-center rounded-md px-7",
                "font-sans text-body-lg font-medium tracking-[0.005em]",
                "border border-transparent bg-clay text-on-clay",
                "transition-[background-color,border-color] duration-150 ease-out",
                "hover:bg-clay-hover active:bg-clay-active active:translate-y-[0.5px]",
                focusRing,
              )}
            >
              Start writing
            </Link>

            <Link
              href="/about"
              className={cn(
                "inline-flex min-h-11 items-center rounded-sm text-body text-ink-soft",
                "underline decoration-ink-faint underline-offset-4",
                "transition-colors duration-150 ease-out",
                "hover:text-(--color-text-accent) hover:decoration-clay",
                focusRing,
              )}
            >
              Why this exists
            </Link>
          </div>

          <p className="mt-8 text-body-sm text-ink-soft">
            No account. It is free, and it stays free.
          </p>
        </div>

        {/*
          Decorative. Hidden below lg so it never displaces the primary action
          on a phone, and aria-hidden because the wordmark in the navbar
          already names the product.
        */}
        <div
          aria-hidden="true"
          className="pointer-events-none hidden lg:col-span-5 lg:flex lg:items-end lg:justify-end lg:overflow-hidden"
        >
          <Logo title={null} className="h-72 w-72 translate-x-10 translate-y-4 text-clay/25" />
        </div>
      </div>
    </section>
  );
}
