package com.headheartfrees.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * The second half of a sign-in: the ticket from {@code /auth/login}, and a code.
 *
 * <p>The ticket travels in the body rather than in a header or a query string.
 * A query string would put a live credential into browser history and into
 * every proxy log between here and the person; a bespoke header would need its
 * own CORS entry for no gain. The body is the one place it is neither logged
 * nor cached by default.
 */
public record TotpLoginRequest(
        @Schema(description = "The ticket returned by /auth/login.")
        @NotBlank(message = "That sign-in attempt has expired. Start again.")
        @Size(max = 4096)
        String ticket,

        @Schema(description = "A six-digit code from an authenticator app, or a backup code.")
        @NotBlank(message = "Enter the code from your authenticator app.")
        @Size(max = 64, message = "Enter the code from your authenticator app.")
        String code) {
}
