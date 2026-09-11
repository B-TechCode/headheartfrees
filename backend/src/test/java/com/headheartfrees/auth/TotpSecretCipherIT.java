package com.headheartfrees.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The secret at rest.
 *
 * <p>The honest claim being tested is narrow and worth stating exactly:
 * somebody holding <em>only</em> the database cannot read a TOTP secret.
 * Somebody holding the database and the application's environment can read
 * every one of them. This asserts the first half, which is the half that
 * encryption can actually deliver - see {@link TotpSecretCipher} for the full
 * statement of what it does not buy.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TotpSecretCipherIT extends TotpTestSupport {

    @Autowired
    private TotpSecretCipher cipher;

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("the stored row contains no recognisable secret")
    void theDatabaseHoldsCiphertext() throws Exception {
        Enrolled enrolled = enrolUser("stored@example.com");

        List<String> stored = new JdbcTemplate(dataSource)
                .queryForList("SELECT secret_ciphertext FROM user_totp", String.class);

        assertThat(stored).hasSize(1);

        String ciphertext = stored.get(0);
        String base32 = enrolled.manualKey().replace(" ", "");

        assertThat(ciphertext).startsWith("v1:");
        assertThat(ciphertext)
                .as("A dump, a stolen backup or a read replica must not be a working second "
                        + "factor. This is the whole reason the column is not a base32 string.")
                .doesNotContain(base32);
    }

    @Test
    @DisplayName("a round trip returns the exact bytes")
    void roundTrips() {
        UUID owner = UUID.randomUUID();
        byte[] secret = "a twenty byte secret".getBytes(StandardCharsets.UTF_8);

        assertThat(cipher.decrypt(cipher.encrypt(secret, owner), owner)).isEqualTo(secret);
    }

    @Test
    @DisplayName("two encryptions of the same secret differ")
    void theNonceIsFresh() {
        UUID owner = UUID.randomUUID();
        byte[] secret = "a twenty byte secret".getBytes(StandardCharsets.UTF_8);

        // A reused GCM nonce is catastrophic rather than merely untidy: two
        // messages under one nonce leak their XOR and destroy the
        // authentication guarantee. Equal ciphertexts would be the visible
        // symptom.
        assertThat(cipher.encrypt(secret, owner)).isNotEqualTo(cipher.encrypt(secret, owner));
    }

    @Test
    @DisplayName("a row cannot be moved from one account to another")
    void theOwnerIsAuthenticated() {
        UUID owner = UUID.randomUUID();
        UUID somebodyElse = UUID.randomUUID();
        byte[] secret = "a twenty byte secret".getBytes(StandardCharsets.UTF_8);

        String ciphertext = cipher.encrypt(secret, owner);

        // The obvious move for somebody with write access to the database and
        // no read access to the key: copy the admin's ciphertext onto an
        // account you control, enrol your own phone against it, sign in.
        // The user id is GCM additional authenticated data, so it fails.
        assertThatThrownBy(() -> cipher.decrypt(ciphertext, somebodyElse))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("a tampered row is refused rather than silently decrypted to rubbish")
    void tamperingIsDetected() {
        UUID owner = UUID.randomUUID();
        String ciphertext =
                cipher.encrypt("a twenty byte secret".getBytes(StandardCharsets.UTF_8), owner);

        // Flip one character of the base64 body.
        char[] chars = ciphertext.toCharArray();
        int index = chars.length - 2;
        chars[index] = chars[index] == 'A' ? 'B' : 'A';

        assertThatThrownBy(() -> cipher.decrypt(new String(chars), owner))
                .isInstanceOf(IllegalStateException.class);
    }
}
