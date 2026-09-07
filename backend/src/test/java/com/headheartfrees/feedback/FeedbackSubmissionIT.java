package com.headheartfrees.feedback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.headheartfrees.auth.AuthTestFixtures;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

/**
 * Submitting a note: anonymously, signed in, and badly.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FeedbackSubmissionIT extends FeedbackTestSupport {

    @Autowired
    private AuthTestFixtures accounts;

    @Test
    @DisplayName("an anonymous submission is stored as PENDING with no user id")
    void anonymousSubmission() throws Exception {
        submit(body(5, "This was a quiet place to put something down."))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(5))
                // The submitter is told what they wrote, not what status it is
                // in: "submitted" and "published" are different things.
                .andExpect(jsonPath("$.status").doesNotExist());

        Feedback stored = feedbackRepository.findAll().getFirst();
        assertThat(stored.getStatus()).isEqualTo(FeedbackStatus.PENDING);
        assertThat(stored.getUserId()).as("Anonymous means no user id").isNull();
    }

    @Test
    @DisplayName("a signed-in submission records the user id")
    void signedInSubmission() throws Exception {
        String token = accounts.accessTokenForUser("writer@example.com");

        mockMvc.perform(post("/api/v1/feedback")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(4, "Signed in, and happy to be named here.")))
                .andExpect(status().isCreated());

        assertThat(feedbackRepository.findAll().getFirst().getUserId())
                .isEqualTo(accounts.idOf("writer@example.com"));
    }

    @Test
    @DisplayName("being signed in does not force attribution")
    void signedInCanStayAnonymous() throws Exception {
        String token = accounts.accessTokenForUser("shy@example.com");

        // No displayName in the body: the person cleared the prefilled field.
        // The row still records who submitted it, for removal requests, but
        // nothing published carries a name.
        mockMvc.perform(post("/api/v1/feedback")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(3, "I would rather not have my name on this one.")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.displayName").doesNotExist());

        assertThat(feedbackRepository.findAll().getFirst().getDisplayName()).isNull();
    }

    @Test
    @DisplayName("an empty display name is stored as absent, not as an empty string")
    void blankNameBecomesNull() throws Exception {
        submit("""
                {"rating":4,"message":"A note with the name field cleared.","displayName":"  "}
                """)
                .andExpect(status().isCreated());

        assertThat(feedbackRepository.findAll().getFirst().getDisplayName()).isNull();
    }

    @Test
    @DisplayName("created_at is truncated to the hour, so it cannot be lined up with a release")
    void createdAtIsCoarse() throws Exception {
        submitPending();

        // The brief forbids linking a feedback row to a vent row even for
        // someone holding the database, and names timestamp correlation. Full
        // precision here would make a release at 14:32:10 and a note at
        // 14:33:45 the same person at this site's traffic.
        assertThat(feedbackRepository.findAll().getFirst().getCreatedAt())
                .isEqualTo(feedbackRepository.findAll().getFirst()
                        .getCreatedAt().truncatedTo(ChronoUnit.HOURS));
    }

    @Test
    @DisplayName("a rating outside 1-5 is refused")
    void ratingIsBounded() throws Exception {
        submit(body(0, "A note with a rating of zero on it.")).andExpect(status().isBadRequest());
        resetRateLimiter();
        submit(body(6, "A note with a rating of six on it.")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("a message shorter than 10 characters is refused")
    void messageHasAFloor() throws Exception {
        submit(body(4, "Thanks")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("\"Thank you.\" is accepted - the floor is 10, not 20")
    void theShortestHonestAnswerIsAccepted() throws Exception {
        // Ten characters exactly. The likeliest real message on this page, and
        // a 20-character floor would have rejected it.
        submit(body(5, "Thank you.")).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("markup is rejected rather than sanitised")
    void markupIsRejected() throws Exception {
        submit(body(4, "<script>alert(1)</script> and some padding text"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        resetRateLimiter();
        submit(body(4, "&lt;script&gt; written as entities instead"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("\"<3\" is allowed, because rejecting every angle bracket would be wrong")
    void heartsSurvive() throws Exception {
        // On a site about feelings, refusing the most common affectionate
        // thing there is would be a poor trade for a simpler regex.
        submit(body(5, "this place helped me a lot <3 thank you"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("a location with a comma is refused, so it cannot become an address")
    void locationIsOnePlace() throws Exception {
        submit("""
                {"rating":4,"message":"A note with a full address attached.",\
                "location":"Mumbai, Maharashtra, India"}
                """)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("a submission cannot ask to be published")
    void statusCannotBeSetByTheClient() throws Exception {
        // There is no status field on the request record, so this is ignored
        // rather than honoured. The assertion is that it did not take effect.
        submit("""
                {"rating":5,"message":"Trying to publish this without a moderator.",\
                "status":"APPROVED"}
                """);

        assertThat(feedbackRepository.findAll())
                .allSatisfy(row -> assertThat(row.getStatus()).isEqualTo(FeedbackStatus.PENDING));
    }
}
