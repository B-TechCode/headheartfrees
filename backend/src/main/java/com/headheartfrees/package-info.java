/**
 * HeadHeartFreeS backend.
 *
 * <p>The application is split into domain packages ({@code auth}, {@code feedback},
 * {@code vent}, {@code donation}) plus shared {@code common} and {@code config}
 * packages. Domain packages are treated as independently deployable modules: each
 * exposes only a public service interface and DTOs, and references other domains by
 * identifier rather than by entity reference. See PROJECT_BRIEF.md section 4.
 */
package com.headheartfrees;
