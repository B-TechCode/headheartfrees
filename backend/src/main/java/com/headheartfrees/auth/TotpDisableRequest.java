package com.headheartfrees.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Password and a current code, both required.
 *
 * <p>See {@code DefaultAuthService.disableTotp} for why the password is asked
 * for again when the caller is already signed in: the threat model here is
 * somebody else holding the session.
 */
public record TotpDisableRequest(
        @Schema(description = "The account password.")
        @NotBlank(message = "Enter your password.")
        @Size(max = 512, message = "Enter your password.")
        String password,

        @Schema(description = "A six-digit code, or a backup code.")
        @NotBlank(message = "Enter the code from your authenticator app.")
        @Size(max = 64, message = "Enter the code from your authenticator app.")
        String code) {
}
