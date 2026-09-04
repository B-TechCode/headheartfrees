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

---
---

# Phase 2 — Design system

**Completed:** 2026-09-04
**Scope:** PROJECT_BRIEF.md §9 row 2 — CSS variables, Tailwind theme, typography
scale, buttons, inputs, cards, navbar, footer, logo component, grain overlay.

---

## 1. Phase completed and current status

**Done.** Every item in the Phase 2 scope exists and builds. All four required
checks pass on the final code (§6).

Two things that are complete but worth reading before Phase 3:

1. **The supplied palette had a bug and its contrast figures were optimistic.**
   Eight `@theme inline` entries were self-referential, and five of the six
   documented contrast ratios overstated the real value — `clay` in particular
   is 4.14:1, not the claimed 4.8:1, which means it **fails AA as a text
   colour**. Both are fixed; details in §3 and §6.
2. **Nothing has been looked at in a browser.** Layout is built mobile-first and
   reasoned through, but no width from 320 to 1920 has been visually confirmed
   in this session, by agreement. See §9.

---

## 2. Files

```
frontend/src/
├── app/
│   ├── icon.svg                     Compact exhale mark as favicon. Ink on light chrome,
│   │                                bone on dark, via prefers-color-scheme inside the SVG.
│   ├── layout.tsx                   MODIFIED. next/font wiring, skip link, Navbar + Footer,
│   │                                flex column so the footer sits at the bottom on short pages.
│   ├── page.tsx                     MODIFIED. Still a Phase 3 placeholder, now on real tokens.
│   └── design-system/page.tsx       NEW. Internal preview of every primitive in every state.
│                                    Client component so chip selection and loading are real.
│                                    DELETE IN PHASE 9.
├── components/
│   ├── ui/
│   │   ├── styles.ts                Shared focus ring, disabled treatment, tap-target constant.
│   │   ├── Logo.tsx                 Logo + Wordmark. Opacity ramp baked in, never per-arc colour.
│   │   ├── Button.tsx               4 variants x default/hover/active/focus/disabled/loading.
│   │   ├── Input.tsx                Single-line field.
│   │   ├── Textarea.tsx             Multi-line. field-sizing:content, looser leading. Phase 6 base.
│   │   ├── Label.tsx                Marks fields optional rather than required.
│   │   ├── FormField.tsx            Label + control + hint + error. Render prop hands the
│   │   │                            control its id and aria-describedby so the wiring
│   │   │                            cannot be forgotten.
│   │   ├── Card.tsx                 raised / sunk / outline + Header, Title, Body. No shadows.
│   │   ├── Chip.tsx                 Selectable, aria-pressed, optional icon slot (empty until Phase 6).
│   │   ├── Badge.tsx                6 tones, text-on-wash rather than saturated fill.
│   │   └── Spinner.tsx              aria-hidden; meaning carried by text or aria-busy.
│   └── layout/
│       ├── Navbar.tsx               Home/Vent/About + Sign in. Hand-rolled mobile disclosure.
│       └── Footer.tsx               Crisis strip, brand blurb, Navigate, Support, donation line.
├── lib/cn.ts                        Class joiner. No clsx, no tailwind-merge.
└── styles/globals.css               REWRITTEN. All tokens, type scale, radii, grain, base layer.
```

**Deleted:** `palette-tokens.css` and `logo-exhale.svg` from the repo root, both
consumed as instructed; `components/ui/.gitkeep` and `components/layout/.gitkeep`,
now that those directories hold real files. `components/sections/.gitkeep`
remains — that directory is still empty until Phase 3.

---

## 3. Key decisions and deviations

Unlike the Phase 1 section, this is a record of decisions actually made in this
session, not inference from code.

### The supplied palette: two defects found and fixed

**D7 — eight self-referential `@theme inline` entries.** `palette-tokens.css`
declared `--color-surface: var(--color-surface)` and the same pattern for
`surface-raised`, `surface-sunk`, `surface-inverse`, `success`, `warning`,
`danger` and `info`. Tailwind emits `@theme` output where `@import "tailwindcss"`
sits, and the hand-written `:root` block lands later, so the valid declaration
won the cascade and the file worked — by accident. Moving the `:root` block above
the import would have silently invalidated every surface and status colour.

Fixed as approved: those eight now point at the raw layer (`var(--hhf-bone)`),
which is order-independent. Every token name and both layers are otherwise
exactly as supplied. Verified: zero self-referential declarations in the emitted
CSS (§6).

