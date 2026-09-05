package com.headheartfrees.vent;

/**
 * The six mood labels a release may optionally carry.
 *
 * <p>This enum is the reason the {@code mood} column is not a free-text field
 * in disguise. PROJECT_BRIEF.md rule 2.1 forbids the vent domain from accepting
 * arbitrary text; a nullable {@code TEXT} column that took whatever the client
 * sent would break that rule while appearing to follow it. Binding the request
 * DTO to this type means an unrecognised value is rejected during
 * deserialisation and never reaches the database.
 *
 * <p>The set is closed on purpose. Adding a value here means adding it to three
 * other places, and all three will fail loudly if you forget:
 *
 * <ol>
 *   <li>the {@code vent_events_mood_allowed} CHECK constraint, in a new Flyway
 *       migration — the database rejects the insert otherwise;</li>
 *   <li>{@code MOODS} in the frontend's {@code lib/moods.ts}, which needs a
 *       label and a line icon;</li>
 *   <li>{@code MoodTest}, which asserts this exact set and fails when it
 *       changes without a decision.</li>
 * </ol>
 *
 * <p>These are states a person arrives in rather than a taxonomy of emotion.
 * Six is enough to feel seen and few enough to scan without deliberating, which
 * matters on a page someone opens when they are already tired.
 */
public enum Mood {
    /** Weighed down. Carrying something. */
    HEAVY,
    /** Wired, racing, unable to settle. */
    ANXIOUS,
    /** Furious or resentful. */
    ANGRY,
    /** Flat and disconnected, feeling little. */
    NUMB,
    /** Depleted past the point of caring. */
    TIRED,
    /** Adrift, without a direction. */
    LOST
}
