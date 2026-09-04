package com.headheartfrees.common.web;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Boots the full application context - including {@code SecurityConfig} and the
 * Spring Security filter chain - so that "the health endpoint is public" is
 * asserted against the same security setup production runs.
 */
@SpringBootTest
@AutoConfigureMockMvc
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("health is reachable without authentication and reports UP")
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/api/v1/health").with(anonymous()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.version").value(not(blankOrNullString())))
                .andExpect(jsonPath("$.time").value(not(blankOrNullString())));
    }

    @Test
    @DisplayName("security is actually enforced, so the 200 above means something")
    void unlistedPathsAreNotPublic() throws Exception {
        mockMvc.perform(get("/api/v1/not-a-real-endpoint").with(anonymous()))
                .andExpect(status().is4xxClientError());
    }
}
