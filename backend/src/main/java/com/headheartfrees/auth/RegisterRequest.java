package com.headheartfrees.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Registration input.
 *
 * <p>The password is deliberately annotated only with {@code @NotBlank} and a
 * generous {@code @Size}. The real rules live in {@link PasswordPolicy}, so
 * that every rejection carries one explanatory sentence rather than a bean
 * validation field error, and so the rules are testable without a web layer.
 *
 * <p>There is no {@code role} field, and there must never be one. Registration
 * always produces a USER; a role in this record is how that stops being true.
 */
public record RegisterRequest(
        @NotBlank(message = "Email is required.")
        @Email(message = "That does not look like an email address.")
        @Size(max = 320, message = "Email is too long.")
        String email,

        @NotBlank(message = "Password is required.")
        @Size(max = 200, message = "Password must be at most 200 characters.")
        String password,

        @Size(max = 80, message = "Display name must be at most 80 characters.")
        String displayName) {
}
