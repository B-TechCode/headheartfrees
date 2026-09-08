package com.headheartfrees.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * A wildcard CORS origin must be impossible to configure.
 *
 * <p>This API is called with credentials - the refresh token is a cookie - so
 * a permissive origin is not a cosmetic mistake. With Spring's origin
 * <em>patterns</em> a wildcard reflects the caller's own origin back and allows
 * credentials, which is a working cross-origin read of an authenticated API
 * from any site the pattern admits.
 *
 * <p>The configuration here uses {@code setAllowedOrigins}, which does not
 * interpret patterns, so the same value would instead silently match nothing
 * and break the frontend. One failure mode is a hole and the other is an
 * outage; both are invisible from the outside, so the value is refused at
 * startup where somebody is watching.
 */
class CorsWildcardRejectedTest {

    @ParameterizedTest
    @ValueSource(strings = {"*", "https://*.example.com", "http://*", "*://example.com"})
    @DisplayName("any wildcard origin is refused at startup")
    void wildcardsAreRefused(String origin) {
        assertThatThrownBy(() -> new CorsProperties(List.of(origin)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("wildcard");
    }

    @Test
    @DisplayName("an origin without a scheme is refused")
    void schemeIsRequired() {
        // "example.com" never matches an Origin header, which always carries a
        // scheme. It would fail closed and look like a CORS bug.
        assertThatThrownBy(() -> new CorsProperties(List.of("example.com")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("scheme");
    }

    @Test
    @DisplayName("a trailing slash is refused")
    void trailingSlashIsRefused() {
        assertThatThrownBy(() -> new CorsProperties(List.of("https://example.com/")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("slash");
    }

    @Test
    @DisplayName("a blank entry is refused")
    void blankIsRefused() {
        assertThatThrownBy(() -> new CorsProperties(Arrays.asList("https://a.example", " ")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("real origins are accepted, including the local default")
    void realOriginsAreAccepted() {
        assertThatCode(() -> new CorsProperties(
                List.of("http://localhost:3000", "https://headheartfrees.app")))
                .doesNotThrowAnyException();

        assertThat(new CorsProperties(List.of("https://headheartfrees.app")).allowedOrigins())
                .containsExactly("https://headheartfrees.app");
    }
}
