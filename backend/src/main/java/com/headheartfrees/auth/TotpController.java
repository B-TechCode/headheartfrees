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
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The second factor: finishing a sign-in, enrolling, and recovery codes.
 *
 * <h2>Which of these are public, and why that is not a hole</h2>
 *
 * {@code /login/totp}, {@code /totp/setup} and {@code /totp/enable} are
 * {@code permitAll} in {@code SecurityConfig}, because the caller legitimately
 * has no session yet - that is the entire point of a challenge. They are not
 * unauthenticated: each one demands a ticket that is only issued after a
 * correct password, has a five-minute life, and is refused by
 * {@link JwtAuthenticationFilter} if anyone tries to present it as a Bearer
 * token.
 *
 * <p>{@code /totp/backup-codes} and {@code /totp/disable} are absent from the
 * public list and therefore fall to {@code anyRequest().authenticated()}. They
 * act on an account that is already signed in.
 *
 * <h2>Rate limiting</h2>
 *
 * {@link RateLimitPolicy#TOTP}, not {@link RateLimitPolicy#AUTH}. A separate
 * bucket for the same reason the existing policies are separate: somebody
 * fumbling a six-digit code three times must not consume the allowance that
 * lets them sign in at all.
 *
 * <p>The IP limit is the outer bound and is the weaker half - HANDOVER section
 * 10.3 records that behind Docker every visitor currently shares one bucket,
 * which makes it either evadable from a second address or a nuisance to
 * everyone. The limit that actually holds is per account, on the
 * {@code user_totp} row, and it compounds. See {@link UserTotp} for the
 * arithmetic.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Two-step sign-in", description = "TOTP enrolment, verification and recovery codes.")
class TotpController {

    private final AuthService authService;
    private final ClientIpRateLimiter rateLimiter;
    private final boolean cookieSecure;

    TotpController(
            AuthService authService, ClientIpRateLimiter rateLimiter, AuthProperties properties) {
        this.authService = authService;
        this.rateLimiter = rateLimiter;
        this.cookieSecure = properties.cookieSecure();
    }

    /**
     * Finishes a sign-in that stopped for a code.
     *
     * <p>This is the only endpoint in the application that turns a challenge
     * ticket into a session, and it does so only after
     * {@code TotpService.verifySecondFactor} has returned without throwing.
     */
    @PostMapping("/login/totp")
    @Operation(
            summary = "Complete a sign-in with a code",
            description = "Takes the ticket from /auth/login and a six-digit code or a backup "
                    + "code. One generic error for every failure mode; 429 with Retry-After "
                    + "when the account's second factor is locked.")
    ResponseEntity<LoginResponse> completeSignIn(
            @Valid @RequestBody TotpLoginRequest request, HttpServletRequest httpRequest) {

        enforceRateLimit(httpRequest);
        TokenPair tokens = authService.completeSecondFactor(request.ticket(), request.code());
        return withSession(LoginResponse.authenticated(bodyOf(tokens)), tokens);
    }

    /**
     * Generates a secret and returns it as a QR code and as text.
     *
     * <p>Grants nothing. The row written here is unconfirmed until
     * {@link #enable} proves a code against it, so an abandoned setup leaves a
     * working account exactly as it was.
     */
    @PostMapping("/totp/setup")
    @Operation(
            summary = "Begin enrolment",
            description = "Returns a new secret as a QR code and as a manual key. The factor is "
                    + "NOT on until /auth/totp/enable verifies a code derived from it.")
    TotpSetupResponse setup(
            @Valid @RequestBody(required = false) TotpSetupRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        enforceRateLimit(httpRequest);
        UUID userId = resolveCaller(authentication, request == null ? null : request.ticket());
        return TotpSetupResponse.from(authService.beginTotpSetup(userId));
    }

    /**
     * Verifies a code against the pending secret and turns the factor on.
     *
     * <p>When the caller arrived on an enrolment ticket - the admin who was
     * stopped at sign-in - this also issues the session they came for, so
     * enrolling and signing in are one step rather than two.
     */
    @PostMapping("/totp/enable")
    @Operation(
            summary = "Turn the second factor on",
            description = "Requires a valid code derived from the pending secret. Returns the "
                    + "backup codes, which are shown once and never again.")
    ResponseEntity<TotpEnableResponse> enable(
            @Valid @RequestBody TotpEnableRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        enforceRateLimit(httpRequest);

        boolean byTicket = authentication == null;
        UUID userId = resolveCaller(authentication, request.ticket());

        TotpEnableResult result = authService.enableTotp(userId, request.code(), byTicket);

        TotpEnableResponse body = new TotpEnableResponse(
                result.backupCodes(),
                result.session() == null ? null : bodyOf(result.session()));

        return result.session() == null
                ? ResponseEntity.ok(body)
                : withSession(body, result.session());
    }

    /** Replaces every backup code. Signed in, and a current code. */
    @PostMapping("/totp/backup-codes")
    @Operation(
            summary = "Generate new recovery codes",
            description = "Replaces all existing codes, which stop working immediately. Shown "
                    + "once.")
    BackupCodesResponse regenerateBackupCodes(
            @Valid @RequestBody TotpCodeRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        enforceRateLimit(httpRequest);
        UUID userId = (UUID) authentication.getPrincipal();
        return new BackupCodesResponse(authService.regenerateBackupCodes(userId, request.code()));
    }

    /**
     * Turns the second factor off. Refused for an ADMIN.
     *
     * <p>Clears the refresh cookie, because every session for the account has
     * just been revoked server-side and leaving a dead cookie in the browser
     * only produces a confusing 401 on the next call.
     */
    @PostMapping("/totp/disable")
    @Operation(
            summary = "Turn the second factor off",
            description = "Requires the account password AND a current code. Refused for admin "
                    + "accounts, which require it. Signs out every device.")
    ResponseEntity<Void> disable(
            @Valid @RequestBody TotpDisableRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        enforceRateLimit(httpRequest);
        UUID userId = (UUID) authentication.getPrincipal();
        authService.disableTotp(userId, request.password(), request.code());

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, RefreshCookie.expired(cookieSecure).toString())
                .build();
    }

    // ---------------------------------------------------------------------

    /**
     * Who is asking: the session if there is one, otherwise the enrolment
     * ticket.
     *
     * <p>The session wins when both are present. A caller holding a live
     * session has already proven more than a ticket does, and preferring the
     * ticket would let a request act on an account other than the signed-in
     * one.
     *
     * @throws InvalidTotpCodeException when there is neither - the same generic
     *         failure as a bad code, because "no ticket" and "expired ticket"
     *         are the same thing from a client's point of view and
     *         distinguishing them says which tickets were once live
     */
    private UUID resolveCaller(Authentication authentication, String ticket) {
        if (authentication != null && authentication.getPrincipal() instanceof UUID userId) {
            return userId;
        }
        if (ticket == null || ticket.isBlank()) {
            throw new InvalidTotpCodeException();
        }
        return authService.resolveEnrolmentTicket(ticket);
    }

    private AccessTokenResponse bodyOf(TokenPair tokens) {
        return AccessTokenResponse.of(
                tokens.accessToken(), tokens.accessTokenTtl().toSeconds(), tokens.user());
    }

    /**
     * Attaches the refresh cookie.
     *
     * <p>Deliberately the same split {@code AuthController} makes: access token
     * into the body, refresh token into the cookie, never the reverse. Two
     * classes now issue sessions, and both go through a method that does only
     * this.
     */
    private <T> ResponseEntity<T> withSession(T body, TokenPair tokens) {
        ResponseCookie cookie =
                RefreshCookie.issue(tokens.refreshToken(), tokens.refreshTokenTtl(), cookieSecure);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(body);
    }

    private void enforceRateLimit(HttpServletRequest httpRequest) {
        ConsumptionProbe probe = rateLimiter.tryConsume(httpRequest, RateLimitPolicy.TOTP);
        if (!probe.isConsumed()) {
            long seconds = Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds();
            throw new RateLimitExceededException(Math.max(1, seconds));
        }
    }
}
