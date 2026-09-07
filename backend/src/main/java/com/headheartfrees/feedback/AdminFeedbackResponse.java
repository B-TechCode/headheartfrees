package com.headheartfrees.feedback;

import java.time.Instant;
import java.util.UUID;

/**
 * One submission as the moderation queue sees it.
 *
 * <p>A separate type from {@link FeedbackResponse} rather than a superset with
 * nullable fields. Two records cost a few lines; one record with a
 * "sometimes populated" status field costs somebody the public endpoint, on
 * the day a refactor moves a null check.
 *
 * <p>{@code userId} is included so a moderator can see whether a note is
 * attributed to an account, which matters when someone writes in asking for
 * their words to be removed. It is an opaque id, not an address: this endpoint
 * does not join to {@code users}, because {@code feedback} may not import an
 * auth type (PROJECT_BRIEF.md section 4).
 */
public record AdminFeedbackResponse(
        UUID id,
        short rating,
        String message,
        String displayName,
        String location,
        FeedbackStatus status,
        UUID userId,
        Instant moderatedAt,
        UUID moderatedBy,
        Instant createdAt) {

    static AdminFeedbackResponse of(Feedback feedback) {
        return new AdminFeedbackResponse(
                feedback.getId(),
                feedback.getRating(),
                feedback.getMessage(),
                feedback.getDisplayName(),
                feedback.getLocation(),
                feedback.getStatus(),
                feedback.getUserId(),
                feedback.getModeratedAt(),
                feedback.getModeratedBy(),
                feedback.getCreatedAt());
    }
}
