package com.headheartfrees.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.util.AntPathMatcher;

/**
 * No entry in {@code PUBLIC_PATHS} may match an admin URL.
 *
 * <h2>Why this exists when an integration test already asserts 401</h2>
 *
 * {@code AdminFeedbackAuthorisationIT} asserts that an anonymous
 * {@code GET /api/v1/admin/feedback} is 401, which is the outcome that
 * matters. But it does <em>not</em> pin the filter-chain ordering, and this
 * was checked rather than assumed: with {@code "/api/v1/**"} added to
 * {@code PUBLIC_PATHS} <em>and</em> the admin matcher moved below it - the
 * exact regression this phase was warned about - all six of its tests still
 * passed.
 *
 * <p>They passed because {@code @PreAuthorize} on
 * {@code AdminFeedbackController} caught the request after the chain let it
 * through. That is the belt-and-braces design working, and it is genuinely
 * reassuring: one mistake is not a breach. It also means a request-level test
 * cannot tell the two layers apart, so it cannot notice when one of them
 * silently stops contributing.
 *
 * <p>This test looks at the rule itself instead. It fails the moment a
 * wildcard that covers an admin route appears in the public list, whichever
 * order the matchers are declared in, and it runs in a millisecond with no
 * context. The annotation and the path rule are then each independently
 * guarded, which is what "two locks" is supposed to mean.
 */
class AdminPathsAreNotPublicTest {

    /** URLs that must never be reachable without the ADMIN role. */
    private static final List<String> ADMIN_URLS = List.of(
            "/api/v1/admin/feedback",
            "/api/v1/admin/feedback/0f8fad5b-d9cb-469f-a165-70867728950e",
            "/api/v1/admin/anything/at/all");

    @Test
    @DisplayName("no public path pattern matches an admin URL")
    void publicPathsDoNotCoverAdmin() throws Exception {
        AntPathMatcher matcher = new AntPathMatcher();
        List<String> publicPaths = publicPaths();

        // A guard on the guard: if reflection stops finding the field, every
        // assertion below would pass vacuously.
        assertThat(publicPaths)
                .as("PUBLIC_PATHS could not be read - this test is proving nothing")
                .isNotEmpty();

        for (String pattern : publicPaths) {
            for (String url : ADMIN_URLS) {
                assertThat(matcher.match(pattern, url))
                        .as("Public path \"%s\" matches admin URL \"%s\". Anything under "
                                + "/api/v1/admin must require the ADMIN role; a wildcard here "
                                + "makes the moderation queue world-readable.", pattern, url)
                        .isFalse();
            }
        }
    }

    @Test
    @DisplayName("the public feedback path is exact, not a wildcard")
    void feedbackPathIsExact() throws Exception {
        // "/api/v1/feedback/**" would look harmless and would be wrong: it is
        // one refactor away from covering a nested admin route, and it opens
        // any future sub-resource by default rather than by decision.
        assertThat(publicPaths())
                .contains("/api/v1/feedback")
                .doesNotContain("/api/v1/feedback/**", "/api/v1/**", "/api/**");
    }

    private List<String> publicPaths() throws Exception {
        Field field = SecurityConfig.class.getDeclaredField("PUBLIC_PATHS");
        field.setAccessible(true);
        return List.of((String[]) field.get(null));
    }
}
