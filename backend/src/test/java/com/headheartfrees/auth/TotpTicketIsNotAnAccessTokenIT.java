package com.headheartfrees.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.headheartfrees.config.AuthProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * <strong>The bug this feature could most easily ship, closed and pinned.</strong>
 *
 * <p>{@link JwtService} signs three things with one key: an access token, a
 * challenge ticket, and an enrolment ticket. All three are HS256 JWTs carrying
 * a user id in {@code sub} and issued by the same issuer. Without a type claim
 * they are interchangeable - and a challenge ticket accepted as a Bearer token
 * would mean the second factor is bypassed using the ticket the server hands
 * out, for free, immediately after a correct password.
 *
 * <p>The whole feature would still look correct from the outside: the login
 * endpoint would refuse to issue a session, every test about codes would pass,
 * and the moderation queue would open to anyone who read the ticket out of the
 * first response.
 *
 * <p>So this test mints the tickets directly, presents each as a Bearer token,
 * and asserts refusal. It uses {@code JwtService} rather than going through the
 * HTTP flow deliberately - it is testing the filter, not the endpoint that
 * happens to produce tickets today.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TotpTicketIsNotAnAccessTokenIT extends TotpTestSupport {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserAccountRepository accounts;

    @Autowired
    private AuthProperties authProperties;

    @Test
    @DisplayName("a challenge ticket is refused as a Bearer token")
    void challengeTicketIsRefused() throws Exception {
        java.util.UUID userId = anAccount();
        String ticket = jwtService.issueTicket(
                userId, JwtService.TYPE_TOTP_CHALLENGE, java.time.Duration.ofMinutes(5));

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ticket))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("an enrolment ticket is refused as a Bearer token")
    void enrolmentTicketIsRefused() throws Exception {
        java.util.UUID userId = anAccount();
        String ticket = jwtService.issueTicket(
                userId, JwtService.TYPE_TOTP_ENROLMENT, java.time.Duration.ofMinutes(5));

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ticket))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("an admin's enrolment ticket does not open the moderation queue")
    void anAdminTicketIsNotAnAdminSession() throws Exception {
        register("ticketadmin@example.com", PASSWORD, "The Admin");
        promoteToAdmin("ticketadmin@example.com");
        java.util.UUID adminId = accounts.findByEmail("ticketadmin@example.com").orElseThrow()
                .getId();

        // Note there is no role claim on a ticket at all, so even a filter that
        // accepted the token would find no authority on it. Both defences are
        // deliberate; this asserts the outcome rather than either mechanism.
        for (String type : new String[] {
            JwtService.TYPE_TOTP_CHALLENGE, JwtService.TYPE_TOTP_ENROLMENT
        }) {
            String ticket =
                    jwtService.issueTicket(adminId, type, java.time.Duration.ofMinutes(5));
            mockMvc.perform(get("/api/v1/admin/feedback")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ticket))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    @DisplayName("a token with no type claim at all is refused")
    void anUntypedTokenIsRefused() throws Exception {
        // This is the exact shape of every access token issued BEFORE this
        // phase: correctly signed, correct issuer, correct subject, correct
        // role - and no type claim. They stop working on deploy, which is one
        // silent refresh per open client and the right direction to fail.
        //
        // It is asserted so that nobody "restores compatibility" by treating a
        // missing claim as an access token. That change would reopen the bypass
        // the rest of this class closes, because a forged or repurposed token
        // can simply omit the claim.
        //
        // Minted here rather than through JwtService, because JwtService can no
        // longer produce this shape - which is the point.
        java.util.UUID userId = anAccount();
        String untyped = untypedTokenFor(userId);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + untyped))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("a token with an unrecognised type is refused")
    void anUnknownTypeIsRefused() throws Exception {
        java.util.UUID userId = anAccount();
        String odd = jwtService.issueTicket(
                userId, "something_else", java.time.Duration.ofMinutes(5));

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + odd))
                .andExpect(status().isUnauthorized());
    }

    /**
     * A pre-phase access token, built independently of the code under test.
     *
     * <p>Signed with the same configured secret, so it is genuinely valid in
     * every respect except the claim this phase added. Anything less - a
     * garbage string, a token signed with a different key - would pass this
     * test for the wrong reason.
     */
    private String untypedTokenFor(java.util.UUID userId) {
        byte[] secret;
        try {
            secret = Base64.getDecoder().decode(authProperties.jwtSecret());
        } catch (IllegalArgumentException notBase64) {
            secret = authProperties.jwtSecret().getBytes(StandardCharsets.UTF_8);
        }
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(
                new ImmutableSecret<>(new SecretKeySpec(secret, "HmacSHA256")));

        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("headheartfrees")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(900))
                .subject(userId.toString())
                .claim(JwtService.ROLE_CLAIM, UserRole.USER.name())
                .build();

        return encoder.encode(JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    @Test
    @DisplayName("a real access token still works, so the filter is not simply refusing everything")
    void anAccessTokenStillWorks() throws Exception {
        // The control. Without it, every assertion above would pass against a
        // filter that rejected all tokens unconditionally.
        String accessToken = accessTokenOf(registerAndLogin("control@example.com"));

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    private java.util.UUID anAccount() throws Exception {
        register("ticket@example.com", PASSWORD, "Somebody");
        return accounts.findByEmail("ticket@example.com").orElseThrow().getId();
    }
}
