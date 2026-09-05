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
 */
@Service
class JwtService {

    /** Private claim holding {@link UserRole#name()}. */
    static final String ROLE_CLAIM = "role";

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
                .build();

        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(ALGORITHM).build(), claims))
                .getTokenValue();
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
