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
     * 10 per minute, covering every second-factor endpoint.
     *
     * <p>Its own bucket rather than sharing {@link #AUTH}, for the reason the
     * separation exists at all: somebody who fumbles a six-digit code three
     * times must not thereby lose the allowance that lets them sign in.
     *
     * <p>Looser than AUTH because a person legitimately retries a code - the
     * digits change every thirty seconds, and typing one as it rolls over is
     * the ordinary case rather than an attack.
     *
     * <p><strong>This is the weaker half of the defence and is not where the
     * security comes from.</strong> Per-IP limiting is close to worthless here:
     * behind Docker every visitor currently shares one bucket (HANDOVER 10.3),
     * which makes this either evadable from a second address or a nuisance to
     * everybody at once. The limit that holds is per account, on the
     * {@code user_totp} row, where it compounds rather than resetting. See
     * {@code UserTotp} for the arithmetic that makes the difference between a
     * 40%-per-year chance of a hit and a 180-year one.
     */
    TOTP(10, Duration.ofMinutes(1)),

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
