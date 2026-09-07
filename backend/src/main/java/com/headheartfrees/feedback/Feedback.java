package com.headheartfrees.feedback;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * One submitted note.
 *
 * <p>Package-private, like {@code VentEvent} and for the same reason
 * (PROJECT_BRIEF.md section 4): nothing outside this package may hold one.
 *
 * <h2>What is not here</h2>
 *
 * No association to {@code UserAccount}. {@link #userId} is a bare UUID with a
 * database foreign key and no JPA mapping, matching {@code refresh_tokens}. A
 * {@code @ManyToOne} would compile and would quietly couple two modules that
 * are supposed to be separable, and {@code ModuleBoundaryArchitectureTest}
 * fails the build if the import ever appears.
 *
 * <p>No link of any kind to {@code vent_events}. There is no column that could
 * carry one and no timestamp precise enough to infer one - see
 * {@link DefaultFeedbackService} for why {@code createdAt} arrives truncated.
 */
@Entity
@Table(name = "feedback")
class Feedback {

    @Id
    private UUID id;

    /** NULL for an anonymous submission. Not a degraded case; the default one. */
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "location")
    private String location;

    @Column(name = "rating", nullable = false)
    private short rating;

    @Column(name = "message", nullable = false)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FeedbackStatus status;

    @Column(name = "moderated_at")
    private Instant moderatedAt;

    @Column(name = "moderated_by")
    private UUID moderatedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Required by JPA. */
    protected Feedback() {
    }

    Feedback(
            UUID id,
            UUID userId,
            String displayName,
            String location,
            short rating,
            String message,
            Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.displayName = displayName;
        this.location = location;
        this.rating = rating;
        this.message = message;
        // Always PENDING. There is no constructor, setter or request field that
        // can create an already-approved row, so nothing can reach /voices
        // without a moderator having acted on it.
        this.status = FeedbackStatus.PENDING;
        this.createdAt = createdAt;
    }

    /**
     * Records a moderation decision, overwriting any previous one.
     *
     * <p>This is a last-write record, not a history: reversing an approval
     * replaces who and when rather than appending. That is a deliberate limit
     * of this phase and is noted in PHASE_LOG.
     */
    void moderate(FeedbackStatus decision, UUID moderatorId, Instant at) {
        this.status = decision;
        this.moderatedBy = moderatorId;
        this.moderatedAt = at;
    }

    UUID getId() {
        return id;
    }

    UUID getUserId() {
        return userId;
    }

    String getDisplayName() {
        return displayName;
    }

    String getLocation() {
        return location;
    }

    short getRating() {
        return rating;
    }

    String getMessage() {
        return message;
    }

    FeedbackStatus getStatus() {
        return status;
    }

    Instant getModeratedAt() {
        return moderatedAt;
    }

    UUID getModeratedBy() {
        return moderatedBy;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
