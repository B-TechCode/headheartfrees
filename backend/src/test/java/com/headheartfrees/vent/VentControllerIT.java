package com.headheartfrees.vent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.headheartfrees.common.web.ClientIpRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The vent endpoints against real Postgres, with the real Flyway migration and
 * the real security chain.
 *
 * <p>Testcontainers rather than H2 on purpose: the migration uses
 * {@code BIGSERIAL}, {@code TIMESTAMPTZ} and a CHECK constraint, and an
 * in-memory database that merely tolerates that SQL would prove nothing about
 * whether production will start.
 */
@SpringBootTest
@AutoConfigureMockMvc
class VentControllerIT extends com.headheartfrees.PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VentEventRepository repository;

    @Autowired
    private ClientIpRateLimiter rateLimiter;

    @BeforeEach
    void resetState() {
        repository.deleteAll();
        // Buckets are per-JVM and would otherwise leak between test methods.
        rateLimiter.reset();
    }

    @Test
    @DisplayName("release with a mood returns 204 and records one row")
    void releaseWithMood() throws Exception {
        mockMvc.perform(post("/api/v1/vent/release")
                        .with(anonymous())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mood\":\"HEAVY\"}"))
                .andExpect(status().isNoContent());

        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.findAll().getFirst().getMood()).isEqualTo(Mood.HEAVY);
    }

    @Test
    @DisplayName("release without a mood is accepted — the chip is optional")
    void releaseWithoutMood() throws Exception {
        mockMvc.perform(post("/api/v1/vent/release")
                        .with(anonymous())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNoContent());

        assertThat(repository.findAll().getFirst().getMood()).isNull();
    }

    @Test
    @DisplayName("release with no body at all still counts")
    void releaseWithNoBody() throws Exception {
        mockMvc.perform(post("/api/v1/vent/release").with(anonymous()))
                .andExpect(status().isNoContent());

        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("an unknown mood is rejected and nothing is stored")
    void unknownMoodIsRejected() throws Exception {
        // The central guarantee of the enum: the column cannot become a
        // free-text field by way of the client sending whatever it likes.
        mockMvc.perform(post("/api/v1/vent/release")
                        .with(anonymous())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mood\":\"DEVASTATED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        assertThat(repository.count()).isZero();
    }

    @Test
    @DisplayName("a free-text mood is rejected, which is the rule 2.1 case")
    void freeTextMoodIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/vent/release")
                        .with(anonymous())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mood\":\"today my father said something I cannot repeat\"}"))
                .andExpect(status().isBadRequest());

        assertThat(repository.count()).isZero();
    }

    @Test
    @DisplayName("unknown body fields are ignored, not stored")
    void unexpectedFieldsAreNotPersisted() throws Exception {
        // If someone wires a client that sends the textarea contents anyway,
        // the server must drop it on the floor rather than bind it.
        mockMvc.perform(post("/api/v1/vent/release")
                        .with(anonymous())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mood\":\"TIRED\",\"content\":\"please do not store this\"}"))
                .andExpect(status().isNoContent());

        VentEvent stored = repository.findAll().getFirst();
        assertThat(stored.getMood()).isEqualTo(Mood.TIRED);
        // The entity has no field that could have taken it; asserted in
        // VentRuleArchitectureTest. Here we simply confirm the request succeeded
        // without the extra field changing anything.
        assertThat(stored.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("stats returns the real count, and it starts at zero")
    void statsReturnsRealCount() throws Exception {
        mockMvc.perform(get("/api/v1/vent/stats").with(anonymous()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReleases").value(0));

        mockMvc.perform(post("/api/v1/vent/release")
                        .with(anonymous())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mood\":\"LOST\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/vent/release").with(anonymous()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/vent/stats").with(anonymous()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReleases").value(2))
                // No countries field: it was removed from the brief because it
                // would have required geolocating an IP we refuse to retain.
                .andExpect(jsonPath("$.countries").doesNotExist());
    }

    @Test
    @DisplayName("both endpoints are reachable without an account")
    void ventIsPublic() throws Exception {
        // Rule 2.2. If a future security change breaks this, it breaks the
        // product's central promise, so it is asserted rather than assumed.
        mockMvc.perform(get("/api/v1/vent/stats").with(anonymous()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/vent/release").with(anonymous()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("the 31st release in a minute is refused with 429 and Retry-After")
    void rateLimitReturns429WithRetryAfter() throws Exception {
        for (int i = 0; i < 30; i++) {
            mockMvc.perform(post("/api/v1/vent/release").with(anonymous()))
                    .andExpect(status().isNoContent());
        }

        mockMvc.perform(post("/api/v1/vent/release").with(anonymous()))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
                .andExpect(jsonPath("$.status").value(429));

        // The refused request must not have been counted.
        assertThat(repository.count()).isEqualTo(30);
    }
}