The same trap was avoided twice more while building — the `next/font` CSS
variables are named `--font-fraunces` / `--font-karla` rather than
`--font-sans` / `--font-display`, because the latter are the Tailwind theme keys
and would have produced the identical bug.

**D8 — the palette's contrast figures were overstated.** Measured against
`#F7F4EF` with the WCAG relative-luminance formula:

| token | claimed | measured | |
|---|---|---|---|
| ink | 14.9:1 | 15.34:1 | understated, harmless |
| ink-soft | 9.1:1 | 9.30:1 | close |
| ink-faint | 4.9:1 | **4.50:1** | overstated; lands a hair under AA |
| clay | 4.8:1 | **4.14:1** | overstated; **fails AA as text** |
| clay-deep | 6.9:1 | 6.03:1 | overstated but still passes |
| white on clay | 4.6:1 | 4.54:1 | close, almost no margin |

The raw hex values were **not** changed — they were supplied and are not mine to
alter. What changed is how they are used, plus the comment block in
`globals.css`, which now carries measured figures since the palette file is
deleted and that comment is the only surviving record.

### Consequent component decisions

| # | Decision | Why |
|---|---|---|
| P1 | `clay` is never a text colour. Links and accent text use `clay-deep`. | 4.14:1 fails AA for text. `clay` is fine for fills, rules and the logo, which need 3:1. |
| P2 | Control borders are `ink-faint` (4.50:1), not `rule-strong` (1.75:1). | SC 1.4.11 needs 3:1 for anything identifying a component. An input whose only boundary is a 1.75:1 hairline is not identifiable. |
| P3 | `ink-faint` dropped as text on `surface`; small text is now `ink-soft`. | 4.4997:1 is under 4.5. It stays as placeholder text on `surface-raised` (4.82:1) and as a control border. |
| P4 | Badge washes reduced from 8% to 4%. | At 8% the warning badge was 4.39:1. At 4% it is 4.60:1 and every other tone improves too. |
| P5 | Crisis helpline underline is `ink-faint`, not `rule-strong`. | A 1.88:1 underline is enough for a decorative divider and not enough to advertise that a phone number is tappable. |
| P6 | No `themeColor` in viewport metadata. | It would tint mobile browser chrome to match the page, but `<meta name="theme-color">` cannot read a CSS variable, so it means a second hardcoded copy of the background that drifts on the next palette change. Left off; a decision for you (§10). |
| P7 | Grain sits at `z-index: 0` with a `.app-layer` class raising every region above it. | The brief requires it not sit above interactive elements. `pointer-events: none` alone would stop clicks but still tint controls. |
| P8 | Type scale is fluid `clamp()` at deliberately off-grid sizes (17px, 15px, 13px). | §8 rules out a default vertical rhythm; a 14/16/18 scale is the giveaway. |
| P9 | Radii are 2/3/5/9/14px. | §8 names uniform default radii as a toolkit tell. |
| P10 | Exactly one shadow token, used only by the mobile menu. | As agreed: borders and surface tokens carry hierarchy. |

### Approved in the plan and applied as agreed

shadcn/ui not installed and the ten primitives hand-rolled (Radix deferred to
Phase 5 for the avatar dropdown only); favicon uses a `prefers-color-scheme`
swap rather than hardcoded clay; Chip ships icon-less with the slot present;
Fraunces + Karla via `next/font/google`.

---

## 4. Versions

No dependencies were added. The primitives are hand-rolled and `cn.ts` replaces
what `clsx` + `tailwind-merge` would have done, so `package.json` is unchanged
from Phase 1 — still Next 15.5.25, React 19.2.8, Tailwind 4.3.3, TypeScript
5.9.3.

Fonts are fetched by `next/font/google` at build time and self-hosted from
`/_next/static/media/`. Verified: five `.woff2` files on disk after build, and no
reference to `fonts.googleapis.com` or `fonts.gstatic.com` anywhere in the build
output.

- **Fraunces** — display and headings. Weights 400/600/700. Chosen for its true
  optical-size axis and slight wonk, which reads as made-by-hand.
- **Karla** — body and UI. Weights 400/500/600. A grotesque with genuinely odd
  letterforms that stays quiet at 15–16px, avoiding the Inter voice §8 rules out.

