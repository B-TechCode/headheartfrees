package com.headheartfrees.config;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Everything auth reads from the environment. Bound from {@code app.auth.*}.
 *
 * <p>No default is a usable secret. {@code jwtSecret} has a development default
 * so the test suite and {@code docker compose up} work on a fresh clone, and
 * that default is long enough to satisfy HS256 - which means an install that
 * forgets to set {@code APP_JWT_SECRET} starts successfully with a secret that
 * is public knowledge. That is a deliberate trade for local ergonomics and it
 * is a production hazard; phase 9 should refuse to start on the default outside
 * the {@code local} profile.
 *
 * @param jwtSecret            HS256 key. At least 256 bits once decoded.
 * @param accessTokenTtl       lifetime of the access token in the response body
 * @param refreshTokenTtl      lifetime of the refresh cookie
 * @param cookieSecure         whether the refresh cookie carries {@code Secure}
 * @param adminBootstrapEmails accounts promoted to ADMIN at startup, if they
 *                             already exist. Never creates an account.
 * @param oauth2SuccessRedirect where Google sign-in sends the browser afterwards
 */
@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
        String jwtSecret,
        Duration accessTokenTtl,
        Duration refreshTokenTtl,
        boolean cookieSecure,
        List<String> adminBootstrapEmails,
        String oauth2SuccessRedirect) {

    public AuthProperties {
        accessTokenTtl = accessTokenTtl == null ? Duration.ofMinutes(15) : accessTokenTtl;
        refreshTokenTtl = refreshTokenTtl == null ? Duration.ofDays(30) : refreshTokenTtl;
        adminBootstrapEmails = adminBootstrapEmails == null
                ? List.of()
                : adminBootstrapEmails.stream()
                        .filter(email -> email != null && !email.isBlank())
                        .map(email -> email.trim().toLowerCase(Locale.ROOT))
                        .toList();
        oauth2SuccessRedirect = oauth2SuccessRedirect == null || oauth2SuccessRedirect.isBlank()
                ? "http://localhost:3000/auth/callback"
                : oauth2SuccessRedirect;
    }
}
