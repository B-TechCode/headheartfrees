package com.headheartfrees.auth;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * {@link PasswordPolicy}, with no Spring context. The rules are pure functions
 * and testing them through HTTP would only make the failures harder to read.
 */
class PasswordPolicyTest {

    private static final String EMAIL = "aakash@example.com";
    private static final String NAME = "Aakash";

    @Nested
    @DisplayName("length")
    class Length {

        @Test
        @DisplayName("twelve characters is accepted, eleven is not")
        void boundary() {
            assertThatCode(() -> PasswordPolicy.validate("abcdefghijkl", EMAIL, NAME))
                    .doesNotThrowAnyException();

            assertThatThrownBy(() -> PasswordPolicy.validate("abcdefghijk", EMAIL, NAME))
                    .isInstanceOf(WeakPasswordException.class)
                    .hasMessageContaining("at least 12");
        }

        @Test
        @DisplayName("a 64-character password is accepted, which is the point of Argon2id")
        void sixtyFourIsFine() {
            assertThatCode(() -> PasswordPolicy.validate("a".repeat(64), EMAIL, NAME))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("64 characters of a multi-byte script is accepted too")
        void multiByteIsNotTruncated() {
            // 64 Devanagari characters is roughly 192 bytes. BCrypt would have
            // silently ignored everything past the 72nd; this test is the
            // reason the project does not use BCrypt.
            assertThatCode(() -> PasswordPolicy.validate("क".repeat(64), EMAIL, NAME))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("the maximum is a work bound, not a policy, and sits far above 64")
        void maximum() {
            assertThatCode(() -> PasswordPolicy.validate("a".repeat(200), EMAIL, NAME))
                    .doesNotThrowAnyException();

            assertThatThrownBy(() -> PasswordPolicy.validate("a".repeat(201), EMAIL, NAME))
                    .isInstanceOf(WeakPasswordException.class)
                    .hasMessageContaining("at most 200");
        }
    }

    @Nested
    @DisplayName("no composition rules")
    class NoCompositionRules {

        @ParameterizedTest
        @DisplayName("a long all-lowercase passphrase is accepted")
        @ValueSource(strings = {
            "the quiet part out loud",
            "seventeen bicycles waiting",
            "purple monday afternoon rain",
        })
        void passphrasesAreFine(String password) {
            assertThatCode(() -> PasswordPolicy.validate(password, EMAIL, NAME))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("nothing forces a digit, a symbol or an uppercase letter")
        void noClassRequirements() {
            // If this test ever fails, someone has added a composition rule.
            // The reason not to is in the PasswordPolicy class comment.
            assertThatCode(() -> PasswordPolicy.validate("aaaaaaaaaaaaaaa", EMAIL, NAME))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("blocklist")
    class Blocklist {

        @ParameterizedTest
        @DisplayName("a long common password is rejected by the blocklist, regardless of case")
        @ValueSource(strings = {
            "correcthorsebatterystaple",
            "CorrectHorseBatteryStaple",
            "passwordpassword",
            "PasswordPassword",
            "thisisapassword",
            "qwertyuiopasdfgh",
        })
        void longCommonPasswordsRejected(String password) {
            // These are all at or above the 12-character minimum, so the
            // blocklist is genuinely what rejects them. That is the case worth
            // testing: a short common password never reaches this rule.
            assertThatThrownBy(() -> PasswordPolicy.validate(password, EMAIL, NAME))
                    .isInstanceOf(WeakPasswordException.class)
                    .hasMessageContaining("commonly used");
        }

        @ParameterizedTest
        @DisplayName("a short common password is rejected on length, before the blocklist")
        @ValueSource(strings = {"password", "qwerty", "letmein", "password123"})
        void shortCommonPasswordsRejectedOnLength(String password) {
            // Documents the ordering rather than asserting a specific message
            // twice: these are blocked either way, and which rule fires first
            // is an implementation detail the user never sees.
            assertThatThrownBy(() -> PasswordPolicy.validate(password, EMAIL, NAME))
                    .isInstanceOf(WeakPasswordException.class)
                    .hasMessageContaining("at least 12");
        }
    }

    @Nested
    @DisplayName("must not contain identity")
    class Identity {

        @Test
        @DisplayName("rejects a password containing the email local part")
        void containsEmailLocalPart() {
            assertThatThrownBy(() -> PasswordPolicy.validate("aakash-summer-2026", EMAIL, NAME))
                    .isInstanceOf(WeakPasswordException.class)
                    .hasMessageContaining("email address");
        }

        @Test
        @DisplayName("rejects a password containing the display name")
        void containsDisplayName() {
            assertThatThrownBy(() ->
                    PasswordPolicy.validate("xxAakashxx-quiet-river", "someone@example.com", NAME))
                    .isInstanceOf(WeakPasswordException.class)
                    .hasMessageContaining("display name");
        }

        @Test
        @DisplayName("the display name check is case-insensitive")
        void displayNameCaseInsensitive() {
            assertThatThrownBy(() ->
                    PasswordPolicy.validate("my-name-is-AAKASH-ok", "someone@example.com", NAME))
                    .isInstanceOf(WeakPasswordException.class)
                    .hasMessageContaining("display name");
        }

        @Test
        @DisplayName("a display name under three characters is not matched")
        void veryShortDisplayNamesAreIgnored() {
            // "Jo" would otherwise reject any password containing "jo", which
            // rejects far more than it protects.
            assertThatCode(() ->
                    PasswordPolicy.validate("enjoying the quiet", "someone@example.com", "Jo"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("a null display name is fine")
        void nullDisplayName() {
            assertThatCode(() ->
                    PasswordPolicy.validate("a quiet long passphrase", EMAIL, null))
                    .doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("null and empty are rejected before anything else")
    void nullAndEmpty() {
        assertThatThrownBy(() -> PasswordPolicy.validate(null, EMAIL, NAME))
                .isInstanceOf(WeakPasswordException.class);
        assertThatThrownBy(() -> PasswordPolicy.validate("", EMAIL, NAME))
                .isInstanceOf(WeakPasswordException.class);
    }
}
