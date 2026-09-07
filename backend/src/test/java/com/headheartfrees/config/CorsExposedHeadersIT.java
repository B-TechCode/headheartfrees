package com.headheartfrees.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.headheartfrees.PostgresTestBase;
import com.headheartfrees.common.web.ClientIpRateLimiter;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * {@code Retry-After} must be readable by the browser, not merely present.
 *
 * <p>A cross-origin response hands script only the CORS-safelisted headers
 * unless the server names the rest in {@code Access-Control-Expose-Headers}.
 * {@code Retry-After} is not safelisted, so before this was configured the
 * frontend saw a 429 whose {@code Retry-After} existed on the wire and was
 * invisible to {@code fetch()} - and {@code ApiError.retryAfterSeconds} was
 * therefore always {@code null} in a browser, on precisely the responses that
 * field exists to carry.
 *
 * <p>{@link com.headheartfrees.auth.AuthRateLimitIT} asserts the header is
 * <em>sent</em>. This asserts it can be <em>read</em>. Both are needed: MockMvc
 * reads response headers directly and would never notice the difference.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CorsExposedHeadersIT extends PostgresTestBase {

    /** The default of {@code app.cors.allowed-origins}. */
    private static final String ORIGIN = "http://localhost:3000";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClientIpRateLimiter rateLimiter;

    /**
     * The limiter is a per-JVM singleton and every test in the suite shares one
     * loopback address. Resetting on both sides means this class neither
     * inherits an exhausted bucket nor leaves one behind for whatever runs
     * next - which matters here more than usual, because one test deliberately
     * exhausts it.
     */
    @BeforeEach
    @AfterEach
    void resetLimiter() {
        rateLimiter.reset();
    }

    @Test
    @DisplayName("a cross-origin response exposes Retry-After to script")
    void retryAfterIsExposed() throws Exception {
        mockMvc.perform(get("/api/v1/health").header(HttpHeaders.ORIGIN, ORIGIN))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                        Matchers.containsString(HttpHeaders.RETRY_AFTER)));
    }

    @Test
    @DisplayName("the 429 that carries Retry-After also exposes it")
    void theRateLimitedResponseExposesIt() throws Exception {
        // The end-to-end claim, on the one response shape that needs it. Six
        // attempts against a 5/min bucket; the sixth is refused.
        for (int attempt = 1; attempt <= 5; attempt++) {
            login();
        }

        mockMvc.perform(post("/api/v1/auth/login")
                        .header(HttpHeaders.ORIGIN, ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@example.com\",\"password\":\"whatever at all\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                        Matchers.containsString(HttpHeaders.RETRY_AFTER)));
    }

    @Test
    @DisplayName("the preflight still permits a credentialed POST from the frontend")
    void preflightAllowsCredentials() throws Exception {
        // Guards the refresh call the whole session restore depends on: an
        // origin echoed back with Allow-Credentials, never a wildcard.
        mockMvc.perform(options("/api/v1/auth/refresh")
                        .header(HttpHeaders.ORIGIN, ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    private void login() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .header(HttpHeaders.ORIGIN, ORIGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nobody@example.com\",\"password\":\"whatever at all\"}"));
    }
}
