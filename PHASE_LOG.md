# HeadHeartFreeS — Phase Log

**Generated:** 2026-09-04
**Scope:** Phase 1 (Scaffold) per [PROJECT_BRIEF.md](PROJECT_BRIEF.md) §9.

> **How this document was produced.** The session that built Phase 1 lost its
> context. This log was reconstructed by reading the working tree on disk and by
> re-running the verification commands. It is not a transcript of the original
> reasoning. Section 3 is explicitly labelled as inferred, and section 10 is
> unavailable. Everything in sections 2, 4, 6, 7 and 8 is read directly from
> files or from real command output.
>
> **Updated 2026-09-04 after a cleanup pass** that closed the two caveats below,
> aligned the brief to the code on Spring Boot, and ran the Docker verification
> that had been skipped. Superseded claims have been rewritten rather than left
> standing; resolved items say so explicitly instead of quietly disappearing.

---

## 1. Phase completed and current status

**Phase 1 - Scaffold: done. Both earlier caveats are now closed.**

Everything the brief's phase-1 row asks for exists: repo layout, `pom.xml`,
`.gitignore`, `.env.example`, both Dockerfiles, `docker-compose.yml`,
`README.md`, a health endpoint, and a booting Next.js app. The backend test
suite passes, the frontend produces a production build, and the full compose
stack has now been built, started, exercised over HTTP and torn down.

Previously open, now resolved:

1. ~~`docker compose up` has never been run.~~ **Run.** Both images build, all
   three containers reach a healthy state, and `/api/v1/health` and the frontend
   root both answer 200 over real HTTP. The brief's section 10 requirement that
   `docker compose up` still works end to end is now genuinely met. Exact
   commands and output in section 6.
2. ~~The common error handler does not exist.~~ **Implemented.** A
   `@RestControllerAdvice` plus a filter-chain handler return the section 6 shape
   for validation, 404, 405, 401, 403 and an internal-error catch-all, covered by
   five tests and confirmed over HTTP against the running container.

One thing worth knowing rather than a caveat: Docker Desktop was not running on
this machine when the verification started, and was started in order to complete
it. It is still running. Nothing else about the machine state was changed.

No phase 2+ work has started. No git commands were run at any point.

---

## 2. Files on disk

Build artefacts (`backend/target/`, `frontend/.next/`, `frontend/node_modules/`)
are present but omitted from the tree; all are gitignored.

