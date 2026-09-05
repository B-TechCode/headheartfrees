/**
 * User accounts, registration, login, JWT issuance and Google OAuth2 sign-in.
 *
 * <p>Module boundary (PROJECT_BRIEF.md section 4): this package exposes a public
 * service interface and DTOs. Its entities and repositories are package-private
 * to the module - no other domain may import them. Other domains refer to a user
 * by {@code UUID} only, and there are no JPA relationships that cross a domain
 * boundary.
 *
 * <p>Authentication is always optional for venting. Nothing added here may make
 * {@code /vent} require an account (PROJECT_BRIEF.md section 2.2).
 *
 * <p>Implemented in phase 5 (was phase 4; the vent flow was reordered ahead of it). Empty by design until then.
 */
package com.headheartfrees.auth;
