package com.headheartfrees.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * Begins enrolment, from either of the two places it can start.
 *
 * <p>{@code ticket} is null when the caller already has a session - a USER
 * turning the feature on voluntarily from {@code /account}. It carries an
 * enrolment ticket when the caller has no session yet, which is the admin who
 * was stopped at sign-in. {@code TotpController} accepts exactly one of the
 * two and refuses a request carrying neither.
 */
public record TotpSetupRequest(
        @Schema(description = "An enrolment ticket from /auth/login. Omit when signed in.")
        @Size(max = 4096)
        String ticket) {
}