```
headheartfrees/
├── PROJECT_BRIEF.md                  Source of truth for the whole build. Not authored by this work.
├── PHASE_LOG.md                      This file.
├── README.md                         Developer entry point: stack, quick start, env, conventions, known noise.
├── .gitignore                        Ignores .env*, target/, node_modules/, .next/, IDE and OS files.
├── .env.example                      Every env var, grouped by the phase that starts using it. Placeholders only.
├── docker-compose.yml                Three services: db, backend, frontend, with healthchecks and local defaults.
├── backend/
│   ├── Dockerfile                    3 stages: maven build → Spring Boot layer extract → temurin JRE runtime, non-root.
│   ├── .dockerignore                 Keeps target/, .git/, markdown and IDE dirs out of the build context.
│   ├── mvnw / mvnw.cmd               Maven wrapper scripts, so Maven need not be installed.
│   ├── .mvn/wrapper/
│   │   └── maven-wrapper.properties  Pins Maven 3.9.11, wrapper 3.3.4, script-only distribution.
│   ├── pom.xml                       Spring Boot parent 3.5.16, Java 21, deliberately minimal dependency set.
│   └── src/
│       ├── main/java/com/headheartfrees/
│       │   ├── HeadHeartFreesApplication.java   Entry point. @SpringBootApplication + @ConfigurationPropertiesScan.
│       │   ├── package-info.java                Documents the module-boundary rule from brief §4.
│       │   ├── common/
│       │   │   ├── package-info.java            States that common may not depend on any domain package.
│       │   │   └── web/
│       │   │       ├── package-info.java        Scope note for shared web plumbing.
│       │   │       ├── HealthController.java    GET /api/v1/health. Public. Backs the compose healthcheck.
│       │   │       ├── HealthResponse.java      Record: { status, version, time }.
│       │   │       ├── ApiErrorResponse.java    The §6 error shape. fieldErrors is null (and so omitted) unless validation failed.
│       │   │       ├── GlobalExceptionHandler.java  @RestControllerAdvice mapping every escaped exception to that shape.
│       │   │       └── SecurityErrorHandler.java    Same shape for filter-chain 401/403, which never reach the advice.
│       │   ├── config/
│       │   │   ├── package-info.java            Config holds wiring, not business rules.
│       │   │   ├── SecurityConfig.java          Stateless chain, CSRF off, CORS on, explicit public path list, SecurityErrorHandler wired in.
│       │   │   ├── CorsProperties.java          Binds app.cors.allowed-origins. Explicit origins, no wildcard.
│       │   │   └── OpenApiConfig.java           Swagger metadata; restates the "no vent text" rule in the API docs.
│       │   ├── auth/package-info.java           Empty by design. Boundary contract for phase 4.
│       │   ├── vent/package-info.java           Empty by design. Enumerates the §2.1 prohibitions for phase 6.
│       │   ├── feedback/package-info.java       Empty by design. Boundary contract for phase 7.
│       │   └── donation/package-info.java       Empty by design. Boundary contract for phase 8.
│       ├── main/resources/
│       │   ├── application.yml                  Port, graceful shutdown, Jackson, virtual threads, CORS, springdoc, logging.
│       │   └── application-local.yml            Local profile: debug logging for com.headheartfrees.
│       └── test/java/com/headheartfrees/common/web/
│           ├── HealthControllerTest.java        2 tests: health is public; unlisted paths are not.
│           └── GlobalExceptionHandlerTest.java  5 tests: validation, 404, 405, 401, and a passing-body control.
└── frontend/
    ├── Dockerfile                    3 stages: deps → build → node:22-alpine standalone runtime, non-root.
    ├── .dockerignore                 Keeps node_modules/, .next/, .env* and IDE dirs out of the build context.
    ├── package.json                  Scripts: dev, build, start, lint, typecheck. Next 15 / React 19 / Tailwind 4.
    ├── package-lock.json             lockfileVersion 3. 329 packages. What `npm ci` installs.
    ├── next.config.ts                output: "standalone", strict mode, no powered-by header, build fails on TS/ESLint errors.
    ├── tsconfig.json                 strict + noUncheckedIndexedAccess + noImplicitOverride. Path alias @/* → ./src/*.
    ├── eslint.config.mjs             Flat config extending next/core-web-vitals and next/typescript.
    ├── postcss.config.mjs            Single plugin: @tailwindcss/postcss (Tailwind v4).
    ├── next-env.d.ts                 Next.js generated ambient types. Gitignored (Next's own default).
    ├── public/.gitkeep               Placeholder so the static dir survives in version control.
    └── src/
        ├── app/
        │   ├── layout.tsx            Root layout: metadata template, viewport, imports globals.css.
        │   └── page.tsx              Deliberately unstyled scaffold placeholder. Real home page is phase 3.
        ├── components/ui/.gitkeep            Placeholder for phase 2 primitives.
        ├── components/layout/.gitkeep        Placeholder for navbar/footer, phase 2.
        ├── components/sections/.gitkeep      Placeholder for page sections, phase 3.
        ├── lib/api.ts                Typed fetch wrapper, ApiError class, Retry-After parsing. Carries the §2.1 rule as a comment.
        └── styles/globals.css        Colour custom properties + @theme mapping, reduced-motion block. Placeholder palette.
```

---

## 3. Key decisions and deviations

> **Inferred from code, not a record of prior reasoning.** The original session's
> context is gone. What follows is read off the files themselves and off comments
> the previous session left behind. Where a comment states a rationale, that is
> quoted as the file's claim — not as verified fact about what was decided or
> what you approved. **Nothing here should be taken as something you signed off
> on.** Any of it may be reversed.

### Deviations from PROJECT_BRIEF.md

