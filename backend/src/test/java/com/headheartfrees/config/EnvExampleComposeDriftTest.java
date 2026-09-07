package com.headheartfrees.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * `.env.example` and `docker-compose.yml` must not drift apart.
 *
 * ===========================================================================
 * Why this test exists
 * ===========================================================================
 *
 * Three times now the code has been right and the wiring wrong, and all three
 * were found by loading a page in a browser rather than by the suite:
 *
 *   1. Phase 5 — every `APP_*` and `GOOGLE_*` variable was documented in
 *      `.env.example` and read by the application, but none were listed in the
 *      compose backend service. The container fell back to `application.yml`
 *      defaults whatever `.env` said, and Google sign-in was unreachable.
 *   2. Phase 5 — `APP_ADMIN_BOOTSTRAP_EMAILS` had never once promoted an
 *      account under compose, for the same reason.
 *   3. Phase 6 — `NEXT_PUBLIC_GOOGLE_SIGN_IN` reached the compose file and the
 *      Dockerfile correctly, but was missing from the operator's `.env`, so the
 *      `:-false` default was baked into the bundle and `/login` drew no Google
 *      button.
 *
 * The shape is always the same: two hand-edited files that have to agree, no
 * compiler and no test holding them together. This asserts the agreement.
 *
 * ===========================================================================
 * What it does and does not prove
 * ===========================================================================
 *
 * It proves a documented variable has a *path* into the container. It cannot
 * prove the application then reads it — phase 5 §4 makes that point at length,
 * and the answer there was a test that sets a bad value and asserts the app
 * refuses to boot (`JwtSecretGuardTest`). Keep both: this one catches the
 * variable that was never plumbed, that one catches the variable that is
 * plumbed to nothing.
 *
 * It also cannot check `.env` itself, which is gitignored and rightly absent
 * from CI. Instance 3 above lived precisely there. What this closes for that
 * case is narrower: `.env.example` stays a complete and accurate template, so
 * an operator diffing their `.env` against it sees the missing key.
 *
 * Deliberately a plain `*Test`, not an `*IT`: no Spring context, no database,
 * nothing to start. It runs in `mvn test`, in under a millisecond, next to
 * `ModuleBoundaryArchitectureTest`, which is the same kind of structural rule.
 */
class EnvExampleComposeDriftTest {

  /**
   * Which prefixes are checked, and where each one has to appear.
   *
   * `NEXT_PUBLIC_*` goes to the frontend's `build.args` rather than its
   * `environment`, and that distinction is the whole point of listing it here:
   * these are inlined into the JavaScript bundle at build time, so a value
   * placed under `environment` would be accepted by compose, would show up in
   * `docker compose exec frontend env`, and would still be missing from the
   * bundle the browser downloads. Putting it under `environment` is exactly the
   * plausible-looking mistake this table rules out.
   *
   * `POSTGRES_*`, `SPRING_*`, `BACKEND_PORT` and `FRONTEND_PORT` are out of
   * scope on purpose — they are consumed by the db service, by Spring Boot's
   * own relaxed binding, or by port mappings, and each would need its own rule.
   * The prefixes here are the ones whose absence has actually shipped a bug.
   */
  private static final Map<String, Location> RULES = Map.of(
      "APP_", new Location("backend", "environment"),
      "GOOGLE_", new Location("backend", "environment"),
      "NEXT_PUBLIC_", new Location("frontend", "build.args"));

  private record Location(String service, String section) {}

  /** `KEY=value` at the start of a line — the only form `.env.example` uses. */
  private static final Pattern ENV_KEY = Pattern.compile("^([A-Z][A-Z0-9_]*)=");

  /** `${VAR}`, `${VAR:-default}`, `${VAR:?msg}` — any compose interpolation. */
  private static final Pattern INTERPOLATION = Pattern.compile("\\$\\{([A-Z][A-Z0-9_]*)[^}]*}");

  @Test
  @DisplayName("every APP_/GOOGLE_/NEXT_PUBLIC_ key in .env.example reaches its compose service")
  void everyDocumentedKeyIsWiredIntoCompose() {
    Set<String> documented = documentedKeys();

    // A guard on the guard. If the parse silently returned nothing — the file
    // moved, the format changed — every assertion below would vacuously pass
    // and this test would sit in the suite proving nothing, which is the
    // failure mode described in phase 5 §3 for OAuth2AbsentConfigIT.
    assertThat(documented)
        .as(".env.example parsed to no APP_/GOOGLE_/NEXT_PUBLIC_ keys at all, which cannot be "
            + "right - the parser or the file has changed shape")
        .isNotEmpty();

    List<String> missing = new ArrayList<>();
    for (String key : documented) {
      Location where = locationFor(key);
      if (!sectionSupplies(where, key)) {
        missing.add("%s -> service '%s', %s".formatted(key, where.service(), where.section()));
      }
    }

    assertThat(missing)
        .as("""
            These keys are documented in .env.example but nothing in docker-compose.yml passes \
            them to the container, so setting them in .env does nothing. Add each to the named \
            service - `KEY: ${KEY:-default}`, or the bare `KEY:` null form when an empty string \
            would be wrong (see the APP_JWT_SECRET comment in docker-compose.yml). \
            NEXT_PUBLIC_* belongs in the frontend's build.args, not its environment: it is \
            inlined into the bundle at build time and needs `docker compose build frontend`.\
            """)
        .isEmpty();
  }

