package com.headheartfrees.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

/** 5/min per IP on the endpoints worth guessing against (section 6). */
@SpringBootTest
@AutoConfigureMockMvc
class AuthRateLimitIT extends AuthTestSupport {

    @Test
    @DisplayName("the sixth login attempt in a minute is refused with 429 and Retry-After")
    void loginIsLimitedToFivePerMinute() throws Exception {
        for (int attempt = 1; attempt <= 5; attempt++) {
            login("nobody@example.com", PASSWORD)
                    .andExpect(status().isUnauthorized());
        }

        login("nobody@example.com", PASSWORD)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429));
    }

    @Test
    @DisplayName("registration draws on the same auth bucket")
    void registrationSharesTheAuthBucket() throws Exception {
        // Register and login share one policy, so five attempts across both
        // exhausts it. Otherwise an attacker alternates endpoints for double
        // the allowance.
        for (int attempt = 1; attempt <= 5; attempt++) {
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RegisterRequest(
                                    "user" + attempt + "@example.com", PASSWORD, "Person"))))
                    .andExpect(status().isCreated());
        }

        login("nobody@example.com", PASSWORD)
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("exhausting the auth limit does not stop someone venting")
    void ventingSurvivesAnExhaustedAuthBucket() throws Exception {
        for (int attempt = 1; attempt <= 6; attempt++) {
            login("nobody@example.com", PASSWORD);
        }

        // The whole reason RateLimitPolicy keys on policy AND address. Rule 2.2
        // says venting is unconditionally available; failed sign-ins must not
        // be able to take it away.
        mockMvc.perform(post("/api/v1/vent/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mood\":\"ANXIOUS\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("/me is not rate limited")
    void meIsNotLimited() throws Exception {
        var login = registerAndLogin("reader@example.com");
        String token = accessTokenOf(login);
        // registerAndLogin already spent two of the five auth tokens; /me draws
        // on no bucket at all, so ten reads must all succeed.
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/v1/auth/me")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }
}