| # | Brief says | Code does | Evidence on disk |
|---|---|---|---|
| D2 | §3 lists JPA, PostgreSQL, Flyway, jjwt, OAuth2 client, Bucket4j, Testcontainers as the backend stack | None are on the classpath | `pom.xml` carries a comment: the classpath is kept small so `spring-boot:run` starts with no database and no credentials; each dependency is added in the phase that first uses it. Consistent with the brief's phase table (§9), but the §3 stack list is not yet satisfied. |
| D3 | §3: "Tailwind CSS" (version unstated) | **Tailwind v4**, CSS-first, no `tailwind.config.js` | `postcss.config.mjs` + `@theme` in `globals.css`. README documents the browser floor this imposes (Safari 16.4+, Chrome 111+, Firefox 128+) and says it "was accepted deliberately". That acceptance was not yours. |
| D4 | §3: "shadcn/ui as a base" | Not installed | Phase 2 territory per §9. Consistent with phasing. |

**Resolved in the 2026-09-04 cleanup pass** (IDs are left stable rather than
renumbered, so the gaps are intentional):

- **D1 — Spring Boot 3.3.x vs 3.5.16.** Resolved in the code's favour: you
  confirmed 3.5.16 is correct and that the brief was the stale side.
  PROJECT_BRIEF.md §3 now reads "Spring Boot — latest supported 3.x GA
  (currently 3.5.16)", so this is no longer a deviation.
- **D5 / D6 — the §6 error shape was referenced but unimplemented.** Now
  implemented; see §2 for the three new classes and §6 for the tests.

### Judgment calls with no basis in the brief either way

| # | Call | Where | What the code says about it |
|---|---|---|---|
| J1 | React 19.2.x chosen | `package.json` | Brief never names a React version. Next 15 supports it. |
| J2 | Virtual threads enabled | `application.yml` (`spring.threads.virtual.enabled: true`) | No comment explaining it. Not requested by the brief. |
| J3 | `-Xlint:all -Werror` on the compiler | `pom.xml` | Commented as implementing §10's "compiles with zero warnings". A strict reading; it will make future dependencies harder to add. |
| J4 | `output: "standalone"` for Next | `next.config.ts` | Serves the frontend Dockerfile's runtime stage. |
| J5 | `db` service present in compose from phase 1 | `docker-compose.yml` | Commented: "the datasource is not wired up until phase 4; `db` is already here so the topology does not change when it is." Means compose starts a Postgres nothing uses. |
| J6 | Health endpoint at `/api/v1/health` | `HealthController.java` | Brief §6 lists no health route. Phase 1 requires "health endpoint", so the path was invented. |
| J7 | `anyRequest().authenticated()` with no auth mechanism | `SecurityConfig.java` | Every non-public path 401s. Intentional per the class Javadoc; the test asserts 4xx rather than a specific code. |
| J8 | Maven wrapper set to `only-script` | `maven-wrapper.properties` | No `maven-wrapper.jar` is committed. The `!.mvn/wrapper/maven-wrapper.jar` negation in `.gitignore` is therefore inert. |
| J9 | App version filtered into `application.yml` via `@project.version@` | `application.yml` + `HealthController` | Ties the health payload's version to the Maven artifact version. |
| J10 | Placeholder palette is warm paper/ink, not grey | `globals.css` | Five `--hhf-*` properties. Comment says they are "deliberately plain, to be replaced wholesale". |

---

## 4. Exact versions

Read from `pom.xml`, `package-lock.json`, Dockerfiles and the local toolchain.

### Pinned in the repo

| Thing | Version | Where |
|---|---|---|
| Spring Boot | **3.5.16** | `backend/pom.xml` parent |
| Java (bytecode target) | **21** | `backend/pom.xml` `<java.version>` |
| springdoc-openapi | **2.9.0** | `backend/pom.xml` `<springdoc.version>` |
| Maven (wrapper-provisioned) | **3.9.11** | `maven-wrapper.properties` |
| Maven wrapper | **3.3.4** | `maven-wrapper.properties` |
| Next.js | **15.5.25** (locked) | `package-lock.json`, spec `^15.5.25` |
| React / React DOM | **19.2.8** (locked) | `package-lock.json`, spec `^19.2.0` |
| Tailwind CSS | **4.3.3** (locked) | `package-lock.json`, spec `^4.3.3` |
| @tailwindcss/postcss | **4.3.3** | `package-lock.json` |
| TypeScript | **5.9.3** | `package-lock.json` |
| ESLint | **9.39.5** | `package-lock.json` |
| eslint-config-next | `^15.5.25` | `package.json` |
| PostgreSQL | **16-alpine** | `docker-compose.yml` |
| Node (build + runtime images) | **22-alpine** | `frontend/Dockerfile` |
| Maven build image | **3.9-eclipse-temurin-21** | `backend/Dockerfile` |
| JRE runtime image | **eclipse-temurin:21-jre-alpine** | `backend/Dockerfile` |
| npm lockfile version | **3** | `package-lock.json` |

