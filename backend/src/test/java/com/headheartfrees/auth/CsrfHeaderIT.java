package com.headheartfrees.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.headheartfrees.common.web.CsrfHeaderFilter;
import jakarta.servlet.http.Cookie;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * The {@code X-Requested-With} requirement on the two cookie-authenticated
 * POSTs.
 *
 * <p>Every request here is built inline rather than through
 * {@code AuthTestSupport.guardedPost}. A test of the guard that got its header
 * from the same helper the rest of the suite uses would still pass if that
 * helper sent the wrong thing, which is the one failure this class exists to
 * catch.
 *
 * <p>The rejection assertions do more than check a status. A guard that refused
 * the request <em>after</em> consuming the refresh token would be worse than no
 * guard at all - it would hand an attacker the forced-rotation outcome the
 * filter is there to prevent, while looking like it worked. So each one also
 * asserts the token survived: the row count is unchanged and the original
 * cookie still refreshes afterwards.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CsrfHeaderIT extends AuthTestSupport {

    private static final String EMAIL = "csrf@example.com";

    /** The default of {@code app.cors.allowed-origins}. */
    private static final String ORIGIN = "http://localhost:3000";

    @Autowired
    private RefreshTokenRepository refreshTokens;

    @Test
    @DisplayName("refresh succeeds with the header")
    void refreshWithHeaderSucceeds() throws Exception {
        MvcResult login = registerAndLogin(EMAIL);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header(CsrfHeaderFilter.HEADER, CsrfHeaderFilter.REQUIRED_VALUE)
                        .cookie(refreshCookieOf(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("logout succeeds with the header")
    void logoutWithHeaderSucceeds() throws Exception {
        MvcResult login = registerAndLogin(EMAIL);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header(CsrfHeaderFilter.HEADER, CsrfHeaderFilter.REQUIRED_VALUE)
                        .cookie(refreshCookieOf(login)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("refresh without the header is refused, and rotates nothing")
    void refreshWithoutHeaderIsRefused() throws Exception {
        MvcResult login = registerAndLogin(EMAIL);
        Cookie cookie = refreshCookieOf(login);
        long before = refreshTokens.count();

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(cookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(CsrfHeaderFilter.CODE))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/refresh"))
                // No Set-Cookie: the browser's copy must be left exactly as it
                // was, or the "rejection" has itself ended the session.
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

        assertThat(refreshTokens.count())
                .as("A refused request must not have spent or issued a token")
                .isEqualTo(before);

        // The real proof: the same cookie still works. If the refused call had
        // consumed it, this would 401 - and in production the next legitimate
        // refresh would trip reuse detection and revoke the whole family, which
        // is precisely the attack.
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header(CsrfHeaderFilter.HEADER, CsrfHeaderFilter.REQUIRED_VALUE)
                        .cookie(cookie))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("logout without the header is refused, and revokes nothing")
    void logoutWithoutHeaderIsRefused() throws Exception {
        MvcResult login = registerAndLogin(EMAIL);
        Cookie cookie = refreshCookieOf(login);

        mockMvc.perform(post("/api/v1/auth/logout").cookie(cookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(CsrfHeaderFilter.CODE))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

        // Still signed in. A logout CSRF that "failed" but revoked the family
        // would have achieved the attacker's goal.
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header(CsrfHeaderFilter.HEADER, CsrfHeaderFilter.REQUIRED_VALUE)
                        .cookie(cookie))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("the rejection code is distinct from UNAUTHORIZED")
    void theCodeIsDistinguishable() throws Exception {
        // A client has to tell "you forgot the header" from "sign in again".
        // Conflating them sends someone to re-authenticate over a caller bug,
        // or retries forever over an expired session.
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(CsrfHeaderFilter.CODE));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header(CsrfHeaderFilter.HEADER, CsrfHeaderFilter.REQUIRED_VALUE))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(Matchers.not(CsrfHeaderFilter.CODE)));
    }

    @Test
    @DisplayName("login and register are not guarded")
    void credentialCarryingEndpointsAreUnguarded() throws Exception {
        // Deliberately unprotected: the credential is in the body, so forging
        // one of these gains an attacker nothing they could not do by calling
        // the endpoint directly. Guarding them would be surface for no benefit.
        register(EMAIL, PASSWORD, "Test Person");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, PASSWORD))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("vent release still works with no header at all")
    void ventIsUntouched() throws Exception {
        // PROJECT_BRIEF.md rule 2.2. There is no session on this path to forge,
        // and it must stay callable by any client with no ceremony. If this
        // ever fails, the filter has grown a path it must not have.
        mockMvc.perform(post("/api/v1/vent/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mood\":\"ANGRY\"}"))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @DisplayName("the preflight allows the header, or the browser never sends the real request")
    void preflightAllowsTheHeader() throws Exception {
        // X-Requested-With is not CORS-safelisted. Without it in
        // allowedHeaders the preflight fails and the frontend cannot call
        // refresh at all - while curl still can, which is the failure that
        // looks like the guard working.
        mockMvc.perform(options("/api/v1/auth/refresh")
                        .header(HttpHeaders.ORIGIN, ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, CsrfHeaderFilter.HEADER))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                        Matchers.containsString(CsrfHeaderFilter.HEADER)))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    @DisplayName("the preflight itself is not blocked by the guard")
    void preflightIsNotGuarded() throws Exception {
        // The preflight carries Access-Control-Request-Headers, never the
        // header itself. A filter that checked every method rather than POST
        // would reject it and make the endpoint unreachable from any browser.
        mockMvc.perform(options("/api/v1/auth/logout")
                        .header(HttpHeaders.ORIGIN, ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk());
    }
}
