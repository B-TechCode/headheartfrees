package com.headheartfrees.common.web;

import java.time.Instant;

/**
 * Liveness payload returned by {@link HealthController}.
 *
 * @param status  always {@code "UP"} - the endpoint only answers when the
 *                application context has started
 * @param version the built artifact version
 * @param time    the moment the response was produced, in UTC
 */
public record HealthResponse(String status, String version, Instant time) {
}
