package com.headheartfrees.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

/**
 * <strong>The most important test in this phase.</strong>
 *
 * <p>PROJECT_BRIEF.md rule 2.2: venting requires no account. Phase 5 introduced
 * an authentication system, and the risk it creates is not that someone
 * deliberately locks the vent endpoints - it is that a filter-chain change, a
 * default, or a "while we're here" edit quietly makes a credential necessary,
 * and nothing notices because every other test authenticates first.
 *
 * <p>So this test never authenticates. It asserts the vent endpoints work with
 * no header, no cookie, and no account in the database at all - and, because
 * the interesting failure is subtler than a 401, that a signed-in caller's
 * identity does not reach the vent table either.
 *
 * <p>It lives in the {@code auth} test package deliberately: it is a constraint
 * <em>on</em> auth, and it should be the first thing someone editing this module
 * sees fail.
 */
@SpringBootTest
@AutoConfigureMockMvc
class VentRemainsAnonymousIT extends AuthTestSupport {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("POST /api/v1/vent/release works with no credentials whatsoever")
    void releaseNeedsNoAccount() throws Exception {
        mockMvc.perform(post("/api/v1/vent/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mood\":\"HEAVY\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/v1/vent/release works with no body at all and no credentials")
    void releaseWithoutBodyNeedsNoAccount() throws Exception {
        mockMvc.perform(post("/api/v1/vent/release"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/v1/vent/stats works with no credentials")
    void statsNeedsNoAccount() throws Exception {
        mockMvc.perform(get("/api/v1/vent/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReleases").isNumber());
    }

    @Test
    @DisplayName("the vent endpoints work when no account exists in the system at all")
    void ventWorksOnAnInstallWithNoUsers() throws Exception {
        // resetAuthState() has emptied the users table. A vent endpoint that
        // needed to resolve a principal would fail here even if it tolerated a
        // missing header.
        mockMvc.perform(post("/api/v1/vent/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mood\":\"TIRED\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("an expired or forged token does not break venting - it is simply ignored")
    void ventIgnoresABadToken() throws Exception {
        // A stale token in a browser must not turn into a 401 on the one part
        // of the site that is supposed to be unconditionally available.
        mockMvc.perform(post("/api/v1/vent/release")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not.a.real.jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mood\":\"NUMB\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("a signed-in caller's release is recorded with no trace of who they are")
    void authenticatedReleaseRecordsNoIdentity() throws Exception {
        MvcResult login = registerAndLogin("venter@example.com");
        String token = accessTokenOf(login);

        mockMvc.perform(post("/api/v1/vent/release")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .cookie(refreshCookieOf(login))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mood\":\"LOST\"}"))
                .andExpect(status().isNoContent());

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        // The schema guard lives in VentSchemaIT; this asserts the runtime
        // consequence. Even with a fully authenticated caller, the row that
        // lands carries a mood and a timestamp and nothing else.
        var columns = jdbc.queryForList(
                """
                SELECT column_name FROM information_schema.columns
                WHERE table_name = 'vent_events'
                """,
                String.class);

        assertThat(columns)
                .as("An authenticated release must not have caused a user column to exist")
                .containsExactlyInAnyOrder("id", "mood", "created_at");

        Long rows = jdbc.queryForObject(
                "SELECT count(*) FROM vent_events WHERE mood = 'LOST'", Long.class);
        assertThat(rows).isNotNull().isPositive();
    }

    @Test
    @DisplayName("the refresh cookie is scoped so it is never sent to a vent endpoint")
    void refreshCookieDoesNotReachVent() throws Exception {
        MvcResult login = registerAndLogin("scoped@example.com");
        Cookie cookie = refreshCookieOf(login);

        // MockMvc will send any cookie it is handed, so this asserts the
        // attribute rather than the browser's behaviour: Path=/api/v1/auth
        // means a real browser never attaches it to /api/v1/vent/**.
        assertThat(cookie.getPath())
                .as("A refresh cookie scoped to / would ride along on every anonymous "
                        + "vent request, which is the ambient identifier rule 2.2 forbids")
                .isEqualTo("/api/v1/auth");
    }
}
