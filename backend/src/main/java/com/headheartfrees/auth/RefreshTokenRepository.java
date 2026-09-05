package com.headheartfrees.auth;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Package-private, per PROJECT_BRIEF.md section 4. */
interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findAllByFamilyId(UUID familyId);

    /**
     * Revokes every unrevoked token in a family in one statement.
     *
     * <p>Bulk update rather than load-and-save: this runs on the reuse-detection
     * path, where the point is to close the window fast and the entities are of
     * no further interest. {@code clearAutomatically} keeps the persistence
     * context from later flushing a stale copy over the top.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update RefreshToken t set t.revokedAt = :now "
            + "where t.familyId = :familyId and t.revokedAt is null")
    int revokeFamily(@Param("familyId") UUID familyId, @Param("now") Instant now);
}
