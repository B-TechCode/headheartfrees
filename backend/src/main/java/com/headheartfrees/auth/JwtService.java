package com.headheartfrees.auth;

import com.headheartfrees.config.AuthProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

/**
 * Mints and verifies the short-lived access token.
 *
 * <p>HS256 with a shared secret, not RS256. There is one service issuing and
 * one service verifying, so an asymmetric key would add key distribution for no
 * benefit. If auth is ever lifted into its own process - which section 4 is
 * written to allow - this becomes the thing to revisit first, because at that
 * point the verifier and the issuer are no longer the same deployment.
 *
 * <p>The token carries the user id as {@code sub} and the role as a private
 * claim. It carries no email and no display name: those change, tokens live 15
 * minutes past the change, and a stale name rendered in a UI is a bug that is
 * tedious to trace. {@code /me} is one query and always current.
 *
 * <h2>Every token says what kind of token it is</h2>
 *
 * From the TOTP phase this class mints three things with one key: an access
 * token, and two short-lived tickets that represent a half-finished sign-in.
 * All three are HS256 JWTs carrying a user id, so <strong>without a type claim
 * they would be interchangeable</strong> - and a challenge ticket accepted as a
 * Bearer token is the entire second factor bypassed with the ticket the server
 * hands out for free after a password. That is the single worst bug this
 * feature could ship, so {@link #TYPE_CLAIM} is mandatory on every token and
 * {@link JwtAuthenticationFilter} refuses anything that is not
 * {@link #TYPE_ACCESS}.
 *
 * <p>The alternative - a separate signing key per token type - is stronger in
 * principle and was not taken: it is a second secret for the operator to
 * generate, set and never lose, guarding against a mistake that one required
 * claim and one filter check already prevent. {@code TotpTicketIsNotAnAccessTokenIT}
 * is what keeps that true.
 *
 * <p><strong>Deploying this invalidates every access token already issued</strong>,
 * because none of them carry the claim. That is fifteen minutes of clients
 * silently refreshing, and it was verified in a browser rather than assumed -
 * see the phase log.
 */
@Service
class JwtService {

    /** Private claim holding {@link UserRole#name()}. */
    static final String ROLE_CLAIM = "role";

    /**
     * Private claim naming what a token is for. Mandatory on all three kinds.
     */
    static final String TYPE_CLAIM = "typ";

    /** A session token. The only kind {@link JwtAuthenticationFilter} accepts. */
    static final String TYPE_ACCESS = "access";

    /**
     * Issued after a correct password on an enrolled account. Buys exactly one
     * thing: the right to present a code at {@code POST /auth/login/totp}.
     */
    static final String TYPE_TOTP_CHALLENGE = "totp_challenge";

    /**
     * Issued after a correct password on an ADMIN account with no second factor
     * yet. Buys exactly two things: {@code /auth/totp/setup} and
     * {@code /auth/totp/enable}. It is what makes "required for admins"
     * deployable without locking the existing admin out - the password still
     * works, and it still reaches a screen with a QR code on it.
     */
    static final String TYPE_TOTP_ENROLMENT = "totp_enrolment";

    private static final String ISSUER = "headheartfrees";
    private static final MacAlgorithm ALGORITHM = MacAlgorithm.HS256;

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final Clock clock;
    private final java.time.Duration accessTokenTtl;

    JwtService(AuthProperties properties, Clock clock) {
        this.clock = clock;
        this.accessTokenTtl = properties.accessTokenTtl();

        SecretKeySpec key = new SecretKeySpec(decodeSecret(properties.jwtSecret()), "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        this.decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(ALGORITHM).build();
    }

    /**
     * Accepts either a base64 secret (what {@code openssl rand -base64 48}
     * produces, and what {@code .env.example} documents) or a raw string, so a
     * developer who pastes a long passphrase instead of base64 gets a working
     * application rather than a stack trace at startup.
     *
     * @throws IllegalStateException if the result is under 256 bits, which
     *         HS256 requires and which Nimbus would otherwise reject later with
     *         a less obvious message
     */
    private static byte[] decodeSecret(String configured) {
        if (configured == null || configured.isBlank()) {
            throw new IllegalStateException(
                    "app.auth.jwt-secret is not set. Generate one with: openssl rand -base64 48");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(configured);
        } catch (IllegalArgumentException notBase64) {
            bytes = configured.getBytes(StandardCharsets.UTF_8);
        }
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "app.auth.jwt-secret must decode to at least 256 bits for HS256; got "
                            + (bytes.length * 8) + " bits. Generate one with: openssl rand -base64 48");
        }
        return bytes;
    }

    String issueAccessToken(UUID userId, UserRole role) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .issuedAt(now)
                .expiresAt(now.plus(accessTokenTtl))
                .subject(userId.toString())
                .claim(ROLE_CLAIM, role.name())
                .claim(TYPE_CLAIM, TYPE_ACCESS)
                .build();

        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(ALGORITHM).build(), claims))
                .getTokenValue();
    }

    /**
     * A ticket for a sign-in that has passed the password and is not finished.
     *
     * <p>Carries no role claim. It is not a session and nothing should be able
     * to read an authority off it, so there is nothing there to read.
     *
     * @param type {@link #TYPE_TOTP_CHALLENGE} or {@link #TYPE_TOTP_ENROLMENT}
     */
    String issueTicket(UUID userId, String type, java.time.Duration ttl) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .subject(userId.toString())
                .claim(TYPE_CLAIM, type)
                .build();

        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(ALGORITHM).build(), claims))
                .getTokenValue();
    }

    /**
     * Verifies a ticket and returns whose it is.
     *
     * <p>{@code expectedType} is checked here rather than by the caller, so
     * that "which kind of ticket is this?" cannot be forgotten at a call site.
     * An expired, forged, or wrong-type ticket is one empty result: the caller
     * answers identically to all three, and one that could tell them apart
     * would be an oracle for which tickets had once been live.
     */
    java.util.Optional<UUID> verifyTicket(String token, String expectedType) {
        if (token == null || token.isBlank()) {
            return java.util.Optional.empty();
        }
        return verify(token)
                .filter(jwt -> expectedType.equals(jwt.getClaimAsString(TYPE_CLAIM)))
                .flatMap(jwt -> {
                    try {
                        return java.util.Optional.of(UUID.fromString(jwt.getSubject()));
                    } catch (IllegalArgumentException | NullPointerException malformed) {
                        return java.util.Optional.empty();
                    }
                });
    }

    /**
     * @return the verified token, or empty if it is malformed, expired, or
     *         signed with a different key. The reason is not reported: the
     *         caller answers 401 either way and a caller that could tell them
     *         apart would be an oracle.
     */
    java.util.Optional<Jwt> verify(String token) {
        try {
            return java.util.Optional.of(decoder.decode(token));
        } catch (JwtException invalid) {
            return java.util.Optional.empty();
        }
    }

    java.time.Duration accessTokenTtl() {
        return accessTokenTtl;
    }
}
