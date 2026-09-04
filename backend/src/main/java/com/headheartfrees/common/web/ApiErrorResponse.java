package com.headheartfrees.common.web;

import java.time.Instant;
import java.util.Map;

/**
 * The single error shape every endpoint returns, as defined in
 * PROJECT_BRIEF.md section 6:
 * {@code { timestamp, status, code, message, path, fieldErrors? }}.
 *
 * <p>{@code fieldErrors} is {@code null} unless the failure was a validation
 * failure. {@code spring.jackson.default-property-inclusion: non_null} drops it
 * from the JSON in that case, which is what makes the field optional rather than
 * an empty object.
 *
 * <p>{@code message} is always safe to show a user. Internal detail - stack
 * traces, exception class names, SQL - never reaches this record; see
 * {@link GlobalExceptionHandler}.
 *
 * @param timestamp   when the error response was produced, UTC, ISO-8601
 * @param status      the HTTP status code, repeated in the body for clients
 *                    that only surface the payload
 * @param code        a stable machine-readable identifier, e.g.
 *                    {@code VALIDATION_FAILED}. Clients switch on this, not on
 *                    {@code message}
 * @param message     a human-readable, non-leaking summary
 * @param path        the request URI that failed
 * @param fieldErrors field name to message, for validation failures only
 */
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        Map<String, String> fieldErrors) {

    public ApiErrorResponse {
        fieldErrors = fieldErrors == null ? null : Map.copyOf(fieldErrors);
    }

    /** An error with no per-field detail. */
    public static ApiErrorResponse of(int status, String code, String message, String path) {
        return new ApiErrorResponse(Instant.now(), status, code, message, path, null);
    }

    /** A validation failure, carrying one message per rejected field. */
    public static ApiErrorResponse withFieldErrors(
            int status, String code, String message, String path, Map<String, String> fieldErrors) {
        return new ApiErrorResponse(Instant.now(), status, code, message, path, fieldErrors);
    }
}
