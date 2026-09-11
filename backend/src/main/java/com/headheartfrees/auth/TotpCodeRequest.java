package com.headheartfrees.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * One code, for the endpoints that already know who is asking.
 *
 * <p>{@code code} takes either a six-digit TOTP code or a backup code. There is
 * no field saying which - see {@code TotpService.verifySecondFactor} for why a
 * "kind" flag would leak what the caller believed they held.
 *
 * <p>The size bound is generous on purpose. It is there to stop a megabyte of
 * text reaching Argon2id, not to validate the format: a tight bound would
 * reject a malformed code with a <em>different</em> response from a wrong one,
 * and that difference is an oracle. Anything that gets past this is answered
 * with the single generic failure.
 */
public record TotpCodeRequest(
        @Schema(description = "A six-digit code from an authenticator app, or a backup code.")
        @NotBlank(message = "Enter the code from your authenticator app.")
        @Size(max = 64, message = "Enter the code from your authenticator app.")
        String code) {
}
