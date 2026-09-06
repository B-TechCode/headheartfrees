package com.headheartfrees.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Both branches of the guard, plus the assumption it rests on.
 *
 * <p>This is a plain unit test rather than a {@code @SpringBootTest}. The
 * rejecting branch is a refusal to build the context, and a test that asserts
 * "the application did not start" through Spring has to assert on an exception
 * wrapped several layers deep in a {@code BeanCreationException}; calling
 * {@link JwtSecretGuard#verify} directly says the same thing without the
 * indirection. That the bean is wired at all is covered by every
 * {@code @SpringBootTest} in the suite: they run under the {@code local}
 * profile on the committed default, so if the guard were ever wrong about the
 * accepting branch, none of them would start.
 */
class JwtSecretGuardTest {

    private static final String REAL_SECRET =
            "Zm9yLXRoaXMtdGVzdC1vbmx5LW5vdC1hLXJlYWwtc2VjcmV0LTk4NzY1NDMyMTA=";

    @Test
    @DisplayName("the committed default is accepted under the local profile")
    void defaultAcceptedUnderLocalProfile() {
        assertThatCode(() -> JwtSecretGuard.verify(
                        JwtSecretGuard.COMMITTED_DEFAULT, List.of(JwtSecretGuard.LOCAL_PROFILE)))
                .as("a fresh clone and `docker compose up` both run on this value")
                .doesNotThrowAnyException();

        // Also when local is one profile among several.
        assertThatCode(() -> JwtSecretGuard.verify(
                        JwtSecretGuard.COMMITTED_DEFAULT, List.of("local", "debug")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("the committed default is rejected when the profile is not local")
    void defaultRejectedOutsideLocalProfile() {
        assertThatIllegalStateException()
                .isThrownBy(() ->
                        JwtSecretGuard.verify(JwtSecretGuard.COMMITTED_DEFAULT, Set.of("prod")))
                .withMessageContaining("APP_JWT_SECRET")
                .withMessageContaining("openssl rand -base64 48");

        // No profile at all is the install that forgot everything, and is the
        // case this guard exists for. It must not read as "not production".
        assertThatIllegalStateException()
                .isThrownBy(() -> JwtSecretGuard.verify(JwtSecretGuard.COMMITTED_DEFAULT, List.of()))
                .withMessageContaining("APP_JWT_SECRET");
    }

    @Test
    @DisplayName("a secret of the operator's own is accepted with no profile set")
    void configuredSecretNeedsNoProfile() {
        assertThatCode(() -> JwtSecretGuard.verify(REAL_SECRET, List.of()))
                .as("the guard checks the value, not the environment it came from")
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("the constant still matches the default in application.yml")
    void constantMatchesTheCommittedDefault() throws IOException {
        String yaml;
        try (InputStream in =
                JwtSecretGuardTest.class.getResourceAsStream("/application.yml")) {
            assertThat(in).as("application.yml on the test classpath").isNotNull();
            yaml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertThat(yaml)
                .as("If the default in application.yml is changed without changing "
                        + "JwtSecretGuard.COMMITTED_DEFAULT, the guard goes on comparing against "
                        + "a string nothing uses and every install boots on a committed secret "
                        + "again, silently.")
                .contains("${APP_JWT_SECRET:" + JwtSecretGuard.COMMITTED_DEFAULT + "}");
    }
}
