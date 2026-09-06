package com.headheartfrees;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * A real Postgres for every test that boots the application context.
 *
 * <p>From phase 4 the application requires a database to start — {@code
 * vent_events} is the first table, and Hibernate validates its mapping against
 * the schema Flyway produced. That makes this base class mandatory for every
 * {@code @SpringBootTest} in the project, not only the vent ones, which is why
 * it lives here rather than in a domain package.
 *
 * <p>The container is static and started once per JVM rather than per class,
 * and is never stopped explicitly: Testcontainers' Ryuk sidecar reaps it when
 * the JVM exits. A database per test class would multiply the suite's runtime
 * for no additional confidence.
 *
 * <p>Postgres 16 to match {@code docker-compose.yml}. Testing against a
 * different major version than production runs would undercut the point of
 * using a real database at all.
 *
 * <h2>Why the local profile</h2>
 *
 * {@code JwtSecretGuard} in the config package refuses to start on the
 * committed {@code app.auth.jwt-secret} default outside the {@code local}
 * profile, and the suite runs on exactly that default. Declaring the profile
 * here says what a test run is - a local install - rather than handing every
 * test class a secret of its own. It also means these tests exercise the
 * guard's accepting branch on every run: if it were wrong, nothing would boot.
 */
@ActiveProfiles("local")
public abstract class PostgresTestBase {

    @SuppressWarnings("resource") // Reaped by Ryuk at JVM exit, not by us.
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("headheartfrees")
                    .withUsername("headheartfrees")
                    .withPassword("headheartfrees");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
