package com.headheartfrees.config;

import java.util.List;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Which immediate peers are allowed to speak for somebody else.
 *
 * <p>Bound from {@code app.rate-limit.*}. See {@code ClientAddressResolver} for
 * the rule and for why an unconditionally trusted {@code X-Forwarded-For} is
 * worse than ignoring the header.
 *
 * @param trustedProxies exact peer addresses, e.g. {@code 172.18.0.1}. Empty by
 *        default, which means no forwarded header is ever honoured — the safe
 *        direction, since the cost is shared buckets rather than no limiter.
 */
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(List<String> trustedProxies) {

    private static final Set<String> LOOPBACK = Set.of("127.0.0.1", "::1", "0:0:0:0:0:0:0:1");

    public RateLimitProperties {
        trustedProxies = trustedProxies == null ? List.of() : List.copyOf(trustedProxies);
        trustedProxies.forEach(entry -> {
            if (entry == null || entry.isBlank()) {
                throw new IllegalArgumentException(
                        "app.rate-limit.trusted-proxies contains a blank entry.");
            }
            if (entry.contains("/")) {
                // CIDR looks like it works and does not: these are compared as
                // strings. Failing at startup beats a range that silently
                // trusts nothing while appearing configured.
                throw new IllegalArgumentException(
                        "app.rate-limit.trusted-proxies does not support CIDR ranges, and got: "
                                + entry + ". List exact addresses.");
            }
            if (entry.equals("*")) {
                throw new IllegalArgumentException(
                        "app.rate-limit.trusted-proxies must not be a wildcard. Trusting every "
                                + "peer means trusting X-Forwarded-For from the client itself, "
                                + "which removes the rate limit entirely.");
            }
        });
    }

    /** Whether {@code address} may speak for a client behind it. */
    public boolean trusts(String address) {
        if (address == null) {
            return false;
        }
        String normalised = address.strip();
        // Loopback is normalised so ::1 and 127.0.0.1 are interchangeable in
        // configuration; a proxy on the same host is the common deployment.
        if (LOOPBACK.contains(normalised)) {
            return trustedProxies.stream().anyMatch(LOOPBACK::contains);
        }
        return trustedProxies.contains(normalised);
    }
}
