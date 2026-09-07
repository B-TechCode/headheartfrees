package com.headheartfrees.auth;

import java.time.Instant;
import java.util.UUID;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Accounts and access tokens, for tests in other packages.
 *
 * <h2>Why this is here rather than in {@code feedback}</h2>
 *
 * Making an ADMIN and minting a token both need types that are deliberately
 * package-private: {@code UserAccount.assignRole} exists only for
 * {@code AdminBootstrap}, and {@code JwtService.issueAccessToken} is not public
 * either. That restriction is worth keeping - PROJECT_BRIEF.md section 4, and
 * the reason there is no promote endpoint anywhere - so the fixture lives in
 * the package that owns those types and exposes exactly two methods across the
 * boundary.
 *
 * <p>The alternative, registering through the API and then promoting via
 * {@code APP_ADMIN_BOOTSTRAP_EMAILS} and a context restart, is the real
 * production path and is already covered by the phase 5 suite. Repeating it
 * per test would cost a context restart each time and would test the bootstrap
 * rather than the thing under test.
 *
 * <p>Test scope only. Nothing in {@code src/main} can see this class, so it
 * cannot become a way for production code to mint a token or make an admin.
 */
@Component
public class AuthTestFixtures {

    private final UserAccountRepository users;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    AuthTestFixtures(
            UserAccountRepository users, JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    /** A registered account with the USER role, and a token for it. */
    public String accessTokenForUser(String email) {
        return tokenFor(create(email, UserRole.USER));
    }

    /** A registered account promoted to ADMIN, and a token for it. */
    public String accessTokenForAdmin(String email) {
        return tokenFor(create(email, UserRole.ADMIN));
    }

    /** The id of an account, so a test can assert on {@code moderated_by}. */
    public UUID idOf(String email) {
        return users.findByEmail(email)
                .orElseThrow(() -> new AssertionError("No account for " + email))
                .getId();
    }

    private UserAccount create(String email, UserRole role) {
        return users.findByEmail(email).orElseGet(() -> {
            UserAccount account = UserAccount.withPassword(
                    email, passwordEncoder.encode("a quiet long passphrase"), "Test Person",
                    Instant.now());
            account.assignRole(role);
            return users.save(account);
        });
    }

    private String tokenFor(UserAccount account) {
        return jwtService.issueAccessToken(account.getId(), account.getRole());
    }

    /**
     * Registers the fixture with any {@code @SpringBootTest} that imports it.
     *
     * <p>Component scanning does not reach test classes, so the bean needs an
     * explicit definition. Kept beside the fixture rather than in each test's
     * own configuration so there is one place to change.
     */
    @TestConfiguration
    public static class Registration {

        @Bean
        AuthTestFixtures authTestFixtures(
                UserAccountRepository users,
                JwtService jwtService,
                PasswordEncoder passwordEncoder) {
            return new AuthTestFixtures(users, jwtService, passwordEncoder);
        }
    }
}
