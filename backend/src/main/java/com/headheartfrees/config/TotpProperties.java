package com.headheartfrees.config;

import java.time.Duration;
import java.util.Base64;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Everything the second factor reads from the environment. Bound from
 * {@code app.totp.*}.
 *
 * @param encryptionKey   base64, decoding to exactly 32 bytes. AES-256 for the
 *                        stored secret. See {@code TotpSecretCipher} for what
 *                        the encryption does and does not protect against, and
 *                        {@link TotpEncryptionKeyGuard} for why the committed
 *                        default is refused outside the {@code local} profile.
 * @param issuer          the label an authenticator app shows beside the code
 * @param challengeTtl    how long the ticket issued after a correct password
 *                        stays usable. Short: it is a half-finished sign-in,
 *                        not a session.
 * @param driftSteps      how many 30-second steps either side of now are
 *                        accepted. See {@code TotpService} for why this is 1.
 * @param backupCodeCount how many single-use recovery codes are issued at
 *                        enrolment
 */
@ConfigurationProperties(prefix = "app.totp")
public record TotpProperties(
        String encryptionKey,
        String issuer,
        Duration challengeTtl,
        int driftSteps,
        int backupCodeCount) {

    /** AES-256. Not configurable: a shorter key is a weaker cipher, silently. */
    public static final int KEY_BYTES = 32;

    public TotpProperties {
        issuer = issuer == null || issuer.isBlank() ? "HeadHeartFreeS" : issuer.trim();
        challengeTtl = challengeTtl == null ? Duration.ofMinutes(5) : challengeTtl;
        driftSteps = driftSteps <= 0 ? 1 : driftSteps;
        backupCodeCount = backupCodeCount <= 0 ? 10 : backupCodeCount;

        if (driftSteps > 2) {
            // Each extra step multiplies the number of simultaneously valid
            // codes, which is the entire brute-force surface. RFC 6238 section
            // 5.2 suggests at most one. Two is already generous; more is a
            // misconfiguration, and failing at startup is cheaper than
            // discovering it in a log nobody reads.
            throw new IllegalArgumentException(
                    "app.totp.drift-steps is " + driftSteps + ". Each step either side of now "
                            + "adds two more simultaneously valid codes and multiplies the "
                            + "brute-force surface; RFC 6238 section 5.2 suggests at most one. "
                            + "A phone further adrift than 60 seconds fails against every other "
                            + "TOTP site too, and the fix belongs in its clock settings.");
        }
    }

    /**
     * @throws IllegalStateException if the configured value is not base64 or
     *         does not decode to exactly {@link #KEY_BYTES} bytes. Both are
     *         startup failures rather than runtime ones, because the
     *         alternative is an application that starts and then cannot read a
     *         single enrolled account.
     */
    public byte[] decodedEncryptionKey() {
        if (encryptionKey == null || encryptionKey.isBlank()) {
            throw new IllegalStateException(
                    "app.totp.encryption-key is not set. Generate one with: "
                            + "openssl rand -base64 32");
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(encryptionKey.trim());
        } catch (IllegalArgumentException notBase64) {
            throw new IllegalStateException(
                    "app.totp.encryption-key is not valid base64. Generate one with: "
                            + "openssl rand -base64 32", notBase64);
        }
        if (decoded.length != KEY_BYTES) {
            // Deliberately stricter than the JWT secret's "at least 256 bits".
            // That one feeds an HMAC, which accepts any length; this one is an
            // AES key, where 16 or 24 bytes would silently select AES-128 or
            // AES-192 instead of failing.
            throw new IllegalStateException(
                    "app.totp.encryption-key must decode to exactly " + KEY_BYTES + " bytes for "
                            + "AES-256, and got " + decoded.length + ". A shorter key would "
                            + "silently select a weaker cipher. Generate one with: "
                            + "openssl rand -base64 32");
        }
        return decoded;
    }
}
