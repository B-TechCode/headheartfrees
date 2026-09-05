package com.headheartfrees.auth;

import java.util.Locale;
import java.util.Set;

/**
 * What counts as an acceptable password.
 *
 * <h2>What this deliberately does not do</h2>
 *
 * No composition rules. No "must contain an uppercase letter, a digit and a
 * symbol". Those rules do not produce strong passwords; they produce
 * {@code Password1!}, because that is the shortest string that satisfies them,
 * and they push people away from the long passphrases that actually resist
 * guessing. Length and a blocklist do more with less.
 *
 * <h2>The maximum, and why Argon2id</h2>
 *
 * The upper bound is {@value #MAX_LENGTH} characters, which exists to bound
 * hashing work per request, not to constrain the user.
 *
 * <p>This is also the reason the project hashes with Argon2id rather than
 * BCrypt. BCrypt silently truncates its input at 72 <em>bytes</em>. Sixty-four
 * ASCII characters fit, so a naive reading says BCrypt satisfies "no maximum
 * below 64" - but 64 characters of Devanagari, or any other multi-byte script,
 * is roughly 192 bytes, and BCrypt would ignore everything past the 72nd. Two
 * different long passphrases would then produce the same hash, and nothing
 * would report an error. Argon2id has no such limit.
 */
final class PasswordPolicy {

    static final int MIN_LENGTH = 12;

    /**
     * A bound on work, not on the user. Argon2id's cost is fixed by its
     * parameters rather than by input length, so this is generous on purpose.
     */
    static final int MAX_LENGTH = 200;

    /**
     * The passwords that appear at the top of every breach corpus.
     *
     * <p>Not a serious dictionary - a real one is megabytes and belongs behind
     * a service call. This is the cheap 90%: it costs one hash-set lookup and
     * stops the handful of strings that a credential-stuffing list tries first.
     * Comparison is case-insensitive, so {@code PASSWORD} is caught too.
     *
     * <p><strong>Most entries are shorter than {@link #MIN_LENGTH} and are
     * already rejected on length before this list is consulted.</strong> They
     * stay for two reasons: the length rule could be lowered later, and a
     * blocklist that silently depends on another rule to do its work is one
     * that stops working without anyone noticing. The entries that actually
     * earn their place are the long ones at the end - a twenty-five character
     * passphrase looks strong and is worthless if it is
     * {@code correcthorsebatterystaple}.
     */
    private static final Set<String> BLOCKED = Set.of(
            "123456", "password", "12345678", "qwerty", "123456789", "12345",
            "1234", "111111", "1234567", "dragon", "123123", "baseball",
            "abc123", "football", "monkey", "letmein", "shadow", "master",
            "666666", "qwertyuiop", "123321", "mustang", "1234567890",
            "michael", "654321", "superman", "1qaz2wsx", "7777777", "121212",
            "000000", "qazwsx", "123qwe", "killer", "trustno1", "jordan",
            "jennifer", "zxcvbnm", "asdfgh", "hunter", "buster", "soccer",
            "harley", "batman", "andrew", "tigger", "sunshine", "iloveyou",
            "charlie", "robert", "thomas", "hockey", "ranger", "daniel",
            "starwars", "klaster", "112233", "george", "computer", "michelle",
            "jessica", "pepper", "1111", "zxcvbn", "555555", "11111111",
            "131313", "freedom", "777777", "pass", "maggie", "159753",
            "aaaaaa", "ginger", "princess", "joshua", "cheese", "amanda",
            "summer", "love", "ashley", "nicole", "chelsea", "biteme",
            "matthew", "access", "yankees", "987654321", "dallas", "austin",
            "thunder", "taylor", "matrix", "mobilemail", "mom", "monitor",
            "monitoring", "montana", "moon", "moscow",
            "password1", "password123", "passw0rd", "p@ssw0rd", "welcome",
            "welcome1", "admin", "administrator", "letmein123", "qwerty123",
            "iloveyou1", "changeme", "secret", "default", "root", "toor",
            // The ones that matter: long enough to pass the length rule, and
            // common enough to be in a stuffing list anyway.
            "correcthorsebatterystaple", "password12345",
            "qwertyuiop123", "iloveyouforever", "letmeinplease",
            "thisisapassword", "passwordpassword", "123456789012",
            "1234567890123", "qwertyuiopasdfgh", "adminadmin123",
            "welcometothejungle", "passwordispassword");

    private PasswordPolicy() {
    }

    /**
     * @throws WeakPasswordException with a reason safe to show the user - every
     *         message here describes the submitted password and reveals nothing
     *         about whether an account exists
     */
    static void validate(String password, String email, String displayName) {
        if (password == null || password.isEmpty()) {
            throw new WeakPasswordException("Password is required.");
        }
        if (password.length() < MIN_LENGTH) {
            throw new WeakPasswordException(
                    "Password must be at least " + MIN_LENGTH + " characters.");
        }
        if (password.length() > MAX_LENGTH) {
            throw new WeakPasswordException(
                    "Password must be at most " + MAX_LENGTH + " characters.");
        }

        String lower = password.toLowerCase(Locale.ROOT);

        if (BLOCKED.contains(lower)) {
            throw new WeakPasswordException(
                    "That password appears in lists of the most commonly used passwords. "
                            + "Please choose another.");
        }

        // Containment, not equality. "aakash-2026-summer" is a weak password for
        // aakash@example.com even though it is neither the email nor the local
        // part on its own, because the local part is the first thing an attacker
        // who knows the address will try as a prefix.
        String localPart = localPartOf(email);
        if (!localPart.isBlank() && lower.contains(localPart)) {
            throw new WeakPasswordException(
                    "Password must not contain the first part of your email address.");
        }

        if (displayName != null && !displayName.isBlank()) {
            String name = displayName.toLowerCase(Locale.ROOT).trim();
            // Two characters would match almost anything; below three this rule
            // rejects far more than it protects.
            if (name.length() >= 3 && lower.contains(name)) {
                throw new WeakPasswordException(
                        "Password must not contain your display name.");
            }
        }
    }

    private static String localPartOf(String email) {
        if (email == null) {
            return "";
        }
        int at = email.indexOf('@');
        String local = at < 0 ? email : email.substring(0, at);
        return local.toLowerCase(Locale.ROOT).trim();
    }
}
