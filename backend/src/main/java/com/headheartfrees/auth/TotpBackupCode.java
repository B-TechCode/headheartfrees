package com.headheartfrees.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * One single-use recovery code, stored as an Argon2id hash.
 *
 * <p>Consuming a code marks it rather than deleting it, so {@code /account} can
 * say honestly how many remain and so the database distinguishes a reused code
 * from an unknown one. The <em>response</em> never distinguishes them - see
 * {@link TotpService}.
 */
@Entity
@Table(name = "user_totp_backup_code")
class TotpBackupCode {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "code_hash", nullable = false, updatable = false)
    private String codeHash;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Required by JPA. */
    protected TotpBackupCode() {
    }

    TotpBackupCode(UUID userId, String codeHash, Instant now) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.codeHash = codeHash;
        this.createdAt = now;
    }

    String getCodeHash() {
        return codeHash;
    }

    boolean isUsed() {
        return usedAt != null;
    }

    void markUsed(Instant now) {
        this.usedAt = now;
    }
}
