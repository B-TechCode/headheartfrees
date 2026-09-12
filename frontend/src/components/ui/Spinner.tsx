import { cn } from "@/lib/cn";

/**
 * Busy indicator.
 *
 * The SVG is `aria-hidden`; the meaning is carried by text. A caller either
 * renders the visually-hidden label built in here (`label`), or sets
 * `aria-busy` on the control that owns the state — `Button` does the latter.
 *
 * Under `prefers-reduced-motion` the global rule in globals.css stops the
 * rotation and the arc renders static. That is deliberate: the accessible
 * meaning never depended on the movement, so nothing is lost when it stops.
 *
 * Contrast, measured as a graphic (SC 1.4.11 wants 3:1 for the parts required
 * to understand it — here, the arc):
 *
 *   arc, accent on surface         11.34:1   PASS
 *   arc, accent-hover on surface    8.37:1   PASS
 *   arc, ink on surface            15.34:1   PASS
 *
 * The arc clears 3:1 in every colour the product uses, accent included. What did
 * read as washed out was the *track*: at 0.25 opacity it measured 1.62:1 in
 * accent and only 1.69:1 in ink, so it was faint regardless of colour and
 * darkening the accent would not have fixed it. The track is the unfilled part
 * of the indicator — decorative under 1.4.11, the same way the empty portion of
 * a progress bar is — so this is a legibility fix, not a conformance one:
 * opacity raised to 0.35 and the stroke thickened, which reads better at the
 * 16px default without flattening the track/arc difference that creates the
 * sense of rotation.
 */
type SpinnerProps = {
  className?: string;
  /** Visually-hidden text announcing what is happening. */
  label?: string;
};

export function Spinner({ className, label }: SpinnerProps) {
  return (
    <>
      <svg
        viewBox="0 0 24 24"
        fill="none"
        aria-hidden="true"
        focusable="false"
        className={cn("h-4 w-4 animate-spin", className)}
      >
        {/* Track, then the arc that reads as motion. */}
        <circle cx="12" cy="12" r="9" stroke="currentColor" strokeWidth="3" opacity="0.35" />
        <path
          d="M21 12a9 9 0 0 0-9-9"
          stroke="currentColor"
          strokeWidth="3"
          strokeLinecap="round"
        />
      </svg>
      {label ? <span className="sr-only">{label}</span> : null}
    </>
  );
}
