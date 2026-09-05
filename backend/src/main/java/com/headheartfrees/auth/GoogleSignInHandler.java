package com.headheartfrees.auth;

import com.headheartfrees.config.AuthProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * What happens after Google says who someone is.
 *
 * <p>Finds or creates the local account, issues a refresh cookie, and redirects
 * to the frontend. <strong>No token is placed in the redirect URL.</strong>
 * A query parameter would end up in browser history, in the referrer of the
 * next request, and in any proxy log along the way. Instead the browser lands
 * on the frontend holding only the httpOnly cookie, and the frontend's first
 * action is {@code POST /api/v1/auth/refresh} to exchange it for an access
 * token. That is phase 6's job; this side is complete without it.
 *
 * <h2>Account linking</h2>
 *
 * Matching is by {@code google_id} first, then by email. An existing
 * password account signing in with Google for the first time is linked rather
 * than duplicated - otherwise one person ends up with two accounts and the
 * CITEXT unique constraint on email rejects the second one anyway, turning a
 * first Google sign-in into an unexplained 500.
 *
 * <p>This trusts Google's assertion that the address belongs to the person
 * signing in, which is the normal assumption for Google as an identity
 * provider and is why {@code email_verified} from the provider is checked
 * before linking.
 */
@Component
class GoogleSignInHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(GoogleSignInHandler.class);

    private final UserAccountRepository users;
    private final RefreshTokenService refreshTokens;
    private final Clock clock;
    private final String successRedirect;
    private final boolean cookieSecure;

    GoogleSignInHandler(
            UserAccountRepository users,
            RefreshTokenService refreshTokens,
            Clock clock,
            AuthProperties properties) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.clock = clock;
        this.successRedirect = properties.oauth2SuccessRedirect();
        this.cookieSecure = properties.cookieSecure();
    }

    @Override
    @Transactional
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {

        if (!(authentication.getPrincipal() instanceof OAuth2User principal)) {
            response.sendRedirect(successRedirect + "?error=oauth2");
            return;
        }

        String googleId = principal.getName();
        String email = principal.getAttribute("email");
        Boolean googleVerified = principal.getAttribute("email_verified");
        String displayName = principal.getAttribute("name");

        if (email == null || !Boolean.TRUE.equals(googleVerified)) {
            // Without a verified address there is nothing safe to match on: an
            // unverified Google address could be anyone's, and linking on it
            // would be an account takeover primitive.
            log.warn("Google sign-in rejected: no verified email on the profile");
            response.sendRedirect(successRedirect + "?error=unverified_email");
            return;
        }

        UserAccount account = users.findByGoogleId(googleId)
                .orElseGet(() -> linkOrCreate(googleId, email, displayName));

        RefreshTokenService.IssuedToken issued = refreshTokens.issueNewFamily(account.getId());
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                RefreshCookie.issue(issued.token(), issued.ttl(), cookieSecure).toString());

        response.sendRedirect(successRedirect);
    }

    private UserAccount linkOrCreate(String googleId, String email, String displayName) {
        return users.findByEmail(email)
                .map(existing -> {
                    existing.linkGoogle(googleId);
                    log.info("Linked Google identity to existing account {}", existing.getId());
                    return users.save(existing);
                })
                .orElseGet(() -> {
                    UserAccount created =
                            UserAccount.fromGoogle(email, googleId, displayName, clock.instant());
                    log.info("Created account {} from Google sign-in", created.getId());
                    return users.save(created);
                });
    }
}
