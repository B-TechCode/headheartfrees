import Link from "next/link";
import { cn } from "@/lib/cn";
import { Logo } from "@/components/ui/Logo";
import { focusRing, secondaryAction } from "@/components/ui/styles";
import { SectionRule } from "@/components/ui/SectionRule";

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
    /*
      `surface-raised`, which starts the home page's alternation: navbar
      `surface`, hero `raised`, How It Works `surface`, Promises `raised`,
      the close `surface`, then the crisis strip `raised` above the sunk
      footer. Every seam down the page is a step.

      Starting on `raised` rather than `surface` is what makes that work. The
      other phasing — hero on `surface` — lands `raised` against `raised` at
      the crisis strip, flattening the one boundary that is not allowed to go
      soft. Contrast improves here rather than degrading: on #FFFCF7 ink is
      16.45 and ink-soft 9.97, against 15.34 and 9.30 on `surface`.
    */
    <section className="border-b border-rule bg-surface-raised">
      <div className="mx-auto max-w-6xl px-4 py-16 sm:px-6 lg:grid lg:grid-cols-12 lg:gap-8 lg:px-8 lg:py-28">
        <div className="lg:col-span-7">
          <SectionRule />

          {/*
            The one piece of colour in the type on this page, and it is the
            last word rather than a phrase: "put it down" is what the product
            does, and the sentence resolves on it.

            `clay-deep` (--color-text-accent), not `clay`. At `text-display`
            this is 40-64px, so WCAG treats it as large text needing 3.0 —
            `clay` would technically clear that at 4.14:1, but globals.css §1
            holds that clay is never a text colour anywhere on this site, and
            one hero is not a reason to make the rule conditional. clay-deep
            measures 6.03:1 on `surface` and 6.46:1 on the `surface-raised`
            this hero actually sits on.
          */}
          <h1 className="mt-6 font-display text-display text-ink">
            Somewhere to put it <span className="text-(--color-text-accent)">down.</span>
          </h1>

          <p className="mt-6 max-w-xl text-h4 leading-relaxed text-ink">
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
                "inline-flex min-h-11 items-center text-body",
                secondaryAction,
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