  @Test
  @DisplayName("compose does not interpolate an APP_/GOOGLE_/NEXT_PUBLIC_ key .env.example omits")
  void everyComposeKeyIsDocumented() {
    Set<String> documented = documentedKeys();
    Set<String> undocumented = new LinkedHashSet<>();

    for (Location where : Set.copyOf(RULES.values())) {
      for (Map.Entry<String, String> entry : sectionOf(where).entrySet()) {
        // The null form (`APP_JWT_SECRET:`) names the variable in its key and
        // has no value to scan, so check both sides of every entry.
        collectCandidates(entry.getKey(), entry.getValue(), undocumented);
      }
    }
    undocumented.removeAll(documented);

    assertThat(undocumented)
        .as("docker-compose.yml passes these through but .env.example never mentions them, so "
            + "nobody deploying this knows they exist. Document each one.")
        .isEmpty();
  }

  private void collectCandidates(String key, String value, Set<String> into) {
    if (isChecked(key)) {
      into.add(key);
    }
    if (value == null) {
      return;
    }
    Matcher matcher = INTERPOLATION.matcher(value);
    while (matcher.find()) {
      if (isChecked(matcher.group(1))) {
        into.add(matcher.group(1));
      }
    }
  }

  /**
   * True when {@code section} actually hands {@code key} to the container.
   *
   * Two forms count. `KEY: ${KEY:-default}` is the ordinary one. A bare `KEY:`
   * with no value is the null form, which compose passes through from the
   * environment and, unlike `${KEY:-}`, leaves *absent* when unset rather than
   * setting it to the empty string — the distinction `APP_JWT_SECRET` depends
   * on to keep a fresh clone booting.
   *
   * The value is scanned as well as the key so that a variable wired under a
   * different name still counts: `SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER}`
   * is a real path for `POSTGRES_USER`, and asserting on key names alone would
   * reject it.
   */
  private boolean sectionSupplies(Location where, String key) {
    Map<String, String> section = sectionOf(where);
    if (section.containsKey(key)) {
      return true;
    }
    return section.values().stream()
        .filter(java.util.Objects::nonNull)
        .anyMatch(value -> {
          Matcher matcher = INTERPOLATION.matcher(value);
          while (matcher.find()) {
            if (matcher.group(1).equals(key)) {
              return true;
            }
          }
          return false;
        });
  }

  private Location locationFor(String key) {
    return RULES.entrySet().stream()
        .filter(rule -> key.startsWith(rule.getKey()))
        .map(Map.Entry::getValue)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("no rule for " + key));
  }

  private boolean isChecked(String key) {
    return RULES.keySet().stream().anyMatch(key::startsWith);
  }

  private Set<String> documentedKeys() {
    Set<String> keys = new LinkedHashSet<>();
    for (String line : readLines(repoRoot().resolve(".env.example"))) {
      Matcher matcher = ENV_KEY.matcher(line.strip());
      if (matcher.find() && isChecked(matcher.group(1))) {
        keys.add(matcher.group(1));
      }
    }
    return keys;
  }

  /**
   * One service section of the compose file, flattened to string values.
   *
   * `environment` accepts a map or a `KEY=value` list and both are handled: the
   * file uses the map form today, and a future edit switching to the list form
   * must not silently turn this test green by parsing to nothing.
   */
  @SuppressWarnings("unchecked")
  private Map<String, String> sectionOf(Location where) {
    Map<String, Object> compose;
    try (var input = Files.newInputStream(repoRoot().resolve("docker-compose.yml"))) {
      compose = new Yaml().load(input);
    } catch (IOException e) {
      throw new UncheckedIOException("could not read docker-compose.yml", e);
    }

    Map<String, Object> services = (Map<String, Object>) compose.get("services");
    assertThat(services).as("docker-compose.yml has no `services` block").isNotNull();

    Map<String, Object> service = (Map<String, Object>) services.get(where.service());
    assertThat(service)
        .as("docker-compose.yml has no `%s` service, which this project requires", where.service())
        .isNotNull();

    Object raw = service;
    for (String segment : where.section().split("\\.")) {
      assertThat(raw)
          .as("`%s` in service `%s` is not a mapping", where.section(), where.service())
          .isInstanceOf(Map.class);
      raw = ((Map<String, Object>) raw).get(segment);
    }

    assertThat(raw)
        .as("service `%s` has no `%s` section - the variables it should carry cannot reach the "
            + "container", where.service(), where.section())
        .isNotNull();

    if (raw instanceof List<?> list) {
      return list.stream()
          .map(String::valueOf)
          .map(entry -> entry.split("=", 2))
          .collect(Collectors.toMap(
              parts -> parts[0],
              parts -> parts.length > 1 ? parts[1] : "",
              (a, b) -> b,
              LinkedHashMap::new));
    }

    Map<String, String> flattened = new LinkedHashMap<>();
    ((Map<String, Object>) raw).forEach(
        (key, value) -> flattened.put(key, value == null ? null : String.valueOf(value)));
    return flattened;
  }

  /**
   * Walks up from the working directory to the directory holding both files.
   *
   * Surefire runs with the working directory set to `backend/`, but `mvn` can
   * also be invoked from the repo root with `-pl backend`, and IDEs pick their
   * own. Walking up costs nothing and makes the test independent of which.
   */
  private Path repoRoot() {
    Path candidate = Paths.get("").toAbsolutePath();
    while (candidate != null) {
      if (Files.isRegularFile(candidate.resolve("docker-compose.yml"))
          && Files.isRegularFile(candidate.resolve(".env.example"))) {
        return candidate;
      }
      candidate = candidate.getParent();
    }
    throw new IllegalStateException(
        "found no ancestor of " + Paths.get("").toAbsolutePath()
            + " containing both docker-compose.yml and .env.example");
  }

  private List<String> readLines(Path path) {
    try {
      return Files.readAllLines(path);
    } catch (IOException e) {
      throw new UncheckedIOException("could not read " + path, e);
    }
  }
}
