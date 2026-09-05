package com.headheartfrees.common.web;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Per-caller token buckets, held in memory.
 *
 * <h2>How the caller is identified, and what happens to that</h2>
 *
 * The key is {@code request.getRemoteAddr()}, hashed with SHA-256 before it is
 * used. Two deliberate decisions sit behind that:
 *
 * <p><strong>{@code X-Forwarded-For} is not trusted.</strong> There is no
 * reverse proxy in front of this application yet, so any such header would be
 * client-supplied and a caller could bypass the limit entirely by varying it.
 * The cost of ignoring it is real and is documented rather than hidden: under
 * {@code docker compose} every request arrives from the Docker gateway address,
 * so all users behind it share one bucket. That is acceptable for local work
 * and is not acceptable in production — phase 9 should configure a trusted
 * proxy (Spring's {@code server.forward-headers-strategy}) at the same time it
 * introduces the real reverse proxy, and not before.
 *
 * <p><strong>The address is hashed, not stored raw.</strong> The limiter needs
 * to tell callers apart, which a hash does; it never needs to know who they
 * are, which a hash prevents. A heap dump of this process therefore contains no
 * visitor IP addresses. Nothing here is written to a database, a log or a
 * metric — the map lives in memory, and the address itself is discarded inside
 * this method.
 *
 * <p>The map is unbounded, which is a known limitation: a large enough spread
 * of source addresses grows it without limit. Acceptable at this scale and
 * flagged for phase 9, where an eviction policy or a shared store belongs.
 */
@Component
public class ClientIpRateLimiter {

    /** PROJECT_BRIEF.md section 6: 30 releases per minute, per IP. */
    private static final int RELEASE_CAPACITY = 30;
    private static final Duration RELEASE_WINDOW = Duration.ofMinutes(1);

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Attempts to consume one token for the caller behind {@code request}.
     *
     * @return the probe, carrying whether it succeeded and how long until a
     *         token is next available
     */
    public ConsumptionProbe tryConsume(HttpServletRequest request) {
        String key = hash(request.getRemoteAddr());
        return buckets.computeIfAbsent(key, unused -> newBucket()).tryConsumeAndReturnRemaining(1);
    }

    /** Visible for tests, which need a clean limiter between cases. */
    public void reset() {
        buckets.clear();
    }

    private static Bucket newBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(RELEASE_CAPACITY)
                        .refillGreedy(RELEASE_CAPACITY, RELEASE_WINDOW)
                        .build())
                .build();
    }

    /**
     * SHA-256 of the address. Not salted: a salt would add nothing here, since
     * the value never leaves this process and is never compared against anything
     * outside it.
     */
    private static String hash(String remoteAddress) {
        String value = remoteAddress == null ? "unknown" : remoteAddress;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandated by the JLS; unreachable on any conformant JVM.
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
