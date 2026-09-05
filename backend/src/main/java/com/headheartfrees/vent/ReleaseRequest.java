package com.headheartfrees.vent;

/**
 * The entire body of {@code POST /api/v1/vent/release}.
 *
 * <p><strong>This record has one field and must keep having one field.</strong>
 * PROJECT_BRIEF.md rule 2.1: what a person writes on the vent page is never
 * transmitted. There is no {@code content}, {@code text}, {@code body} or
 * {@code message} here, and adding one is not a feature request — it is the
 * specific mistake the rule was written to prevent.
 *
 * <p>{@code mood} is a {@link Mood}, not a {@code String}. That is the point: a
 * free-text field is still a free-text field when it is named "mood", so the
 * type closes it to six known values and Jackson rejects everything else before
 * the request reaches a controller method.
 *
 * <p>{@code VentRuleArchitectureTest} fails the build if a String field ever
 * appears on this record or any other type in this package.
 *
 * @param mood optional. Null when the person released without picking a chip,
 *             which is expected and common.
 */
public record ReleaseRequest(Mood mood) {
}
