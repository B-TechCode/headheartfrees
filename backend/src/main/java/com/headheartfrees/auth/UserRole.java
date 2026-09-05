package com.headheartfrees.auth;

/**
 * The two roles. Public because {@link UserSummary} exposes it and the admin
 * endpoints in later phases will reference it by name in {@code @PreAuthorize}.
 *
 * <p>There is no self-service path to {@link #ADMIN}. Registration always
 * writes {@link #USER}, no endpoint accepts a role in its request body, and the
 * only promotion is {@code APP_ADMIN_BOOTSTRAP_EMAILS} applied at startup to an
 * account that already exists.
 */
public enum UserRole {
    USER,
    ADMIN;

    /** The authority name Spring Security expects, e.g. {@code ROLE_ADMIN}. */
    String authority() {
        return "ROLE_" + name();
    }
}
