package com.headheartfrees.config;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Registers the Google client only when it is actually configured.
 *
 * <h2>Why this class exists at all</h2>
 *
 * The obvious approach - declaring the registration in {@code application.yml}
 * with {@code client-id: ${GOOGLE_CLIENT_ID:}} - does not work, and fails in a
 * way worth recording. Spring Boot's {@code OAuth2ClientProperties.validate()}
 * runs over every <em>declared</em> registration and throws
 * {@code "Client id of registration 'google' must not be empty"} when one has a
 * blank id. Declaring the block with an empty default therefore guarantees the
 * exact startup failure the requirement was written to prevent: no
 * {@code GOOGLE_CLIENT_ID}, no application - and password login, which has
 * nothing to do with Google, goes down with it.
 *
 * <p>The registration has to be genuinely <em>absent</em>, not present and
 * empty. YAML cannot express that conditionally, so it is done here, before the
 * properties are bound and validated.
 *
 * <h2>Why not just use the long property names</h2>
 *
 * An operator could set
 * {@code SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID} directly
 * and skip all of this. This exists so that {@code .env.example} can say
 * {@code GOOGLE_CLIENT_ID} instead, which is the name on the credential in the
 * Google Cloud Console and the name someone would guess.
 *
 * <p>Both variables must be present and non-blank. A client id with no secret
 * is a misconfiguration that would fail later and less clearly, so it is
 * treated as "not configured" here.
 *
 * <h2>How it is registered</h2>
 *
 * {@code META-INF/spring.factories}, under the
 * {@code org.springframework.boot.env.EnvironmentPostProcessor} key, and it has
 * to be that file. It was originally registered in
 * {@code META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports},
 * which is the modern-looking form and is read for {@code AutoConfiguration}
 * only - so nothing loaded this class, from phase 5 until 2026-09-06. Everything
 * below describes behaviour that was correct and unreachable.
 */
public class GoogleOAuth2EnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PREFIX = "spring.security.oauth2.client.registration.google.";
    private static final String SOURCE_NAME = "headheartfrees-google-oauth2";

    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment, SpringApplication application) {

        String clientId = environment.getProperty("GOOGLE_CLIENT_ID");
        String clientSecret = environment.getProperty("GOOGLE_CLIENT_SECRET");

        if (isBlank(clientId) || isBlank(clientSecret)) {
            // Not configured. Leave the registration undeclared so that
            // OAuth2ClientProperties has nothing to validate and Spring builds
            // no ClientRegistrationRepository - which is the state
            // SecurityConfig's ObjectProvider is written to handle, and the
            // state OAuth2AbsentConfigIT asserts.
            return;
        }

        Map<String, Object> properties = new HashMap<>();
        properties.put(PREFIX + "client-id", clientId);
        properties.put(PREFIX + "client-secret", clientSecret);
        properties.put(PREFIX + "scope", "openid,email,profile");
        // Named so the consent screen and any Google-side logging show
        // something recognisable rather than "google".
        properties.put(PREFIX + "client-name", "Google");

        // addLast: anything set explicitly, including a test's
        // @TestPropertySource, still wins over these defaults.
        environment.getPropertySources().addLast(new MapPropertySource(SOURCE_NAME, properties));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
