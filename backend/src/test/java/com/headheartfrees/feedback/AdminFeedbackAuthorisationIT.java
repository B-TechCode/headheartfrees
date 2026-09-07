package com.headheartfrees.feedback;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.headheartfrees.auth.AuthTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

/**
 * The first method security in the project, tested properly.
 *
 * <h2>Why this class exists</h2>
 *
 * Phase 5 fixed the 401-vs-403 mapping and could only test it through the
 * filter chain, because nothing was annotated with {@code @PreAuthorize}. Its
 * log recorded the gap as an open item and named this phase as the one that
 * would close it. Method security takes a different route to the response than
 * a filter-chain rejection does - an {@code AccessDeniedException} thrown by
 * an interceptor after the request has been dispatched, reaching
 * {@code GlobalExceptionHandler} rather than {@code SecurityErrorHandler} -
 * so the phase 5 tests genuinely did not cover it.
 *
 * <h2>Why 401 and 403 must not be swapped</h2>
 *
 * They mean different things to a client and produce different behaviour.
 * A 401 says "you are not signed in", so a client re-authenticates. A signed-in
 * USER who receives 401 from an admin route is therefore sent to sign in again
 * over a permission they will never have, and does it in a loop. The
 * distinction is the difference between "log in" and "you cannot do this".
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminFeedbackAuthorisationIT extends FeedbackTestSupport {

    @Autowired
    private AuthTestFixtures accounts;

    @Test
    @DisplayName("anonymous GET of the queue is 401, not 200 and not 403")
    void anonymousIsUnauthorised() throws Exception {
        // This is also the ordering guard. `/api/v1/admin/**` is declared
        // before every permitAll entry in SecurityConfig; if a later edit puts
        // a broader public matcher above it, the queue becomes world-readable
        // and this line turns red.
        mockMvc.perform(get("/api/v1/admin/feedback"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("anonymous PATCH is 401 as well")
    void anonymousCannotModerate() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/feedback/" + submitPending())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("an authenticated USER is 403, not 401")
    void userIsForbidden() throws Exception {
        String token = accounts.accessTokenForUser("plain@example.com");

        mockMvc.perform(get("/api/v1/admin/feedback").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("an authenticated USER cannot moderate either")
    void userCannotModerate() throws Exception {
        String token = accounts.accessTokenForUser("plain2@example.com");

        mockMvc.perform(patch("/api/v1/admin/feedback/" + submitPending())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("an ADMIN gets the queue")
    void adminIsAllowed() throws Exception {
        String token = accounts.accessTokenForAdmin("boss@example.com");

        mockMvc.perform(get("/api/v1/admin/feedback").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    @DisplayName("an ADMIN can moderate")
    void adminCanModerate() throws Exception {
        String token = accounts.accessTokenForAdmin("boss2@example.com");

        mockMvc.perform(patch("/api/v1/admin/feedback/" + submitPending())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }
}
