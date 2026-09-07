package com.headheartfrees.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * No auth response body ever carries a secret.
 *
 * <p>The mistake this catches is a serialisation mistake, and serialisation
 * mistakes ship quietly: returning the entity instead of the DTO, adding a
 * field to a record "for the frontend", a Jackson mixin that stops applying.
 * None of those look wrong in a diff and none of them fail an existing test,
 * because every other test asserts what a response <em>does</em> contain.
 *
 * <p>This one asserts what no response may contain, and it sweeps every auth
 * endpoint rather than the one being changed.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthResponseLeakageIT extends AuthTestSupport {

    private static final String EMAIL = "leak@example.com";

    /**
     * Substrings that must never appear in an auth response body, in any
     * casing. The Argon2 prefix is included because it is what a leaked hash
     * actually starts with - a field named something unexpected would still be
     * caught by its value.
     */
    private static final List<String> FORBIDDEN_SUBSTRINGS = List.of(
            "password_hash",
            "passwordhash",
            "token_hash",
            "tokenhash",
            "$argon2",
            "refreshtoken",
            "refresh_token",
            "googleid",
            "google_id");

    @Autowired
    private UserAccountRepository users;

    @Test
    @DisplayName("no auth response body contains a hash, a token field, or the refresh token")
    void noAuthResponseLeaksSecrets() throws Exception {
        register(EMAIL, PASSWORD, "Leak Test");
        MvcResult login = login(EMAIL, PASSWORD).andExpect(status().isOk()).andReturn();

        String accessToken = accessTokenOf(login);
        String refreshTokenValue = refreshTokenValueOf(login);
        String storedHash = users.findByEmail(EMAIL).orElseThrow().getPasswordHash();

        List<MvcResult> responses = new ArrayList<>();
        responses.add(login);

        // Register (the shared 201).
        responses.add(mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("other@example.com", PASSWORD, "Other"))))
                .andReturn());

        // Refresh.
        responses.add(mockMvc.perform(guardedPost("/api/v1/auth/refresh")
                        .cookie(refreshCookieOf(login)))
                .andReturn());

        // /me.
        responses.add(mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andReturn());

        // A failed login, because error bodies are responses too.
        responses.add(login(EMAIL, "the wrong long passphrase").andReturn());

        // An unauthenticated /me, same reason.
        responses.add(mockMvc.perform(get("/api/v1/auth/me")).andReturn());

        for (MvcResult response : responses) {
            String body = response.getResponse().getContentAsString();
            String uri = response.getRequest().getRequestURI();
            String lower = body.toLowerCase(Locale.ROOT);

            for (String forbidden : FORBIDDEN_SUBSTRINGS) {
                assertThat(lower)
                        .as("%s response must not contain %s. Body was: %s", uri, forbidden, body)
                        .doesNotContain(forbidden);
            }

            assertThat(body)
                    .as("%s response must not contain the stored password hash", uri)
                    .doesNotContain(storedHash);

            assertThat(body)
                    .as("%s response must not contain the raw refresh token - it belongs "
                            + "in the httpOnly cookie and nowhere else", uri)
                    .doesNotContain(refreshTokenValue);
        }
    }

    @Test
    @DisplayName("the rotated refresh token does not appear in the refresh response body either")
    void rotatedTokenStaysInTheCookie() throws Exception {
        MvcResult login = registerAndLogin(EMAIL);

        MvcResult refreshed = mockMvc.perform(guardedPost("/api/v1/auth/refresh")
                        .cookie(refreshCookieOf(login)))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(refreshed.getResponse().getContentAsString())
                .doesNotContain(refreshTokenValueOf(refreshed));
    }

    @Test
    @DisplayName("UserSummary has no field that could carry a secret")
    void userSummaryShapeIsLocked() {
        // Structural, not behavioural: a new component on this record is how a
        // secret would reach a body without any controller changing. The list
        // is exhaustive on purpose, so adding a field fails here and the author
        // has to justify it.
        assertThat(UserSummary.class.getRecordComponents())
                .extracting(java.lang.reflect.RecordComponent::getName)
                .containsExactly("id", "email", "displayName", "role", "emailVerified", "createdAt");
    }

    @Test
    @DisplayName("AccessTokenResponse cannot carry a refresh token")
    void accessTokenResponseShapeIsLocked() {
        assertThat(AccessTokenResponse.class.getRecordComponents())
                .extracting(java.lang.reflect.RecordComponent::getName)
                .as("A refreshToken component here would put it in the body, which is "
                        + "exactly the separation the cookie exists to maintain")
                .containsExactly("accessToken", "tokenType", "expiresIn", "user");
    }
}
