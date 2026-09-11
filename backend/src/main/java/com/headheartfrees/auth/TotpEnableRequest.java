package com.headheartfrees.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Proves a code against the pending secret and turns the factor on.
 *
 * <p>The code is not optional and there is no "enable without verifying" path.
 * That is the whole reason this endpoint exists separately from
 * {@code /totp/setup}: enrolling on an unproven secret is how somebody locks
 * themselves out with a mistyped manual key, and it fails at the next sign-in
 * rather than here where it can still be fixed.
 */
public record TotpEnableRequest(
        @Schema(description = "An enrolment ticket from /auth/login. Omit when signed in.")
        @Size(max = 4096)
        String ticket,

        @Schema(description = "Six digits from the authenticator that just scanned the code.")
        @NotBlank(message = "Enter the six-digit code from your authenticator app.")
        @Size(max = 64, message = "Enter the six-digit code from your authenticator app.")
        String code) {
}
