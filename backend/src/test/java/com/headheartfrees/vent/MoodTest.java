package com.headheartfrees.vent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pins the mood set.
 *
 * <p>The same six values live in three other places — the Flyway CHECK
 * constraint, the frontend's {@code lib/moods.ts}, and the icon set beside it —
 * and nothing in the compiler connects them. This test is the tripwire: adding
 * or renaming a mood fails here, and the failure message says what else needs
 * changing.
 */
class MoodTest {

    @Test
    @DisplayName("the mood set is exactly the six agreed values, in order")
    void moodSetIsLocked() {
        assertThat(Arrays.stream(Mood.values()).map(Enum::name))
                .as("""
                        The mood set changed. Four places must agree, and only this one \
                        is checked by the compiler:
                          1. this enum
                          2. the vent_events_mood_allowed CHECK constraint, in a NEW \
                        Flyway migration (never edit an applied one)
                          3. MOODS in frontend/src/lib/moods.ts
                          4. the line icon for the new mood in frontend/src/components/ui/MoodIcon.tsx
                        """)
                .containsExactly("HEAVY", "ANXIOUS", "ANGRY", "NUMB", "TIRED", "LOST");
    }

    @Test
    @DisplayName("mood is an enum, so the wire format cannot carry arbitrary text")
    void moodIsClosed() {
        // The guarantee this test protects is not "there are six moods" but
        // "the set is closed at all". A String field with six documented values
        // would pass the test above and still break rule 2.1.
        assertThat(Mood.class.isEnum()).isTrue();
    }
}
