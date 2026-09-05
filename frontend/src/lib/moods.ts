/**
 * The six moods, mirroring the backend `Mood` enum.
 *
 * The wire value is the uppercase name and is the only thing sent to the
 * server. The label is what a person reads and never leaves the browser.
 *
 * Four places hold this set and nothing connects them at compile time:
 *
 *   1. `Mood.java`
 *   2. the `vent_events_mood_allowed` CHECK constraint, in a Flyway migration
 *   3. this file
 *   4. `MoodIcon.tsx`
 *
 * `MoodTest` on the backend asserts the exact set and names the other three in
 * its failure message, so a change here that is not mirrored there fails the
 * build rather than silently sending a value Postgres will reject.
 */

export const MOODS = [
  { value: "HEAVY", label: "Heavy" },
  { value: "ANXIOUS", label: "Anxious" },
  { value: "ANGRY", label: "Angry" },
  { value: "NUMB", label: "Numb" },
  { value: "TIRED", label: "Tired" },
  { value: "LOST", label: "Lost" },
] as const;

export type Mood = (typeof MOODS)[number]["value"];
