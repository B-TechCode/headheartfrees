-- TOTP as a second factor on sign-in.
--
-- Two tables rather than columns on `users`, for three reasons:
--
--   1. `users` is read on every /me and every refresh. There is no reason to
--      drag a credential through those reads.
--   2. A missing row is an unambiguous "not enrolled". Nullable columns on
--      `users` would be three states pretending to be two, and the third -
--      a secret present but unconfirmed - is exactly the state that matters.
--   3. The developer recovery procedure becomes a DELETE against a table with
--      one purpose, rather than an UPDATE whose WHERE clause could damage the
--      account it is meant to rescue. See HANDOVER "if an admin is locked out".
--
-- Nothing here has any relationship to vent_events, and none may ever be
-- added. Rule 2.2 makes venting anonymous; this phase adds a second factor to
-- SIGN-IN and touches the vent path in no way at all.
-- VentUnaffectedByTotpIT asserts that from the outside.

CREATE TABLE user_totp (
    user_id           UUID        PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,

    -- AES-256-GCM, keyed by APP_TOTP_ENCRYPTION_KEY, with the user id as
    -- additional authenticated data so a row cannot be lifted from one
    -- account to another. Format: 'v1:' || base64(nonce || ciphertext || tag).
    --
    -- READ THIS BEFORE ASSUMING THE SECRET IS SAFE. Encryption here separates
    -- "someone holds the database" from "someone holds the database AND the
    -- application's environment". A dump, a stolen backup, a read replica or
    -- SQL injection yields ciphertext and nothing else. Anyone who can read
    -- the server's environment variables can decrypt every row and mint codes
    -- indefinitely. See TotpSecretCipher for the full statement.
    secret_ciphertext TEXT        NOT NULL,

    -- NULL until a code generated from the secret has been verified. A row in
    -- that state is a pending enrolment and grants nothing: sign-in treats it
    -- as not enrolled. Enabling without verifying is how somebody locks
    -- themselves out with a mistyped secret, so it is not possible here.
    confirmed_at      TIMESTAMPTZ NULL,

    -- The RFC 6238 time step of the last code accepted for this account.
    -- A code is refused when its step is <= this value, which kills replay
    -- within the drift window as well as reuse of the same code. See
    -- TotpService.
    last_used_step    BIGINT      NULL,

    -- Consecutive failures, reset to zero ONLY by a success - not by a lock
    -- expiring. That is what makes the backoff below compound instead of
    -- resetting every fifteen minutes.
    failed_attempts   INTEGER     NOT NULL DEFAULT 0,

    -- Set when failed_attempts crosses a threshold. While this is in the
    -- future, no code and no backup code is accepted for this account.
    locked_until      TIMESTAMPTZ NULL,

    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Backup codes: password equivalents, so they are hashed, never stored.
--
-- Argon2id via the application's existing PasswordEncoder, NOT the SHA-256
-- used for refresh_tokens.token_hash. The argument that justifies a plain
-- digest there - 256 bits from SecureRandom, so there is no dictionary - does
-- not carry here: a backup code is ~50 bits, and SHA-256 at 50 bits is days of
-- GPU work against a leaked table. Nor is a deterministic lookup needed: the
-- user id is always known at the moment a code is verified, so the ten rows
-- for that account are fetched and compared one at a time.
CREATE TABLE user_totp_backup_code (
    id         UUID        PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code_hash  TEXT        NOT NULL,

    -- Single use. The row is MARKED rather than deleted, so "how many are
    -- left" is answerable honestly on /account, and so a reused code is
    -- distinguishable from an unknown one in the database while staying
    -- indistinguishable in the response.
    used_at    TIMESTAMPTZ NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Every verification loads one account's codes; every regeneration deletes
-- them by account.
CREATE INDEX idx_user_totp_backup_code_user ON user_totp_backup_code (user_id);
