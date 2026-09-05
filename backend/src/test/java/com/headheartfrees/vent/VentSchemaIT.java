package com.headheartfrees.vent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Asserts the shape of the table itself, after the real Flyway migration has
 * run against real Postgres.
 *
 * <p>{@link VentRuleArchitectureTest} guards the Java side; this guards the
 * other end. A future migration could add an {@code ip_address} column without
 * any Java type mentioning it — the entity would ignore the column and every
 * other test would still pass, while the database quietly began retaining
 * exactly what rule 2.1 forbids. This test is what fails in that case.
 */
@SpringBootTest
class VentSchemaIT extends com.headheartfrees.PostgresTestBase {

    @Autowired
    private DataSource dataSource;

    /** Any column whose presence would mean the rule had been broken. */
    private static final List<String> FORBIDDEN_COLUMNS = List.of(
            "content", "text", "body", "message", "note", "entry",
            "user_id", "author_id", "session_id", "device_id",
            "ip", "ip_address", "ip_hash", "remote_addr", "user_agent");

    @Test
    @DisplayName("vent_events has exactly three columns and none of them hold content")
    void ventEventsSchemaIsMinimal() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        List<String> columns = new ArrayList<>(jdbc.queryForList(
                """
                SELECT column_name
                FROM information_schema.columns
                WHERE table_name = 'vent_events'
                ORDER BY ordinal_position
                """,
                String.class));

        assertThat(columns)
                .as("vent_events is a counter. Read PROJECT_BRIEF.md 2.1 before "
                        + "changing this assertion rather than the migration.")
                .containsExactly("id", "mood", "created_at");

        assertThat(columns)
                .as("A column matching one of these names means vent_events has started "
                        + "storing something it must never store.")
                .doesNotContainAnyElementsOf(FORBIDDEN_COLUMNS);
    }

    @Test
    @DisplayName("the database rejects a mood outside the enum, even on a direct insert")
    void checkConstraintRejectsUnknownMood() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        // The application already rejects this. The constraint is the second
        // line of defence: it means the column cannot be widened into a
        // free-text field by anything that bypasses the API.
        assertThatThrownBy(() ->
                jdbc.update("INSERT INTO vent_events (mood) VALUES (?)", "ANYTHING_AT_ALL"))
                .hasMessageContaining("vent_events_mood_allowed");
    }

    @Test
    @DisplayName("a null mood is allowed, because the chip is optional")
    void nullMoodIsAllowed() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("INSERT INTO vent_events (mood) VALUES (NULL)");

        Long count = jdbc.queryForObject(
                "SELECT count(*) FROM vent_events WHERE mood IS NULL", Long.class);
        assertThat(count).isNotNull().isPositive();

        jdbc.update("DELETE FROM vent_events WHERE mood IS NULL");
    }
}
