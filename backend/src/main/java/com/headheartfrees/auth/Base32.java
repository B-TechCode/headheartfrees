package com.headheartfrees.auth;

import java.util.Locale;

/**
 * RFC 4648 base32, the encoding every authenticator app expects a TOTP secret
 * in.
 *
 * <h2>Why this is written here rather than pulled from a library</h2>
 *
 * The instruction behind this phase was not to hand-roll RFC 6238, and it is
 * followed: the code derivation - the HMAC, the dynamic truncation, the time
 * step arithmetic - is {@code java-otp}'s, and none of it is reimplemented
 * here. {@link TotpService} hands that library a {@link javax.crypto.SecretKey}
 * built from raw bytes and never needs base32 at all.
 *
 * <p>Base32 is needed only at the edges: the string a person types into their
 * authenticator by hand, and the {@code secret=} parameter of the
 * {@code otpauth://} URI behind the QR code. It is an alphabet substitution
 * over 5-bit groups with no secret and no timing sensitivity, and the failure
 * mode is loud rather than subtle - a wrong table or a wrong bit order produces
 * a secret whose codes never match, which is caught by the first scan and by
 * {@link Base32Test}'s RFC 4648 section 10 vectors. That is a different class
 * of risk from getting a truncation offset quietly wrong.
 *
 * <p>The alternative was adding commons-codec, which is not on this
 * classpath, for twenty-five lines.
 *
 * <h2>Padding</h2>
 *
 * {@link #encode} emits no {@code =} padding. RFC 4648 permits omitting it when
 * the length is known from context, authenticator apps universally accept the
 * unpadded form, and a {@code =} in a URI query parameter is one more thing to
 * escape. {@link #decode} accepts padding anyway, so a secret pasted from
 * somewhere else still works.
 */
final class Base32 {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private Base32() {
    }

    /** Encodes to unpadded upper-case base32. */
    static String encode(byte[] data) {
        StringBuilder out = new StringBuilder((data.length * 8 + 4) / 5);
        int buffer = 0;
        int bitsHeld = 0;

        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsHeld += 8;
            while (bitsHeld >= 5) {
                bitsHeld -= 5;
                out.append(ALPHABET.charAt((buffer >>> bitsHeld) & 0x1F));
            }
        }
        if (bitsHeld > 0) {
            // Left-align the remaining bits in a 5-bit group, per RFC 4648.
            out.append(ALPHABET.charAt((buffer << (5 - bitsHeld)) & 0x1F));
        }
        return out.toString();
    }

    /**
     * Decodes base32, ignoring padding, spaces and case.
     *
     * <p>Spaces are ignored because the manual-entry key is displayed in groups
     * of four and people paste it back with the grouping intact.
     *
     * @throws IllegalArgumentException on a character outside the alphabet
     */
    static byte[] decode(String encoded) {
        String cleaned = encoded.replace("=", "").replace(" ", "").replace("-", "")
                .toUpperCase(Locale.ROOT);

        byte[] out = new byte[cleaned.length() * 5 / 8];
        int written = 0;
        int buffer = 0;
        int bitsHeld = 0;

        for (int i = 0; i < cleaned.length(); i++) {
            int value = ALPHABET.indexOf(cleaned.charAt(i));
            if (value < 0) {
                throw new IllegalArgumentException("Not base32 at index " + i);
            }
            buffer = (buffer << 5) | value;
            bitsHeld += 5;
            if (bitsHeld >= 8) {
                bitsHeld -= 8;
                out[written++] = (byte) ((buffer >>> bitsHeld) & 0xFF);
            }
        }
        return out;
    }

    /**
     * The manual-entry form: upper case, in groups of four.
     *
     * <p>A 32-character run of unbroken base32 is copied wrongly by hand more
     * often than not, and the grouping is stripped again by {@link #decode}.
     */
    static String grouped(String encoded) {
        StringBuilder out = new StringBuilder(encoded.length() + encoded.length() / 4);
        for (int i = 0; i < encoded.length(); i++) {
            if (i > 0 && i % 4 == 0) {
                out.append(' ');
            }
            out.append(encoded.charAt(i));
        }
        return out.toString();
    }
}