**Hermetic-build trade-off, for revisiting at Phase 9:** `next/font/google`
needs network access during `docker compose build`. `npm ci` already imposes
that, so it is not a new dependency, but it is a second one. During one local
build Next logged `Retrying 1/3...` twice before succeeding — the fetch is not
always first-time reliable. Switching to `next/font/local` with committed
`.woff2` files would make builds fully offline-capable at the cost of binaries in
the repo.

---

## 5. Deferred

| Item | Phase |
|---|---|
| Radix (avatar dropdown only) | 5 |
| Mood line icons for the Chip `icon` slot | 6 |
| Home and About pages, asymmetric hero | 3 |
| `lib/safety.ts` crisis keyword list | 6 |
| Deleting `/design-system` | 9 |
| Real pages behind the footer's Support links — Crisis Resources, Community Guidelines, Privacy Policy, Contact Us | unassigned; see §9 |
| Lighthouse and automated a11y audit | 9 |

---

## 6. Verification actually run

All four required commands, on the final code, in this session.

### `npm run typecheck` — **PASSED**

```
> tsc --noEmit
EXIT=0
```

### `npm run lint` — **PASSED**

```
> eslint .
EXIT=0
```

### `npm run build` — **PASSED**

```
 ✓ Compiled successfully in 10.6s
   Linting and checking validity of types ...
 ✓ Generating static pages (6/6)

Route (app)                                 Size  First Load JS
┌ ○ /                                      162 B         106 kB
├ ○ /_not-found                            993 B         104 kB
├ ○ /design-system                       4.51 kB         107 kB
└ ○ /icon.svg                                0 B            0 B
+ First Load JS shared by all             103 kB
```

### `docker compose build frontend` — **PASSED**

```
 Image headheartfrees-frontend Built
EXIT=0
```

Rebuilt after the last source edit — an earlier passing build was discarded
because it predated a change.

### Emitted-CSS checks

Run against the built stylesheet, because "it compiles" would not have caught
the palette bug:

```
self-referential declarations found: 0

--color-surface:var(--hhf-bone);
--color-surface-raised:var(--hhf-bone-raised);
--color-surface-sunk:var(--hhf-bone-sunk);
--color-surface-inverse:var(--hhf-ink);
--color-success:var(--hhf-success);

--font-sans:var(--font-karla),ui-sans-serif,system-ui,-apple-system,sans-serif;
--font-display:var(--font-fraunces),ui-serif,Georgia,"Times New Roman",serif;

feTurbulence occurrences: 1
```

Each token now appears exactly once and resolves to the raw palette.

### Contrast audit — **30 combinations, 0 failures**

Computed from the hex values with the WCAG 2.x relative-luminance formula, over
the combinations actually present in the code. The first run found **6
failures**; all six are fixed and the re-run is clean. Selected results:

```
ink body on raised (cards, inputs, crisis strip)        16.45:1    4.5  PASS
ink-soft on page (footer heads, captions, overlines)     9.30:1    4.5  PASS
ink-faint placeholder on raised                          4.82:1    4.5  PASS
clay-deep link on page                                   6.03:1    4.5  PASS
white on clay (primary)                                  4.54:1    4.5  PASS
warning on warning/4 wash                                4.60:1    4.5  PASS
focus ring on page                                       6.27:1    3.0  PASS
control border ink-faint on page                         4.50:1    3.0  PASS
selected chip border clay on raised                      4.44:1    3.0  PASS
--------------------------------------------------------------------------
failures: 0
```

Two exclusions, both deliberate and stated rather than quietly dropped:

- `disabled-text` on `disabled-surface` is 2.49:1. WCAG exempts disabled
  controls, and a disabled control must read as unavailable.
- `rule` (1.32:1) and `rule-strong` (1.75:1) are decorative separators. They are
  no longer used as the boundary of any interactive control.

### Render smoke test

A production server was started locally on :3100 and the routes fetched:

```
/                HTTP 200   26894 bytes
/design-system   HTTP 200   60882 bytes
/icon.svg        HTTP 200   image/svg+xml
```

Rendered HTML confirms `tel:9152987821`, `tel:18602662345`, `sms:741741` and the
helpline heading are present, and that the string `Donate` and `Feedback` appear
nowhere in the navigation. Server stopped afterwards; port 3100 confirmed clear.

### Hex audit

```
hex in .ts/.tsx:  NONE
files containing hex:  src/app/icon.svg, src/styles/globals.css
```