### Local machine used for verification

| Thing | Version |
|---|---|
| JDK | **22.0.1** (compiles to target 21) |
| Node.js | **v22.18.0** |
| npm | **10.9.3** |
| Docker | **29.2.1** |
| Docker Compose | **v5.0.2** |

Note the JDK mismatch: the build ran on **JDK 22**, not 21. Bytecode targets 21,
and the Docker images use Temurin 21, so container behaviour may differ subtly
from what was tested locally.

---

## 5. Dependencies deliberately deferred

Absent from `pom.xml` and `package.json` on purpose, per the phase table.

| Dependency | Belongs to phase |
|---|---|
| Spring Data JPA | 4 — Auth backend (first entity) |
| PostgreSQL JDBC driver | 4 |
| Flyway (+ `src/main/resources/db/migration`) | 4 |
| jjwt (JWT issuance/parsing) | 4 |
| Spring Security OAuth2 Client (Google) | 4 |
| Bucket4j (rate limiting) | 4 — first needed by the auth endpoints' 5/min limit |
| Testcontainers | 4 — first test needing a real Postgres |
| shadcn/ui | 2 — Design system |
| Framer Motion | 2 |
| react-hook-form + zod | 5 — Auth frontend (first real form) |
| GitHub Actions workflow | 9 — Hardening |
| `lib/safety.ts` crisis keyword list | 6 — Vent flow |

The `.env.example` file already documents the phase-4 variables
(`APP_JWT_*`, `GOOGLE_OAUTH_*`, `SPRING_DATASOURCE_*`) with an explicit
"unused in phase 1" note, so the deferral is visible to anyone deploying.

---

## 6. Verification actually run

Every command below was run on this machine, in this session. Output is real and
trimmed only for length.

### `cd backend && mvnw.cmd verify` - **PASSED**

```
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 4.091 s
       -- in com.headheartfrees.common.web.GlobalExceptionHandlerTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.400 s
       -- in com.headheartfrees.common.web.HealthControllerTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] Building jar: D:\headheartfrees\backend\target\headheartfrees-backend-0.1.0-SNAPSHOT.jar
[INFO] BUILD SUCCESS
```

7 tests, all passing. Compiled clean under `-Xlint:all -Werror`, including the
three new classes and the new test.

The five `GlobalExceptionHandlerTest` cases: a rejected `@Valid` body returns 400
with one `fieldErrors` entry per invalid field; a valid body returns 204 (a
control, so the first test cannot pass for the wrong reason); an unmapped public
path returns 404 with `fieldErrors` **absent** rather than empty; a wrong-method
request returns 405; and an unauthenticated request returns 401 in the same
shape - that last one going through `SecurityErrorHandler`, not the advice.

Non-fatal stderr noise, unchanged and none of it a failure: the generated
security password, two springdoc "enabled by default" warnings, and Mockito and
JVM dynamic-agent warnings from running on JDK 22.

### `cd frontend && npm ci && npm run build` - **PASSED**

```
added 329 packages, and audited 330 packages in 3m
2 vulnerabilities (1 moderate, 1 high)
```

```
   ▲ Next.js 15.5.25
 ✓ Compiled successfully in 2.8s
   Linting and checking validity of types ...
 ✓ Generating static pages (4/4)

Route (app)                                 Size  First Load JS
┌ ○ /                                      123 B         103 kB
└ ○ /_not-found                            993 B         104 kB
+ First Load JS shared by all             103 kB
```

Because `next.config.ts` sets `ignoreBuildErrors: false` and
`ignoreDuringBuilds: false`, this also proves TypeScript strict and ESLint are
clean. The 2 advisories are transitive `postcss` issues reached through `next`
itself; the only offered fix is `next@16`, a major bump, not taken. See section 9.

### `docker compose config` - **PASSED**

Exit 0, all `${VAR:-default}` fallbacks resolved with no `.env` present.

### `docker compose build` - **PASSED**

