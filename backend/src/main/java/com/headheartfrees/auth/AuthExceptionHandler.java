package com.headheartfrees.auth;

import com.headheartfrees.common.web.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Renders the auth module's own exceptions in the shape from
 * PROJECT_BRIEF.md section 6.
 *
 * <p>Lives in {@code auth} rather than in {@code GlobalExceptionHandler} so that
 * {@code common.web} does not have to import this module's types. The advice is
 * ordered ahead of the global one, which keeps its catch-all from turning these
 * into 500s.
 *
 * <h2>The important property of this class</h2>
 *
 * {@link InvalidCredentialsException} and {@link RefreshTokenReuseException}
 * produce <strong>byte-identical</strong> responses: same status, same code,
 * same message. A reused refresh token is a serious event and it is logged as
 * one, but the client is told exactly what it would be told for a typo. Any
 * difference here - a distinct code, a more specific message, even a different
 * status - would let a caller probe which tokens had once been valid.
 */
@RestControllerAdvice
@Order(0)
class AuthExceptionHandler {

    /** The one message for every authentication failure. */
    private static final String GENERIC_MESSAGE = "Invalid email or password.";

    /**
     * The one message for every failure of the code step.
     *
     * <p>Wrong code, expired ticket, forged ticket, a code already used, an
     * unknown backup code, a spent backup code, an account that is not enrolled
     * - all of it, this sentence. It says nothing about which part was wrong,
     * because each of those distinctions tells an attacker something: whether a
     * ticket is still live, whether the account is enrolled, whether a guessed
     * backup code was ever real.
     *
     * <p>It mentions the clock because that is the one cause a person can
     * actually fix, it is by far the most common non-malicious reason for a
     * correct-looking code to fail, and saying so reveals nothing - the drift
     * window is a property of the protocol, not of the attempt.
     */
    private static final String INVALID_CODE_MESSAGE =
            "That code is not valid. Check your authenticator app and that your phone's clock is "
                    + "set automatically, then try again.";

    /**
     * Wrong password, unknown address, Google-only account, unknown refresh
     * token, expired refresh token. All 401, all identical.
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException ex, HttpServletRequest request) {

        return unauthorized(request);
    }

    /**
     * Deliberately indistinguishable from the above. The family has already
     * been revoked by the time this is thrown; see {@link RefreshTokenService}.
     */
    @ExceptionHandler(RefreshTokenReuseException.class)
    ResponseEntity<ApiErrorResponse> handleReuse(
            RefreshTokenReuseException ex, HttpServletRequest request) {

        return unauthorized(request);
    }

    /**
     * 400, with the reason. Safe to state: every {@link PasswordPolicy} message
     * describes the submitted password and says nothing about whether an
     * account exists at that address.
     */
    @ExceptionHandler(WeakPasswordException.class)
    ResponseEntity<ApiErrorResponse> handleWeakPassword(
            WeakPasswordException ex, HttpServletRequest request) {

        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(
                        HttpStatus.BAD_REQUEST.value(),
                        "WEAK_PASSWORD",
                        ex.getMessage(),
                        request.getRequestURI()));
    }

    /**
     * Every second-factor failure, rendered identically. See
     * {@link InvalidTotpCodeException}.
     */
    @ExceptionHandler(InvalidTotpCodeException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidTotpCode(
            InvalidTotpCodeException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.of(
                        HttpStatus.UNAUTHORIZED.value(),
                        "INVALID_TOTP_CODE",
                        INVALID_CODE_MESSAGE,
                        request.getRequestURI()));
    }

    /**
     * 429 with {@code Retry-After}, and the only distinguishable failure in
     * this flow.
     *
     * <p>{@link TotpLockedException} carries the argument for why that is safe:
     * this response is only reachable by somebody who has already passed the
     * password check, so it discloses nothing they do not have, and an admin
     * whose correct code is silently refused has no way to tell a lockout from
     * a broken authenticator.
     *
     * <p>{@code Retry-After} is readable cross-origin because
     * {@code SecurityConfig} names it in {@code Access-Control-Expose-Headers}.
     * Without that line the frontend shows the vaguer sentence.
     */
    @ExceptionHandler(TotpLockedException.class)
    ResponseEntity<ApiErrorResponse> handleTotpLocked(
            TotpLockedException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.retryAfterSeconds()))
                .body(ApiErrorResponse.of(
                        HttpStatus.TOO_MANY_REQUESTS.value(),
                        "TOTP_LOCKED",
                        "Too many incorrect codes. Two-step sign-in is locked for this account "
                                + "for a while. Wait, then try again, or use a backup code.",
                        request.getRequestURI()));
    }

    /**
     * 403. Safe to state plainly: the caller is authenticated as the account in
     * question and is being told a rule about their own account.
     */
    @ExceptionHandler(TotpNotPermittedException.class)
    ResponseEntity<ApiErrorResponse> handleNotPermitted(
            TotpNotPermittedException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiErrorResponse.of(
                        HttpStatus.FORBIDDEN.value(),
                        "TOTP_NOT_PERMITTED",
                        ex.getMessage(),
                        request.getRequestURI()));
    }

    private static ResponseEntity<ApiErrorResponse> unauthorized(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.of(
                        HttpStatus.UNAUTHORIZED.value(),
                        "UNAUTHORIZED",
                        GENERIC_MESSAGE,
                        request.getRequestURI()));
    }
}