`globals.css` is the token source. `icon.svg` is the one approved exception — a
favicon cannot inherit `currentColor` from a page. Nothing else names a colour.

### NOT verified — stated plainly

- **No browser. No visual confirmation at any width.** 320 / 375 / 414 / 768 /
  1024 / 1440 / 1920 have not been looked at. Layout is mobile-first with fluid
  type and reasoned breakpoints, but "it renders correctly at 414px" is a claim
  this session cannot make. By agreement — you are checking these in DevTools.
- **iOS on-screen keyboard against the textarea.** Needs a real device; open.
- **Keyboard traversal was not executed.** The markup is built for it — skip
  link, `aria-expanded`/`aria-controls` on the menu toggle, Escape-to-close with
  focus return, `focus-visible` rings everywhere, and a global `:focus-visible`
  backstop — but no one has actually tabbed through it.
- **Screen-reader testing.** None. `FormField`'s live region, `aria-pressed` on
  Chip and `aria-busy` on Button are correct by construction, not by observation.
- **The grain overlay has never been seen.** It is present in the CSS and
  correct in structure; whether 0.055 opacity reads as paper rather than noise
  is a judgement that needs eyes.
- **Font rendering.** Fraunces and Karla are correctly wired and self-hosted;
  how the pairing actually looks is unverified.
- **Lighthouse / axe.** Not run. Phase 9.

---

## 7. How to run it

Unchanged from Phase 1. The preview route is at **`/design-system`**, linked
from the placeholder home page, and it renders every primitive in every state —
the fastest way to review this phase.

```bash
cd frontend && npm run dev      # then open http://localhost:3000/design-system
```

---

## 8. Environment variables

None added. Phase 2 introduces no configuration.

---

## 9. Broken, incomplete, or stubbed

1. **No visual verification at any breakpoint.** The largest gap in this phase.
   Highest-risk spots, in order: the navbar between 768px and 1024px where the
   wordmark, three links and Sign in first share a row; the footer's
   `[2fr_1fr_1fr]` grid at its `lg` threshold; and the crisis strip's wrap
   behaviour at 320px, where three helplines must stack without cramping.
2. **Footer Support links point at routes that do not exist.** `/crisis-resources`,
   `/community-guidelines`, `/privacy` and `/contact` all 404 today, as do
   `/vent`, `/about`, `/login` and `/support`. Expected mid-build, but the footer
   currently offers a person in distress a "Crisis Resources" link that goes
   nowhere. The helpline numbers themselves work, which is what matters most —
   but this should not reach anything public-facing. Not assigned to a phase by
   the brief; flagging it as needing one.
3. **`prefers-reduced-motion` stops the spinner.** The global rule sets
   `animation-duration: 0.01ms !important`, so `Spinner` renders as a static arc
   for those users. Deliberate — the meaning is carried by `aria-busy` and the
   visually-hidden label, not the movement — but it means a reduced-motion user
   sees a button that dims and stops responding with no moving indicator.
4. **`Textarea` relies on `field-sizing: content`,** which is not in every
   browser in the Tailwind v4 floor. `min-h-36` and `rows` are the fallback, so
   it degrades to a fixed box rather than breaking.
5. **No dark mode.** The palette defines one light scheme. The favicon handles
   both chromes; the page does not. Not in the brief.
6. **`components/sections/` is still an empty `.gitkeep`.** Phase 3.
7. **The `postcss` advisories from Phase 1 are unchanged** — 1 high, 1 moderate,
   transitive through `next`, fix requires `next@16`. Untouched this phase.
8. **`/design-system` ships in the production bundle.** 4.51 kB, not linked from
   the navbar, but publicly reachable. Scheduled for deletion in Phase 9.
9. **`.vscode/settings.json` still regenerates.** Unchanged from the Phase 1
   note; harmless, gitignored.

---

## 10. What Phase 3 needs from Phase 2, and open questions

**Available for Phase 3:** the full token set (colour, type, radii, one shadow),
ten primitives, Navbar and Footer already mounted in the root layout, the grain
layer, and `.app-layer` for any new full-width region.

Phase 3 should not need to define a single colour or font size. If it does, the
scale has a gap worth fixing in `globals.css` rather than patching at the call
site.

**Open questions for you:**

