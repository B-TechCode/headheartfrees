package com.headheartfrees.common.web;

/**
 * Thrown when a caller exceeds a rate limit. Rendered as 429 with a
 * {@code Retry-After} header by {@link GlobalExceptionHandler}.
 */
public class RateLimitExceededException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final long retryAfterSeconds;

    public RateLimitExceededException(long retryAfterSeconds) {
        super("Rate limit exceeded");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    /** Seconds until the next token is available. Never negative. */
    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
