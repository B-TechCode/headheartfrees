-- Phase 7: feedback.
--
-- ===========================================================================
-- The rule this table exists under
-- ===========================================================================
--
-- Feedback IS stored. Vent text is NOT. Nothing here may connect a row in this
-- table to a row in vent_events: no foreign key, no shared identifier, and no
-- timestamp precise enough to correlate the two.
--
-- That last one is the non-obvious half, and it is why created_at is truncated
-- to the hour on write (see DefaultFeedbackService). A release at 14:32:10 and
-- a feedback row at 14:33:45 would, at this site's traffic, be the same person
-- to anyone holding a database dump - which is exactly the link the brief
-- forbids. Hour granularity keeps the moderation queue roughly ordered while
-- making proximity correlation useless.
--
-- moderated_at is NOT truncated. It records a moderator acting on their own
-- queue, not a visitor, so there is nothing to correlate it against.

CREATE TABLE feedback (
    id            UUID        PRIMARY KEY,

    -- A plain UUID column with a database-level FK and NO JPA association, the
    -- same shape as refresh_tokens.user_id. PROJECT_BRIEF.md section 4: modules
    -- reference each other by id, never by entity. ModuleBoundaryArchitectureTest
    -- fails the build if feedback imports an auth type.
    --
    -- NULL for an anonymous submission, which is a first-class case rather than
    -- a degraded one. Being signed in must never force attribution.
    --
    -- ON DELETE SET NULL, not CASCADE: if an account goes away, the note stays
    -- and simply stops being attributed. Deleting published writing because its
    -- author closed an account would be a surprising thing for this site to do.
    user_id       UUID        NULL REFERENCES users(id) ON DELETE SET NULL,

    -- What the person chose to be called here. Deliberately NOT read from
    -- users.display_name at render time: the value is copied in at submission,
    -- so changing an account name later does not silently rewrite words already
    -- published under the old one.
    display_name  TEXT        NULL,

    location      TEXT        NULL,

    rating        SMALLINT    NOT NULL,
    message       TEXT        NOT NULL,

    -- PENDING, APPROVED or REJECTED. Nothing is public until a moderator acts.
    status        TEXT        NOT NULL DEFAULT 'PENDING',

    -- The audit trail. A moderation queue without one leaves the reviewer
    -- unable to answer "did I publish this, and when" - which they will need,
    -- because Community Guidelines promise that people can ask for removal.
    -- Both are rewritten on every decision: this records the CURRENT decision
    -- and its author, not a history. See the phase 7 log for what a real
    -- history would need.
    moderated_at  TIMESTAMPTZ NULL,
    moderated_by  UUID        NULL REFERENCES users(id) ON DELETE SET NULL,

    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Second line of defence behind the Java enum, matching the pattern used by
-- vent_events_mood_allowed and users_role_allowed. A direct INSERT cannot
-- invent a fourth status and make a row that no code path can render.
ALTER TABLE feedback
    ADD CONSTRAINT feedback_status_allowed
    CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'));

-- Section 5 specifies 1-5. Enforced here as well as in bean validation so that
-- a rating of 0 or 9 cannot reach the table by any route.
ALTER TABLE feedback
    ADD CONSTRAINT feedback_rating_range
    CHECK (rating BETWEEN 1 AND 5);

-- The public wall reads exactly one status, newest first. Without this the
-- query is a sequential scan plus a sort on every page load of /voices.
CREATE INDEX idx_feedback_status_created_at
    ON feedback (status, created_at DESC);
