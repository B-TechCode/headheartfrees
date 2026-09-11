package com.headheartfrees.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * Google sign-in is refused for an admin account.
 *
 * <h2>Why this matters more than it looks</h2>
 *
 * {@link GoogleSignInHandler} is a second, entirely separate route to a
 * session. It never calls {@code AuthService.login}, so a second factor bolted
 * onto that endpoint alone would be decorative: an admin whose account is
 * linked to Google would sign in with one factor, and the requirement would
 * hold only for people who were not trying to get around it.
 *
 * <p>This is driven through the handler directly rather than through a real
 * OAuth2 round trip, because the thing under test is the decision the handler
 * makes once Google has already said yes. Faking Google's answer is exactly the
 * situation being defended against.
 */
@SpringBootTest
class AdminGoogleSignInRefusedIT extends com.headheartfrees.PostgresTestBase {

    private static final String ADMIN_EMAIL = "googleadmin@example.com";
    private static final String USER_EMAIL = "googleuser@example.com";
    private static final String GOOGLE_ID = "google-subject-1234";

    @Autowired
    private GoogleSignInHandler handler;

    @Autowired
    private UserAccountRepository accounts;

    @Autowired
    private RefreshTokenRepository refreshTokens;

    @Autowired
    private Clock clock;

    @org.junit.jupiter.api.BeforeEach
    void reset() {
        refreshTokens.deleteAll();
        accounts.deleteAll();
    }

    @Test
    @DisplayName("an admin account is refused, with a reason and no session")
    void adminIsRefused() throws Exception {
        UserAccount admin = accounts.save(
                UserAccount.withPassword(ADMIN_EMAIL, "hash", "The Admin", clock.instant()));
        admin.assignRole(UserRole.ADMIN);
        accounts.save(admin);

        MockHttpServletResponse response = signInWithGoogle(ADMIN_EMAIL, GOOGLE_ID);

        assertThat(response.getRedirectedUrl())
                .as("The refusal must carry a reason. A bare redirect back to the sign-in page "
                        + "leaves somebody clicking the Google button repeatedly with no idea "
                        + "why it does nothing.")
                .endsWith("?error=admin_password_required");

        assertThat(response.getCookie(RefreshCookie.NAME))
                .as("A refresh cookie here is the entire second factor bypassed")
                .isNull();
        assertThat(refreshTokens.findAll()).isEmpty();
    }

    @Test
    @DisplayName("a refused admin is not linked to Google and nothing is written")
    void refusingWritesNothing() throws Exception {
        UserAccount admin = accounts.save(
                UserAccount.withPassword(ADMIN_EMAIL, "hash", "The Admin", clock.instant()));
        admin.assignRole(UserRole.ADMIN);
        accounts.save(admin);

        signInWithGoogle(ADMIN_EMAIL, GOOGLE_ID);

        // The refusal is repeatable and reversible: if the role is ever
        // removed, Google sign-in simply works again, and nothing about the
        // account has been quietly changed in the meantime.
        assertThat(accounts.findByEmail(ADMIN_EMAIL).orElseThrow().getGoogleId())
                .as("Linking on a refused sign-in would leave a trace of an attempt that was "
                        + "supposed to have no effect")
                .isNull();
    }

    @Test
    @DisplayName("an ordinary user signs in with Google exactly as before")
    void ordinaryUserIsUntouched() throws Exception {
        MockHttpServletResponse response = signInWithGoogle(USER_EMAIL, "google-subject-5678");

        assertThat(response.getRedirectedUrl())
                .as("Regular users keep Google exactly as it was. This phase narrows one "
                        + "account, not the feature.")
                .doesNotContain("error=");
        assertThat(response.getCookie(RefreshCookie.NAME)).isNotNull();
        assertThat(accounts.findByEmail(USER_EMAIL)).isPresent();
    }

    @Test
    @DisplayName("a user who is promoted to admin later loses Google sign-in")
    void promotionRemovesGoogleSignIn() throws Exception {
        // First sign-in as an ordinary user: allowed, and linked.
        signInWithGoogle(USER_EMAIL, "google-subject-5678");
        assertThat(accounts.findByEmail(USER_EMAIL).orElseThrow().getGoogleId()).isNotNull();

        // Then APP_ADMIN_BOOTSTRAP_EMAILS promotes them, the documented way to
        // make an admin. The existing Google link must stop being a route in.
        UserAccount account = accounts.findByEmail(USER_EMAIL).orElseThrow();
        account.assignRole(UserRole.ADMIN);
        accounts.save(account);
        refreshTokens.deleteAll();

        MockHttpServletResponse response = signInWithGoogle(USER_EMAIL, "google-subject-5678");

        assertThat(response.getRedirectedUrl()).endsWith("?error=admin_password_required");
        assertThat(refreshTokens.findAll()).isEmpty();
    }

    private MockHttpServletResponse signInWithGoogle(String email, String googleId)
            throws Exception {

        OAuth2User principal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of(
                        "sub", googleId,
                        "email", email,
                        "email_verified", true,
                        "name", "Somebody"),
                "sub");

        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(
                new MockHttpServletRequest(),
                response,
                new TestingAuthenticationToken(principal, null, List.of()));
        return response;
    }
}
