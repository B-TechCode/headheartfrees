import Link from "next/link";
import type { Metadata } from "next";
import { cn } from "@/lib/cn";
import { focusRing } from "@/components/ui/styles";

export const metadata: Metadata = {
  title: "Page not found",
};

/*
 * Root not-found boundary.
 *
 * Living at `app/not-found.tsx` means Next renders it inside `app/layout.tsx`,
 * so the navbar, footer, fonts and grain come with it — the default 404 had
 * none of them, which is why an unmatched URL was landing on unstyled black.
 *
 * The copy is deliberately not "This page could not be found." Someone who
 * mistypes a URL on this site may be having a bad enough day already; the page
 * should be matter-of-fact, take the blame off them, and point at the two
 * things they are most likely to have been looking for.
 */
export default function NotFound() {
  return (
    <div className="mx-auto max-w-2xl px-4 py-20 sm:px-6 lg:py-28">
      <p className="font-sans text-overline font-semibold tracking-[0.085em] text-ink-soft uppercase">
        404
      </p>

      <h1 className="mt-3 font-display text-h1 text-ink">That page isn&rsquo;t here.</h1>

      <p className="mt-4 max-w-prose text-body-lg text-ink-soft">
        The link may be old, or the address slightly off. Either way it is not something you
        did wrong.
      </p>

      <p className="mt-3 max-w-prose text-body text-ink-soft">
        Nothing you have written is affected — it never leaves your browser to begin with.
      </p>

      {/*
        Anchors, not Buttons: these navigate. A <button> inside a <Link> is
        invalid HTML, so the Button token classes are applied to the anchor
        directly rather than nesting the two.
      */}
      <div className="mt-8 flex flex-col gap-3 sm:flex-row sm:items-center">
        <Link
          href="/"
          className={cn(
            "inline-flex h-12 items-center justify-center rounded-md px-5",
            "font-sans text-body font-medium tracking-[0.005em]",
            "border border-transparent bg-accent text-on-accent",
            "transition-[background-color,border-color,color] duration-150 ease-out",
            "hover:bg-accent-hover active:bg-accent-active active:translate-y-[0.5px]",
            focusRing,
          )}
        >
          Back to home
        </Link>

        <Link
          href="/vent"
          className={cn(
            "inline-flex h-12 items-center justify-center rounded-md px-5",
            "font-sans text-body font-medium tracking-[0.005em]",
            "border border-ink-faint bg-surface-raised text-ink",
            "transition-[background-color,border-color,color] duration-150 ease-out",
            "hover:bg-surface-sunk active:bg-surface-sunk active:translate-y-[0.5px]",
            focusRing,
          )}
        >
          Go to the vent
        </Link>
      </div>
    </div>
  );
}