1. **`themeColor`** (P6). Do you want mobile browser chrome tinted to the paper
   background? It costs one hardcoded hex in `layout.tsx` that cannot reference
   a token. I left it out; say the word and it is one line.
2. **The four Support routes** (§9 item 2). Which phase owns Crisis Resources,
   Community Guidelines, Privacy Policy and Contact Us? Crisis Resources in
   particular reads as something that should not stay a dead link for six more
   phases.
3. **`clay` at 4.14:1.** I have kept the supplied hex and worked around it by
   never using clay as text. The alternative is darkening `--hhf-clay` slightly
   so it clears 4.5:1 and becomes usable for text. That is a change to a palette
   you supplied, so I did not make it. Worth a decision before Phase 3 commits
   to a visual language around it.
4. **Wordmark at 320px.** "HeadHeartFreeS" in Fraunces at `text-h4` beside the
   mark is the widest fixed element in the navbar. If it crowds the menu button
   on the narrowest screens, the fix is either a smaller wordmark or mark-only
   below `sm` — I would rather you look at it than guess.

---

## Phase 2 verification summary

| Check | Result |
|---|---|
| `npm run typecheck` | **PASS** — exit 0 |
| `npm run lint` | **PASS** — exit 0 |
| `npm run build` | **PASS** — 6/6 static pages |
| `docker compose build frontend` | **PASS** — image built |
| Self-referential CSS tokens | **PASS** — 0 found, was 8 |
| Contrast audit (30 combinations) | **PASS** — 0 failures, was 6 |
| Hex outside globals.css | **PASS** — only the approved favicon |
| Fonts self-hosted | **PASS** — no gstatic/googleapis reference |
| Render smoke test | **PASS** — /, /design-system, /icon.svg all 200 |
| Navbar excludes Donate/Feedback | **PASS** — absent from rendered HTML |
| Visual check at 7 breakpoints | **NOT RUN** — no browser this session |
| Keyboard traversal | **NOT RUN** — built for it, not executed |
| Screen-reader testing | **NOT RUN** |
| iOS keyboard vs textarea | **NOT RUN** — needs a real device |
| Lighthouse / axe | **NOT RUN** — Phase 9 |

---

# Phase 2a — Post-review fixes

**Completed:** 2026-09-04
**Scope:** three fixes raised after you reviewed the rendered Phase 2 output.

---

## 1. Status

**All three done.** One of them did not need the fix you specified, and that is
covered honestly below rather than reported as a silent pass.

---

## 2. Files

```
frontend/src/
├── app/not-found.tsx            NEW. Branded 404 inside the root layout.
└── components/ui/Spinner.tsx    MODIFIED. Track opacity 0.25 -> 0.35, stroke 2.5 -> 3.
```

No other file changed. No dependencies added.

---

## 3. What was done

### Fix 1 — branded 404 page

`app/not-found.tsx` at the app root, so Next renders it inside `app/layout.tsx`
and it inherits the navbar, footer, fonts and grain automatically. That is what
the default 404 was missing — it rendered outside the layout entirely, which is
why an unmatched URL landed on unstyled black.

- Heading in Fraunces (`font-display text-h1`), body in Karla.
- Copy is in the product's voice: *"That page isn't here."* then *"The link may
  be old, or the address slightly off. Either way it is not something you did
  wrong."* followed by a line noting nothing written is affected, because it
  never leaves the browser. Explicitly not "This page could not be found."
- Two links: **Back to home** (clay) and **Go to the vent** (secondary).
- Tokens only. The hex audit still reports zero hex values in any `.ts`/`.tsx`.

One implementation note: the two actions are anchors carrying the Button token
classes rather than `<Button>` elements. A `<button>` nested inside a `<Link>`
is invalid HTML, so the classes are applied to the anchor directly. If a third
place needs this, that is the point to give `Button` an `asChild`-style escape
hatch rather than copy the classes a third time.

### Fix 2 — Spinner contrast: **the stated condition did not fire**

You asked me to check the clay Spinner as a graphic against the 3:1 threshold
and darken it to `clay-deep` if it failed. Measured:

```
arc, clay on surface              4.14:1   PASS
arc, clay-deep on surface         6.03:1   PASS
arc, ink on surface              15.34:1   PASS
```

**Clay passes 3:1**, so no darkening was required. Worth adding: the
design-system page had already been rendering `clay-deep` since the Phase 2
contrast pass, so the swatch you reviewed was the 6.03:1 variant — darkening
further was not available as a fix anyway.

