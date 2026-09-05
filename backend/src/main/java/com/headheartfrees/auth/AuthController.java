package com.headheartfrees.auth;

import com.headheartfrees.common.web.ClientIpRateLimiter;
import com.headheartfrees.common.web.RateLimitExceededException;
import com.headheartfrees.common.web.RateLimitPolicy;
import com.headheartfrees.config.AuthProperties;
import io.github.bucket4j.ConsumptionProbe;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The auth endpoints.
 *
 * <p>This is the only class that decides which token goes in the body and which
 * goes in the cookie. Keeping that split in one place is what makes it
 * reviewable: {@link TokenPair} carries both, and exactly one method turns it
 * into a response.
 *
 * <p>Register, login and refresh are rate limited at 5/min per IP
 * (PROJECT_BRIEF.md section 6). {@code /me} and {@code /logout} are not - the
 * first is an ordinary authenticated read, and throttling the second would
 * leave people unable to sign out.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Registration, sign-in and session refresh.")
class AuthController {

    private final AuthService authService;
    private final ClientIpRateLimiter rateLimiter;
    private final boolean cookieSecure;

    AuthController(
            AuthService authService, ClientIpRateLimiter rateLimiter, AuthProperties properties) {
        this.authService = authService;
        this.rateLimiter = rateLimiter;
        this.cookieSecure = properties.cookieSecure();
    }

    /**
     * Always 201, always the same body.
     *
     * <p>An existing address produces this response too, having created
     * nothing. See {@link DefaultAuthService#register} for why, including why
     * the password is hashed on that path anyway.
     */
    @PostMapping("/register")
    @Operation(
            summary = "Register an account",
            description = "Returns the same 201 and the same body whether or not the address "
                    + "was already registered. This is deliberate: a distinguishable response "
                    + "would let anyone test whether a given person has an account here.")
    ResponseEntity<RegistrationResponse> register(
            @Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {

        enforceRateLimit(httpRequest);
        authService.register(request.email(), request.password(), request.displayName());
        return ResponseEntity.status(HttpStatus.CREATED).body(RegistrationResponse.shared());
    }

    @PostMapping("/login")
    @Operation(
            summary = "Sign in",
            description = "Access token in the body, refresh token in an httpOnly cookie. "
                    + "One generic error for every failure mode.")
    ResponseEntity<AccessTokenResponse> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {

        enforceRateLimit(httpRequest);
        return respondWithTokens(authService.login(request.email(), request.password()));
    }

    /**
     * Rotates the refresh cookie and issues a new access token.
     *
     * <p>A reused token throws {@link RefreshTokenReuseException} after the
     * whole family has been revoked, and is rendered as a plain 401 - identical
     * to any other refresh failure, because a client that could distinguish
     * them could probe for live tokens.
     */
    @PostMapping("/refresh")
    @Operation(
            summary = "Rotate the session",
            description = "Consumes the refresh cookie and sets a new one. Reusing an already "
                    + "spent token revokes every token from that sign-in.")
    ResponseEntity<AccessTokenResponse> refresh(
            @CookieValue(name = RefreshCookie.NAME, required = false) String refreshToken,
            HttpServletRequest httpRequest) {

        enforceRateLimit(httpRequest);
        return respondWithTokens(authService.refresh(refreshToken));
    }

    /**
     * 204, and the cookie is cleared, whether or not the token was real.
     *
     * <p>Not rate limited: a person who cannot sign out because they signed in
     * too often is a worse outcome than the abuse this would prevent.
     */
    @PostMapping("/logout")
    @Operation(summary = "Sign out", description = "Revokes every token from this sign-in.")
    ResponseEntity<Void> logout(
            @CookieValue(name = RefreshCookie.NAME, required = false) String refreshToken) {

        authService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, RefreshCookie.expired(cookieSecure).toString())
                .build();
    }

    /**
     * The current user.
     *
     * <p>The principal is the user id put there by {@link JwtAuthenticationFilter},
     * so this is one lookup and always current - which is why the access token
     * carries no email or display name of its own.
     */
    @GetMapping("/me")
    @Operation(summary = "The signed-in user", description = "401 when unauthenticated.")
    UserSummary me(Authentication authentication) {
        return authService.summarise((UUID) authentication.getPrincipal());
    }

    /**
     * The single point where a {@link TokenPair} becomes a response: access
     * token into the body, refresh token into the cookie, and never the
     * reverse.
     */
    private ResponseEntity<AccessTokenResponse> respondWithTokens(TokenPair tokens) {
        ResponseCookie cookie =
                RefreshCookie.issue(tokens.refreshToken(), tokens.refreshTokenTtl(), cookieSecure);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(AccessTokenResponse.of(
                        tokens.accessToken(), tokens.accessTokenTtl().toSeconds(), tokens.user()));
    }

    /** 5/min per IP on the endpoints that can be used to guess credentials. */
    private void enforceRateLimit(HttpServletRequest httpRequest) {
        ConsumptionProbe probe = rateLimiter.tryConsume(httpRequest, RateLimitPolicy.AUTH);
        if (!probe.isConsumed()) {
            long seconds = Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds();
            throw new RateLimitExceededException(Math.max(1, seconds));
        }
    }
}
