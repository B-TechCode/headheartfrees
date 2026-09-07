package com.headheartfrees.common.web;

import java.time.Duration;

/**
 * The rate limits from PROJECT_BRIEF.md section 6, in one place.
 *
 * <p>Each constant is a separate bucket per caller: {@link ClientIpRateLimiter}
 * keys on policy <em>and</em> address, so exhausting the auth allowance cannot
 * stop the same person from venting. That separation is not incidental. Venting
 * requires no account (rule 2.2), and a shared bucket would let failed sign-in
 * attempts lock someone out of the one part of the site that is supposed to be
 * unconditionally available.
 */
public enum RateLimitPolicy {

    /** 30 releases per minute. */
    RELEASE(30, Duration.ofMinutes(1)),

    /**
     * 5 per minute, covering register, login and refresh. Tight because these
     * are the endpoints worth guessing against; logout and /me are excluded so
     * that a throttled caller can still sign out and still read their own
     * account.
     */
    AUTH(5, Duration.ofMinutes(1)),

    /**
     * 3 per hour, covering feedback submission (section 6).
     *
     * <p>Far tighter than the others because this is the only endpoint on the
     * site that stores text a stranger wrote and later shows it to the public.
     * The limit is the first line against someone pasting the same thing forty
     * times; the moderation queue is the second, and is the one that matters.
     *
     * <p>An hour window rather than a minute is deliberate: a minute-based
     * limit would be exhausted and forgotten by the time anyone noticed, and
     * three notes in an hour is already more than an honest submitter needs.
     */
    FEEDBACK(3, Duration.ofHours(1));

    private final int capacity;
    private final Duration window;

    RateLimitPolicy(int capacity, Duration window) {
        this.capacity = capacity;
        this.window = window;
    }

    public int capacity() {
        return capacity;
    }

    public Duration window() {
        return window;
    }
}