The first attempt failed, for an environment reason rather than a repo one:

```
failed to connect to the docker API at npipe:////./pipe/dockerDesktopLinuxEngine;
check if the path is correct and if the daemon is running
```

Docker Desktop was installed but not running (`com.docker.service` stopped,
start type Manual). This is also why the earlier `docker compose config` had
passed while nothing else Docker-related had been attempted - `config` is purely
client-side and never contacts the daemon. Docker Desktop was started, the
daemon came up (server 29.2.1), and the build was re-run:

```
#34 [backend extract 4/4] RUN java -Djarmode=tools -jar app.jar extract --layers --destination extracted
#34 DONE 1.3s
...
#40 naming to docker.io/library/headheartfrees-backend:latest done
 Image headheartfrees-frontend Built
 Image headheartfrees-backend Built
===BUILD_EXIT:0===
```

**No Dockerfile changes were needed.** Both multi-stage builds worked as
written, including the two steps flagged as unproven in the previous revision of
this log: the backend's `java -Djarmode=tools ... extract --layers` and the
frontend's standalone-output copy.

### `docker compose up -d` - **PASSED**

```
 Container headheartfrees-db-1 Started
 Container headheartfrees-db-1 Healthy
 Container headheartfrees-backend-1 Started
 Container headheartfrees-backend-1 Healthy
 Container headheartfrees-frontend-1 Started
```

```
headheartfrees-backend-1   running   Up 13 seconds (healthy)
headheartfrees-db-1        running   Up 19 seconds (healthy)
headheartfrees-frontend-1  running   Up 7 seconds (healthy)
```

All three containers reached `healthy`. The `depends_on: service_healthy`
ordering worked: Postgres first, then the backend, then the frontend.

### `curl http://localhost:8080/api/v1/health` - **PASSED**

```
{"status":"UP","version":"0.1.0-SNAPSHOT","time":"2026-09-04T04:16:29.384363813Z"}
--- HTTP 200 ---
```

The `@project.version@` Maven filtering works inside the image - `version` is the
real artifact version, not the literal placeholder.

### `curl -I http://localhost:3000/` - **PASSED**

```
HTTP/1.1 200 OK
x-nextjs-cache: HIT
x-nextjs-prerender: 1
Content-Type: text/html; charset=utf-8
Content-Length: 5320
```

### Error shape confirmed over real HTTP, not just MockMvc

Run against the live container while the stack was up:

```
GET /api/v1/vent/does-not-exist
{"timestamp":"2026-09-04T04:16:31.454423189Z","status":404,"code":"NOT_FOUND",
 "message":"No endpoint matches this request.","path":"/api/v1/vent/does-not-exist"}
--- HTTP 404 ---

GET /api/v1/auth/me
{"timestamp":"2026-09-04T04:16:31.520502747Z","status":401,"code":"UNAUTHORIZED",
 "message":"Authentication is required to access this resource.","path":"/api/v1/auth/me"}
--- HTTP 401 ---

POST /api/v1/health
{"timestamp":"2026-09-04T04:16:31.576648651Z","status":405,"code":"METHOD_NOT_ALLOWED",
 "message":"This endpoint does not support POST requests.","path":"/api/v1/health"}
--- HTTP 405 ---
```

All three match PROJECT_BRIEF.md section 6 exactly, and `fieldErrors` is absent
rather than `null` or `{}` on non-validation errors - which is what makes the
field genuinely optional for the frontend's `ApiErrorBody` type.

### `docker compose down` - **PASSED**

Exit 0. All three containers stopped and removed, network removed. The
`headheartfrees_postgres-data` volume was **kept** - `down` was run without
`-v`, as specified.

### Checks still NOT run

- **Frontend-to-backend call in a browser.** The frontend renders the API base
  URL but no page calls the API yet, so CORS has never been exercised by a real
  cross-origin request. First real test is phase 5.
- **`npm run lint` / `npm run typecheck` as standalone commands** - covered
  transitively by the build.
- **Responsive checks** at 320/375/414/768/1024/1440/1920 - nothing designed yet.
- **Accessibility / Lighthouse audit** - phase 9.
- **A 500-path test.** `handleUnexpected` is written and reviewed but no test
  forces an unexpected exception, so the catch-all is the one branch of the
  error handler never executed. See section 9.
