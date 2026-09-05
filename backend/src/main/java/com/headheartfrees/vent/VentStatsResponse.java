package com.headheartfrees.vent;

/**
 * Response for {@code GET /api/v1/vent/stats}.
 *
 * <p>One figure, and it is the real one. PROJECT_BRIEF.md section 6 originally
 * specified {@code { totalReleases, countries }}; a country breakdown needs the
 * caller's IP geolocated, and both rule 2.1 and the {@code vent_events}
 * definition forbid retaining an IP at all. The field could not have been built
 * honestly, so it was removed from the brief rather than faked here.
 *
 * @param totalReleases every row in {@code vent_events}. Early on this will be a
 *                      small number. It is not rounded, padded, given a
 *                      thousands floor, or seeded — if it says 4, four people
 *                      pressed the button.
 */
public record VentStatsResponse(long totalReleases) {
}
