package com.headheartfrees.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;

/** Logout actually invalidates the session rather than only clearing the cookie. */
@SpringBootTest
@AutoConfigureMockMvc
class AuthLogoutIT extends AuthTestSupport {

    private static final String EMAIL = "leaving@example.com";

    @Autowired
    private RefreshTokenRepository refreshTokens;

    @Test
    @DisplayName("logout revokes the token, so the cookie is useless afterwards")
    void logoutInvalidatesTheRefreshToken() throws Exception {
        MvcResult login = registerAndLogin(EMAIL);
        Cookie cookie = refreshCookieOf(login);

        mockMvc.perform(post("/api/v1/auth/logout").cookie(cookie))
                .andExpect(status().isNoContent());

        assertThat(refreshTokens.findAll())
                .as("Server-side revocation, not just a cleared cookie. A client that "
                        + "kept a copy must not be able to refresh with it.")
                .allMatch(RefreshToken::isRevoked);

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(cookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("logout clears the cookie with matching attributes")
    void logoutClearsTheCookie() throws Exception {
        MvcResult login = registerAndLogin(EMAIL);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(refreshCookieOf(login)))
                .andExpect(status().isNoContent())
                .andReturn();

        String setCookie = result.getResponse().getHeader("Set-Cookie");
        assertThat(setCookie).isNotNull();
        assertThat(setCookie).contains(RefreshCookie.NAME + "=");
        assertThat(setCookie).contains("Max-Age=0");
        // Path must match the issued cookie or the browser treats this as a
        // different cookie and leaves the original in place.
        assertThat(setCookie).contains("Path=/api/v1/auth");
    }

    @Test
    @DisplayName("logout revokes every token in the family, not only the one presented")
    void logoutRevokesTheWholeFamily() throws Exception {
        MvcResult login = registerAndLogin(EMAIL);
        MvcResult rotated = mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(refreshCookieOf(login)))
                .andExpect(status().isOk())
                .andReturn();

        mockMvc.perform(post("/api/v1/auth/logout").cookie(refreshCookieOf(rotated)))
                .andExpect(status().isNoContent());

        assertThat(refreshTokens.findAll())
                .hasSize(2)
                .allMatch(RefreshToken::isRevoked);
    }

    @Test
    @DisplayName("logout with no cookie is still 204")
    void logoutWithoutCookie() throws Exception {
        // Signing out when already signed out is not an error worth reporting,
        // and a 4xx here would make the frontend's sign-out path conditional
        // for no reason.
        mockMvc.perform(post("/api/v1/auth/logout")).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("logout with an unknown token is 204 and says nothing")
    void logoutWithUnknownToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(new Cookie(RefreshCookie.NAME, "never-issued")))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("logout is not rate limited")
    void logoutIsNotRateLimited() throws Exception {
        // The auth bucket is 5/min. Someone who cannot sign out because they
        // signed in too often is a worse outcome than the abuse this prevents.
        MvcResult login = registerAndLogin(EMAIL);
        Cookie cookie = refreshCookieOf(login);

        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/v1/auth/logout").cookie(cookie))
                    .andExpect(status().isNoContent());
        }
    }
}
