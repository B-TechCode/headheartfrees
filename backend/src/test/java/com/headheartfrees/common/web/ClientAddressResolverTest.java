package com.headheartfrees.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.headheartfrees.config.RateLimitProperties;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * X-Forwarded-For is honoured only from a configured proxy, and read from the
 * right.
 *
 * <p>The two failures this pins are opposites. Ignoring the header entirely
 * puts everyone behind a proxy in one bucket, which is a denial of service
 * against real users. Trusting it from anyone removes the limiter completely,
 * because a caller sets a different value per request and gets a fresh bucket
 * each time. The second is much worse, so the default is the first.
 */
class ClientAddressResolverTest {

    private static ClientAddressResolver resolver(String... trusted) {
        return new ClientAddressResolver(new RateLimitProperties(List.of(trusted)));
    }

    private static MockHttpServletRequest request(String peer, String forwardedFor) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(peer);
        if (forwardedFor != null) {
            request.addHeader("X-Forwarded-For", forwardedFor);
        }
        return request;
    }

    @Test
    @DisplayName("with no trusted proxies the header is ignored entirely")
    void headerIgnoredByDefault() {
        // The default configuration. A caller spoofing the header gains
        // nothing: the peer address is what keys the bucket.
        assertThat(resolver().resolve(request("203.0.113.9", "1.2.3.4")))
                .isEqualTo("203.0.113.9");
    }

    @Test
    @DisplayName("a header from an untrusted peer is ignored")
    void untrustedPeerCannotSpeakForOthers() {
        assertThat(resolver("172.18.0.1").resolve(request("203.0.113.9", "1.2.3.4")))
                .isEqualTo("203.0.113.9");
    }

    @Test
    @DisplayName("a header from the trusted proxy is honoured")
    void trustedProxyIsHonoured() {
        assertThat(resolver("172.18.0.1").resolve(request("172.18.0.1", "203.0.113.9")))
                .isEqualTo("203.0.113.9");
    }

    @Test
    @DisplayName("the rightmost untrusted entry wins, not the leftmost")
    void readsFromTheRight() {
        // The bypass this prevents. Everything left of the entry our own proxy
        // appended is client-supplied: a caller sends "1.2.3.4" and the proxy
        // appends the real address to it. Taking the leftmost value - the
        // common implementation - would key the bucket on whatever the caller
        // typed, and varying it per request removes the limit.
        assertThat(resolver("172.18.0.1").resolve(
                        request("172.18.0.1", "1.2.3.4, 203.0.113.9")))
                .isEqualTo("203.0.113.9");
    }

    @Test
    @DisplayName("trusted proxies in the chain are skipped")
    void skipsTrustedHops() {
        assertThat(resolver("172.18.0.1", "10.0.0.1").resolve(
                        request("172.18.0.1", "203.0.113.9, 10.0.0.1")))
                .isEqualTo("203.0.113.9");
    }

    @Test
    @DisplayName("a chain of nothing but trusted proxies falls back to the peer")
    void allTrustedFallsBackToPeer() {
        assertThat(resolver("172.18.0.1", "10.0.0.1").resolve(
                        request("172.18.0.1", "10.0.0.1")))
                .isEqualTo("172.18.0.1");
    }

    @Test
    @DisplayName("an empty or absent header falls back to the peer")
    void emptyHeaderFallsBack() {
        assertThat(resolver("172.18.0.1").resolve(request("172.18.0.1", null)))
                .isEqualTo("172.18.0.1");
        assertThat(resolver("172.18.0.1").resolve(request("172.18.0.1", "   ")))
                .isEqualTo("172.18.0.1");
    }

    @Test
    @DisplayName("loopback forms are interchangeable in configuration")
    void loopbackIsNormalised() {
        assertThat(resolver("127.0.0.1").resolve(request("::1", "203.0.113.9")))
                .isEqualTo("203.0.113.9");
    }
}
