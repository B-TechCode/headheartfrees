import type { ReactNode } from "react";
import { cn } from "@/lib/cn";

/**
 * The h1 and standfirst for the secondary pages.
 *
 * Deliberately not centred, and the eyebrow sits above the rule rather than
 * below it, so these pages open on the same left axis as the home hero instead
 * of switching to a centred-column layout halfway through the site.
 */
export function PageHeader({
  eyebrow,
  title,
  lede,
  children,
  className,
}: {
  eyebrow?: string;
  title: string;
  lede?: string;
  children?: ReactNode;
  className?: string;
}) {
  return (
    <header className={cn("max-w-2xl", className)}>
      {eyebrow ? (
        <p className="font-sans text-overline font-semibold tracking-[0.085em] text-ink-soft uppercase">
          {eyebrow}
        </p>
      ) : null}

      <h1 className={cn("font-display text-h1 text-ink", eyebrow ? "mt-3" : null)}>{title}</h1>

      {/*
        The lede is a standfirst, not body copy, and now reads as one: up a
        step to `text-h4` (18px) and to full `ink` rather than `ink-soft`.

        `leading-relaxed` is deliberate. The `h4` token carries a 1.36 line
        height because it was scaled for a heading, and several of these ledes
        run four or five lines — /support's is sixty words. 18px at 1.625 is
        29px of leading against the 28.5px the old `body-lg` had, so the
        paragraph gains size and weight without the rhythm tightening under it.

        Contrast goes up, not down: ink-soft 9.30 to ink 15.34 on `surface`.
      */}
      {lede ? <p className="mt-4 text-h4 leading-relaxed text-ink">{lede}</p> : null}

      {children}
    </header>
  );
}
