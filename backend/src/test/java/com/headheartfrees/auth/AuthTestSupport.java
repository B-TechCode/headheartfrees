package com.headheartfrees.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.headheartfrees.common.web.ClientIpRateLimiter;
import com.headheartfrees.common.web.CsrfHeaderFilter;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
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

    /**
     * A POST carrying the header {@link CsrfHeaderFilter} requires.
     *
     * <p>{@code /refresh} and {@code /logout} are refused without it, so every
     * test that is not <em>about</em> that guard goes through here. Written as
     * one helper rather than repeated inline so that the day the header name
     * changes, the suite does not need thirty edits to agree with the filter.
     *
     * <p>{@code CsrfHeaderIT} deliberately does not use this - it builds the
     * bare request itself, because a test of the guard that obtained its header
     * from a helper would pass if the helper were wrong.
     */
    protected static MockHttpServletRequestBuilder guardedPost(String path) {
        return post(path).header(CsrfHeaderFilter.HEADER, CsrfHeaderFilter.REQUIRED_VALUE);
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

    /**
     * The access token from a login, a TOTP completion, or a refresh.
     *
     * <p>Two shapes, because {@code /login} and {@code /login/totp} wrap the
     * session in a discriminated {@link LoginResponse} while {@code /refresh}
     * returns the bare {@code AccessTokenResponse}. Handled here rather than at
     * thirty call sites.
     *
     * <p>Fails loudly on a challenge response rather than returning null. A
     * test that thought it had signed in and silently got a ticket would fail
     * later, somewhere unrelated, with a 401.
     */
    protected String accessTokenOf(MvcResult result) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode session = json.has("session") ? json.get("session") : json;
        if (session == null || !session.has("accessToken")) {
            throw new AssertionError(
                    "No access token on the response. This is a challenge, not a session. Body: "
                            + json);
        }
        return session.get("accessToken").asText();
    }

    /** The {@code status} discriminator from a {@code /login} response. */
    protected String loginStatusOf(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("status")
                .asText();
    }

    /** The challenge or enrolment ticket from a {@code /login} response. */
    protected String ticketOf(MvcResult result) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        if (!json.has("ticket")) {
            throw new AssertionError("No ticket on the response. Body: " + json);
        }
        return json.get("ticket").asText();
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
