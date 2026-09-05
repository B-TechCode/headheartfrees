package com.headheartfrees.common.web;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Verifies that failures come back in the one error shape from
 * PROJECT_BRIEF.md section 6, through the real filter chain and the real
 * {@code DispatcherServlet}.
 *
 * <p>No production endpoint accepts a request body yet, so a probe controller is
 * registered for the duration of this test to give bean validation something to
 * reject. It lives here rather than in {@code src/main} deliberately - and
 * outside {@code /api/v1/vent/**} in particular, so that no test fixture with a
 * free-text field ever sits inside the vent namespace (PROJECT_BRIEF.md 2.1).
 */
@SpringBootTest
@AutoConfigureMockMvc
class GlobalExceptionHandlerTest extends com.headheartfrees.PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;

    // -- validation -------------------------------------------------------

    @Test
    @WithMockUser
    @DisplayName("a rejected body returns 400 with one entry per invalid field")
    void validationFailureReportsFieldErrors() throws Exception {
        String invalidBody = """
                { "name": "  ", "amount": 0 }
                """;

        mockMvc.perform(post("/__test/validation-probe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.path").value("/__test/validation-probe"))
                .andExpect(jsonPath("$.timestamp").value(not(blankOrNullString())))
                .andExpect(jsonPath("$.message").value(not(blankOrNullString())))
                // Both fields are invalid, and both are named.
                .andExpect(jsonPath("$.fieldErrors.name").value(not(blankOrNullString())))
                .andExpect(jsonPath("$.fieldErrors.amount").value(not(blankOrNullString())));
    }

    @Test
    @WithMockUser
    @DisplayName("a valid body passes, so the test above fails for the right reason")
    void validBodyIsAccepted() throws Exception {
        String validBody = """
                { "name": "ok", "amount": 3 }
                """;

        mockMvc.perform(post("/__test/validation-probe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isNoContent());
    }

    // -- 404 --------------------------------------------------------------

    @Test
    @DisplayName("an unmapped public path returns 404 in the standard shape")
    void unmappedPathReturnsNotFound() throws Exception {
        // Under /api/v1/vent/**, which is permitAll, so this reaches the
        // dispatcher and 404s rather than being turned away as a 401.
        mockMvc.perform(get("/api/v1/vent/does-not-exist").with(anonymous()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/v1/vent/does-not-exist"))
                .andExpect(jsonPath("$.timestamp").value(not(blankOrNullString())))
                // Optional by contract: absent, not an empty object.
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }

    // -- 405 --------------------------------------------------------------

    @Test
    @DisplayName("a mapped path called with the wrong method returns 405")
    void wrongMethodReturnsMethodNotAllowed() throws Exception {
        mockMvc.perform(post("/api/v1/health").with(anonymous()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.path").value("/api/v1/health"));
    }

    // -- 401 --------------------------------------------------------------

    @Test
    @DisplayName("a filter-chain rejection uses the same shape as controller errors")
    void unauthenticatedRequestReturnsStandardShape() throws Exception {
        // Rejected by Spring Security before dispatch, so this exercises
        // SecurityErrorHandler rather than GlobalExceptionHandler.
        mockMvc.perform(get("/__test/validation-probe").with(anonymous()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/__test/validation-probe"))
                .andExpect(jsonPath("$.timestamp").value(not(blankOrNullString())))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }

    // -- fixtures ---------------------------------------------------------

    @TestConfiguration
    static class ProbeConfiguration {

        @Bean
        ValidationProbeController validationProbeController() {
            return new ValidationProbeController();
        }
    }

    @RestController
    static class ValidationProbeController {

        @PostMapping(path = "/__test/validation-probe", consumes = MediaType.APPLICATION_JSON_VALUE)
        ResponseEntity<Void> accept(@Valid @RequestBody ProbeRequest request) {
            return ResponseEntity.noContent().build();
        }
    }

    record ProbeRequest(@NotBlank String name, @Min(1) int amount) {
    }
}
