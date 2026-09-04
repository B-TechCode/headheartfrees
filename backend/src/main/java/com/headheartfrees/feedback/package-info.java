/**
 * Feedback submission, the public approved-feedback list, and moderation.
 *
 * <p>Module boundary (PROJECT_BRIEF.md section 4): feedback records an optional
 * author as a {@code UUID} user id. It must not import an entity or repository
 * from {@code auth}, and must not declare a JPA relationship to one.
 *
 * <p>Unlike vent content, feedback IS stored - it is submitted deliberately and
 * shown publicly once approved.
 *
 * <p>Implemented in phase 7. Empty by design until then.
 */
package com.headheartfrees.feedback;
