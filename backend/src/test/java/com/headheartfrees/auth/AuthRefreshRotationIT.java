package com.headheartfrees.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Rotation, and the reuse detection that makes rotation worth having.
 *
 * <p>The reuse case is the reason this table exists. Rotation on its own hands
 * a thief a fresh token and tells nobody; what closes that is noticing that a
 * spent token came back.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthRefreshRotationIT extends AuthTestSupport {

    private static final String EMAIL = "rotate@example.com";

    @Autowired
    private RefreshTokenRepository refreshTokens;

    @Test
    @DisplayName("refresh issues a new access token and a different refresh token")
    void refreshRotates() throws Exception {
        MvcResult login = registerAndLogin(EMAIL);
        String firstRefresh = refreshTokenValueOf(login);

        MvcResult refreshed = mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(refreshCookieOf(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(EMAIL))
                .andReturn();

        assertThat(refreshTokenValueOf(refreshed))
                .as("Rotation means a new value every time")
                .isNotEqualTo(firstRefresh);
    }

    @Test
    @DisplayName("the rotated token stays in the same family")
    void rotationKeepsOneFamily() throws Exception {
        MvcResult login = registerAndLogin(EMAIL);

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookieOf(login)))
                .andExpect(status().isOk());

        assertThat(refreshTokens.findAll())
                .as("Two rows - the spent one and its successor")
                .hasSize(2)
                .extracting(RefreshToken::getFamilyId)
                .containsOnly(refreshTokens.findAll().getFirst().getFamilyId());
    }

    @Test
    @DisplayName("the raw token is never stored; only its digest is")
    void tokensAreStoredAsDigests() throws Exception {
        MvcResult login = registerAndLogin(EMAIL);
        String raw = refreshTokenValueOf(login);

        assertThat(refreshTokens.findByTokenHash(raw))
                .as("Looking up by the raw value must find nothing - the column holds a digest")
                .isEmpty();
        assertThat(refreshTokens.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("reusing a spent token is refused and revokes the whole family")
    void reuseRevokesTheFamily() throws Exception {
        MvcResult login = registerAndLogin(EMAIL);
        Cookie stolen = refreshCookieOf(login);

        // The legitimate client rotates.
        MvcResult rotated = mockMvc.perform(post("/api/v1/auth/refresh").cookie(stolen))
                .andExpect(status().isOk())
                .andReturn();
        Cookie legitimate = refreshCookieOf(rotated);

        // The thief presents the copy they took before the rotation.
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(stolen))
                .andExpect(status().isUnauthorized());

        assertThat(refreshTokens.findAll())
                .as("Every token in the family is revoked, including the one the "
                        + "legitimate client is still holding")
                .allMatch(RefreshToken::isRevoked);

        // And the legitimate client is now locked out too. That is the intended
        // outcome: the server cannot tell victim from thief, so both re-auth.
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(legitimate))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("a reused token and an unknown token produce identical responses")
    void reuseIsIndistinguishableFromGarbage() throws Exception {
        MvcResult login = registerAndLogin(EMAIL);
        Cookie stolen = refreshCookieOf(login);
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(stolen)).andExpect(status().isOk());

        MvcResult reused = mockMvc.perform(post("/api/v1/auth/refresh").cookie(stolen))
                .andExpect(status().isUnauthorized())
                .andReturn();

        MvcResult garbage = mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie(RefreshCookie.NAME, "not-a-real-token")))
                .andExpect(status().isUnauthorized())
                .andReturn();

        var reusedJson = objectMapper.readTree(reused.getResponse().getContentAsString());
        var garbageJson = objectMapper.readTree(garbage.getResponse().getContentAsString());

        assertThat(reusedJson.get("code").asText())
                .as("A distinguishable reuse response lets a caller probe for live tokens")
                .isEqualTo(garbageJson.get("code").asText());
        assertThat(reusedJson.get("message").asText())
                .isEqualTo(garbageJson.get("message").asText());
    }

    @Test
    @DisplayName("refresh with no cookie at all is 401, not 500")
    void refreshWithoutCookie() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("two separate logins are separate families, and revoking one spares the other")
    void familiesAreIndependent() throws Exception {
        register(EMAIL, PASSWORD, "Person");
        MvcResult sessionOne = login(EMAIL, PASSWORD).andExpect(status().isOk()).andReturn();
        MvcResult sessionTwo = login(EMAIL, PASSWORD).andExpect(status().isOk()).andReturn();

        Cookie one = refreshCookieOf(sessionOne);
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(one)).andExpect(status().isOk());
        // Reuse on session one.
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(one))
                .andExpect(status().isUnauthorized());

        // Five auth calls have now been spent (register, two logins, two
        // refreshes) and the limit is 5/min per IP. This test is about family
        // isolation, not throttling, so clear the buckets rather than let a 429
        // masquerade as the assertion below failing.
        resetRateLimiter();

        // Session two is a different family and must be untouched: signing in
        // on a second device should not be collateral damage.
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookieOf(sessionTwo)))
                .andExpect(status().isOk());
    }
}
