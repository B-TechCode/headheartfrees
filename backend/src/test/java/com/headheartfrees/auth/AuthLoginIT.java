package com.headheartfrees.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;

/** Login: the happy path, the token split, and one message for every failure. */
@SpringBootTest
@AutoConfigureMockMvc
class AuthLoginIT extends AuthTestSupport {

    private static final String EMAIL = "person@example.com";

    @Test
    @DisplayName("login returns an access token in the body and a refresh cookie")
    void loginSucceeds() throws Exception {
        register(EMAIL, PASSWORD, "Person");

        MvcResult result = login(EMAIL, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.user.email").value(EMAIL))
                .andExpect(jsonPath("$.user.role").value("USER"))
                .andReturn();

        assertThat(accessTokenOf(result).split("\\."))
                .as("A JWT has three dot-separated parts")
                .hasSize(3);
        assertThat(refreshTokenValueOf(result)).isNotBlank();
    }

    @Test
    @DisplayName("the refresh cookie is httpOnly, Secure, SameSite=Strict and scoped away from vent")
    void refreshCookieAttributes() throws Exception {
        register(EMAIL, PASSWORD, "Person");

        MvcResult result = login(EMAIL, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(cookie().httpOnly(RefreshCookie.NAME, true))
                .andExpect(cookie().secure(RefreshCookie.NAME, true))
                .andReturn();

        String setCookie = result.getResponse().getHeader("Set-Cookie");
        assertThat(setCookie).isNotNull();
        assertThat(setCookie).contains("SameSite=Strict");
        assertThat(setCookie)
                .as("Scoped to /api/v1/auth so it never rides along on a vent request")
                .contains("Path=/api/v1/auth");
    }

    @Test
    @DisplayName("the access token is never placed in a cookie")
    void accessTokenIsNotACookie() throws Exception {
        register(EMAIL, PASSWORD, "Person");
        MvcResult result = login(EMAIL, PASSWORD).andExpect(status().isOk()).andReturn();

        String accessToken = accessTokenOf(result);
        for (jakarta.servlet.http.Cookie cookie : result.getResponse().getCookies()) {
            assertThat(cookie.getValue())
                    .as("Cookie %s must not carry the access token", cookie.getName())
                    .isNotEqualTo(accessToken);
        }
    }

    @Test
    @DisplayName("the refresh token is never placed in the body")
    void refreshTokenIsNotInBody() throws Exception {
        register(EMAIL, PASSWORD, "Person");
        MvcResult result = login(EMAIL, PASSWORD).andExpect(status().isOk()).andReturn();

        assertThat(result.getResponse().getContentAsString())
                .as("The refresh token belongs in the cookie and nowhere else")
                .doesNotContain(refreshTokenValueOf(result));
    }

    @Test
    @DisplayName("a wrong password and an unknown address give the identical response")
    void failuresAreIndistinguishable() throws Exception {
        register(EMAIL, PASSWORD, "Person");

        MvcResult wrongPassword = login(EMAIL, "the wrong long passphrase")
                .andExpect(status().isUnauthorized())
                .andReturn();

        MvcResult unknownUser = login("nobody@example.com", PASSWORD)
                .andExpect(status().isUnauthorized())
                .andReturn();

        // The timestamp field differs by milliseconds, so compare the fields
        // that carry meaning rather than the whole body.
        var wrong = objectMapper.readTree(wrongPassword.getResponse().getContentAsString());
        var unknown = objectMapper.readTree(unknownUser.getResponse().getContentAsString());

        assertThat(unknown.get("code").asText()).isEqualTo(wrong.get("code").asText());
        assertThat(unknown.get("message").asText())
                .as("One message. Never 'no such user' versus 'wrong password'.")
                .isEqualTo(wrong.get("message").asText());
        assertThat(unknown.get("status").asInt()).isEqualTo(wrong.get("status").asInt());
    }

    @Test
    @DisplayName("the failure message names neither the address nor the reason")
    void failureMessageSaysNothing() throws Exception {
        login("someone@example.com", PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password."))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("login is case-insensitive on the address")
    void loginIsCaseInsensitive() throws Exception {
        register("Person@Example.com", PASSWORD, "Person");
        login("person@example.com", PASSWORD).andExpect(status().isOk());
    }
}