- **Brief section 10's "no vent text near the server"** as an active automated
  check. Still holds trivially - no vent code exists - and is still unguarded.

---

## 7. How to run it right now

### With Docker

```bash
docker compose up --build
```

No `.env` needed - every value has a local default. Then:
- Frontend: http://localhost:3000
- Health: http://localhost:8080/api/v1/health
- Swagger UI: http://localhost:8080/swagger-ui.html

**This path is now verified end to end** (section 6): both images build, all
three containers go healthy, and both endpoints answer 200. Docker Desktop must
actually be running first - the daemon is not started on demand on this machine,
and `docker compose config` will happily pass without it.

Stop with `docker compose down`. Add `-v` if you also want to drop the
`headheartfrees_postgres-data` volume, which nothing writes to until phase 4.

### Without Docker

```bash
# Terminal 1 - backend on :8080
cd backend
./mvnw spring-boot:run        # mvnw.cmd on Windows

# Terminal 2 - frontend on :3000
cd frontend
npm install                   # or npm ci
npm run dev
```

Postgres is not required for either app in phase 1 - nothing connects to a
database yet.

On startup the backend logs a generated security password. That is
`spring-boot-starter-security`'s default in-memory user. It is unusable (no form
login, no HTTP basic in the filter chain) and disappears in phase 4.

---

## 8. Environment variables introduced

All are declared in `.env.example` with placeholder values. No secrets are
committed. `.gitignore` excludes `.env` and `.env.*` while allowing
`.env.example`.

| Variable | Purpose | Used yet? |
|---|---|---|
| `POSTGRES_DB` | Database name for the compose `db` service | Yes — by Postgres |
| `POSTGRES_USER` | Postgres superuser | Yes — by Postgres |
| `POSTGRES_PASSWORD` | Postgres password | Yes — by Postgres |
| `POSTGRES_PORT` | Host port mapped to Postgres 5432 | Yes — compose |
| `SPRING_DATASOURCE_URL` | JDBC URL for the backend | **No — phase 4.** Passed into the container, read by nothing (no driver on the classpath). |
| `SPRING_DATASOURCE_USERNAME` | Datasource user | **No — phase 4** |
| `SPRING_DATASOURCE_PASSWORD` | Datasource password | **No — phase 4** |
| `BACKEND_PORT` | Backend HTTP port; `server.port` default 8080 | Yes |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile; `local` enables debug logging | Yes |
| `APP_CORS_ALLOWED_ORIGINS` | Comma-separated exact origins allowed to call the API with credentials. Bound to `CorsProperties`. Not a wildcard, because the refresh cookie needs `allowCredentials`. | Yes |
| `APP_JWT_SECRET` | Base64 HS256 signing key, ≥256 bits. `.env.example` gives the `openssl rand -base64 48` recipe. | **No — phase 4** |
| `APP_JWT_ACCESS_TOKEN_TTL` | Access token lifetime (`PT15M`) | **No — phase 4** |
| `APP_JWT_REFRESH_TOKEN_TTL` | Refresh token lifetime (`P30D`) | **No — phase 4** |
| `GOOGLE_OAUTH_CLIENT_ID` | Google OAuth2 client id | **No — phase 4** |
| `GOOGLE_OAUTH_CLIENT_SECRET` | Google OAuth2 client secret | **No — phase 4** |
| `FRONTEND_PORT` | Host port mapped to the frontend's 3000 | Yes |
| `NEXT_PUBLIC_API_BASE_URL` | API origin the browser calls. **Inlined into the JS bundle at build time** — a `docker compose build` is required to change it; restarting with a new value does nothing. Documented in three places (`.env.example`, both Dockerfile and compose comments, README). | Yes |

Also set inside images, not via `.env`: `JAVA_OPTS`
(`-XX:MaxRAMPercentage=75.0`), `NODE_ENV`, `NEXT_TELEMETRY_DISABLED`, `PORT`,
`HOSTNAME`.

---

## 9. Broken, incomplete, or stubbed

Ordered roughly by how likely each is to bite. Three items from the previous
revision are gone: the unproven Docker path (now verified, section 6), the
missing error handler (now implemented), and the stray `.vscode/settings.json`
(deleted - with a caveat, item 11 below).

