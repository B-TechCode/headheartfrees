import Link from "next/link";
import { cn } from "@/lib/cn";
import { Logo } from "@/components/ui/Logo";
import { focusRing } from "@/components/ui/styles";
import { FOOTER_HELPLINES } from "@/lib/helplines";

/**
 * PROJECT_BRIEF.md §7: the footer has no Navigate column. Home, Vent and About
 * are in the navbar, and repeating them here left a three-item column too thin
 * to justify its own heading. Donate and Feedback must not appear in the footer
 * at all — the old design had them and they must not come back.
 */
const SUPPORT_LINKS = [
  { href: "/crisis-resources", label: "Crisis Resources" },
  { href: "/community-guidelines", label: "Community Guidelines" },
  { href: "/privacy", label: "Privacy Policy" },
  { href: "/contact", label: "Contact Us" },
] as const;

/*
 * The footer strip renders a subset of lib/helplines.ts. It is not a second
 * copy: that file is the single source of truth, and it exists because these
 * numbers drifted once already.
 */

export function Footer() {
  return (
    <footer className="app-layer mt-auto border-t border-rule bg-surface">
      <CrisisStrip />

      {/*
        ===================================================================
        Why this is a two-track grid and not `justify-between`
        ===================================================================

        It used to be `md:justify-between`, which anchored the brand block to
        the left edge and the Support column to the right. With the Navigate
        column gone there was nothing in the middle, so at 1440 the brand text
        ended around x=598 and the Support heading began around x=1105 — a
        507px hole, measured off a real render, that read as a column having
        been deleted rather than as a decision.

        It did not shrink on smaller desktops either: the container is capped
        at `max-w-6xl`, so 1440 and 1920 produced the identical gap while the
        surrounding page margins grew, which is what made 1920 look worst.

        The fix is a fixed first track rather than a flexible gap. Column one
        is exactly the brand block's own measure (28rem = `max-w-md`), the gap
        is fixed, and column two starts immediately after it — at 1088px of
        content that puts the Support heading at x=544, the horizontal centre.
        The remaining space falls to the right of the pair, which is where the
        rest of this site already puts it: every page here is a left-aligned
        measure inside a wider container (§8 asks for asymmetry, not for
        centring), so the footer now matches the composition above it instead
        of fighting it.

        The bottom bar uses the same two tracks, so "Support this space" sits
        directly under the Support column and the two rows read as one grid.

        Breakpoints: stacked below md; even halves at md, where 28rem plus the
        gap would leave the second column too narrow for "Community
        Guidelines"; the fixed measure from lg up.
      */}
      <div className="mx-auto max-w-6xl px-4 py-12 sm:px-6 lg:px-8 lg:py-16">
        <div
          className={cn(
            "grid grid-cols-1 gap-10",
            "md:grid-cols-2 md:gap-x-12",
            "lg:grid-cols-[28rem_1fr] lg:gap-x-24",
          )}
        >
          <div className="min-w-0 max-w-md">
            <Logo title={null} className="h-8 w-8 text-ink" />
            <p className="mt-4 font-display text-h4 text-ink">A place to put it down.</p>
            <p className="mt-2 text-body-sm text-ink-soft">
              What you write here never leaves your browser. It is not stored, not sent, and
              not read by anyone — including us.
            </p>
          </div>

          <FooterColumn title="Support" links={SUPPORT_LINKS} />
        </div>

        {/*
          The same two tracks as the block above, so the copyright sits under
          the brand and the support line under the Support column. Previously
          both were pushed to opposite edges by `justify-between` and had the
          same several-hundred-pixel hole between them.
        */}
        <div
          className={cn(
            "mt-12 grid grid-cols-1 gap-4 border-t border-rule pt-6",
            "md:grid-cols-2 md:items-center md:gap-x-12",
            "lg:grid-cols-[28rem_1fr] lg:gap-x-24",
          )}
        >
          <p className="text-caption text-ink-soft">
            &copy; {new Date().getFullYear()} HeadHeartFreeS
          </p>

          {/* Quiet by design — a soft line, not a banner and not a modal. */}
          <Link
            href="/support"
            className={cn(
              "inline-flex min-h-11 items-center justify-self-start rounded-sm",
              "text-body-sm text-ink-soft",
              "underline decoration-rule-strong underline-offset-4",
              "transition-colors duration-150 ease-out",
              "hover:text-(--color-text-accent) hover:decoration-clay",
              focusRing,
            )}
          >
            Support this space
          </Link>
        </div>
      </div>
    </footer>
  );
}

