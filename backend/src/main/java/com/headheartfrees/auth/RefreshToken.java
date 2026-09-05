package com.headheartfrees.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * One issued refresh token, stored as a digest.
 *
 * <p>Rows are kept after they are spent rather than deleted, because a spent
 * row is what makes reuse detectable: a token that is presented, found, and
 * already revoked means two parties hold tokens descended from one login.
 *
 * <p>{@code userId} is a plain {@link UUID} column and deliberately not a
 * {@code @ManyToOne} to {@link UserAccount}. The database has the foreign key;
 * the object model does not, so this table can move with the module.
 */
@Entity
@Table(name = "refresh_tokens")
class RefreshToken {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    /** SHA-256 hex of the token. The token itself is never stored anywhere. */
    @Column(name = "token_hash", nullable = false, unique = true, updatable = false)
    private String tokenHash;

    /**
     * Shared by every token descended from one login. Revoking a family is how
     * a detected theft logs out both the thief and the victim.
     */
    @Column(name = "family_id", nullable = false, updatable = false)
    private UUID familyId;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Required by JPA. */
    protected RefreshToken() {
    }

    RefreshToken(UUID userId, String tokenHash, UUID familyId, Instant expiresAt, Instant now) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.familyId = familyId;
        this.expiresAt = expiresAt;
        this.createdAt = now;
    }

    UUID getUserId() {
        return userId;
    }

    UUID getFamilyId() {
        return familyId;
    }

    Instant getExpiresAt() {
        return expiresAt;
    }

    boolean isRevoked() {
        return revokedAt != null;
    }

    boolean isExpiredAt(Instant now) {
        return !now.isBefore(expiresAt);
    }

    /** Usable exactly once: not revoked, and not past its expiry. */
    boolean isUsableAt(Instant now) {
        return !isRevoked() && !isExpiredAt(now);
    }

    void revokeAt(Instant now) {
        if (this.revokedAt == null) {
            this.revokedAt = now;
        }
    }
}
