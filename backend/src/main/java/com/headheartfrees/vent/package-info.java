/**
 * The vent domain: an anonymous release counter, and nothing else.
 *
 * <p><strong>Vent text never leaves the browser</strong> (PROJECT_BRIEF.md
 * section 2.1). It is not sent, not logged, and not "saved then deleted". The
 * release endpoint accepts an optional mood label only.
 *
 * <p>Concretely, for anything in this package:
 * <ul>
 *   <li>No DTO may carry a {@code content}, {@code text}, {@code body} or
 *       equivalent free-text field. A DTO with one is a bug, not a feature.</li>
 *   <li>The {@code vent_events} table stores an id, an optional mood and a
 *       timestamp. No content, no user id, no IP address.</li>
 *   <li>Nothing here may write request bodies to a log.</li>
 * </ul>
 *
 * <p>Implemented in phase 4. These rules are no longer only documentation:
 * {@code VentRuleArchitectureTest} fails the build if any type in this package
 * declares a String field, and the {@code vent_events_mood_allowed} CHECK
 * constraint stops the database accepting a mood outside {@link Mood}.
 */
package com.headheartfrees.vent;
