package com.headheartfrees.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns on {@code @Scheduled}, for {@code RefreshTokenCleanup}.
 *
 * <p>A separate class rather than an annotation on the application class, so
 * that a test which does not want a scheduler running can exclude exactly this
 * configuration.
 *
 * <p><strong>Every instance runs every job.</strong> There is no distributed
 * lock, so two backends would both run the cleanup. The one job that exists is
 * an idempotent delete, where a race is harmless. Anything added here that is
 * <em>not</em> idempotent needs leader election first.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
