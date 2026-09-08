package com.headheartfrees.common.web;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
 * <p><strong>{@code X-Forwarded-For} is trusted only from a configured
 * proxy.</strong> Phase 9 moved that decision into
 * {@link ClientAddressResolver}: the header is honoured when the immediate peer
 * is listed in {@code app.rate-limit.trusted-proxies}, and ignored otherwise.
 * The list is empty by default, so an unconfigured deployment behaves exactly
 * as this class did before — every caller behind the Docker gateway shares one
 * bucket. That is a denial of service against real users and it is the safe
 * failure; trusting the header blindly is a complete bypass, and is the unsafe
 * one. See that class for why the address is read from the right.

 * <p><strong>The buckets are per instance.</strong> They live in this
 * process's heap, so two backends behind a load balancer enforce the limit
 * twice over and a caller gets N times the allowance. This holds for a single
 * backend only; a shared store (Bucket4j supports Redis and others) is what
 * changes that, and it is recorded in PHASE_LOG rather than implied away.
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

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private final ClientAddressResolver addresses;

    ClientIpRateLimiter(ClientAddressResolver addresses) {
        this.addresses = addresses;
    }

    /**
     * Attempts to consume one token for the caller behind {@code request},
     * against the given policy.
     *
     * <p>The key is the policy name and the hashed address together, so each
     * policy has its own bucket per caller. Phase 5 added the second policy;
     * before it, this class hard-coded the release limit. Sharing one bucket
     * across policies would mean failed sign-ins consuming a person's ability
     * to vent, which rule 2.2 does not allow.
     *
     * @return the probe, carrying whether it succeeded and how long until a
     *         token is next available
     */
    public ConsumptionProbe tryConsume(HttpServletRequest request, RateLimitPolicy policy) {
        String key = policy.name() + ':' + hash(addresses.resolve(request));
        return buckets.computeIfAbsent(key, unused -> newBucket(policy))
                .tryConsumeAndReturnRemaining(1);
    }

    /** Visible for tests, which need a clean limiter between cases. */
    public void reset() {
        buckets.clear();
    }

    private static Bucket newBucket(RateLimitPolicy policy) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(policy.capacity())
                        .refillGreedy(policy.capacity(), policy.window())
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
