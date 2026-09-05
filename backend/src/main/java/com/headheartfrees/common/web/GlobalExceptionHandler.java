package com.headheartfrees.common.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Renders every exception that escapes a controller as the one error shape
 * defined in PROJECT_BRIEF.md section 6.
 *
 * <p>Two rules govern everything here:
 *
 * <ol>
 *   <li><strong>Nothing internal leaks.</strong> Exception messages, class names
 *       and stack traces are logged server-side and replaced with a fixed,
 *       reviewed string in the response. This matters most in
 *       {@link #handleUnexpected}, which is the only handler that sees exceptions
 *       nobody anticipated.</li>
 *   <li><strong>{@code code} is the contract.</strong> Clients branch on the
 *       stable {@code code} value; {@code message} is prose and may be reworded.</li>
 * </ol>
 *
 * <p>Requests rejected by the Spring Security filter chain never reach a
 * controller, so they never reach this class. Those are rendered in the same
 * shape by {@link SecurityErrorHandler}, which is wired into the filter chain.
 * The 401/403 handlers below cover the other path: an exception raised after
 * dispatch, such as a method-security check inside a controller.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Returned for any unanticipated failure. Deliberately says nothing. */
    private static final String GENERIC_MESSAGE =
            "Something went wrong on our end. Please try again.";

    // -- 400 --------------------------------------------------------------

    /** A {@code @Valid} request body failed bean validation. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleBodyValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            // First message wins: a field with several violations reports one.
            fieldErrors.putIfAbsent(fieldError.getField(), messageOf(fieldError));
        }
        for (ObjectError globalError : ex.getBindingResult().getGlobalErrors()) {
            fieldErrors.putIfAbsent(globalError.getObjectName(), messageOf(globalError));
        }

        return respond(ApiErrorResponse.withFieldErrors(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_FAILED",
                "One or more fields are invalid.",
                request.getRequestURI(),
                fieldErrors));
    }

    /** A {@code @Validated} method parameter or path variable failed validation. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            fieldErrors.putIfAbsent(lastPathSegment(violation), violation.getMessage());
        }

        return respond(ApiErrorResponse.withFieldErrors(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_FAILED",
                "One or more fields are invalid.",
                request.getRequestURI(),
                fieldErrors));
    }

    /**
     * Malformed JSON, or a body that could not be bound at all.
     *
     * <p>The parser's message is not echoed: it can quote the offending payload,
     * and for this application a request body is the last thing to repeat back.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableBody(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        log.debug("Unreadable request body on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return respond(ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "MALFORMED_REQUEST",
                "The request body could not be read.",
                request.getRequestURI()));
    }

    /** A required query parameter was absent. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException ex, HttpServletRequest request) {

        return respond(ApiErrorResponse.withFieldErrors(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_FAILED",
                "One or more fields are invalid.",
                request.getRequestURI(),
                Map.of(ex.getParameterName(), "is required")));
    }

    /** A parameter could not be coerced to its declared type. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        return respond(ApiErrorResponse.withFieldErrors(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_FAILED",
                "One or more fields are invalid.",
                request.getRequestURI(),
                Map.of(ex.getName(), "has the wrong type")));
    }

    // -- 401 / 403 --------------------------------------------------------

    /** Raised after dispatch; filter-chain rejections go through {@link SecurityErrorHandler}. */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleUnauthenticated(
            AuthenticationException ex, HttpServletRequest request) {

        log.debug("Authentication failure on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return respond(ApiErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                "UNAUTHORIZED",
                "Authentication is required to access this resource.",
                request.getRequestURI()));
    }

    /**
     * Typically a method-security check inside a controller.
     *
     * <p>This must stay ahead of {@link #handleUnexpected}: without an explicit
     * handler the catch-all would turn a deliberate 403 into a 500.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {

        log.debug("Access denied on {} {}", request.getMethod(), request.getRequestURI(), ex);

        // 401 for a caller with no identity, 403 for one who has an identity
        // that is not enough. Phase 1 answered 403 unconditionally, which was
        // invisible while no method security existed; @PreAuthorize arrives
        // with the admin endpoints and makes it reachable, so it is fixed here.
        //
        // Only this advice needs the distinction. The filter chain already gets
        // it right, because ExceptionTranslationFilter calls the entry point
        // (401) for anonymous callers and the access-denied handler (403) only
        // for authenticated ones. What reaches this method instead is an
        // AccessDeniedException thrown inside the dispatcher, where that
        // routing has already happened and cannot be consulted.
        if (isAnonymous()) {
            return respond(ApiErrorResponse.of(
                    HttpStatus.UNAUTHORIZED.value(),
                    "UNAUTHORIZED",
                    "Authentication is required to access this resource.",
                    request.getRequestURI()));
        }

        return respond(ApiErrorResponse.of(
                HttpStatus.FORBIDDEN.value(),
                "FORBIDDEN",
                "You do not have permission to access this resource.",
                request.getRequestURI()));
    }

    /**
     * True when nobody is authenticated on this request.
     *
     * <p>All three cases mean the same thing: no context, no authentication, or
     * Spring's anonymous placeholder token. Checking only for {@code null}
     * would miss the third, which is the common one whenever anonymous
     * authentication is enabled.
     */
    private static boolean isAnonymous() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        return authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken;
    }

    // -- 404 / 405 / 415 --------------------------------------------------

    /**
     * No handler and no static resource matched.
     *
     * <p>Both types are handled: {@code NoResourceFoundException} is what Spring
     * Boot raises once the resource handler has also declined, and
     * {@code NoHandlerFoundException} is raised when static resource mapping is
     * switched off.
     */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            Exception ex, HttpServletRequest request) {

        return respond(ApiErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                "NOT_FOUND",
                "No endpoint matches this request.",
                request.getRequestURI()));
    }

    /** The path exists but not for this HTTP method. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

        return respond(ApiErrorResponse.of(
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                "METHOD_NOT_ALLOWED",
                "This endpoint does not support " + ex.getMethod() + " requests.",
                request.getRequestURI()));
    }

    /** Wrong {@code Content-Type}. */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {

        return respond(ApiErrorResponse.of(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
                "UNSUPPORTED_MEDIA_TYPE",
                "This endpoint does not accept that content type.",
                request.getRequestURI()));
    }

    // -- 429 --------------------------------------------------------------

    /**
     * Rate limit exceeded.
     *
     * <p>{@code Retry-After} is required by PROJECT_BRIEF.md section 6, and the
     * frontend's {@code ApiError} already parses it. The value is also repeated
     * in {@code fieldErrors} so a client that only reads the JSON body can still
     * find it.
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleRateLimited(
            RateLimitExceededException ex, HttpServletRequest request) {

        long seconds = Math.max(1, ex.getRetryAfterSeconds());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(seconds))
                .body(ApiErrorResponse.withFieldErrors(
                        HttpStatus.TOO_MANY_REQUESTS.value(),
                        "RATE_LIMITED",
                        "Too many requests. Try again shortly.",
                        request.getRequestURI(),
                        Map.of("retryAfterSeconds", Long.toString(seconds))));
    }

    // -- 500 --------------------------------------------------------------

    /**
     * Everything not matched above.
     *
     * <p>The exception is logged in full at ERROR with the request line, and the
     * client receives {@link #GENERIC_MESSAGE} and nothing else. Do not be
     * tempted to pass {@code ex.getMessage()} through here - that is how database
     * URLs, file paths and internal identifiers end up in a browser.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception ex, HttpServletRequest request) {

        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return respond(ApiErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_ERROR",
                GENERIC_MESSAGE,
                request.getRequestURI()));
    }

    // -- helpers ----------------------------------------------------------

    private static ResponseEntity<ApiErrorResponse> respond(ApiErrorResponse body) {
        return ResponseEntity.status(body.status()).body(body);
    }

    /** Falls back to a fixed string so a null constraint message cannot produce a null value. */
    private static String messageOf(ObjectError error) {
        String message = error.getDefaultMessage();
        return message == null || message.isBlank() ? "is invalid" : message;
    }

    /** {@code create.request.email} to {@code email}, which is what a client can act on. */
    private static String lastPathSegment(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        int lastDot = path.lastIndexOf('.');
        return lastDot < 0 ? path : path.substring(lastDot + 1);
    }
}
