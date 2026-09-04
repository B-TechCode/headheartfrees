import { cn } from "@/lib/cn";

/**
 * The exhale mark: a fixed point with three arcs opening away from it, each
 * lighter and thinner than the last.
 *
 * Colour comes from `currentColor`, so the mark inherits whatever text colour
 * its container sets — it works on ink, on clay and on bone without a second
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

/**
 * The mark plus the wordmark, set in the display serif.
 *
 * Spacing follows the logo brief: roughly 0.6em between mark and words, with
 * the wordmark at the mark's optical height.
 */
export function Wordmark({ className }: { className?: string }) {
  return (
    <span className={cn("inline-flex items-center gap-[0.6em]", className)}>
      <Logo title={null} className="h-7 w-7 shrink-0" />
      <span className="font-display text-h4 leading-none font-semibold tracking-[-0.01em]">
        HeadHeartFreeS
      </span>
    </span>
  );
}
