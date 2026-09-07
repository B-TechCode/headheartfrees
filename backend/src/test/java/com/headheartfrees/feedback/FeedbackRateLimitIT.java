package com.headheartfrees.feedback;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Three submissions an hour per address (PROJECT_BRIEF.md section 6).
 *
 * <p>This is the one feedback class that never calls {@code resetRateLimiter}
 * mid-test. Every other class does, and calling it is a statement that the
 * test is about something else.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FeedbackRateLimitIT extends FeedbackTestSupport {

    @Test
    @DisplayName("the fourth submission in an hour is refused with Retry-After")
    void fourthIsRefused() throws Exception {
        for (int attempt = 1; attempt <= 3; attempt++) {
            submit(body(4, "Submission number " + attempt + ", perfectly ordinary."))
                    .andExpect(status().isCreated());
        }

        submit(body(4, "The fourth one, which should not be stored."))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
                // The header is only readable by a browser because
                // SecurityConfig exposes it across origins; CorsExposedHeadersIT
                // guards that half.
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    @DisplayName("a refused submission stores nothing")
    void refusedSubmissionsAreNotStored() throws Exception {
        for (int attempt = 1; attempt <= 3; attempt++) {
            submit(body(3, "Filling the bucket, message " + attempt + " of three."))
                    .andExpect(status().isCreated());
        }
        submit(body(3, "This one is refused and must not reach the table."))
                .andExpect(status().isTooManyRequests());

        org.assertj.core.api.Assertions.assertThat(feedbackRepository.count())
                .as("The refused submission must not have been written")
                .isEqualTo(3);
    }
}
