package com.headheartfrees.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link TotpEncryptionKeyGuard}, and the drift check that keeps it honest.
 *
 * <p>The important test here is {@link #theGuardGuardsTheValueThatIsActuallyUsed()}.
 * The guard compares against a copy of the default because YAML cannot
 * reference a Java constant, and a copy is a thing that goes stale: change
 * {@code application.yml} without changing the constant and this class keeps
 * passing every other test while guarding a string nothing uses. That failure
 * is silent, and the consequence is an install encrypting every second factor
 * with a key printed in a public repository.
 *
 * <p>Modelled directly on {@code JwtSecretGuardTest}, which exists for the same
 * reason.
 */
class TotpEncryptionKeyGuardTest {

    @Test
    @DisplayName("the committed default is refused outside the local profile")
    void refusesTheCommittedDefault() {
        assertThatThrownBy(() -> TotpEncryptionKeyGuard.verify(
                        TotpEncryptionKeyGuard.COMMITTED_DEFAULT, List.of("production")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_TOTP_ENCRYPTION_KEY")
                // The message has to say that losing it is unrecoverable,
                // because the operator reading it is about to generate one and
                // is the only person who will ever be able to keep it.
                .hasMessageContaining("cannot be regenerated");
    }

    @Test
    @DisplayName("no profile at all is also refused")
    void refusesWhenNoProfileIsSet() {
        // The install that forgot the profile is the same install that forgot
        // the key. Refusing is the safe direction.
        assertThatThrownBy(() -> TotpEncryptionKeyGuard.verify(
                        TotpEncryptionKeyGuard.COMMITTED_DEFAULT, List.of()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("the committed default is allowed under the local profile")
    void allowsTheDefaultLocally() {
        assertThatCode(() -> TotpEncryptionKeyGuard.verify(
                        TotpEncryptionKeyGuard.COMMITTED_DEFAULT, List.of("local")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("a key of the operator's own is allowed anywhere")
    void allowsAConfiguredKey() {
        assertThatCode(() -> TotpEncryptionKeyGuard.verify(
                        "c29tZXRoaW5nLWVsc2UtZW50aXJlbHktMzIteC1ieXRlcw==", List.of("production")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("the guard guards the value application.yml actually uses")
    void theGuardGuardsTheValueThatIsActuallyUsed() throws IOException {
        String yaml = Files.readString(
                Path.of("src/main/resources/application.yml"), StandardCharsets.UTF_8);

        assertThat(yaml)
                .as("TotpEncryptionKeyGuard.COMMITTED_DEFAULT has drifted from the default in "
                        + "application.yml. The guard is now watching a string nothing uses, and "
                        + "an install that forgets APP_TOTP_ENCRYPTION_KEY will start happily on "
                        + "a key that is public in this repository.")
                .contains("APP_TOTP_ENCRYPTION_KEY:" + TotpEncryptionKeyGuard.COMMITTED_DEFAULT);
    }

    @Test
    @DisplayName("the committed default is a valid AES-256 key, so only the guard stops it")
    void theDefaultIsOtherwiseUsable() {
        // If the default were malformed, the application would fail for a
        // different reason and this guard would never be the thing that caught
        // it - which would make every test above pass while proving nothing.
        assertThat(new TotpProperties(
                        TotpEncryptionKeyGuard.COMMITTED_DEFAULT, null, null, 1, 10)
                        .decodedEncryptionKey())
                .hasSize(TotpProperties.KEY_BYTES);
    }
}
