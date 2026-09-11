package com.headheartfrees.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A registered person.
 *
 * <p>Named {@code UserAccount} rather than {@code User} because
 * {@code org.springframework.security.core.userdetails.User} is a type this
 * package unavoidably reads about, and two {@code User}s in one codebase is a
 * long-running source of wrong imports.
 *
 * <p>Package-private, per PROJECT_BRIEF.md section 4: the {@code auth} module
 * exposes {@link AuthService} and DTOs, never its entities. Other domains
 * reference a person by {@link UUID} and hold no JPA relationship to this
 * class. {@code ModuleBoundaryArchitectureTest} enforces that against the
 * {@code vent} package specifically.
 *
 * <p><strong>This class is never serialised to a response.</strong>
 * {@link #passwordHash} would go with it. Controllers return {@link UserSummary};
 * {@code AuthResponseLeakageIT} asserts no auth response body ever contains a
 * hash.
 */
@Entity
@Table(name = "users")
class UserAccount {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * CITEXT in Postgres, so equality and the unique constraint are already
     * case-insensitive at the database. Stored as written rather than
     * lowercased, so a person's own capitalisation survives being shown back
     * to them.
     */
    /**
     * {@code columnDefinition} is required, not decorative. Without it
     * Hibernate's {@code ddl-auto: validate} expects {@code varchar(255)},
     * finds {@code citext}, and refuses to start the application - which is
     * exactly the check working as intended, since it cannot know the two are
     * compatible unless told.
     */
    @Column(name = "email", nullable = false, unique = true, columnDefinition = "citext")
    private String email;

    /** Null for a Google-only account. Never leaves this package. */
    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "google_id", unique = true)
    private String googleId;

    @Column(name = "display_name")
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Required by JPA. */
    protected UserAccount() {
    }

    private UserAccount(
            UUID id,
            String email,
            String passwordHash,
            String googleId,
            String displayName,
            Instant now) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.googleId = googleId;
        this.displayName = displayName;
        // Always USER. There is no constructor parameter for role, so no call
        // site can create an admin, which is a stronger guarantee than
        // remembering not to pass one.
        this.role = UserRole.USER;
        this.emailVerified = false;
        this.createdAt = now;
        this.updatedAt = now;
    }

    static UserAccount withPassword(String email, String passwordHash, String displayName, Instant now) {
        return new UserAccount(UUID.randomUUID(), email, passwordHash, null, displayName, now);
    }

    static UserAccount fromGoogle(String email, String googleId, String displayName, Instant now) {
        return new UserAccount(UUID.randomUUID(), email, null, googleId, displayName, now);
    }

    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }

    UUID getId() {
        return id;
    }

    String getEmail() {
        return email;
    }

    String getPasswordHash() {
        return passwordHash;
    }

    String getGoogleId() {
        return googleId;
    }

    String getDisplayName() {
        return displayName;
    }

    UserRole getRole() {
        return role;
    }

    boolean isEmailVerified() {
        return emailVerified;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    /** Links an existing password account to Google on first Google sign-in. */
    void linkGoogle(String newGoogleId) {
        this.googleId = newGoogleId;
    }

    /**
     * The one mutation that can produce an ADMIN, called only by
     * {@link AdminBootstrap} from a startup environment variable.
     */
    void assignRole(UserRole newRole) {
        this.role = newRole;
    }

    /**
     * @param totpEnabled           whether a confirmed second factor exists
     * @param backupCodesRemaining  unused recovery codes
     *
     * <p>The second-factor state is passed in rather than read here. This
     * entity deliberately knows nothing about {@code user_totp}: a JPA
     * association would drag a credential into every {@code /me} and every
     * refresh, and the two tables are separate for exactly that reason.
     */
    UserSummary toSummary(boolean totpEnabled, long backupCodesRemaining) {
        return new UserSummary(
                id,
                email,
                displayName,
                role,
                emailVerified,
                createdAt,
                totpEnabled,
                // One source of truth for "who must hold a second factor".
                // Inlining `role == ADMIN` here would be a second place to
                // update the day that policy changes, and the kind of second
                // place nobody finds.
                TotpService.isRequiredFor(role),
                backupCodesRemaining);
    }
}
