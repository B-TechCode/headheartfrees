package com.headheartfrees.config;

import java.util.Collection;
import java.util.List;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Refuses to start when the application is running on the JWT secret that is
 * committed to this repository.
 *
 * <h2>What this defends</h2>
 *
 * {@code application.yml} gives {@code app.auth.jwt-secret} a working default
 * so that a fresh clone and the test suite both run with no configuration. That
 * default is long enough for HS256, so an install that never sets
 * {@code APP_JWT_SECRET} does not fail - it starts, and it signs tokens with a
 * key that anyone can read on GitHub. Reading it is enough to mint a valid
 * access token for any user id, with {@code role} set to {@code ADMIN}. No
 * password, no database access and no request to the server is needed, and
 * nothing in a log would distinguish the forged token from a real one.
 *
 * <p>The trade was accepted for local ergonomics and recorded as an open item
 * in phase 5. This closes it by keeping the default and narrowing where it is
 * allowed: under the {@code local} profile it is a convenience, and anywhere
 * else it is a startup failure.
 *
 * <h2>Why the profile, and not something cleverer</h2>
 *
 * "Is this production?" has no reliable answer at startup. The profile is the
 * one signal the operator sets deliberately, {@code docker-compose.yml} already
 * defaults it to {@code local}, and the failure direction is the safe one: an
 * install that sets no profile at all - the exact install that also forgot
 * {@code APP_JWT_SECRET} - is refused rather than trusted.
 *
 * <p>The check fires on the configured value, not on where it came from. Anyone
 * who copies the default into {@code APP_JWT_SECRET}, an {@code .env} file or a
 * secret manager gets the same refusal, which is right: the secret is public
 * wherever it is typed.
 */
@Component
class JwtSecretGuard implements InitializingBean {

    /**
     * The default in {@code application.yml}, duplicated here because YAML
     * cannot reference a Java constant. {@code JwtSecretGuardTest} asserts the
     * two are still the same string - if they drift, this class keeps passing
     * while guarding a value nothing uses.
     */
    static final String COMMITTED_DEFAULT =
            "ZGV2ZWxvcG1lbnQtb25seS1zZWNyZXQtbm90LWZvci1hbnl0aGluZy1yZWFsLTAxMjM0NTY3ODk=";

    static final String LOCAL_PROFILE = "local";

    private final AuthProperties properties;
    private final Environment environment;

    JwtSecretGuard(AuthProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        verify(properties.jwtSecret(), List.of(environment.getActiveProfiles()));
    }

    /**
     * @param configuredSecret the bound value of {@code app.auth.jwt-secret}
     * @param activeProfiles   the profiles Spring resolved; empty when none
     *                         were set, which is not a pass
     * @throws IllegalStateException if the committed default is in use outside
     *         the {@code local} profile
     */
    static void verify(String configuredSecret, Collection<String> activeProfiles) {
        if (!COMMITTED_DEFAULT.equals(configuredSecret) || activeProfiles.contains(LOCAL_PROFILE)) {
            return;
        }
        throw new IllegalStateException(
                "app.auth.jwt-secret is still the development default from application.yml, "
                        + "and the active profile is not '" + LOCAL_PROFILE + "'. That default is "
                        + "committed to the repository, so anyone who can read the source can "
                        + "forge an access token for any account, including an ADMIN one. Set "
                        + "APP_JWT_SECRET to a secret of your own: openssl rand -base64 48");
    }
}
