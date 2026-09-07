package com.headheartfrees.feedback;

import java.util.UUID;

/**
 * One published note, as {@code /voices} sees it.
 *
 * <p><strong>The absent fields are the design.</strong> There is no
 * {@code userId}, no {@code status}, no {@code moderatedBy}, no
 * {@code moderatedAt} and no {@code createdAt} — not filtered out at
 * serialisation time, but with nowhere in the type to put them. A future
 * change that wants to expose who wrote something has to add a field to this
 * record, which is a visible act in a diff, rather than forgetting a
 * {@code @JsonIgnore}, which is not.
 *
 * <p>{@code createdAt} is absent for a further reason. It is stored truncated
 * to the hour precisely so it cannot be lined up against {@code vent_events},
 * and publishing it would hand that correlation to anyone with a browser
 * rather than only to someone holding the database.
 *
 * @param id          so the client can key a list. Reveals nothing: it is
 *                    random and maps to no person.
 * @param rating      1-5.
 * @param message     the note.
 * @param displayName null when the person chose not to be named.
 * @param location    null when not given.
 */
public record FeedbackResponse(
        UUID id,
        short rating,
        String message,
        String displayName,
        String location) {

    static FeedbackResponse of(Feedback feedback) {
        return new FeedbackResponse(
                feedback.getId(),
                feedback.getRating(),
                feedback.getMessage(),
                feedback.getDisplayName(),
                feedback.getLocation());
    }
}
