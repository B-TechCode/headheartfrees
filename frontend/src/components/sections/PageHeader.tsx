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

      {lede ? <p className="mt-4 text-body-lg text-ink-soft">{lede}</p> : null}

      {children}
    </header>
  );
}
