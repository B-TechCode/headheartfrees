package com.headheartfrees.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * {@link Base32} against the published RFC 4648 test vectors.
 *
 * <p>These are the vectors from section 10 of the RFC, which is the reason it
 * was acceptable to write the encoder rather than take a dependency for it:
 * there is an authoritative answer to check against, and a wrong bit order or a
 * wrong alphabet fails here immediately rather than producing a secret whose
 * codes silently never match.
 *
 * <p>The vectors are given padded in the RFC and this encoder emits none, so
 * the expected values below have the {@code =} removed. {@link Base32#decode}
 * accepts either.
 */
class Base32Test {

    @ParameterizedTest(name = "\"{0}\" encodes to {1}")
    @CsvSource({
        "'',''",
        "f,MY",
        "fo,MZXQ",
        "foo,MZXW6",
        "foob,MZXW6YQ",
        "fooba,MZXW6YTB",
        "foobar,MZXW6YTBOI",
    })
    @DisplayName("RFC 4648 section 10 vectors")
    void rfc4648Vectors(String plain, String encoded) {
        String input = plain == null ? "" : plain;
        String expected = encoded == null ? "" : encoded;

        assertThat(Base32.encode(input.getBytes(StandardCharsets.UTF_8))).isEqualTo(expected);
        assertThat(new String(Base32.decode(expected), StandardCharsets.UTF_8)).isEqualTo(input);
    }

    @Test
    @DisplayName("a round trip survives every length up to a full secret")
    void roundTrips() {
        SecureRandom random = new SecureRandom();
        for (int length = 0; length <= 40; length++) {
            byte[] original = new byte[length];
            random.nextBytes(original);
            assertThat(Base32.decode(Base32.encode(original)))
                    .as("round trip at length %d", length)
                    .isEqualTo(original);
        }
    }

    @Test
    @DisplayName("padding, spaces, dashes and lower case all decode")
    void decodingIsForgiving() {
        byte[] expected = "foobar".getBytes(StandardCharsets.UTF_8);

        // The manual key is displayed in groups of four and gets pasted back
        // with the grouping intact, so this is the ordinary case rather than an
        // edge one.
        assertThat(Base32.decode("MZXW 6YTB OI")).isEqualTo(expected);
        assertThat(Base32.decode("mzxw6ytboi")).isEqualTo(expected);
        assertThat(Base32.decode("MZXW6YTBOI======")).isEqualTo(expected);
        assertThat(Base32.decode("MZXW-6YTB-OI")).isEqualTo(expected);
    }

    @Test
    @DisplayName("a character outside the alphabet is rejected, not silently mapped")
    void rejectsInvalidCharacters() {
        // 0, 1, 8 and 9 are not in the base32 alphabet. Quietly treating an
        // unknown character as zero is how a mistyped manual key becomes a
        // working-looking secret that never produces a matching code.
        assertThatThrownBy(() -> Base32.decode("MZXW0YTB"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("grouping is inserted every four characters and survives a decode")
    void groupingIsCosmetic() {
        String encoded = Base32.encode("foobar".getBytes(StandardCharsets.UTF_8));

        assertThat(Base32.grouped(encoded)).isEqualTo("MZXW 6YTB OI");
        assertThat(Base32.decode(Base32.grouped(encoded)))
                .isEqualTo("foobar".getBytes(StandardCharsets.UTF_8));
    }
}
