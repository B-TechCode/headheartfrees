package com.headheartfrees.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Browser origins permitted to call the API with credentials.
 *
 * <p>Bound from {@code app.cors.*}. The list is explicit rather than a wildcard
 * because the refresh token travels in a cookie, and credentialed CORS requests
 * cannot use {@code *}.
 *
 * @param allowedOrigins exact origins, e.g. {@code https://headheartfrees.app}
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
        allowedOrigins.forEach(CorsProperties::validate);
    }

    /**
     * Refuses to start on an origin that would widen credentialed CORS.
     *
     * <h2>Why this is a startup failure and not a warning</h2>
     *
     * {@code Access-Control-Allow-Origin: *} together with
     * {@code Allow-Credentials: true} is rejected by browsers, so the naive
     * mistake fails loudly on its own. The dangerous version is subtler:
     * Spring's {@code allowedOriginPatterns} accepts wildcards like
     * {@code https://*.example.com} or {@code *} and <em>reflects</em> the
     * caller's origin back with credentials allowed — which is a working
     * cross-origin read of an authenticated API, from any site the pattern
     * admits.
     *
     * <p>This configuration deliberately uses {@code setAllowedOrigins}, which
     * does not interpret patterns, so a {@code *} here would simply never
     * match and CORS would silently stop working. Either way the value is
     * wrong, and both failure modes are hard to spot from the outside: one is
     * a hole, the other is an outage. Refusing to boot turns both into a
     * message at deploy time.
     *
     * <p>{@code CorsWildcardRejectedTest} pins every form of this.
     */
    private static void validate(String origin) {
        if (origin == null || origin.isBlank()) {
            throw new IllegalArgumentException(
                    "app.cors.allowed-origins contains a blank entry. List exact origins, "
                            + "e.g. https://headheartfrees.app");
        }
        if (origin.contains("*")) {
            throw new IllegalArgumentException(
                    "app.cors.allowed-origins must not contain a wildcard, and got: " + origin
                            + ". The refresh token is a cookie, so this API is called with "
                            + "credentials; a wildcard origin is either rejected by the browser "
                            + "or, with origin patterns, reflects the caller back and allows any "
                            + "matching site to read authenticated responses. List exact origins.");
        }
        if (!origin.startsWith("http://") && !origin.startsWith("https://")) {
            throw new IllegalArgumentException(
                    "app.cors.allowed-origins entries must be full origins including the scheme, "
                            + "and got: " + origin);
        }
        if (origin.endsWith("/")) {
            // An Origin header never has a trailing slash, so this would never
            // match and CORS would fail with no obvious cause.
            throw new IllegalArgumentException(
                    "app.cors.allowed-origins entries must not end in a slash, and got: " + origin);
        }
    }
}
