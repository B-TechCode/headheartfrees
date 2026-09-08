package com.headheartfrees.auth;

import com.headheartfrees.config.AuthProperties;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deletes refresh-token rows that can no longer prove anything.
 *
 * <h2>The problem</h2>
 *
 * {@code refresh_tokens} gains one row per refresh and nothing removed them.
 * A single active person refreshing every fifteen minutes produces roughly
 * 35,000 rows a year, and none of them were ever read again after their token
 * expired. Left alone the table grows without bound.
 *
 * <h2>Why rows cannot simply be deleted on rotation</h2>
 *
 * Spent rows are the mechanism, not debris. Rotation hands out a new token and
 * marks the old one revoked; if that revoked row is later presented, two
 * parties hold tokens descended from one sign-in and the whole family is
 * revoked. Delete the row and the presentation looks like an unknown token —
 * a plain 401, with the theft undetected and the thief's own token still live.
 * So the retention window is a security parameter, not housekeeping.
 *
 * <h2>The window: 30 days past expiry</h2>
 *
 * {@code APP_JWT_REFRESH_TOKEN_TTL} is 30 days by default, so a row survives
 * its full lifetime plus another 30 — up to 60 days in total. Detection needs
 * the row for the entire period a stolen token could plausibly be replayed,
 * which is bounded by that TTL; the extra month is forensic margin for anyone
 * investigating afterwards. The window is derived from the configured TTL
 * rather than hardcoded, so lengthening the TTL lengthens retention with it.
 *
 * <h2>Two clauses, and why the second one is needed</h2>
 *
 * <ol>
 *   <li><strong>Expired and revoked.</strong> The obvious case: the token is
 *       past its life and has already been superseded or revoked.
 *   <li><strong>Expired and never revoked.</strong> Less obvious and easy to
 *       leave out — which would leave the growth in place, because a session
 *       that is simply abandoned leaves its last token expiring unrevoked, and
 *       that is the common ending for a session. Deleting these is safe: an
 *       expired token is refused on expiry whether or not a row exists, so the
 *       row carries no detection value once it is past the window.
 * </ol>
 *
 * <p>Neither clause touches a row that is revoked but not yet expired, which
 * is exactly the population reuse detection reads.
 */
@Component
class RefreshTokenCleanup {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanup.class);

    private final RefreshTokenRepository refreshTokens;
    private final AuthProperties properties;
    private final Clock clock;

    RefreshTokenCleanup(
            RefreshTokenRepository refreshTokens, AuthProperties properties, Clock clock) {
        this.refreshTokens = refreshTokens;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Runs hourly, and once shortly after startup.
     *
     * <p>{@code fixedDelay} rather than {@code cron}: the work is idempotent
     * and its exact time of day is irrelevant, and a fixed delay cannot pile up
     * if one run is slow. The initial delay lets the application finish
     * starting before it touches the database.
     *
     * <p><strong>Every instance runs this.</strong> With more than one backend
     * they will occasionally run together; the delete is idempotent and racing
     * deletes of the same rows are harmless, so no lock is taken. That is a
     * deliberate limit of a per-instance scheduler and is recorded in
     * PHASE_LOG.
     */
    @Scheduled(initialDelayString = "PT2M", fixedDelayString = "PT1H")
    @Transactional
    public void purgeExpiredTokens() {
        Instant cutoff = Instant.now(clock).minus(properties.refreshTokenTtl());

        int deleted = refreshTokens.deleteExpiredBefore(cutoff);

        if (deleted > 0) {
            // Counts only. No token material, no user ids, no addresses - this
            // line is safe to keep at INFO in an environment serving real
            // people, which is the standard the rest of this codebase holds to.
            log.info("Refresh token cleanup removed {} row(s) expired before {}", deleted, cutoff);
        }
    }
}
