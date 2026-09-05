import type { Mood } from "@/lib/moods";
import { cn } from "@/lib/cn";

/**
 * The mood icons.
 *
 * One idea, six variations: **every icon is a single line crossing the box, and
 * what the line does is the meaning.** Nothing is a face, nothing is a symbol
 * you have to have learned, and none of them is an emoji — PROJECT_BRIEF.md §8
 * names emoji standing in for icons as one of the tells to avoid, and the old
 * design did exactly that.
 *
 * The line language is borrowed from the exhale mark in `Logo.tsx`, which is
 * also built from strokes of varying weight rather than from a picture of
 * anything. Six unrelated pictograms would have read as downloaded.
 *
 *   Heavy    the line sags under load
 *   Anxious  a tight tremor
 *   Angry    one sharp spike
 *   Numb     flat, with a piece missing
 *   Tired    descends and settles low
 *   Lost     breaks into pieces that drift off the line
 *
 * All are `aria-hidden`: the chip's visible label carries the meaning, and
 * announcing "heavy icon, Heavy" to a screen reader is noise.
 */

const PATHS: Record<Mood, React.ReactNode> = {
  // Bowed deeply in the middle, as though something is resting on it.
  HEAVY: <path d="M3 8q9 12 18 0" />,

  // High frequency, low amplitude. Unsettled rather than dramatic.
  ANXIOUS: <path d="M3.5 12l1.5-3 1.5 6 1.5-6 1.5 6 1.5-6 1.5 6 1.5-6 1.5 6 1.5-3" />,

  // Flat, then one steep spike, then flat. The anger is the discontinuity.
  ANGRY: <path d="M3 16h6l2.5-11 2.5 11h6" />,

  // A straight line with a piece cut out. Absence, not distress.
  NUMB: (
    <>
      <path d="M3 12h6.5" />
      <path d="M14.5 12h6.5" />
    </>
  ),

  // Starts high, gives way, settles and stays down.
  TIRED: <path d="M3 8c6 0 6.5 8 9 8s6-1 9-1" />,

  // Three fragments, none of them on the same line as the others.
  LOST: (
    <>
      <path d="M3 13.5l4-1" />
      <path d="M10 10.5l4 1" />
      <path d="M17 14.5l4-2" />
    </>
  ),
};

export function MoodIcon({ mood, className }: { mood: Mood; className?: string }) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.5"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
      className={cn("h-4 w-4", className)}
    >
      {PATHS[mood]}
    </svg>
  );
}
