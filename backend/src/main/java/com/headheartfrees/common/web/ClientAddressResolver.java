package com.headheartfrees.common.web;

import com.headheartfrees.config.RateLimitProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Works out which address a request should be rate limited against.
 *
 * <h2>The rule</h2>
 *
 * {@code X-Forwarded-For} is honoured <strong>only</strong> when the immediate
 * peer — {@code getRemoteAddr()} — is one of the addresses configured in
 * {@code app.rate-limit.trusted-proxies}. Otherwise the header is ignored
 * entirely and the peer address is used.
 *
 * <h2>Why not simply trust the header</h2>
 *
 * An unconditionally trusted {@code X-Forwarded-For} is <em>worse than having
 * no header support at all</em>. It is client-supplied, so a caller who wants
 * to defeat the limiter sets a different random value on every request, gets a
 * fresh bucket each time, and the 5/min on login stops existing. The current
 * behaviour — everyone behind the proxy sharing one bucket — is a denial of
 * service against legitimate users; blind trust is a complete bypass for the
 * attacker. Neither is acceptable, and they are fixed by different halves of
 * this class.
 *
 * <h2>Why the last entry, counting from the right</h2>
 *
 * {@code X-Forwarded-For} is a list, appended to by each hop:
 * {@code client, proxy1, proxy2}. Everything to the left of the entry added by
 * <em>our</em> proxy is attacker-controlled — a client can send a header with
 * fabricated entries and the proxy will append to it rather than replace it.
 *
 * <p>So this walks from the <strong>right</strong>, skipping addresses that are
 * themselves trusted proxies, and takes the first one that is not. With one
 * proxy configured that is the rightmost entry, which is the only value that
 * proxy actually observed. Taking the leftmost entry — the common
 * implementation — is precisely the bypass described above, because the
 * leftmost value is whatever the client typed.
 *
 * <h2>Default</h2>
 *
 * The trusted list is <strong>empty by default</strong>, which reproduces the
 * previous behaviour exactly: no header is ever honoured. A deployment behind a
 * proxy must name that proxy's address deliberately. Getting this wrong in the
 * safe direction costs shared buckets; getting it wrong in the unsafe direction
 * costs the limiter, so the default is the safe one.
 */
@Component
public class ClientAddressResolver {

    private static final String FORWARDED_FOR = "X-Forwarded-For";

    private final RateLimitProperties properties;

    ClientAddressResolver(RateLimitProperties properties) {
        this.properties = properties;
    }

    /**
     * The address to key a bucket on.
     *
     * @return never {@code null}; falls back to the peer address, and to
     *         {@code "unknown"} only if the container supplies none.
     */
    public String resolve(HttpServletRequest request) {
        String peer = request.getRemoteAddr();

        if (peer == null || !properties.trusts(peer)) {
            // Not behind a proxy we know about, so the header - if any - is
            // whatever the caller decided to send. Ignore it.
            return peer == null ? "unknown" : peer;
        }

        String header = request.getHeader(FORWARDED_FOR);
        if (header == null || header.isBlank()) {
            return peer;
        }

        String[] hops = header.split(",");
        for (int i = hops.length - 1; i >= 0; i--) {
            String candidate = hops[i].strip();
            if (candidate.isEmpty()) {
                continue;
            }
            if (!properties.trusts(candidate)) {
                return candidate;
            }
        }

        // Every entry was a trusted proxy, which means no client address was
        // ever recorded. The peer is the most specific thing left.
        return peer;
    }
}
