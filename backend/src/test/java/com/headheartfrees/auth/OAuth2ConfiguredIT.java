package com.headheartfrees.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.headheartfrees.PostgresTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Google sign-in is reachable when the two credentials are set.
 *
 * <h2>Why this exists</h2>
 *
 * {@link OAuth2AbsentConfigIT} covers the case where Google is unconfigured,
 * and for phase 5 it was the only test touching any of this. It asserts that no
 * {@code ClientRegistrationRepository} exists - which was true, and stayed true
 * with credentials set, because
 * {@link com.headheartfrees.config.GoogleOAuth2EnvironmentPostProcessor} was
 * registered in a file Spring Boot does not read for this interface. The suite
 * was green the entire time. A pair of tests where only the negative one exists
 * will pass for a class that is never loaded, so this is the positive half.
 *
 * <p>The credentials are inlined test properties rather than real ones. The
 * post-processor reads {@code GOOGLE_CLIENT_ID} from the {@code Environment},
 * so anything that puts it there works, and a fake id proves the plumbing
 * exactly as well as a real one: Spring builds the authorization URL from the
 * id without contacting Google. It also keeps the test independent of whatever
 * the developer happens to have exported.
 */
@SpringBootTest(
        properties = {
            "GOOGLE_CLIENT_ID=test-client-id",
            "GOOGLE_CLIENT_SECRET=test-client-secret"
        })
@AutoConfigureMockMvc
class OAuth2ConfiguredIT extends PostgresTestBase {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("the post-processor runs and Spring builds a client registration")
    void clientRegistrationExists() {
        assertThat(context.getBeanNamesForType(ClientRegistrationRepository.class))
                .as("empty here means GoogleOAuth2EnvironmentPostProcessor did not run - "
                        + "check how it is registered in META-INF/spring.factories")
                .isNotEmpty();
    }

    @Test
    @DisplayName("/oauth2/authorization/google redirects to Google rather than 404ing")
    void authorizationEndpointRedirectsToGoogle() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(
                        "Location",
                        org.hamcrest.Matchers.startsWith(
                                "https://accounts.google.com/o/oauth2/v2/auth")))
                .andExpect(header().string(
                        "Location", org.hamcrest.Matchers.containsString("client_id=test-client-id")));
    }

    @Test
    @DisplayName("the scopes the phase-5 sign-in handler needs are requested")
    void requestsOpenIdEmailAndProfile() throws Exception {
        // GoogleSignInHandler reads email and name off the OIDC user, so a
        // registration that came up with default scopes would fail at the
        // callback rather than here.
        String location = mockMvc.perform(get("/oauth2/authorization/google"))
                .andReturn()
                .getResponse()
                .getHeader("Location");

        assertThat(location).isNotNull();
        assertThat(java.net.URLDecoder.decode(location, java.nio.charset.StandardCharsets.UTF_8))
                .contains("openid")
                .contains("email")
                .contains("profile");
    }
}
