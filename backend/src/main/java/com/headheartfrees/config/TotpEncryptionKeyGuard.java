package com.headheartfrees.config;

import java.util.Collection;
import java.util.List;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Refuses to start when the TOTP secrets are encrypted with the key that is
 * committed to this repository.
 *
 * <p>Exactly the shape of {@link JwtSecretGuard}, for exactly its reason, and
 * written as a second class rather than folded into it because the two secrets
 * fail differently and the operator needs to be told which one is wrong.
 *
 * <p>{@code application.yml} gives {@code app.totp.encryption-key} a working
 * default so a fresh clone and the test suite run with no configuration. That
 * default is a valid 32-byte key, so nothing downstream rejects it - an install
 * that never sets {@code APP_TOTP_ENCRYPTION_KEY} starts happily and encrypts
 * every second factor with a key printed in this repository, which is the same
 * as not encrypting them at all. Reading it plus a database dump is enough to
 * generate valid codes for the admin account indefinitely.
 *
 * <p>The profile, not a cleverer test: "is this production?" has no reliable
 * answer at startup, the profile is the one signal an operator sets
 * deliberately, and the failure direction is safe - an install that set no
 * profile is the same install that forgot the key, and it is refused rather
 * than trusted.
 */
@Component
class TotpEncryptionKeyGuard implements InitializingBean {

    /**
     * The default in {@code application.yml}, duplicated here because YAML
     * cannot reference a Java constant. {@code TotpEncryptionKeyGuardTest}
     * asserts the two are still the same string; if they drift, this class
     * keeps passing while guarding a value nothing uses.
     */
    static final String COMMITTED_DEFAULT = "ZGV2ZWxvcG1lbnQtb25seS10b3RwLWtleS0wMTIzNDU=";

    private final TotpProperties properties;
    private final Environment environment;

    TotpEncryptionKeyGuard(TotpProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        verify(properties.encryptionKey(), List.of(environment.getActiveProfiles()));
    }

    static void verify(String configuredKey, Collection<String> activeProfiles) {
        if (!COMMITTED_DEFAULT.equals(configuredKey)
                || activeProfiles.contains(JwtSecretGuard.LOCAL_PROFILE)) {
            return;
        }
        throw new IllegalStateException(
                "app.totp.encryption-key is still the development default from application.yml, "
                        + "and the active profile is not '" + JwtSecretGuard.LOCAL_PROFILE
                        + "'. That default is committed to the repository, so anyone who can read "
                        + "the source and obtain a database dump can decrypt every TOTP secret and "
                        + "generate valid codes for the admin account indefinitely. Set "
                        + "APP_TOTP_ENCRYPTION_KEY to a key of your own: openssl rand -base64 32. "
                        + "KEEP IT - it cannot be regenerated, and losing it forces every enrolled "
                        + "account to re-enrol. See HANDOVER section 7.");
    }
}
