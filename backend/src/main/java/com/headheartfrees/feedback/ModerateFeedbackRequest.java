package com.headheartfrees.feedback;

import jakarta.validation.constraints.NotNull;

/**
 * A moderation decision.
 *
 * <p>{@code PENDING} is rejected: it is the state a submission starts in, not
 * a decision anybody makes. Allowing it would let a moderator return an item
 * to the queue and lose the record of having read it, which is the one thing
 * the audit columns exist to prevent.
 *
 * <p>Either decision is accepted from any current state, so an approval can be
 * reversed and a rejection reconsidered. That is required rather than
 * convenient: Community Guidelines promise people can ask for their words to
 * come down.
 */
public record ModerateFeedbackRequest(

        @NotNull(message = "Please choose APPROVED or REJECTED.")
        Decision status) {

    public enum Decision {
        APPROVED,
        REJECTED;

        FeedbackStatus toStatus() {
            return this == APPROVED ? FeedbackStatus.APPROVED : FeedbackStatus.REJECTED;
        }
    }
}
