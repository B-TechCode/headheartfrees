package com.headheartfrees.feedback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.headheartfrees.auth.AuthTestFixtures;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

/**
 * A PENDING row must not reach {@code /voices} under any parameter
 * combination, and the public shape must not carry the private fields.
 *
 * <h2>Why the parameter sweep</h2>
 *
 * The obvious test - submit one note, check the list is empty - passes against
 * an implementation that filters in the controller and would keep passing if
 * somebody added {@code ?status=} for the admin UI's convenience. So this
 * class throws the whole surface at it: negative pages, huge sizes, an
 * injected sort, and a status parameter that does not exist. Every one must
 * return nothing.
 *
 * <p>The structural defence is upstream of all of this - the repository method
 * takes no status argument and the response record has nowhere to put a
 * {@code userId} - and that is what actually makes the property hold. These
 * tests are the alarm on it.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PublicFeedbackNeverLeaksIT extends FeedbackTestSupport {

    @Autowired
    private AuthTestFixtures accounts;

    /** Every way a caller might try to widen the public list. */
    private static final List<String> HOSTILE_QUERIES = List.of(
            "",
            "?page=0&size=50",
            "?page=-1&size=-5",
            "?page=0&size=100000",
            "?size=2147483647",
            "?status=PENDING",
            "?status=",
            "?sort=status,desc",
            "?sort=createdAt,asc&status=PENDING&page=0&size=50");

    @Test
    @DisplayName("a pending note is invisible under every parameter combination")
    void pendingNeverAppears() throws Exception {
        submitPending();
        submitPending();

        assertThat(feedbackRepository.count())
            .as("The rows must exist, or this test proves nothing")
            .isEqualTo(2);

        for (String query : HOSTILE_QUERIES) {
            mockMvc.perform(get("/api/v1/feedback" + query))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items").isEmpty());
        }
    }

    @Test
    @DisplayName("a rejected note is invisible too")
    void rejectedNeverAppears() throws Exception {
        String id = submitPending();
        moderate(id, "REJECTED");

        for (String query : HOSTILE_QUERIES) {
            mockMvc.perform(get("/api/v1/feedback" + query))
                    .andExpect(jsonPath("$.items").isEmpty());
        }
    }

    @Test
    @DisplayName("an approved note appears, carrying only the public fields")
    void approvedAppearsWithoutPrivateFields() throws Exception {
        String id = submitPending();
        moderate(id, "APPROVED");

        mockMvc.perform(get("/api/v1/feedback"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].rating").value(4))
                .andExpect(jsonPath("$.items[0].message").isNotEmpty())
                // The fields that must never be here. `doesNotExist` rather
                // than `isEmpty`: a null would still be a field on the wire
                // telling a reader that the concept exists.
                .andExpect(jsonPath("$.items[0].userId").doesNotExist())
                .andExpect(jsonPath("$.items[0].status").doesNotExist())
                .andExpect(jsonPath("$.items[0].moderatedBy").doesNotExist())
                .andExpect(jsonPath("$.items[0].moderatedAt").doesNotExist())
                // Absent because publishing it would hand the vent-correlation
                // risk to anyone with a browser. See DefaultFeedbackService.
                .andExpect(jsonPath("$.items[0].createdAt").doesNotExist());
    }

    @Test
    @DisplayName("a withdrawn note disappears again")
    void approvalIsReversible() throws Exception {
        String id = submitPending();

        moderate(id, "APPROVED");
        mockMvc.perform(get("/api/v1/feedback"))
                .andExpect(jsonPath("$.items.length()").value(1));

        // Community Guidelines promise removal on request, so this direction
        // has to work as well as the other one.
        moderate(id, "REJECTED");
        mockMvc.perform(get("/api/v1/feedback"))
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    @DisplayName("the public list is reachable with no account at all")
    void publicListNeedsNoAccount() throws Exception {
        // No Authorization header anywhere in this class, which is the point:
        // /voices is public and the release flow it follows requires no account.
        mockMvc.perform(get("/api/v1/feedback"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    private void moderate(String id, String decision) throws Exception {
        String token = accounts.accessTokenForAdmin("mod@example.com");
        mockMvc.perform(patch("/api/v1/admin/feedback/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"%s\"}".formatted(decision)))
                .andExpect(status().isOk());
    }
}
