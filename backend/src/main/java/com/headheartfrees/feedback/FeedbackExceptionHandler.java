package com.headheartfrees.feedback;

import com.headheartfrees.common.web.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * This module's own exceptions, in the section 6 shape.
 *
 * <p>Lives here rather than in {@code GlobalExceptionHandler} for the same
 * reason {@code AuthExceptionHandler} does: {@code common.web} must not import
 * a domain type. Ordered ahead of the global advice so its catch-all does not
 * turn a known 404 into a 500.
 */
@RestControllerAdvice
@Order(0)
class FeedbackExceptionHandler {

    /**
     * Moderating an id that is not there.
     *
     * <p>The message carries no detail about what else exists. This endpoint is
     * ADMIN-only, so probing is not the concern it would be on a public route,
     * but there is no reason for the shape of the answer to differ from the
     * rest of the site.
     */
    @ExceptionHandler(FeedbackNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleNotFound(
            FeedbackNotFoundException exception, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of(
                        HttpStatus.NOT_FOUND.value(),
                        "FEEDBACK_NOT_FOUND",
                        "That submission no longer exists.",
                        request.getRequestURI()));
    }
}
