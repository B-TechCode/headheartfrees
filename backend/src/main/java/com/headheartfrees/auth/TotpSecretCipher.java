package com.headheartfrees.auth;

import com.headheartfrees.config.TotpProperties;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * AES-256-GCM over the TOTP secret, so the database alone is not a second
 * factor.
 *
 * <h2>Read this before assuming the secret is safe</h2>
 *
 * <p>A TOTP secret is a credential of exactly the same standing as a password
 * hash, with one difference that makes it worse: it is not a hash. Whoever
 * holds the plaintext secret can generate valid codes for that account
 * forever, silently, without ever touching this application. Stored as a bare
 * base32 string, a database backup left in the wrong bucket would be a complete
 * bypass of the entire feature.
 *
 * <p>So it is encrypted, and here is precisely what that buys and what it does
 * not:
 *
 * <ul>
 *   <li><strong>It does buy</strong> a separation between "someone holds the
 *       database" and "someone holds the database <em>and</em> the
 *       application's environment". A dump, a stolen backup, a read replica, a
 *       cloud console, or SQL injection yields ciphertext and nothing else.
 *   <li><strong>It does not buy</strong> protection from anyone who has the
 *       running application. Root on the app server, read access to its
 *       environment variables, or any path that makes the process print its
 *       own configuration is enough to decrypt every row. The key and the data
 *       are in the same place at runtime, necessarily, because the application
 *       has to be able to verify a code.
 * </ul>
 *
 * <p>That is the honest state. It is a meaningful improvement over plaintext
 * and it is not a vault.
 *
 * <h2>The user id is authenticated, not just the ciphertext</h2>
 *
 * GCM's additional authenticated data is the owner's {@link UUID}. The secret
 * is therefore bound to the account it was enrolled for: copying a ciphertext
 * from one row to another - the obvious move for someone with write access to
 * the database but no read access to the key - produces an authentication tag
 * failure rather than a working second factor on the target account.
 *
 * <h2>Losing the key</h2>
 *
 * There is no recovery. Every enrolled account must re-enrol. Backup codes
 * still work, because they are Argon2id hashes rather than ciphertext, and past
 * those the developer recovery SQL in HANDOVER is what is left. This is
 * documented beside {@code APP_JWT_SECRET} in HANDOVER section 7 for the same
 * reason: it is a secret that must be kept, not one that can be regenerated.
 */
@Component
class TotpSecretCipher {

    /**
     * Versioned so a future key rotation or algorithm change can be told apart
     * from this one at rest, rather than guessed at by length.
     */
    private static final String PREFIX = "v1:";

    /** 96 bits, the size GCM is specified and optimised for. */
    private static final int NONCE_BYTES = 12;

    /** 128 bits, the full tag. */
    private static final int TAG_BITS = 128;

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    TotpSecretCipher(TotpProperties properties) {
        this.key = new SecretKeySpec(properties.decodedEncryptionKey(), "AES");
    }

    /** @return {@code v1:} followed by base64 of nonce ‖ ciphertext ‖ tag */
    String encrypt(byte[] plaintext, UUID owner) {
        byte[] nonce = new byte[NONCE_BYTES];
        random.nextBytes(nonce);

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
            cipher.updateAAD(aad(owner));
            byte[] sealed = cipher.doFinal(plaintext);

            byte[] combined = new byte[nonce.length + sealed.length];
            System.arraycopy(nonce, 0, combined, 0, nonce.length);
            System.arraycopy(sealed, 0, combined, nonce.length, sealed.length);
            return PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (GeneralSecurityException e) {
            // AES-GCM is mandatory on every conformant JVM and the key length
            // was validated at startup, so this is unreachable rather than
            // handled. The message deliberately carries no key material.
            throw new IllegalStateException("Could not encrypt a TOTP secret", e);
        }
    }

    /**
     * @throws IllegalStateException if the row was written with a different
     *         key, a different owner, or has been tampered with. All three are
     *         one failure from here on purpose - the caller cannot act
     *         differently on any of them, and a message that distinguished them
     *         would describe the key to whoever can read logs.
     */
    byte[] decrypt(String stored, UUID owner) {
        if (stored == null || !stored.startsWith(PREFIX)) {
            throw new IllegalStateException(
                    "A user_totp row is not in the expected 'v1:' format.");
        }
        byte[] combined = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
        if (combined.length <= NONCE_BYTES) {
            throw new IllegalStateException("A user_totp row is too short to be valid.");
        }

        byte[] nonce = new byte[NONCE_BYTES];
        System.arraycopy(combined, 0, nonce, 0, NONCE_BYTES);

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
            cipher.updateAAD(aad(owner));
            return cipher.doFinal(combined, NONCE_BYTES, combined.length - NONCE_BYTES);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(
                    "A TOTP secret could not be decrypted. Either APP_TOTP_ENCRYPTION_KEY is not "
                            + "the key this row was written with, or the row has been altered. "
                            + "See HANDOVER section 7 - there is no way to recover the secret "
                            + "without the original key, and the account must re-enrol.", e);
        }
    }

    private static byte[] aad(UUID owner) {
        return owner.toString().getBytes(StandardCharsets.UTF_8);
    }
}
