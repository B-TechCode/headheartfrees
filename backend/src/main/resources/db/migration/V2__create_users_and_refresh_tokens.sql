-- Phase 5: authentication.
--
-- Two tables. Neither has any relationship to vent_events, and none may ever
-- be added: PROJECT_BRIEF.md rule 2.2 makes venting anonymous, and a foreign
-- key from a vent row to a user is exactly how that stops being true.
-- VentSchemaIT and ModuleBoundaryArchitectureTest both fail the build if that
-- boundary is crossed from either direction.

-- CITEXT gives case-insensitive equality and uniqueness on email in one place,
-- so "Aakash@example.com" and "aakash@example.com" cannot both exist and either
-- spelling logs in. The alternative - TEXT plus a unique index on lower(email) -
-- works too, but pushes the lower() into every query and is one forgotten call
-- away from a duplicate account. Verified available in postgres:16-alpine, and
-- supported by RDS, Cloud SQL, Neon and Supabase.
CREATE EXTENSION IF NOT EXISTS citext;

CREATE TABLE users (
    id             UUID        PRIMARY KEY,
    email          CITEXT      NOT NULL UNIQUE,

    -- NULL for an account that exists only via Google. Such a user has no
    -- password to check, and the login endpoint must not treat a NULL hash as
    -- "any password matches" - see DefaultAuthService.login.
    password_hash  TEXT        NULL,

    -- NULL until the account is linked to Google. UNIQUE so one Google account
    -- cannot be attached to two users, but nullable, so ordinary accounts do
    -- not collide with each other on NULL (Postgres treats NULLs as distinct
    -- for uniqueness).
    google_id      TEXT        NULL UNIQUE,

    display_name   TEXT        NULL,

    -- USER or ADMIN. No endpoint writes this column; registration always
    -- inserts USER. The only promotion path is APP_ADMIN_BOOTSTRAP_EMAILS,
    -- applied at startup to an already-registered account. See AdminBootstrap.
    role           TEXT        NOT NULL DEFAULT 'USER',

    -- Nothing is gated on this yet. There is no mail transport in this project,
    -- so no verification message can be sent and nothing may depend on the
    -- column being true. See the phase 5 log for what would have to exist
    -- before it can mean anything.
    email_verified BOOLEAN     NOT NULL DEFAULT false,

    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Second line of defence behind the Java enum, the same pattern as
-- vent_events_mood_allowed: a direct INSERT cannot invent a third role.
ALTER TABLE users
    ADD CONSTRAINT users_role_allowed
    CHECK (role IN ('USER', 'ADMIN'));

-- An account must be reachable by at least one credential. A row with neither
-- a password nor a Google link cannot be logged into by anybody and is almost
-- certainly a bug in whatever created it.
ALTER TABLE users
    ADD CONSTRAINT users_has_a_credential
    CHECK (password_hash IS NOT NULL OR google_id IS NOT NULL);


-- Refresh token rotation with reuse detection.
--
-- Rotation alone is weaker than it sounds: if a token is stolen and the thief
-- uses it after the legitimate client has already rotated, plain rotation just
-- issues the thief a fresh token and nobody notices. What closes that is
-- keeping the spent rows and grouping them by family_id. A presented token that
-- exists but is already revoked means two parties hold tokens from one login,
-- so the whole family is revoked and both are forced to re-authenticate.
--
-- token_hash is SHA-256 of the token, never the token itself. A digest rather
-- than Argon2 because this column is looked up by value on every refresh, which
-- needs to be deterministic; that is safe here in a way it would not be for a
-- password, because the token is 32 bytes from SecureRandom and there is no
-- dictionary to run against it.
CREATE TABLE refresh_tokens (
    id         UUID        PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash TEXT        NOT NULL UNIQUE,
    family_id  UUID        NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Revoking a family on reuse touches every row sharing family_id.
CREATE INDEX idx_refresh_tokens_family ON refresh_tokens (family_id);

-- Logout and "revoke everything for this user" both scan by user.
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);

-- Supports pruning expired rows. Nothing prunes them yet; the table grows one
-- row per refresh and that is flagged in the phase 5 log rather than solved
-- here, because a scheduled job is a phase 9 concern.
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);
