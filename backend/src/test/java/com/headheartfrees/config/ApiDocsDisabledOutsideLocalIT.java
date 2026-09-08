package com.headheartfrees.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.headheartfrees.PostgresTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Swagger UI and the OpenAPI JSON must not exist outside the local profile.
 *
 * <h2>Why this needs its own boot</h2>
 *
 * Every other test in this project runs under {@code @ActiveProfiles("local")},
 * because {@code JwtSecretGuard} refuses to start on the committed development
 * secret anywhere else. That is correct, and it means the entire suite has only
 * ever observed the configuration where these endpoints are <em>on</em>.
 *
 * <p>So this class starts the application the way a deployment does: a profile
 * that is not {@code local}, and a real signing secret supplied as a property.
 * Without it, "disabled by default" is a claim about a YAML file that nothing
 * checks — and the default is the case that matters, since a deployment which
 * forgets to set a profile at all gets it.
 *
 * <p>What is being kept off is not cosmetic. {@code /v3/api-docs} is a machine
 * -readable map of every endpoint, its request schema and its validation rules,
 * served to anybody who asks.
 *
 * <p>{@code inheritProfiles = false} is what stops the base class's
 * {@code local} from being merged in and quietly making this test vacuous.
 */
@SpringBootTest(properties = {
    // A real 256-bit secret, so JwtSecretGuard permits a non-local boot.
    "app.auth.jwt-secret=dGhpcy1pcy1hLXRlc3Qtb25seS1zZWNyZXQtd2l0aC0zMitieXRlcy1pbi1pdA==",
})
@AutoConfigureMockMvc
@ActiveProfiles(value = "ci", inheritProfiles = false)
class ApiDocsDisabledOutsideLocalIT extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("the OpenAPI JSON is not served outside local")
    void apiDocsAreOff() throws Exception {
        // 404 rather than 401: the endpoint does not exist at all, so there is
        // nothing to authenticate against and nothing to probe.
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Swagger UI is not served outside local")
    void swaggerUiIsOff() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("the application's own health endpoint still works")
    void healthStillWorks() throws Exception {
        // The guard on the guard: if the context failed to start in a way that
        // 404'd everything, the two assertions above would pass for the wrong
        // reason. This proves the app is actually up and serving.
        mockMvc.perform(get("/api/v1/health")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("no actuator endpoint is exposed")
    void actuatorIsClosed() throws Exception {
        // Never 200. `env` and `configprops` in particular would publish which
        // configuration and which secrets are set.
        mockMvc.perform(get("/actuator")).andExpect(status().is4xxClientError());
        mockMvc.perform(get("/actuator/env")).andExpect(status().is4xxClientError());
        mockMvc.perform(get("/actuator/health")).andExpect(status().is4xxClientError());
    }
}