/**
 * The helpline strip.
 *
 * This is the most important element on the page and it is built that way: body
 * text at full ink contrast (14.9:1), not caption grey; every number a real
 * link with a 44px target; and enough room around it that it is not skimmed
 * past.
 *
 * What it deliberately is *not*: red, bordered in danger colour, prefixed with
 * a warning icon, or headed with the word "crisis". Someone reaching this strip
 * is having a hard enough day without the page raising its voice at them. It
 * should read as steady and available — the tone of a light left on, not an
 * alarm. `surface-raised` plus a clay hairline gives it presence without
 * urgency.
 */
function CrisisStrip() {
  return (
    <section
      aria-labelledby="helplines-heading"
      className="border-b border-rule bg-surface-raised"
    >
      <div className="mx-auto max-w-6xl px-4 py-8 sm:px-6 lg:px-8">
        {/* A single clay hairline: present, not loud. */}
        <span aria-hidden="true" className="block h-px w-10 bg-clay" />

        <h2 id="helplines-heading" className="mt-4 font-display text-h4 text-ink">
          If you would rather talk to someone.
        </h2>
        <p className="mt-1 text-body-sm text-ink-soft">
          Free and confidential. Hours are listed because not every line runs all night.
        </p>

        <ul className="mt-5 flex flex-col gap-1 sm:flex-row sm:flex-wrap sm:gap-x-10 sm:gap-y-1">
          {FOOTER_HELPLINES.map((line) => (
            <li key={line.name}>
              <a
                href={line.href}
                className={cn(
                  "group flex min-h-11 flex-wrap items-baseline gap-x-2 rounded-sm py-1",
                  "text-body text-ink",
                  "transition-colors duration-150 ease-out",
                  focusRing,
                )}
              >
                <span className="font-medium">{line.name}</span>
                {line.prefix ? (
                  <span className="text-body-sm text-ink-soft">{line.prefix}</span>
                ) : null}
                {/*
                  A visible underline, not a hairline. `rule-strong` is 1.88:1
                  against this surface — legible enough for a decorative
                  divider, too faint to advertise that a phone number is
                  tappable. On this strip in particular the affordance has to
                  be obvious.
                */}
                <span
                  className={cn(
                    "underline decoration-ink-faint decoration-from-font underline-offset-4",
                    "group-hover:text-(--color-text-accent) group-hover:decoration-clay",
                  )}
                >
                  {line.number}
                </span>
                <span className="text-caption text-ink-soft">{line.hours}</span>
              </a>
            </li>
          ))}
        </ul>

        <p className="mt-4 text-body-sm text-ink-soft">
          <Link
            href="/crisis-resources"
            className={cn(
              "rounded-sm underline decoration-ink-faint underline-offset-4",
              "hover:text-(--color-text-accent) hover:decoration-clay",
              focusRing,
            )}
          >
            More helplines, including outside India
          </Link>
        </p>
      </div>
    </section>
  );
}

/**
 * The Support column.
 *
 * ===========================================================================
 * Why this is not a micro-label any more
 * ===========================================================================
 *
 * It was an 11px uppercase overline in `ink-soft` over four 14px links, which
 * is the treatment for one column among four. As the only column beside the
 * brand block it read as leftover chrome — the visual weight said "site map
 * fragment" while the position said "half the footer".
 *
 * Three changes, all typographic. Nothing was added to fill space: the same
 * four links, no new ones, and the Navigate column stays gone.
 *
 * 1. The clay hairline above the heading. The same motif the crisis strip and
 *    `/vent/released` open with, so the column is announced the way every
 *    other section of this site is rather than just starting.
 * 2. The heading is `text-h4` in full ink, matching "A place to put it down."
 *    across the grid. Two headings of equal weight on one baseline is what
 *    makes the row read as two columns instead of a block and an appendix.
 * 3. Links move from `body-sm`/`ink-soft` to `body`/`ink`. They are the only
 *    route to four real pages — Crisis Resources among them — and they were
 *    set quieter than the copyright line.
 */
function FooterColumn({
  title,
  links,
}: {
  title: string;
  links: ReadonlyArray<{ href: string; label: string }>;
}) {
  return (
    <div className="min-w-0">
      <span aria-hidden="true" className="block h-px w-10 bg-clay" />

      <h2 className="mt-4 font-display text-h4 text-ink">{title}</h2>

      <ul className="mt-3 flex flex-col">
        {links.map((link) => (
          <li key={link.href}>
            <Link
              href={link.href}
              className={cn(
                "inline-flex min-h-11 items-center rounded-sm text-body text-ink",
                "underline decoration-transparent underline-offset-4",
                "transition-colors duration-150 ease-out",
                "hover:text-(--color-text-accent) hover:decoration-clay",
                focusRing,
              )}
            >
              {link.label}
            </Link>
          </li>
        ))}
      </ul>
    </div>
  );
}