1. **`postcss` advisories via `next`** (1 high, 1 moderate) - the only open
   security finding. Reached transitively through `next`'s own dependency on
   `postcss <=8.5.22`: XSS via unescaped `</style>`, plus three
   `sourceMappingURL` arbitrary-file-read issues. `npm audit fix --force`
   installs `next@16.3.4`, a major bump. Not taken. Build-time-only path, but
   real. Revisit at phase 9, or sooner if Next ships a 15.x patch.
2. **The 500 catch-all is untested.** `handleUnexpected` never runs in the test
   suite, so the one handler whose entire job is to leak nothing is also the one
   with no test proving it does not. Add a test that throws from a controller
   once there is a service layer to throw from.
3. **401 vs 403 for anonymous requests.** `GlobalExceptionHandler` maps
   `AccessDeniedException` to 403 unconditionally. Spring's own convention is to
   answer 401 when the caller is anonymous and 403 only when authenticated. This
   is invisible today (no method security exists) but will matter in phase 4 once
   `@PreAuthorize` appears on the admin endpoints.
4. **All six domain packages are empty** apart from `package-info.java`. Correct
   for phase 1, but `auth`, `vent`, `feedback` and `donation` compile to nothing.
5. **`components/ui`, `components/layout`, `components/sections` are `.gitkeep`
   only.** Phase 2.
6. **The palette is a placeholder.** Five neutral `--hhf-*` values. The real
   palette has not been supplied, so contrast has never been checked against
   anything final.
7. **`page.tsx` is intentionally unstyled** and prints the API base URL. It must
   not survive phase 3.
8. **No automated guard on rule 2.1.** The prohibition on vent text reaching the
   server is enforced by documentation only - package Javadoc and comments in
   `api.ts`. Nothing fails a build if someone adds a `content` field. Worth a
   test or an ArchUnit rule when `vent` gets real code in phase 6.
9. **`-Werror` may become an obstacle.** Any future dependency that triggers a
   deprecation warning breaks the build rather than warning. It held fine through
   this pass, including the three new classes.
10. **`spring-boot-starter-security` default user** logs a password each boot.
    Cosmetic; resolves itself in phase 4.
11. **`.vscode/settings.json` comes back on its own.** It was deleted during this
    pass and the VS Code Java extension regenerated it, identically, within
    minutes of the next Java edit. Deleting it again is not a durable fix. It is
    harmless - `.gitignore` excludes `.vscode/`, so it never reaches the repo,
    which was the whole reason for removing it. Left deleted at handover; expect
    it to reappear locally and ignore it when it does.

---

## 10. What Phase 2 needs from Phase 1, and open questions

**Unavailable.**

This section cannot be written honestly from the working tree. The prior
session's context is gone, so I have no record of what was flagged for you, what
you were asked, or what remained open when work stopped. Reconstructing it would
mean inventing your side of a conversation I do not have.

The one hard dependency that is objectively stated in the brief itself, rather
than inferred: **§8 says the palette "will be supplied"**, and phase 2 is the
design system. `globals.css` is built as a one-file swap for exactly this.
Everything else — questions, trade-offs you may have already ruled on — needs to
come from you.

---

## Verification summary

| Check | Result |
|---|---|
| `mvnw.cmd verify` | **PASS** - 7/7 tests, BUILD SUCCESS, clean under `-Werror` |
| `npm ci` | **PASS** - 329 packages, 2 advisories (see section 9) |
| `npm run build` | **PASS** - 4/4 static pages; TS strict + ESLint clean transitively |
| `docker compose config` | **PASS** - exit 0, all defaults resolved |
| `docker compose build` | **PASS** - both images, no Dockerfile changes needed |
| `docker compose up -d` | **PASS** - all three containers healthy |
| `curl /api/v1/health` | **PASS** - HTTP 200, correct version string |
| `curl -I localhost:3000/` | **PASS** - HTTP 200 |
| Error shape over HTTP | **PASS** - 404 / 401 / 405 all match section 6 |
| `docker compose down` | **PASS** - exit 0, clean teardown, volume kept |
| Browser CORS round-trip | **NOT RUN** - no page calls the API yet |
| 500 catch-all path | **NOT RUN** - see section 9 item 2 |
| Responsive / a11y checks | **NOT RUN** - nothing designed yet |
