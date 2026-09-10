import Link from "next/link";
import { cn } from "@/lib/cn";
import { Logo } from "@/components/ui/Logo";
import { focusRing } from "@/components/ui/styles";
import { FOOTER_HELPLINES } from "@/lib/helplines";
import { SOCIAL_LINKS, type SocialIconName } from "@/lib/social";
import { SectionRule } from "@/components/ui/SectionRule";

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
    /*
      The footer body sits on `surface-sunk`, not `surface`, so the page ends
      on a distinct zone rather than running out.

      This also sharpens the crisis strip rather than dulling it. The strip is
      `surface-raised` and was a 1.07:1 step off the `surface` around it —
      barely a boundary at all. Against `surface-sunk` that step is 1.17:1, so
      the strip reads as raised out of the footer instead of merely sitting in
      it. The strip itself is untouched.

      Nothing in this block uses `ink-faint`, which is the token that would
      have failed here: it is 4.12:1 on `surface-sunk`, under the 4.5 AA needs
      for normal text. The only `ink-faint` in this file is inside the crisis
      strip, which stays on `surface-raised` where it measures 4.82:1.

      Measured on `surface-sunk` #EFEAE1: ink 14.05, ink-soft 8.52, clay-deep
      (every hover) 5.52, clay hairline 3.79 against a 3.0 graphic threshold.
    */
    <footer className="app-layer mt-auto border-t border-rule bg-surface-sunk">
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

          {/*
            The second track carries both the support line and the social row.

            The brief for the icons was "copyright on one side, icons on the
            other", and that is what this is — but they are not pushed to the
            right edge of the track. This grid's whole reason for existing is
            the comment above: `justify-between` put a measured 507px hole in
            the middle of the footer at 1440, and the fix was to stop treating
            the far edge as a place to anchor things. Sending the icons there
            would rebuild the hole one row lower, with the support line and the
            icons at opposite ends of a 544px track.

            So the two sit together at the start of the track, opposite the
            copyright. They stack below `lg` rather than at `md`: at `md` this
            cell is 336px and the line plus four 44px targets is ~338px, which
            is a wrap waiting to happen at exactly the width where the grid
            first goes two-up.
          */}
          <div
            className={cn(
              "flex flex-col items-start gap-3",
              "lg:flex-row lg:items-center lg:gap-x-8",
            )}
          >
            {/* Quiet by design — a soft line, not a banner and not a modal. */}
            <Link
              href="/support"
              className={cn(
                "inline-flex min-h-11 items-center rounded-sm",
                "text-body-sm text-ink-soft",
                "underline decoration-rule-strong underline-offset-4",
                "transition-colors duration-150 ease-out",
                "hover:text-(--color-text-accent) hover:decoration-clay",
                focusRing,
              )}
            >
              Support this space
            </Link>

            <SocialRow />
          </div>
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
      <SectionRule />

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

/**
 * The social row.
 *
 * ===========================================================================
 * Where the links come from
 * ===========================================================================
 *
 * `lib/social.ts`, and nothing here is hardcoded. Read that file's header
 * before touching this: the four URLs are the **developer's personal
 * accounts** and are temporary. Unlike the contact address and the payment ID,
 * they are live and working, so nothing on this page marks them as
 * placeholder — this component renders them exactly as it would render the
 * project's own.
 *
 * ===========================================================================
 * Why these are not brand logos
 * ===========================================================================
 *
 * The recognisable LinkedIn, Facebook and GitHub marks are trademarks with
 * usage terms attached — permitted uses, minimum clear space, prohibited
 * recolouring. This site draws its own glyphs instead, in the same hand as
 * every other icon here: 24-unit box, 1.75 stroke, round caps, `currentColor`.
 * They are metaphors rather than marks — a briefcase for the professional
 * profile, two figures for the social one, a branch for the code host, a globe
 * for the personal site.
 *
 * **The cost is real and worth stating: a briefcase is not as instantly
 * readable as the LinkedIn glyph.** That is why the accessible name is the
 * platform's own name and why `title` puts the same word in a hover tooltip.
 * The icon narrows the guess; the name settles it.
 *
 * ===========================================================================
 * The anchor
 * ===========================================================================
 *
 * - `aria-label` carries the real name. A screen reader announces "LinkedIn,
 *   link", not "link" — the SVG is `aria-hidden`, so without the label these
 *   would be four unnamed links in a row, which is close to the worst
 *   available outcome for a keyboard or screen reader user.
 * - `h-11 w-11` is the 44px target from PROJECT_BRIEF.md §8, around a 20px
 *   glyph. The `-ml-3` cancels the first target's own left padding so the
 *   glyphs line up with the text above them rather than sitting indented by
 *   12px; at `lg` the row moves inline and the offset is dropped.
 * - `target="_blank"` with `rel="noopener noreferrer"`. These leave the site,
 *   and `noopener` is what stops the opened page reaching back through
 *   `window.opener`.
 */
function SocialRow() {
  return (
    <ul className="-ml-3 flex items-center lg:ml-0">
      {SOCIAL_LINKS.map((link) => (
        <li key={link.label}>
          <a
            href={link.href}
            target="_blank"
            rel="noopener noreferrer"
            aria-label={link.label}
            title={link.label}
            className={cn(
              "inline-flex h-11 w-11 items-center justify-center rounded-sm",
              "text-ink-soft",
              "transition-colors duration-150 ease-out",
              "hover:text-(--color-text-accent)",
              focusRing,
            )}
          >
            <SocialIcon name={link.icon} />
          </a>
        </li>
      ))}
    </ul>
  );
}

/**
 * The four glyphs, drawn to the house spec: `0 0 24 24`, `fill="none"`, 1.75
 * stroke in `currentColor`, round caps and joins. The same numbers the navbar
 * menu button and the account chevron use, so the footer does not introduce a
 * second icon weight.
 *
 * Always `aria-hidden`. The name lives on the anchor.
 */
function SocialIcon({ name }: { name: SocialIconName }) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.75"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
      className="h-5 w-5"
    >
      {name === "briefcase" ? (
        <>
          <rect x="3" y="7.5" width="18" height="12" rx="2" />
          <path d="M9 7.5V6a1.5 1.5 0 0 1 1.5-1.5h3A1.5 1.5 0 0 1 15 6v1.5" />
          <path d="M3 13h18" />
        </>
      ) : null}

      {name === "people" ? (
        <>
          <circle cx="9.25" cy="8.25" r="3.25" />
          <path d="M3.5 19.5a5.75 5.75 0 0 1 11.5 0" />
          <path d="M16.25 5.4a3.25 3.25 0 0 1 0 5.7" />
          <path d="M17.5 14.4a5.75 5.75 0 0 1 3 5.1" />
        </>
      ) : null}

      {name === "branch" ? (
        <>
          <circle cx="7" cy="5.75" r="2.5" />
          <circle cx="7" cy="18.25" r="2.5" />
          <circle cx="17" cy="5.75" r="2.5" />
          <path d="M7 8.25v7.5" />
          <path d="M17 8.25v1.5a4 4 0 0 1-4 4H7" />
        </>
      ) : null}

      {name === "globe" ? (
        <>
          <circle cx="12" cy="12" r="8.25" />
          <path d="M3.75 12h16.5" />
          <path d="M12 3.75c2.1 2.35 3.15 5.1 3.15 8.25S14.1 17.9 12 20.25c-2.1-2.35-3.15-5.1-3.15-8.25S9.9 6.1 12 3.75Z" />
        </>
      ) : null}
    </svg>
  );
}
