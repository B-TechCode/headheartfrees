package com.headheartfrees.auth;

import com.headheartfrees.common.web.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
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

    private static ResponseEntity<ApiErrorResponse> unauthorized(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.of(
                        HttpStatus.UNAUTHORIZED.value(),
                        "UNAUTHORIZED",
                        GENERIC_MESSAGE,
                        request.getRequestURI()));
    }
}
