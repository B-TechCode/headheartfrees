package com.headheartfrees.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * <strong>The test that is not optional.</strong>
 *
 * <p>PROJECT_BRIEF.md rule 2.2: venting requires no account. This phase added a
 * mandatory second factor to sign-in, and the risk it creates is not that
 * somebody deliberately locks {@code /vent} - it is that a filter-chain entry,
 * a new public path, a changed principal type or a "while we're here" edit
 * quietly makes a credential necessary, and nothing notices because every other
 * test in this phase authenticates first.
 *
 * <p>{@code VentRemainsAnonymousIT} already does this job for phase 5, and this
 * class does not replace it. It is a second one, written against the specific
 * things this phase introduced: an account that owes a factor, a ticket that
 * looks like a token, and a login endpoint that now sometimes refuses to issue
 * a session.
 *
 * <p>It never authenticates successfully, anywhere.
 */
@SpringBootTest
@AutoConfigureMockMvc
class VentUnaffectedByTotpIT extends TotpTestSupport {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("POST /api/v1/vent/release works with no credentials, after all of this")
    void releaseNeedsNoAccount() throws Exception {
        mockMvc.perform(post("/api/v1/vent/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mood\":\"HEAVY\"}"))
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
    @DisplayName("venting works on an install whose only account is an admin owing a factor")
    void ventWorksWhileTheAdminCannotSignIn() throws Exception {
        register("locked-out-admin@example.com", PASSWORD, "The Admin");
        promoteToAdmin("locked-out-admin@example.com");

        // The admin is mid-enrolment and holds no session. The one account on
        // this install cannot currently reach the moderation queue.
        login("locked-out-admin@example.com", PASSWORD)
                .andExpect(jsonPath("$.status").value("TOTP_ENROLMENT_REQUIRED"));

        resetRateLimiter();

        // None of which has anything to do with the product.
        mockMvc.perform(post("/api/v1/vent/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mood\":\"TIRED\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/vent/stats")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("a challenge ticket presented to a vent endpoint is ignored, not rejected")
    void aTicketDoesNotBreakVenting() throws Exception {
        Enrolled enrolled = enrolUser("venter@example.com");
        String ticket = ticketOf(login(enrolled.email(), PASSWORD).andReturn());

        // A ticket is refused as a Bearer token everywhere - but "refused" must
        // mean "treated as anonymous" here, not "401". A stale credential in a
        // browser must never turn into a failure on the one part of the site
        // that is unconditionally available.
        mockMvc.perform(post("/api/v1/vent/release")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ticket)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mood\":\"NUMB\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/vent/stats")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ticket))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("an access token that predates the type claim does not break venting either")
    void aTokenWithNoTypeClaimDoesNotBreakVenting() throws Exception {
        // Every access token issued before this phase lacks the type claim and
        // is now refused. Refused must still mean anonymous here.
        mockMvc.perform(post("/api/v1/vent/release")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not.a.real.jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mood\":\"LOST\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("none of this added a column to vent_events")
    void theVentTableIsUnchanged() throws Exception {
        Enrolled enrolled = enrolUser("schema@example.com");
        String ticket = ticketOf(login(enrolled.email(), PASSWORD).andReturn());
        completeSignIn(ticket, signInCodeFor(enrolled)).andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/vent/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mood\":\"HEAVY\"}"))
                .andExpect(status().isNoContent());

        var columns = new JdbcTemplate(dataSource).queryForList(
                """
                SELECT column_name FROM information_schema.columns
                WHERE table_name = 'vent_events'
                """,
                String.class);

        assertThat(columns)
                .as("A second factor on sign-in has no business changing what a release records")
                .containsExactlyInAnyOrder("id", "mood", "created_at");
    }

    @Test
    @DisplayName("the new tables have no relationship to vent_events, in either direction")
    void noForeignKeyTouchesTheVentTable() {
        var references = new JdbcTemplate(dataSource).queryForList(
                """
                SELECT tc.table_name || ' -> ' || ccu.table_name
                FROM information_schema.table_constraints tc
                JOIN information_schema.constraint_column_usage ccu
                  ON tc.constraint_name = ccu.constraint_name
                WHERE tc.constraint_type = 'FOREIGN KEY'
                  AND (tc.table_name = 'vent_events' OR ccu.table_name = 'vent_events')
                """,
                String.class);

        assertThat(references)
                .as("A foreign key between a vent row and anything identifying is how rule 2.2 "
                        + "dies quietly. It must stay empty.")
                .isEmpty();
    }
}
