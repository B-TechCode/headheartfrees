package com.headheartfrees.feedback;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.headheartfrees.PostgresTestBase;
import com.headheartfrees.auth.AuthTestFixtures;
import com.headheartfrees.common.web.ClientIpRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Shared fixtures for the feedback integration tests.
 *
 * <p>Not named {@code *IT} and not annotated, so failsafe does not try to run
 * it as a suite of its own - the same arrangement as {@code AuthTestSupport}.
 *
 * <p>The rate limiter is reset before each test because {@code FEEDBACK} is
 * three per <em>hour</em> and every test in the JVM shares one loopback
 * address. Without the reset, the fourth submission anywhere in the suite
 * starts failing with 429 for reasons unrelated to what is being tested.
 * {@code FeedbackRateLimitIT} is the one class that does not reset mid-test.
 */
@Import(AuthTestFixtures.Registration.class)
abstract class FeedbackTestSupport extends PostgresTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected FeedbackRepository feedbackRepository;

    @Autowired
    private ClientIpRateLimiter rateLimiter;

    @BeforeEach
    void resetFeedbackState() {
        feedbackRepository.deleteAll();
        rateLimiter.reset();
    }

    /** Clears the buckets mid-test, for tests that are not about the limiter. */
    protected void resetRateLimiter() {
        rateLimiter.reset();
    }

    protected ResultActions submit(String json) throws Exception {
        return mockMvc.perform(post("/api/v1/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    /** A valid body, with the given message. */
    protected static String body(int rating, String message) {
        return "{\"rating\":%d,\"message\":\"%s\"}".formatted(rating, message);
    }

    /**
     * Submits one valid note and returns its id, resetting the limiter first.
     *
     * <p>The reset is what lets a test set up several rows without tripping a
     * three-per-hour limit that it is not testing.
     */
    protected String submitPending() throws Exception {
        resetRateLimiter();
        ResultActions result = submit(body(4, "A perfectly ordinary note about the site."));
        JsonNode json = objectMapper.readTree(
                result.andReturn().getResponse().getContentAsString());
        return json.get("id").asText();
    }
}
