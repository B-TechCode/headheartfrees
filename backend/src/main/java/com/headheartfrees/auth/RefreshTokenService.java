package com.headheartfrees.auth;

import com.headheartfrees.config.AuthProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues, rotates and revokes refresh tokens.
 *
 * <h2>Why the stored value is a digest and not a hash-with-salt</h2>
 *
 * {@link #hash} is plain SHA-256. For a password that would be indefensible;
 * here it is correct. The column is looked up by value on every refresh, so the
 * transform has to be deterministic, and the input is 32 bytes from
 * {@link SecureRandom} rather than something a person chose. There is no
 * dictionary to run against 256 bits of entropy, so the slow-hash property that
 * matters for passwords buys nothing, and a per-row salt would make the lookup
 * impossible.
 *
 * <h2>Reuse detection</h2>
 *
 * Spent rows are kept. A presented token that exists and is already revoked
 * means two parties hold tokens descended from one login - the legitimate
 * client and someone who copied it. Which is which is unknowable from here, so
 * the whole family is revoked and both must sign in again. Losing a session is
 * the correct price for that ambiguity.
 */
@Service
class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    /** 256 bits. Base64url-encoded to 43 characters for the cookie. */
    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository repository;
    private final Clock clock;
    private final Duration ttl;
    private final SecureRandom random = new SecureRandom();

    RefreshTokenService(RefreshTokenRepository repository, AuthProperties properties, Clock clock) {
        this.repository = repository;
        this.clock = clock;
        this.ttl = properties.refreshTokenTtl();
    }

    /** A brand new family. Called on password login and on Google sign-in. */
    @Transactional
    IssuedToken issueNewFamily(UUID userId) {
        return issue(userId, UUID.randomUUID());
    }

    /**
     * Consumes one token and issues its successor in the same family.
     *
     * <h2>{@code noRollbackFor} is load-bearing, not a tidy-up</h2>
     *
     * The reuse path revokes the family and then throws. With the default
     * rollback rules that throw would undo the revocation in the same
     * transaction - the caller would get a 401, the family would stay live, and
     * reuse detection would be decorative. The test
     * {@code reuseRevokesTheFamily} fails exactly this way if the annotation is
     * removed, and it is an easy thing to remove while tidying.
     *
     * <p>{@code DefaultAuthService.refresh} carries the same exclusion, because
     * with {@code REQUIRED} propagation both methods share one physical
     * transaction and either of them marking it rollback-only is enough to lose
     * the write.
     *
     * @throws RefreshTokenReuseException if it was already spent, after
     *         revoking every token in the family
     * @throws InvalidCredentialsException if it is unknown or expired
     */
    @Transactional(noRollbackFor = RefreshTokenReuseException.class)
    IssuedToken rotate(String presentedToken) {
        Instant now = clock.instant();
        RefreshToken stored = repository.findByTokenHash(hash(presentedToken))
                // Unknown token. Could be a forgery, could be a token from
                // before a family was revoked and pruned. Nothing to revoke.
                .orElseThrow(InvalidCredentialsException::new);

        if (stored.isRevoked()) {
            int revoked = repository.revokeFamily(stored.getFamilyId(), now);
            // Logged at WARN with no token material and no user email - the
            // family id is enough to correlate, and is meaningless outside this
            // database.
            log.warn("Refresh token reuse detected; revoked {} remaining token(s) in family {}",
                    revoked, stored.getFamilyId());
            throw new RefreshTokenReuseException();
        }

        if (stored.isExpiredAt(now)) {
            throw new InvalidCredentialsException();
        }

        stored.revokeAt(now);
        repository.save(stored);
        return issue(stored.getUserId(), stored.getFamilyId());
    }

    /**
     * Revokes the presented token's entire family.
     *
     * <p>Silent when the token is unknown: logout is not an endpoint that
     * should report whether a token was real, and a client logging out twice is
     * not an error worth surfacing.
     */
    @Transactional
    void revokeFamilyOf(String presentedToken) {
        if (presentedToken == null || presentedToken.isBlank()) {
            return;
        }
        Optional<RefreshToken> stored = repository.findByTokenHash(hash(presentedToken));
        stored.ifPresent(token -> repository.revokeFamily(token.getFamilyId(), clock.instant()));
    }

    private IssuedToken issue(UUID userId, UUID familyId) {
        Instant now = clock.instant();
        byte[] raw = new byte[TOKEN_BYTES];
        random.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

        repository.save(new RefreshToken(userId, hash(token), familyId, now.plus(ttl), now));
        return new IssuedToken(userId, token, ttl);
    }

    /**
     * SHA-256 hex. See the class comment for why this is not Argon2.
     */
    private static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // Mandated by the JLS; unreachable on any conformant JVM.
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    /**
     * The raw token, which exists in memory only long enough to become a cookie.
     *
     * <p>Carries {@code userId} so that the rotation path does not have to
     * re-derive who the token belongs to - it is already known from the row
     * that was just consumed, and looking it up again from the presented token
     * would mean hashing it a second time.
     */
    record IssuedToken(UUID userId, String token, Duration ttl) {
    }
}
