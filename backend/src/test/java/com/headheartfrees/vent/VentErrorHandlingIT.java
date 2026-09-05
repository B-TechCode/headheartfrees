package com.headheartfrees.vent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The 500 path.
 *
 * <p>The Phase 2a log flagged this as the one branch of
 * {@code GlobalExceptionHandler} with no test: the handler whose entire job is
 * to leak nothing was also the one nothing exercised. There is a service layer
 * to throw from now, so this closes it.
 *
 * <p>What is actually being checked is not "returns 500" but "returns 500 and
 * says nothing". The exception below carries a deliberately incriminating
 * message, and the assertions prove none of it reaches the client.
 */
@SpringBootTest
@AutoConfigureMockMvc
class VentErrorHandlingIT extends com.headheartfrees.PostgresTestBase {

    /** Text that must never appear in a response body. */
    private static final String LEAKY_MESSAGE =
            "jdbc:postgresql://db:5432/headheartfrees password=hunter2 at com.headheartfrees.internal";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VentService ventService;

    @Test
    @DisplayName("an unexpected failure returns 500 in the standard shape and leaks nothing")
    void unexpectedFailureLeaksNothing() throws Exception {
        willThrow(new IllegalStateException(LEAKY_MESSAGE))
                .given(ventService).recordRelease(any());

        mockMvc.perform(post("/api/v1/vent/release")
                        .with(anonymous())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mood\":\"HEAVY\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.path").value("/api/v1/vent/release"))
                .andExpect(jsonPath("$.timestamp").exists())
                // The generic message, not the exception's.
                .andExpect(jsonPath("$.message")
                        .value("Something went wrong on our end. Please try again."))
                // No stack trace, no exception type, no cause.
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(jsonPath("$.fieldErrors").doesNotExist())
                // And nothing anywhere in the body resembling the internals.
                .andExpect(result -> {
                    String body = result.getResponse().getContentAsString();
                    if (body.contains("hunter2")
                            || body.contains("jdbc:")
                            || body.contains("IllegalStateException")
                            || body.contains("com.headheartfrees.internal")) {
                        throw new AssertionError(
                                "500 response leaked internal detail. Body was: " + body);
                    }
                });
    }
}
