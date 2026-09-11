package com.headheartfrees.auth;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import com.headheartfrees.config.TotpProperties;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalLong;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Enrolment, verification and recovery for the second factor.
 *
 * <h2>What is delegated and what is not</h2>
 *
 * The RFC 6238 derivation - HMAC, dynamic truncation, time-step arithmetic - is
 * {@code com.eatthepath:java-otp} and is not reimplemented here. What this
 * class owns is everything the RFC does not specify, and that a naive
 * implementation gets wrong: where the secret lives, how wide the drift window
 * is, what stops a code being replayed, and what stops a million guesses.
 *
 * <h2>The four properties this class is responsible for</h2>
 *
 * <ol>
 *   <li><strong>Enabling requires proof.</strong> {@link #beginSetup} writes an
 *       unconfirmed row that grants nothing. Only {@link #enable}, given a code
 *       derived from that exact secret, turns it on. A mistyped manual key
 *       therefore fails at enrolment, where the person can see it, rather than
 *       at the next sign-in, where it would be a lockout.
 *   <li><strong>Replay is refused.</strong> {@link UserTotp#hasSpent} rejects
 *       every step at or below the last accepted one.
 *   <li><strong>Guessing is bounded.</strong> {@link UserTotp#recordFailure}
 *       runs a compounding lockout ladder. The arithmetic is on that class.
 *   <li><strong>Failures are indistinguishable.</strong> Everything throws
 *       {@link InvalidTotpCodeException}, with {@link TotpLockedException} the
 *       one documented exception.
 * </ol>
 *
 * <h2>Why one method accepts both a code and a backup code</h2>
 *
 * {@link #verifySecondFactor} discriminates on shape: six digits is a TOTP
 * code, anything else is tried as a backup code. Two endpoints would leak which
 * one the caller believed they held, and a "which kind is this?" flag in the
 * request would do the same. One field, one answer.
 *
 * <h2>{@code noRollbackFor} is load-bearing, exactly as on the reuse path</h2>
 *
 * Every failure here records something and then throws: {@link #failAndThrow}
 * increments the attempt counter, sets the lock when it crosses a threshold,
 * saves, and raises {@link InvalidTotpCodeException}. Under the default
 * rollback rules that throw undoes the save in the same transaction - the
 * caller gets a 401, the counter returns to zero, and <strong>the attempt limit
 * becomes decorative</strong>. Six digits behind a limit that does not count is
 * a million guesses at whatever rate the network allows.
 *
 * <p>This is the same trap {@code RefreshTokenService.rotate} documents for
 * reuse detection, and it failed the same way here before the annotation
 * existed: {@code TotpAttemptLimitIT.theSequenceStops} got a sixth 401 where it
 * expected a 429, and {@code aCorrectCodeIsRefusedWhileLocked} got a working
 * sign-in. The exclusion is needed on {@code DefaultAuthService} as well,
 * because with {@code REQUIRED} propagation both share one physical transaction
 * and either marking it rollback-only is enough to lose the write.
 */
@Service
class TotpService {

    private static final Logger log = LoggerFactory.getLogger(TotpService.class);

    /**
     * 160 bits, which is both what HMAC-SHA1 uses internally and what every
     * authenticator app expects. Longer buys nothing here - HMAC-SHA1 folds a
     * longer key down to its block size - and 32 base32 characters is already
     * at the edge of what somebody will type by hand.
     */
    private static final int SECRET_BYTES = 20;

    /**
     * Backup code alphabet: Crockford-style, with I, L, O, U and the digits 0
     * and 1 removed.
     *
     * <p>Those are the characters people transcribe wrongly from a screen or a
     * piece of paper, and a backup code is read at the worst possible moment -
     * phone lost, already locked out, probably in a hurry.
     */
    private static final String BACKUP_ALPHABET = "23456789ABCDEFGHJKMNPQRSTVWXYZ";

    private static final int BACKUP_CODE_LENGTH = 10;

    /** Where the dash is printed. Cosmetic, and stripped on the way back in. */
    private static final int BACKUP_GROUP = 5;

    private final UserTotpRepository totps;
    private final TotpBackupCodeRepository backupCodes;
    private final TotpSecretCipher cipher;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final TotpProperties properties;
    private final TimeBasedOneTimePasswordGenerator generator =
            new TimeBasedOneTimePasswordGenerator();
    private final SecureRandom random = new SecureRandom();

    TotpService(
            UserTotpRepository totps,
            TotpBackupCodeRepository backupCodes,
            TotpSecretCipher cipher,
            PasswordEncoder passwordEncoder,
            Clock clock,
            TotpProperties properties) {
        this.totps = totps;
        this.backupCodes = backupCodes;
        this.cipher = cipher;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.properties = properties;
    }

    /**
     * Whether this role must hold a second factor.
     *
     * <p>ADMIN, and only ADMIN. A USER account grants the ability to attach a
     * display name to a note that a human then moderates; the admin account
     * grants the moderation queue itself, which is the only place on this site
     * where one person can read what strangers wrote about their lives.
     * Forcing enrolment on somebody who signed up to leave one piece of
     * feedback is friction with no corresponding benefit - so it is offered to
     * them, not required.
     */
    static boolean isRequiredFor(UserRole role) {
        return role == UserRole.ADMIN;
    }

    @Transactional(readOnly = true)
    boolean isEnrolled(UUID userId) {
        return totps.findByUserId(userId).filter(UserTotp::isConfirmed).isPresent();
    }

    @Transactional(readOnly = true)
    long remainingBackupCodes(UUID userId) {
        return backupCodes.countByUserIdAndUsedAtIsNull(userId);
    }

    /**
     * Generates a fresh secret, stores it unconfirmed, and returns it in the
     * two forms an authenticator can take it.
     *
     * <p>Calling this again replaces any unconfirmed secret, which is what
     * somebody who abandoned a half-finished enrolment needs. Calling it on a
     * <em>confirmed</em> account also replaces the secret - the row goes back
     * to unconfirmed, and until {@link #enable} succeeds the old secret keeps
     * working. That ordering matters: a setup screen opened by accident must
     * not turn off a working second factor.
     */
    @Transactional
    TotpSetup beginSetup(UUID userId, String email) {
        byte[] secret = new byte[SECRET_BYTES];
        random.nextBytes(secret);

        Instant now = clock.instant();
        String ciphertext = cipher.encrypt(secret, userId);

        UserTotp existing = totps.findByUserId(userId).orElse(null);
        if (existing == null) {
            totps.save(new UserTotp(userId, ciphertext, now));
        } else {
            existing.replaceSecret(ciphertext, now);
            totps.save(existing);
        }

        String base32 = Base32.encode(secret);
        String uri = otpauthUri(base32, email);
        return new TotpSetup(Base32.grouped(base32), uri, TotpQrCode.svgDataUri(uri));
    }

    /**
     * Confirms enrolment and issues the recovery codes.
     *
     * @return the plaintext backup codes, the only time they exist anywhere but
     *         in the person's own hands
     * @throws InvalidTotpCodeException if the code does not match the pending
     *         secret
     */
    @Transactional(noRollbackFor = InvalidTotpCodeException.class)
    List<String> enable(UUID userId, String submittedCode) {
        UserTotp totp = totps.findByUserId(userId).orElseThrow(InvalidTotpCodeException::new);
        Instant now = clock.instant();

        requireNotLocked(totp, now);

        OptionalLong step = matchingStep(totp, submittedCode, now);
        if (step.isEmpty()) {
            // Counted, because an enrolment screen is otherwise an unlimited
            // oracle against a secret the caller already holds. It costs
            // nothing to be consistent and it keeps one code path.
            failAndThrow(totp, now);
        }

        totp.recordSuccess(step.getAsLong(), now);
        totp.confirm(now);
        totps.save(totp);

        log.info("Account {} enrolled a second factor", userId);
        return issueBackupCodes(userId, now);
    }

    /**
     * The sign-in check. Accepts a six-digit code or a backup code.
     *
     * @throws TotpLockedException if there have been too many recent failures
     * @throws InvalidTotpCodeException for every other failure, including an
     *         account with no confirmed enrolment
     */
    @Transactional(noRollbackFor = InvalidTotpCodeException.class)
    void verifySecondFactor(UUID userId, String submitted) {
        UserTotp totp = totps.findByUserId(userId)
                .filter(UserTotp::isConfirmed)
                .orElseThrow(InvalidTotpCodeException::new);

        Instant now = clock.instant();
        requireNotLocked(totp, now);

        String cleaned = submitted == null ? "" : submitted.trim();

        if (looksLikeTotpCode(cleaned)) {
            OptionalLong step = matchingStep(totp, cleaned, now);
            if (step.isPresent() && !totp.hasSpent(step.getAsLong())) {
                totp.recordSuccess(step.getAsLong(), now);
                totps.save(totp);
                return;
            }
            // A correct code at an already-spent step fails here, and is
            // deliberately indistinguishable from a wrong one. Somebody
            // replaying a code they watched being typed learns nothing about
            // whether it was ever right.
            failAndThrow(totp, now);
        }

        if (consumeBackupCode(userId, cleaned, now)) {
            totp.recordBackupCodeSuccess(now);
            totps.save(totp);
            log.info("Account {} signed in with a backup code", userId);
            return;
        }

        failAndThrow(totp, now);
    }

    /**
     * Replaces every code, gated on a current code.
     *
     * <p>Without this an account that has spent eight of ten codes slides
     * quietly toward having none, and finds out at the moment it matters.
     */
    @Transactional(noRollbackFor = InvalidTotpCodeException.class)
    List<String> regenerateBackupCodes(UUID userId, String submittedCode) {
        UserTotp totp = totps.findByUserId(userId)
                .filter(UserTotp::isConfirmed)
                .orElseThrow(() -> new TotpNotPermittedException(
                        "Set up two-step sign-in before generating recovery codes."));

        Instant now = clock.instant();
        requireNotLocked(totp, now);

        OptionalLong step = matchingStep(totp, submittedCode, now);
        if (step.isEmpty() || totp.hasSpent(step.getAsLong())) {
            failAndThrow(totp, now);
        }
        totp.recordSuccess(step.getAsLong(), now);
        totps.save(totp);

        return issueBackupCodes(userId, now);
    }

    /**
     * Turns the second factor off and destroys every trace of it.
     *
     * <p>Refused for an ADMIN: the role requires it, so the only thing
     * disabling could achieve is forced re-enrolment at the next sign-in. The
     * caller is responsible for having checked the password first - see
     * {@code DefaultAuthService.disableTotp}.
     */
    @Transactional
    void disable(UUID userId, UserRole role) {
        if (isRequiredFor(role)) {
            throw new TotpNotPermittedException(
                    "Two-step sign-in is required for admin accounts and cannot be turned off.");
        }
        forget(userId);
        log.info("Account {} turned off its second factor", userId);
    }

    /** Everything for one account, removed. */
    @Transactional
    void forget(UUID userId) {
        totps.deleteByUserId(userId);
        backupCodes.deleteByUserId(userId);
    }

    // ---------------------------------------------------------------------

    private void requireNotLocked(UserTotp totp, Instant now) {
        if (totp.isLockedAt(now)) {
            Duration remaining = totp.lockRemainingAt(now);
            throw new TotpLockedException(remaining.toSeconds() + 1);
        }
    }

    private void failAndThrow(UserTotp totp, Instant now) {
        totp.recordFailure(now);
        totps.save(totp);
        throw new InvalidTotpCodeException();
    }

    private static boolean looksLikeTotpCode(String value) {
        if (value.length() != 6) {
            return false;
        }
        for (int i = 0; i < 6; i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * The step whose code matches, if any.
     *
     * <h2>The drift window is one step, and that is a decision</h2>
     *
     * One step either side of now: 30 seconds of tolerance in each direction,
     * 90 seconds in total, three simultaneously valid codes.
     *
     * <p>RFC 6238 section 5.2 suggests at most one step, and the reason is
     * arithmetic. Every extra step adds two more live codes and multiplies the
     * brute-force surface. Going to two steps would be a 67% larger target in
     * exchange for tolerating a phone a full minute out of sync - a phone that
     * already fails against every other TOTP site, whose real fix is in its own
     * clock settings, and which the error copy can point at without revealing
     * anything about the attempt.
     *
     * <p>The replay rule pulls the same way: a wider window is a longer
     * capture-and-replay opportunity, so widening it would weaken two things at
     * once.
     *
     * <h2>Every step is evaluated</h2>
     *
     * The loop does not break on a match. Three HMACs cost nothing measurable,
     * and a loop that returned early would take a different amount of time
     * depending on which step matched.
     */
    private OptionalLong matchingStep(UserTotp totp, String submitted, Instant now) {
        if (submitted == null || !looksLikeTotpCode(submitted.trim())) {
            return OptionalLong.empty();
        }
        String code = submitted.trim();

        SecretKey key = new SecretKeySpec(
                cipher.decrypt(totp.getSecretCiphertext(), totp.getUserId()), "HmacSHA1");
        long stepSeconds = generator.getTimeStep().toSeconds();

        OptionalLong matched = OptionalLong.empty();
        for (int offset = -properties.driftSteps(); offset <= properties.driftSteps(); offset++) {
            Instant at = now.plusSeconds(offset * stepSeconds);
            String expected;
            try {
                expected = generator.generateOneTimePasswordString(key, at);
            } catch (InvalidKeyException e) {
                // The key came from our own SecureRandom and was proven usable
                // at enrolment, so this is unreachable. Failing closed rather
                // than letting an exception become an accidental "pass".
                throw new IllegalStateException("A stored TOTP secret is not a usable key", e);
            }
            if (constantTimeEquals(expected, code) && matched.isEmpty()) {
                matched = OptionalLong.of(Math.floorDiv(at.getEpochSecond(), stepSeconds));
            }
        }
        return matched;
    }

    /**
     * Compares without leaking where the first difference is.
     *
     * <p>A plain {@code equals} on a six-digit string returns at the first
     * differing character. What leaks is small and the attack over a network is
     * impractical, but the fix is one call and the alternative is arguing about
     * it.
     */
    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Tries {@code submitted} against every unused code for this account.
     *
     * <p>Argon2id cannot be looked up by value, so this is a linear scan over
     * at most ten rows. That is affordable precisely because the lockout ladder
     * caps how often it can happen: at most five scans before the account
     * locks, then five more per lockout window.
     *
     * <p>Used rows are skipped rather than matched, so a code that has been
     * spent behaves exactly like one that never existed.
     */
    private boolean consumeBackupCode(UUID userId, String submitted, Instant now) {
        String normalised = normaliseBackupCode(submitted);
        if (normalised.length() != BACKUP_CODE_LENGTH) {
            return false;
        }
        for (TotpBackupCode candidate : backupCodes.findByUserId(userId)) {
            if (candidate.isUsed()) {
                continue;
            }
            if (passwordEncoder.matches(normalised, candidate.getCodeHash())) {
                candidate.markUsed(now);
                backupCodes.save(candidate);
                return true;
            }
        }
        return false;
    }

    /** Dashes, spaces and case are presentation; the stored code has none. */
    private static String normaliseBackupCode(String submitted) {
        return submitted.replace("-", "").replace(" ", "").toUpperCase(Locale.ROOT);
    }

    /**
     * Fresh codes, replacing whatever was there.
     *
     * <p>The old rows are deleted rather than marked, because regeneration is
     * an explicit "those are gone" - leaving them usable would mean the printed
     * sheet somebody has just thrown away is still a way into the account.
     */
    private List<String> issueBackupCodes(UUID userId, Instant now) {
        backupCodes.deleteByUserId(userId);

        List<String> plaintext = new ArrayList<>(properties.backupCodeCount());
        for (int i = 0; i < properties.backupCodeCount(); i++) {
            String code = randomBackupCode();
            plaintext.add(formatBackupCode(code));
            // Argon2id, the same encoder as passwords. NOT the SHA-256 used for
            // refresh tokens: the argument that justifies a bare digest there -
            // 256 bits from SecureRandom, so there is no dictionary to run -
            // does not survive the drop to 50 bits, where SHA-256 is days of
            // GPU work against a leaked table.
            backupCodes.save(new TotpBackupCode(userId, passwordEncoder.encode(code), now));
        }
        return List.copyOf(plaintext);
    }

    private String randomBackupCode() {
        StringBuilder out = new StringBuilder(BACKUP_CODE_LENGTH);
        for (int i = 0; i < BACKUP_CODE_LENGTH; i++) {
            out.append(BACKUP_ALPHABET.charAt(random.nextInt(BACKUP_ALPHABET.length())));
        }
        return out.toString();
    }

    private static String formatBackupCode(String raw) {
        return raw.substring(0, BACKUP_GROUP) + "-" + raw.substring(BACKUP_GROUP);
    }

    /**
     * The {@code otpauth://} URI, per the de-facto Key Uri Format.
     *
     * <p>The label is {@code Issuer:account} and the {@code issuer} parameter
     * repeats it. Both are needed in practice: older authenticators read the
     * label prefix, newer ones read the parameter, and an app that gets neither
     * files the entry under a blank name - which, on a phone holding a dozen
     * codes, is close to losing it.
     */
    private String otpauthUri(String base32Secret, String email) {
        String issuer = properties.issuer();
        String label = encode(issuer) + ":" + encode(email);
        return "otpauth://totp/" + label
                + "?secret=" + base32Secret
                + "&issuer=" + encode(issuer)
                + "&algorithm=SHA1"
                + "&digits=" + generator.getPasswordLength()
                + "&period=" + generator.getTimeStep().toSeconds();
    }

    private static String encode(String value) {
        // URLEncoder is form encoding, which turns a space into '+'. In a URI
        // path segment that is a literal plus rather than a space, so it is
        // corrected here.
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    /**
     * What the enrolment screen needs.
     *
     * @param manualKey  grouped in fours, for typing by hand
     * @param otpauthUri the URI behind the QR
     * @param qrDataUri  an SVG {@code data:} URI for an {@code <img>}
     */
    record TotpSetup(String manualKey, String otpauthUri, String qrDataUri) {
    }
}
