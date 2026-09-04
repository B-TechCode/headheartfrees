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
        <circle cx="12" cy="12" r="9" stroke="currentColor" strokeWidth="2.5" opacity="0.25" />
        <path
          d="M21 12a9 9 0 0 0-9-9"
          stroke="currentColor"
          strokeWidth="2.5"
          strokeLinecap="round"
        />
      </svg>
      {label ? <span className="sr-only">{label}</span> : null}
    </>
  );
}
