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
    }
}
