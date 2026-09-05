package com.headheartfrees.vent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * One press of the release button.
 *
 * <p>Three columns, and the absences matter more than the presences: no
 * content, no user id, no IP address, no session or device identifier. A row
 * here records that a release happened and optionally how the person felt. It
 * cannot be traced to a person, and it holds nothing of what they wrote,
 * because what they wrote was never sent.
 *
 * <p>Package-private by design. PROJECT_BRIEF.md section 4 requires each domain
 * to expose only a service interface and DTOs; nothing outside {@code vent} may
 * import this entity, and no other domain may hold a JPA relationship to it.
 */
@Entity
@Table(name = "vent_events")
class VentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Stored as the enum name, matching the {@code vent_events_mood_allowed}
     * CHECK constraint. STRING rather than ORDINAL so that reordering the enum
     * cannot silently rewrite the meaning of existing rows.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "mood")
    private Mood mood;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Required by JPA. */
    protected VentEvent() {
    }

    VentEvent(Mood mood, Instant createdAt) {
        this.mood = mood;
        this.createdAt = createdAt;
    }

    Long getId() {
        return id;
    }

    Mood getMood() {
        return mood;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
