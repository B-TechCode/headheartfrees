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

    /**
     * Deletes rows whose token expired before {@code cutoff}, revoked or not.
     *
     * <p>Both cases are covered on purpose and the second is the one easy to
     * omit. A revoked-and-expired row is the obvious candidate. An
     * <em>unrevoked</em> expired row is the ending of every abandoned session -
     * the common case - and leaving those behind would leave the table growing
     * exactly as before. Deleting them is safe because an expired token is
     * refused on its expiry whether or not a row is found, so the row has no
     * detection value once it is past the window.
     *
     * <p>What this must never touch is a row that is <strong>revoked but not
     * yet expired</strong>: that is precisely the population
     * {@code RefreshTokenService} reads to notice a spent token coming back.
     * The predicate is on {@code expiresAt} alone, so those rows are outside
     * it by construction. {@code RefreshTokenCleanupIT} pins that.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from RefreshToken t where t.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}
