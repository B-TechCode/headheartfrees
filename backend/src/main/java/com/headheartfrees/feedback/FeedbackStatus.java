package com.headheartfrees.feedback;

/**
 * Where a submission is in moderation.
 *
 * <p>Stored as the name, matching the {@code feedback_status_allowed} CHECK
 * constraint. Nothing reaches {@code /voices} except {@link #APPROVED}.
 */
public enum FeedbackStatus {

    /** Submitted, not yet read. The only status a submission can be created in. */
    PENDING,

    /** Read and published. The one status the public endpoint will return. */
    APPROVED,

    /**
     * Read and not published, or published and then withdrawn.
     *
     * <p>The row is kept rather than deleted: Community Guidelines promise that
     * people can ask for removal, and a moderator who has to reverse a decision
     * needs the thing they are reversing to still exist.
     */
    REJECTED
}
