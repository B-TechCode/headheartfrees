package com.headheartfrees.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Login input.
 *
 * <p>No {@code @Email} on the address. A wrongly-formatted address should fail
 * as ordinary bad credentials, not as a validation error: a 400 for "not an
 * email" and a 401 for "wrong password" are distinguishable, and the difference
 * tells an attacker which addresses are worth trying properly.
 */
public record LoginRequest(
        @NotBlank(message = "Email is required.")
        @Size(max = 320)
        String email,

        @NotBlank(message = "Password is required.")
        @Size(max = 200)
        String password) {
}