The faintness you saw is real, but it is the **track**, not the arc:

```
track @0.25 opacity, clay         1.37:1
track @0.25 opacity, clay-deep    1.45:1
track @0.25 opacity, ink          1.69:1
```

Faint in every colour, ink included, so changing the accent would have moved it
from 1.37 to 1.45 and fixed nothing.

The track is the unfilled part of the indicator — decorative under SC 1.4.11 in
the same way the empty portion of a progress bar is, since the arc is the part
required to understand the control. So this is a legibility change, not a
conformance one: **track opacity 0.25 → 0.35, stroke 2.5 → 3** on both track and
arc. That reads better at the 16px default without flattening the track/arc
difference that creates the sense of rotation. The reasoning and the measured
figures are recorded in the component's doc comment so this is not re-litigated
later.

If it still reads light to you on screen, the next lever is the stroke weight or
the default size, not the colour — say so and I will take it further.

### Fix 3 — dead Support routes now land on the branded 404

Confirmed by request against a production server, checking status code plus
three markers in the returned HTML:

```
/crisis-resources       HTTP 404 | branded-404-copy:1 | navbar:1 | footer-helplines:1
/community-guidelines   HTTP 404 | branded-404-copy:1 | navbar:1 | footer-helplines:1
/privacy                HTTP 404 | branded-404-copy:1 | navbar:1 | footer-helplines:1
/contact                HTTP 404 | branded-404-copy:1 | navbar:1 | footer-helplines:1
```

Every other unrouted path behaves the same:

```
/vent /about /login /support /definitely-not-a-page   all HTTP 404, all branded
```

The status code is still a correct 404 — the page is branded, not faked into a
200.

**The part that matters most:** `footer-helplines:1` confirms `tel:9152987821`
is present in the 404 response body. Someone who clicks "Crisis Resources" and
hits a dead link still lands on a page carrying the actual helpline numbers.
That does not make the dead link acceptable, but it does lower the cost of it
considerably while those four pages remain unbuilt.

---

## 4. Verification actually run

| Check | Result |
|---|---|
| `npm run typecheck` | **PASS** — exit 0 |
| `npm run lint` | **PASS** — exit 0 |
| `npm run build` | **PASS** — 6/6 static pages, exit 0 |
| `docker compose build frontend` | **PASS** — image built, exit 0 |
| Hex in `.ts`/`.tsx` | **PASS** — none |
| 4 Support routes → branded 404 | **PASS** — 404 + navbar + footer + helplines on all four |
| 5 further dead routes → branded 404 | **PASS** |
| Spinner arc ≥ 3:1 as a graphic | **PASS** — 4.14:1 clay, 6.03:1 clay-deep |

Build output:

```
Route (app)                                 Size  First Load JS
┌ ○ /                                      165 B         106 kB
├ ○ /_not-found                            123 B         103 kB
├ ○ /design-system                       4.51 kB         107 kB
└ ○ /icon.svg                                0 B            0 B
```

### NOT verified

- **The 404 page has not been looked at in a browser.** Its structure and tokens
  are confirmed in the rendered HTML, but the same visual gap from Phase 2
  applies: no width has been eyeballed. The layout is a single narrow column, so
  it is the lowest-risk page in the build, but that is reasoning, not observation.
- **The Spinner change has not been seen rendered.** Whether 0.35 and a 3px
  stroke actually resolve what you noticed is unconfirmed — the numbers moved in
  the right direction, your eyes are the test.
- Keyboard, screen-reader, Lighthouse: unchanged from Phase 2, still not run.

---

## 5. Still open

Unchanged from the Phase 2 log except where noted:

1. **The four Support pages are still unbuilt** and still unassigned to a phase.
   The 404 they hit is now branded and carries the helplines, which was the
   urgent part, but "Crisis Resources" remains a link that does not go to crisis
   resources. Question 2 in the Phase 2 §10 still stands.
2. **`clay` at 4.14:1** — still the supplied hex, still worked around by never
   using clay as text. Phase 2 §10 question 3 still stands, and Fix 2 above is a
   second data point: clay is fine for graphics, and the ceiling it imposes keeps
   coming up.
3. **`themeColor`** — still deliberately absent. Phase 2 §10 question 1.
4. Visual verification at the seven breakpoints, keyboard traversal, screen
   reader, iOS keyboard, Lighthouse — all still outstanding.

