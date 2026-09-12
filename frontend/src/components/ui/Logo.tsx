import { cn } from "@/lib/cn";

/**
 * The exhale mark: a fixed point with three arcs opening away from it, each
 * lighter and thinner than the last.
 *
 * Colour comes from `currentColor`, so the mark inherits whatever text colour
 * its container sets — it works on ink, on accent and on bone without a second
 * file.
 *
 * Two rules from the logo brief, both load-bearing:
 *
 * - **Never recolour the arcs individually.** The opacity ramp is the whole
 *   idea; per-arc hues destroy it. That is why the opacities are baked in here
 *   rather than exposed as props.
 * - **Never animate on load.** On a site people open when they are struggling,
 *   a logo that performs is a logo that intrudes.
 *
 * Below 20px the third arc disappears and the second turns to mud, so small
 * sizes use the compact variant — which lives in `app/icon.svg` as the favicon.
 */
type LogoProps = {
  className?: string;
  /**
   * Accessible name. Pass `null` when the mark sits beside a visible wordmark,
   * which makes it presentational and stops screen readers announcing the
   * brand twice.
   */
  title?: string | null;
};

export function Logo({ className, title = "HeadHeartFreeS" }: LogoProps) {
  const decorative = title === null;

  return (
    <svg
      viewBox="0 0 48 48"
      fill="none"
      className={cn("h-7 w-7", className)}
      {...(decorative
        ? { "aria-hidden": true, focusable: false }
        : { role: "img", "aria-label": title })}
    >
      <circle cx="13" cy="24" r="4.5" fill="currentColor" />
      <path
        d="M22 15.5a11 11 0 0 1 0 17"
        stroke="currentColor"
        strokeWidth="3.25"
        strokeLinecap="round"
        opacity="0.85"
      />
      <path
        d="M29.5 10a18.5 18.5 0 0 1 0 28"
        stroke="currentColor"
        strokeWidth="2.5"
        strokeLinecap="round"
        opacity="0.5"
      />
      <path
        d="M37 5.5a26 26 0 0 1 0 37"
        stroke="currentColor"
        strokeWidth="1.75"
        strokeLinecap="round"
        opacity="0.25"
      />
    </svg>
  );
}

/*
 * `Wordmark` — the exhale mark plus "HeadHeartFreeS" in the display serif —
 * was removed on 2026-09-13 when the navbar moved to the client's logo mark
 * plus "HHFreeS". The navbar was its only caller.
 *
 * It is deleted rather than left unused deliberately. It rendered the site's
 * name in full beside a mark the navbar no longer shows, so anyone reaching
 * for it would have reintroduced a third brand lockup at a moment when there
 * are already two names in play — see the note in PHASE_LOG and HANDOVER about
 * the navbar reading HHFreeS while page titles and metadata say
 * HeadHeartFreeS. `Logo` itself is still used, by the footer brand block and
 * the hero watermark, and is still the favicon via app/icon.svg.
 */
