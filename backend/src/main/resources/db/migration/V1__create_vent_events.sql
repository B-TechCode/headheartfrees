-- The first table in the project.
--
-- vent_events is a counter and nothing else. Read PROJECT_BRIEF.md rule 2.1
-- before changing this file.
--
-- Columns that must never be added here:
--   * any content / text / body / message column. Vent text is never
--     transmitted, so there is nothing to store even if we wanted it.
--   * user_id. A release is anonymous even when the person is signed in.
--   * ip_address, or any column derived from one. The caller's IP is used to
--     key an in-memory rate-limit bucket and is never written down.
--   * a session or device identifier, which would make rows linkable.
--
-- What is left is deliberately not enough to identify anyone: a serial id, an
-- optional mood label from a fixed six-value set, and a timestamp.

CREATE TABLE vent_events (
    id         BIGSERIAL   PRIMARY KEY,
    mood       TEXT        NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- The mood column is TEXT, but it is not free text. The application maps it to
-- a fixed enum and rejects anything else before it reaches the database; this
-- constraint is the second line of that defence, so a direct INSERT cannot
-- widen the column into the free-text field rule 2.1 forbids.
ALTER TABLE vent_events
    ADD CONSTRAINT vent_events_mood_allowed
    CHECK (mood IS NULL OR mood IN ('HEAVY', 'ANXIOUS', 'ANGRY', 'NUMB', 'TIRED', 'LOST'));

-- /api/v1/vent/stats is a COUNT(*) over the whole table, which needs no index.
-- This one supports the "releases over time" reporting the counter implies
-- later, and keeps the table ordered for any future pruning.
CREATE INDEX idx_vent_events_created_at ON vent_events (created_at);
