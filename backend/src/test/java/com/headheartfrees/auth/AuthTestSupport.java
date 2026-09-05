package com.headheartfrees.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.headheartfrees.common.web.ClientIpRateLimiter;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Shared fixtures for the auth integration tests.
 *
 * <p>Not named {@code *IT} and not annotated: this is a base class, and
 * failsafe would otherwise try to run it as a suite of its own.
 *
 * <p>Every subclass wipes both tables and resets the rate limiter before each
 * test. The limiter is a per-JVM singleton with a 5/min auth bucket, so without
 * the reset the fifth test in a class starts failing with 429 for reasons that
 * have nothing to do with what it is testing.
 */
abstract class AuthTestSupport extends com.headheartfrees.PostgresTestBase {

    static final String PASSWORD = "a quiet long passphrase";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private ClientIpRateLimiter rateLimiter;

    @Autowired
    private UserAccountRepository users;

    @Autowired
    private RefreshTokenRepository refreshTokens;

    /**
     * Clears the auth buckets mid-test.
     *
     * <p>Needed by any test that legitimately makes more than five auth calls
     * while testing something other than rate limiting - the limit is 5/min per
     * IP and every test shares one loopback address. Calling this is a
     * statement that the test is not about the limiter; {@link AuthRateLimitIT}
     * never calls it.
     */
    protected void resetRateLimiter() {
        rateLimiter.reset();
    }

    @BeforeEach
    void resetAuthState() {
        // Refresh tokens first: the FK is ON DELETE CASCADE, but deleting users
        // first would rely on that rather than testing against a clean slate.
        refreshTokens.deleteAll();
        users.deleteAll();
        rateLimiter.reset();
    }

    protected void register(String email, String password, String displayName) throws Exception {
        String body = objectMapper.writeValueAsString(
                new RegisterRequest(email, password, displayName));
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    /** Registers and signs in, returning the full login result. */
    protected MvcResult registerAndLogin(String email) throws Exception {
        register(email, PASSWORD, "Test Person");
        return login(email, PASSWORD).andExpect(status().isOk()).andReturn();
    }

    protected org.springframework.test.web.servlet.ResultActions login(String email, String password)
            throws Exception {
        String body = objectMapper.writeValueAsString(new LoginRequest(email, password));
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    protected String accessTokenOf(MvcResult result) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("accessToken").asText();
    }

    /**
     * The refresh cookie MockMvc recorded, ready to send back on the next
     * request. Fails loudly rather than returning null, because a silent null
     * here turns a real bug into a confusing 401 three lines later.
     */
    protected Cookie refreshCookieOf(MvcResult result) {
        Cookie cookie = result.getResponse().getCookie(RefreshCookie.NAME);
        if (cookie == null) {
            throw new AssertionError(
                    "No " + RefreshCookie.NAME + " cookie on the response. Set-Cookie was: "
                            + result.getResponse().getHeaders("Set-Cookie"));
        }
        return cookie;
    }

    protected String refreshTokenValueOf(MvcResult result) {
        return refreshCookieOf(result).getValue();
    }
}
