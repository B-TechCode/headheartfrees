package com.headheartfrees.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * The cleanup deletes what cannot prove anything, and nothing else.
 *
 * <p>The row this must never remove is the one that is <strong>revoked but not
 * yet expired</strong>. That population is the entire mechanism of reuse
 * detection: a spent token coming back is only noticed because its row is
 * still there. Delete it early and a stolen token produces a plain 401 while
 * the thief's own token stays live and the family is never revoked.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RefreshTokenCleanupIT extends AuthTestSupport {

    @Autowired
    private RefreshTokenRepository refreshTokens;

    @Autowired
    private RefreshTokenCleanup cleanup;

    @Autowired
    private UserAccountRepository users;

    /** Writes a row directly, so expiry and revocation can be placed in time. */
    private String given(Instant expiresAt, Instant revokedAt) {
        // Identified by its hash rather than its id: the entity exposes no
        // getId(), and findByTokenHash is the lookup the service itself uses.
        String hash = UUID.randomUUID().toString();
        // user_id is NOT NULL with a foreign key, so a real account is needed
        // even though this test is not about the account.
        UUID userId = users
                .save(UserAccount.withPassword(
                        "holder-" + UUID.randomUUID() + "@example.com",
                        "not-a-real-hash", "Holder", Instant.now()))
                .getId();
        RefreshToken token = new RefreshToken(
                userId, hash, UUID.randomUUID(),
                expiresAt, Instant.now().minus(Duration.ofDays(90)));
        if (revokedAt != null) {
            token.revokeAt(revokedAt);
        }
        refreshTokens.save(token);
        return hash;
    }

    @Test
    @DisplayName("a revoked row that has NOT expired survives - reuse detection needs it")
    void keepsRevokedButLive() {
        Instant future = Instant.now().plus(Duration.ofDays(5));
        String hash = given(future, Instant.now().minus(Duration.ofDays(1)));

        cleanup.purgeExpiredTokens();

        assertThat(refreshTokens.findByTokenHash(hash))
                .as("A revoked, unexpired row is exactly what detects a replayed token")
                .isPresent();
    }

    @Test
    @DisplayName("a row expired inside the window survives")
    void keepsRecentlyExpired() {
        // Expired yesterday. The window is the refresh TTL (30 days), so this
        // is still well inside it.
        String hash = given(Instant.now().minus(Duration.ofDays(1)), Instant.now());

        cleanup.purgeExpiredTokens();

        assertThat(refreshTokens.findByTokenHash(hash)).isPresent();
    }

    @Test
    @DisplayName("a revoked row expired past the window is deleted")
    void deletesOldRevoked() {
        String hash = given(
                Instant.now().minus(Duration.ofDays(45)),
                Instant.now().minus(Duration.ofDays(45)));

        cleanup.purgeExpiredTokens();

        assertThat(refreshTokens.findByTokenHash(hash)).isEmpty();
    }

    @Test
    @DisplayName("an UNREVOKED row expired past the window is deleted too")
    void deletesOldUnrevoked() {
        // The clause easy to leave out, and the one that matters for growth:
        // an abandoned session ends with its last token expiring unrevoked,
        // which is the common ending. A rule of "expired AND revoked" would
        // keep these forever and the table would grow exactly as before.
        String hash = given(Instant.now().minus(Duration.ofDays(45)), null);

        cleanup.purgeExpiredTokens();

        assertThat(refreshTokens.findByTokenHash(hash))
                .as("Expired and never revoked still has no detection value")
                .isEmpty();
    }
}
