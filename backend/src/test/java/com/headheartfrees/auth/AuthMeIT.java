package com.headheartfrees.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;

/** {@code /me} - the one auth endpoint that requires authentication. */
@SpringBootTest
@AutoConfigureMockMvc
class AuthMeIT extends AuthTestSupport {

    private static final String EMAIL = "me@example.com";

    @Test
    @DisplayName("/me returns the current user when a valid token is presented")
    void meWhenAuthenticated() throws Exception {
        MvcResult login = registerAndLogin(EMAIL);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenOf(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.displayName").value("Test Person"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.emailVerified").value(false))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    @DisplayName("/me never exposes a password hash or a Google id")
    void meExposesNothingSecret() throws Exception {
        MvcResult login = registerAndLogin(EMAIL);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenOf(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password_hash").doesNotExist())
                .andExpect(jsonPath("$.googleId").doesNotExist());
    }

    @Test
    @DisplayName("/me without a token is 401 in the documented error shape")
    void meWhenAnonymous() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/me"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("a garbage bearer token is 401, not 500")
    void meWithGarbageToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("a token signed with the wrong key is 401")
    void meWithForgedToken() throws Exception {
        // Structurally valid JWT, signed with a key this application does not
        // hold. Verification must reject it rather than trusting the claims.
        String forged = "eyJhbGciOiJIUzI1NiJ9"
                + ".eyJzdWIiOiIwMDAwMDAwMC0wMDAwLTAwMDAtMDAwMC0wMDAwMDAwMDAwMDEiLCJyb2xlIjoiQURNSU4ifQ"
                + ".Zm9yZ2VkLXNpZ25hdHVyZS10aGF0LWlzLW5vdC12YWxpZA";

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + forged))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("an Authorization header that is not Bearer is treated as anonymous")
    void meWithNonBearerHeader() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz"))
                .andExpect(status().isUnauthorized());
    }
}
