package com.headheartfrees.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.headheartfrees.PostgresTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The application starts, and every non-Google auth path works, with no Google
 * credentials configured.
 *
 * <p>This is the test for the {@code ObjectProvider} in {@code SecurityConfig}.
 * Injecting {@link ClientRegistrationRepository} directly would be the obvious
 * way to wire {@code oauth2Login()}, and it would work perfectly on a machine
 * with {@code GOOGLE_CLIENT_ID} set - then fail to start on every machine
 * without it, taking password login down with it. The failure mode is a refusal
 * to boot, so it cannot be caught by a test that assumes a running context;
 * it has to be a test whose whole point is that the context came up.
 *
 * <h2>Why the registration is removed rather than blanked</h2>
 *
 * Setting the properties to empty strings via {@code @TestPropertySource} is
 * the intuitive way to force this state, and it does the opposite of what is
 * wanted: an empty {@code client-id} is a <em>declared</em> registration, and
 * Spring Boot's {@code OAuth2ClientProperties.validate()} rejects it with
 * "Client id of registration 'google' must not be empty". The test would then
 * fail on that error rather than on the condition being tested.
 *
 * <p>So the initializer below removes the property source that
 * {@link com.headheartfrees.config.GoogleOAuth2EnvironmentPostProcessor} adds.
 * That keeps the test meaningful on a developer machine which does have real
 * Google credentials in its environment.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = OAuth2AbsentConfigIT.RemoveGoogleRegistration.class)
class OAuth2AbsentConfigIT extends PostgresTestBase {

    /**
     * Strips the injected Google registration, simulating an install with no
     * {@code GOOGLE_CLIENT_ID}.
     */
    static class RemoveGoogleRegistration
            implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            applicationContext
                    .getEnvironment()
                    .getPropertySources()
                    .remove("headheartfrees-google-oauth2");
        }
    }

    @Autowired
    private ApplicationContext context;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("the context starts with no Google client configured")
    void contextStartsWithoutGoogle() {
        // Reaching this line is most of the assertion: @SpringBootTest would
        // have failed the test during startup otherwise.
        assertThat(context).isNotNull();
    }

    @Test
    @DisplayName("no ClientRegistrationRepository bean exists, which is the condition being handled")
    void noClientRegistrationRepository() {
        assertThat(context.getBeanNamesForType(ClientRegistrationRepository.class))
                .as("Spring only creates this when a client-id has text. If this is "
                        + "non-empty the test is no longer exercising the absent case.")
                .isEmpty();
    }

    @Test
    @DisplayName("password auth is unaffected by Google being unconfigured")
    void passwordAuthStillWorks() throws Exception {
        // The endpoint is reachable and answers on its own terms - a 401 for a
        // nonexistent account, not a 404 or a startup-related 500.
        mockMvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("venting is unaffected too")
    void ventStillWorks() throws Exception {
        mockMvc.perform(get("/api/v1/vent/stats")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("the Google entry point is simply not mapped, rather than erroring")
    void googleEntryPointIsAbsent() throws Exception {
        // With no registration there is no /oauth2/authorization/google handler.
        // The path is in PUBLIC_PATHS, so this is a 404 from the dispatcher
        // rather than a 401 from the filter chain - the distinction matters
        // because a 401 here would mean the path was mapped but unreachable.
        mockMvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().isNotFound());
    }
}
