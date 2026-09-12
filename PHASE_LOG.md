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

---
---

# Phase 3 — Static pages

**Completed:** 2026-09-04
**Scope:** Home and About from the brief's phase table, plus the four Support
routes you reassigned to this phase so that a dead "Crisis Resources" link does
not survive to anything public.

---

## 1. Status

**Done.** Six pages, all returning 200, all with unique titles and descriptions
and exactly one `h1` each.

The headline outcome is not a page, though. It is that **a crisis number that
had been live in the footer for two phases was wrong**, and re-verifying it was
the only reason we found out. Details in §3.

---

## 2. Files

```
frontend/src/
├── app/
│   ├── page.tsx                          REWRITTEN. Placeholder deleted in full,
│   │                                     including the API base URL debug line.
│   ├── about/page.tsx                    NEW
│   ├── crisis-resources/page.tsx         NEW
│   ├── community-guidelines/page.tsx     NEW
│   ├── privacy/page.tsx                  NEW
│   └── contact/page.tsx                  NEW
├── components/
│   ├── sections/                         (was a lone .gitkeep, now removed)
│   │   ├── Hero.tsx                      Asymmetric 7/5 split, oversized cropped mark.
│   │   ├── HowItWorks.tsx                Three steps: arrive, write, release.
│   │   ├── Promises.tsx                  Replaces the fabricated testimonial block.
│   │   ├── ClosingAction.tsx             One line, one link.
│   │   ├── PageHeader.tsx                Shared h1 + standfirst for secondary pages.
│   │   ├── DonationSection.tsx           Quiet donation block for /about.
│   │   └── HelplineList.tsx              Helplines as page content, built to be scanned.
│   ├── ui/
│   │   ├── Callout.tsx                   NEW primitive. note / important / caution.
│   │   └── Prose.tsx                     NEW primitive. Long-form typography, 68ch measure.
│   └── layout/Footer.tsx                 MODIFIED. Reads from lib/helplines.ts, shows hours.
├── lib/
│   ├── helplines.ts                      NEW. Single source of truth for crisis numbers.
│   └── contact.ts                        NEW. The one placeholder address to edit.
└── styles/globals.css                    MODIFIED. Grain 0.055 -> 0.085, freq 0.9 -> 0.65.

PROJECT_BRIEF.md                          MODIFIED. §7 crisis-detection number corrected.
```

---

## 3. The Vandrevala correction

**PROJECT_BRIEF.md §7 specified Vandrevala Foundation as 1860-2662-345. That
number is not on any current Vandrevala page.** Their contact page,
free-counselling page and FAQ all list only **+91 9999 666 555**. The sources
still carrying the old number are news posts and directories from 2019–2021.

It had shipped in the footer in Phase 2 and was scheduled to reach
`lib/safety.ts` in Phase 6. Corrected in all four places you asked for: the new
crisis page, the footer, and both mentions in the brief.

I could not establish that 1860-2662-345 was formally discontinued — only that
no operator source carries it. Per the standing rule that a wrong crisis number
is worse than a missing one, absence of confirmation was treated as
disqualifying.

### Every number, and where it was verified

| Service | Number | Hours | Verified against |
|---|---|---|---|
| Tele-MANAS | 14416 | 24/7 | telemanas.mohfw.gov.in, JIPMER, MoHFW notices |
| Vandrevala Foundation | 9999666555 | 24/7 | vandrevalafoundation.com contact page |
| AASRA | 022-27546669 | 24 hours | aasra.info contact page |
| SNEHA | 044-24640050 | 24 hours | snehaindia.org |
| iCall | 9152987821 | **Mon–Sat 10am–8pm** | icallhelpline.org |
| Crisis Text Line | HOME to 741741 | 24/7 | crisistextline.org |

Two numbers were **rejected** rather than included:

- **1860-2662-345** (Vandrevala) — absent from the operator's own site.
- **9820466726** (AASRA) — widely republished as the AASRA helpline, but on
  aasra.info it is listed as a named individual's contact, not the helpline.
  The helpline is the landline above.

SNEHA was checked as you asked and their own site confirmed 044-24640050, so it
is included. AASRA and SNEHA are both labelled as landlines on the page.

### Hours are now shown everywhere

The footer strip was headed *"these lines are open"*, which the list did not
keep — iCall does not run overnight. The heading is now *"If you would rather
talk to someone."* with the subline *"Hours are listed because not every line
runs all night,"* and every entry carries its hours in both the footer and on
`/crisis-resources`. Ordering is by availability: Tele-MANAS, Vandrevala, then
iCall with its hours stated.

`lib/helplines.ts` now holds the one list that the footer, the crisis page and
the Phase 6 panel all read from. Each entry records `verifiedFrom` for the next
audit. This is the structural fix for the drift, not just a corrected string.

---

## 4. No fabricated social proof

None of the Figma material was reproduced: no counts, no country figures, no
percentage, no testimonials, no attributed names. Not as placeholder, not
commented out, not as a TODO.

Audited against the rendered HTML of all six pages:

```
12,400: 0   12400: 0   47 countries: 0   98%: 0   feel lighter: 0
Trusted by: 0   Mumbai,: 0   Toronto: 0   Bangalore: 0   Berlin: 0
testimonial: 1   star: 1
```

Both non-zero hits are innocent and were checked individually. "testimonial"
occurs once, on `/about`, in the sentence saying there are none: *"You will not
find visitor counts, testimonials or satisfaction figures on this site, because
there are none that would be true."* "star" is the substring in *"Start
writing"* on the home hero.

The `Promises` section replaces the testimonial block. Its five claims are all
properties of the software as built and checkable against the code today.

---

## 5. Copy decisions

- Home `h1` is **"Somewhere to put it down."** as approved.
- No em-dash asides in body copy, no adjective triads, no landing-page verbs.
- `/about` does not claim clinical outcomes. It says the site is not therapy,
  not a substitute for it, and explicitly that *"We are not going to tell you it
  will make you feel better. It might do nothing."*
- `/crisis-resources` leads with numbers, not prose. The "not an emergency
  service" statement sits **below** the numbers deliberately: it is necessary
  and honest, but making someone in distress read a liability notice before
  reaching a phone number is the wrong order.
- `/community-guidelines` opens by stating that venting is unmoderated because
  nothing is stored, which sets the scope and doubles as the clearest statement
  of the privacy model.
- `/privacy` carries the legal-review notice at the top, not the bottom, and
  describes Phase 4 as a future change rather than pre-writing a policy for
  accounts nobody can create.

---

## 6. Contact page — the one line to edit

`/contact` is complete apart from the address. **Edit this line and nothing
else:**

```
frontend/src/lib/contact.ts  line 20

export const CONTACT_EMAIL = "REPLACE-ME@example.invalid";
```

Then set the line below it to `false`:

```
frontend/src/lib/contact.ts  line 24

export const CONTACT_EMAIL_IS_PLACEHOLDER = true;   ->   false
```

The page, the `mailto:` and the displayed address all read from those two
constants, and `/community-guidelines` and `/privacy` link to the same mailto.
While the flag is `true`, `/contact` shows a visible notice saying no address is
connected yet and the address renders struck through, so nobody emails a void
and waits. The placeholder is `@example.invalid` rather than a plausible domain
on purpose.

---

## 7. The two side checks

### Grain — was marginal, now raised

Measured rather than eyeballed. `fractalNoise` writes noise into alpha as well
as RGB, so effective per-pixel alpha is about half the layer opacity:

```
opacity   darkest px   lightest px   luminance spread
0.055     #F0EDE8      #F7F4EF        7 levels
0.085     #EDEAE5      #F7F4F0       10 levels
0.100     #EBE8E3      #F7F5F0       13 levels
```

Seven levels is above the ~2-level JND, so it was not invisible — but
`baseFrequency: 0.9` produced ~1.1 CSS px features, which a 2x display averages
away, so on the screens most people use it was close to nothing.

Raised to **0.085** and **baseFrequency 0.65** as instructed, giving ~10 levels
at ~1.5 CSS px. Confirmed in the emitted CSS. Still under the ~0.12 where it
starts reading as screen noise rather than paper. **Not visually confirmed** —
you are looking at it next.

### "Support this space" — a paste artefact, as you suspected

Confirmed present exactly once on every route tested, including 404s:

```
/ 200:1   /design-system 200:1   /crisis-resources 404:1
/privacy 404:1   /definitely-missing 404:1
```

One occurrence in `Footer.tsx`, no conditional, footer mounted in the root
layout. No change made.

---

## 8. Verification actually run

| Check | Result |
|---|---|
| `npm run typecheck` | **PASS** — exit 0 |
| `npm run lint` | **PASS** — exit 0 |
| `npm run build` | **PASS** — 11/11 static pages, exit 0 |
| `docker compose build frontend` | **PASS** — image built, exit 0 |
| Six routes return 200 | **PASS** |
| Exactly one `h1` per page | **PASS** — 1 on every page |
| Heading order, no skips | **PASS** |
| Unique title + description per page | **PASS** |
| Fabricated social proof | **PASS** — zero |
| Hex outside globals.css / icon.svg | **PASS** — none in `.ts`/`.tsx` |
| Stale Vandrevala number in output | **PASS** — zero occurrences |
| Crisis page: all 6 numbers + 112/102 | **PASS** |
| Footer helplines on every page | **PASS** |
| Grain values in emitted CSS | **PASS** — `opacity:.085`, `baseFrequency='0.65'` |

Build output:

```
Route (app)                                 Size  First Load JS
┌ ○ /                                      164 B         106 kB
├ ○ /_not-found                            123 B         103 kB
├ ○ /about                                 164 B         106 kB
├ ○ /community-guidelines                  173 B         106 kB
├ ○ /contact                               173 B         106 kB
├ ○ /crisis-resources                      173 B         106 kB
├ ○ /design-system                       4.51 kB         107 kB
├ ○ /icon.svg                                0 B            0 B
└ ○ /privacy                               173 B         106 kB
```

### Footer links — corrected claim

You asked me to confirm no footer link 404s. **The four you assigned to this
phase are fixed. Three other links in the footer and navbar still 404**, and I
am not going to report that as a clean pass:

```
/                     200      /crisis-resources     200
/about                200      /community-guidelines 200
/contact              200      /privacy              200
/vent                 404  <-- Phase 6
/support              404  <-- Phase 8
/login                404  <-- Phase 5 (navbar)
```

`/vent` is linked from the navbar, the footer Navigate column, the home hero,
the closing action, `/about` and `/crisis-resources`. It is the most-linked
route on the site and it is the product's whole point, so it is currently the
most conspicuous dead link in the build. All three land on the branded 404 from
Phase 2a, which carries the helplines.

### NOT verified

- **No browser. No visual check at any width.** 320 / 375 / 414 / 768 / 1024 /
  1440 / 1920 all unconfirmed, as in Phase 2. The riskiest new layout is the
  home hero's 7/5 grid at the `lg` boundary, where the oversized mark is
  positioned with `translate-x-10` inside `overflow-hidden` — that is the most
  likely place to see an unintended scrollbar or a clipped mark.
- **The grain change is unseen.** Numbers moved as intended; whether 0.085 reads
  as paper is yours to judge.
- **Keyboard traversal, screen reader, Lighthouse, axe** — none run, unchanged
  from Phase 2.
- **`tel:` and `sms:` links have not been tested on a real handset.** They are
  correctly formed, and `sms:741741` deliberately carries no prefilled body
  because the syntax differs across iOS and Android.
- **Nobody has called these numbers.** Verification means each one is published
  by its operator on its own current site, not that a call was placed.
- **The helpline hours have not been re-checked against public holidays** or any
  seasonal variation the operators may run.

---

## 9. Broken, incomplete, or stubbed

1. **`/vent`, `/support` and `/login` 404**, and `/vent` is linked from six
   places. Phases 6, 8 and 5.
2. **`/contact` has no working address.** Placeholder, visibly marked. §6 above
   has the exact line.
3. **`/design-system` is now unreachable by clicking.** The Phase 2 placeholder
   home page held its only link and that page is gone. Still live at the URL,
   still slated for deletion in Phase 9.
4. **The privacy policy has not had legal review.** Stated at the top of the
   page itself.
5. **Helpline numbers need re-verification on a schedule.** `lib/helplines.ts`
   records `verifiedFrom` per entry and the crisis page shows the check date.
   Nothing enforces a re-check; this drifted once and will drift again.
6. **`postcss` advisories unchanged** — 1 high, 1 moderate, transitive through
   `next`, needs `next@16`.
7. **No dark mode.** Unchanged.
8. **`clay` at 4.14:1** still means the accent cannot be used for text. It came
   up again here: every accent link on these pages uses `clay-deep`.

---

## 10. Open questions

1. **`/vent` is the most-linked route on the site and it 404s.** Six links point
   at it. Worth considering whether Phase 6 moves up, or whether it gets a
   holding page sooner.
2. **Contact address** — needed before any deploy.
3. **Helpline re-verification cadence.** Monthly? Quarterly? A dated check in
   `lib/helplines.ts` is only as good as the habit of returning to it.
4. **`themeColor`** — still absent, unchanged from Phase 2.
5. **AASRA and SNEHA are landlines.** Both are labelled as such. If you would
   rather the page led with mobile-reachable numbers only, that is a content
   call I would rather you made.

---

# Phase 3a — Footer trim and scroll-behavior opt-in

**Completed:** 2026-09-04
**Scope:** three small changes after the Phase 3 review.

---

## 1. Status

**All three done.** One is worth a sentence of correction rather than a clean
tick: the `scroll-behavior` warning does not appear where I could observe it.
See §3.

---

## 2. Files

```
frontend/src/components/layout/Footer.tsx    Navigate column removed, layout rebalanced.
frontend/src/app/layout.tsx                  data-scroll-behavior="smooth" on <html>.
PROJECT_BRIEF.md                             §7 gains a Footer subsection ruling the column out.
```

---

## 3. What was done

### Change 1 — Navigate column removed and the footer rebalanced

`NAVIGATE_LINKS` and its `FooterColumn` are gone. The footer now carries the
crisis strip (untouched, in place), the brand blurb, the Support column, and the
"Support this space" line.

The layout needed more than deleting a column. The old grid was
`sm:grid-cols-2 lg:grid-cols-[2fr_1fr_1fr]`. Dropping to `[2fr_1fr]` would have
produced exactly the lopsidedness the change was meant to remove: at `max-w-6xl`
the 2fr column is about 700px while the brand block is capped at `max-w-sm`
(384px), leaving roughly 300px of dead space with nothing in it.

Replaced with a flex row anchored to both edges:

```
flex flex-col gap-10 md:flex-row md:justify-between md:gap-12
```

The gap between the two blocks is now structural rather than a leftover
fraction. The brand block gained `min-w-0 max-w-md`, up from `max-w-sm`, so it
carries a little more of the width now that it is one of two things rather than
one of three.

**The split waits until `md` (768px), not `sm` (640px).** At 640 the brand text
at `max-w-md` plus the Support column plus the gap exceeds the available width,
and the brand would be squeezed into a narrow measure. Below `md` the two blocks
stack, which cannot be lopsided.

Widths reasoned through, not seen:

| Width | Behaviour |
|---|---|
| 320 / 375 / 414 | Stacked. Brand, then Support. |
| 640 (sm) | Still stacked, deliberately. |
| 768 (md) | Side by side. ~720px usable, brand up to 448 + gap 48 + Support ~170 fits. |
| 1024 / 1440 / 1920 | Side by side inside `max-w-6xl`, anchored left and right. |

### Change 2 — the brief now rules the column out

§7's Navbar paragraph referred to "the footer's Navigate column", which would
have licensed a later phase to rebuild it. It now reads as a Navbar rule plus a
new **Footer** subsection that describes what the footer contains and states:

> **There is no Navigate column, and one must not be added.** Home, Vent and
> About are in the navbar; repeating them in the footer left a three-item column
> too thin to carry its own heading. Removed 2026-09-04. Donate and Feedback
> must not appear anywhere in the footer either.

The Donate/Feedback prohibition is preserved and now covers the whole footer
rather than one named column.

### Change 3 — `data-scroll-behavior="smooth"`, with a caveat

Added to `<html>` in `layout.tsx` and confirmed in the rendered output:

```
<html lang="en" data-scroll-behavior="smooth" class="__variable_d4d11e __variable_08b68a">
```

**I could not reproduce the warning.** `next build` emits no line matching
`scroll` or `warn`, before or after the change — I captured the full build log
both times specifically to check. That is consistent with the linked Next.js
page: `missing-data-scroll-behavior` is logged by the client at runtime, so it
surfaces in the browser console and under `next dev`, not in a production build.

So the attribute is correct and is now in place, and the reasoning is sound —
`globals.css` does set `scroll-behavior: smooth` on `html`, which is exactly the
condition the warning describes. But I am recording that I verified the
attribute is present rather than that I watched the warning disappear, because
only the first of those is something I observed.

---

## 4. Verification actually run

| Check | Result |
|---|---|
| `npm run typecheck` | **PASS** — exit 0 |
| `npm run lint` | **PASS** — exit 0 |
| `npm run build` | **PASS** — 11/11 static pages, exit 0 |
| `docker compose build frontend` | **PASS** — image built, exit 0 |
| Six routes still 200 | **PASS** |
| `Navigate` heading absent from footer | **PASS** — 0 occurrences on every page checked |
| All 4 Support links present | **PASS** — 4/4 on all six pages |
| "Support this space" still once per page | **PASS** |
| `data-scroll-behavior` on `<html>` | **PASS** — present in rendered HTML |
| Crisis strip unmoved | **PASS** — untouched, still first in the footer |

### NOT verified

- **The rebalanced footer has not been seen at any width.** The `md` breakpoint
  choice and the two-block anchoring are reasoned from the numbers above, not
  observed. This is the one change in this batch where "looks right" is the
  actual acceptance criterion and I cannot supply it.
- **The scroll-behavior warning was never observed disappearing**, for the
  reason in §3.
- Keyboard, screen reader, Lighthouse: unchanged, still not run.

---

## 5. Still open

Unchanged from Phase 3 §10:

1. **`/vent` still 404s** and is now linked from five places rather than six,
   since the footer Navigate column that carried one of them is gone. Still the
   most conspicuous dead link in the build. Phase 6.
2. **`/support` and `/login` still 404.** Phases 8 and 5.
3. **Contact address is still a placeholder** — `lib/contact.ts` line 20, and
   the flag on line 24.
4. **Helpline re-verification cadence** still undecided.
5. **Privacy policy still needs legal review.**


---

# Phase 4 — Vent release path, persisted

Verification completed 2026-09-05.

## 1. Status

The vent release path is implemented and now actually verified end to end:
`POST /api/v1/vent/release` writes a row, `GET /api/v1/vent/stats` counts them,
and `/vent` and `/vent/released` render against the running backend. The
counter table holds a mood and a timestamp and nothing else.

Two things in this section are not "the feature worked". One is an environment
note about Docker, and one is a build defect found *by* the verification —
the phase-4 integration tests had never executed. Both are below.

---

## 2. The Docker outage, and what it blocked

Between the phase-4 implementation work and this verification, the Docker
daemon on the development machine stopped serving requests. Testcontainers hung
at `DockerClientFactory` rather than failing fast, so the backend suite did not
error — it sat. A Windows restart fixed the daemon (`docker run --rm
hello-world` succeeds, server 29.7.2).

**What it blocked, for the duration:** everything in this phase that touches a
real database. That is `mvnw verify` (every `@SpringBootTest` extends
`PostgresTestBase`, so the whole suite needs a container, not just the vent
tests), the Flyway-against-real-Postgres check, and `docker compose up`. During
the outage the phase-4 code was written and reviewed but not once run against
Postgres.

### Environment note — `DOCKER_HOST`, not a code change

Independent of the outage, Testcontainers probes the wrong pipe on this machine:

```
> docker context ls
NAME              DOCKER ENDPOINT
default           npipe:////./pipe/docker_engine
desktop-linux *   npipe:////./pipe/dockerDesktopLinuxEngine
```

The active context is `desktop-linux`, but `DOCKER_HOST` is unset, so
Testcontainers falls back to the `default` endpoint — `//./pipe/docker_engine` —
which nothing is listening on. It waits rather than failing.

Set it for the run only:

```bash
DOCKER_HOST='npipe:////./pipe/dockerDesktopLinuxEngine' ./mvnw verify
```

**This is deliberately not committed anywhere.** It is a property of one
developer's Docker Desktop install, not of the project. Putting it in
`pom.xml`, a `.mvn/jvm.config`, or a `~/.testcontainers.properties` checked into
the repo would hard-code one machine's named pipe into a cross-platform build
and break every Linux and macOS contributor. Recorded here so the next person
who sees the hang has the two-minute answer instead of the two-hour one.

---

## 3. Defect found by the verification: the integration tests never ran

The first clean `mvnw verify` reported:

```
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Thirteen tests, all green. But the suite contains sixteen more, and the three
`*IT` classes were not among the thirteen:

| Class | In the 13? |
|---|---|
| `GlobalExceptionHandlerTest` (5) | yes |
| `HealthControllerTest` (2) | yes |
| `MoodTest` (2) | yes |
| `VentRuleArchitectureTest` (4) | yes |
| `VentControllerIT` (9) | **no** |
| `VentErrorHandlingIT` (1) | **no** |
| `VentSchemaIT` (3) | **no** |

Cause: surefire's default includes are `Test*.java`, `*Test.java`,
`*Tests.java`, `*TestCase.java` — none of which match `*IT.java` — and the
`build` section of `pom.xml` had only `spring-boot-maven-plugin` and
`maven-compiler-plugin`. There was no failsafe plugin, so nothing picked the
integration tests up. Maven compiled all three classes and then ran none of
them, and reported BUILD SUCCESS.

This is the worst shape a gap can take. `VentSchemaIT` is the test that asserts
`vent_events` has no content column — the rule 2.1 guard on the database side.
It was written, it was correct, it was passing when invoked by hand, and it was
protecting nothing, because the build never called it. A guard that does not run
still *reads* as a guard in the log.

**Fix — the only code change in this verification pass:** added
`maven-failsafe-plugin` to `pom.xml`, bound to `integration-test` and `verify`,
with a comment recording why. No test or application code was touched. The
three IT classes passed on the first invocation both before the pom change (run
directly, to confirm the code was sound and only the wiring was missing) and
after it.

`mvnw verify` now runs 26 tests:

```
[INFO] Tests run: 13, ...  (surefire)
[INFO] --- failsafe:3.5.6:integration-test ---
[INFO] Tests run: 13, ...  (failsafe: 9 + 1 + 3)
[INFO] BUILD SUCCESS
```

---

## 4. Verification actually run

| Check | Result |
|---|---|
| `mvnw verify` with `DOCKER_HOST` set | **PASS** — 26 tests, 0 failures, 0 errors, BUILD SUCCESS |
| Testcontainers starts a real Postgres | **PASS** — postgres:16-alpine, reported version 16.15 |
| Flyway applies `V1__create_vent_events.sql` | **PASS** — "Successfully applied 1 migration to schema public, now at version v1" |
| Flyway is idempotent on a second context | **PASS** — "Schema public is up to date. No migration necessary." |
| `vent_events` columns, via `VentSchemaIT` | **PASS** — `containsExactly("id", "mood", "created_at")` |
| No forbidden column present | **PASS** — 17-name blocklist, none found |
| CHECK constraint rejects a direct bad insert | **PASS** — `vent_events_mood_allowed` fires |
| `docker compose up -d --build` | **PASS** — db, backend, frontend all reach healthy |
| `GET /vent` | **PASS** — 200 |
| `GET /vent/released` | **PASS** — 200 |
| `POST /api/v1/vent/release` `{"mood":"HEAVY"}` | **PASS** — **204** |
| `POST /api/v1/vent/release` `{}` | **PASS** — 204, mood optional |
| Release increments the stats count | **PASS** — `{"totalReleases":0}` → two POSTs → `{"totalReleases":2}` |
| Live schema in the compose database | **PASS** — 3 columns, verified by `psql`, not only by the test |
| `docker compose down` | **PASS** — all three containers and the network removed |

The POST returns **204, not 200**. That is correct and deliberate — there is no
response body because there is nothing to return; the release is not
acknowledged with content. Recorded because the phase-4 request asked for 200
and a future reader should not "fix" the 204.

Live schema as it exists in the running stack, not in a test fixture:

```
   Column   |           Type           | Nullable |            Default
------------+--------------------------+----------+--------------------------------
 id         | bigint                   | not null | nextval('vent_events_id_seq')
 mood       | text                     |          |
 created_at | timestamp with time zone | not null | now()
Check constraints:
    "vent_events_mood_allowed" CHECK (mood IS NULL OR (mood = ANY (...)))
```

And the two rows the smoke test produced — the whole of what a release retains:

```
 id | mood  |          created_at
----+-------+-------------------------------
  1 | HEAVY | 2026-09-05 15:46:52.36052+00
  2 |       | 2026-09-05 15:46:52.472579+00
```

No content, no user, no IP. Row 2 is a release with no mood chip, which is why
that column is empty rather than absent.

---

## 5. Crisis keyword list — measured, then fixed, then re-measured

`frontend/src/lib/safety.ts`, 53 patterns, exercised through the real module
(the harness strips the TypeScript annotations rather than copying the list, so
what is measured is what ships).

The list was measured, found to have a defect serious enough to fix in this
pass, fixed, and re-measured. Both sets of numbers are below, because the
before/after is the point.

### Final numbers

| | Before | After |
|---|---|---|
| Corpus size | 42 should-match / 49 should-not | 58 / 58 |
| False negatives | 0 | **0** |
| False positives | 5 | **5** |
| Precision | 0.894 | **0.921** |
| Held-out recall | 0/24 | **2/24** |
| Patterns | 38 | **53** |

### The defect: bare nouns handed a crisis panel to the bereaved

The measured false positive that mattered was `i lost my dad to suicide`, firing
on the bare noun `suicide`. Not a matcher limitation — the list contradicting
its own stated rule. The module header says it matches "first-person statements
of intent, never distress vocabulary", and gives `kill myself` in / `kill me`
out as the worked example. A bare noun is exactly distress vocabulary. Bare
`overdose` had the identical problem: `my brother died of an overdose`.

The consequence was that someone bereaved by suicide — writing about the worst
thing that has happened to them, with no intent of their own — got a crisis
panel telling them to call a helpline. That is the specific credibility-spending
failure the header is written to prevent, sitting inside the list the header
describes.

**This is closed.** Both bare nouns are gone, replaced by first-person
constructions. All four bereavement probes now pass clean: `i lost my dad to
suicide`, `my brother died of an overdose`, `we lost her to an overdose last
year`, `my cousin took his own life`.

### What the fix took, which was more than it first appeared

Three changes were specified: rejoin `my self`, add inflections, drop the bare
nouns. Applied literally they made precision **worse** — 10 false positives
against the enlarged corpus, up from 5 — and introduced a new false negative.
The first attempt at the first-person forms used the preposition alone:

| New false positive | Fired on |
|---|---|
| `a talk about suicide prevention at work` | `about suicide` |
| `a training about suicide awareness` | `about suicide` |
| `an article about overdosing on caffeine` | `about overdosing` |
| `that was a good jumping off point` | `jumping off` |
| `we used the audit as a jumping off point` | `jumping off` |

And `i thought about an overdose` — a real disclosure — stopped matching, because
dropping the bare noun removed the only pattern that had covered it.

One cause in all of them: **a preposition is not a first-person marker.** The
carrier verb has to be inside the pattern. So `about suicide` became `thinking
about suicide` / `thought about suicide` / `thinking of suicide` / `considering
suicide`; `about overdosing` became the four `thinking|thought about
overdosing|an overdose` forms, which also recovers the false negative; and
`jumping off` was made to require an article.

Bare `jump off` was tightened the same way, which was not asked for. It had the
identical collision in the rarer `jump off point`, and leaving one half loose
while tightening the other only invites the next person to re-loosen both.
Recorded as a deliberate scope extension rather than buried.

`normalise()` now rejoins `my self` into `myself` after the whitespace collapse.
One rule covers `kill`, `hurt`, `cut`, `harm` and `hang`, which is why the list
did not need a spaced variant of every entry.

### The five remaining false positives

| Phrase | Fires on |
|---|---|
| `i dont want to kill myself i just want it to stop` | `kill myself` |
| `i used to want to die but not anymore` | `want to die` |
| `i am not suicidal, i just needed to say this somewhere` | `suicidal` |
| `i would never hurt myself` | `hurt myself` |
| `im hanging myself out to dry here` | `hanging myself` |

**The first four are negation and past tense, and they are not keyword work.**
Substring matching has no notion of "not", "never", or "used to", and it cannot
acquire one by adding, removing or reshaping entries — every phrase in that
column is a phrase the list *should* contain. Closing them needs actual natural
language processing: negation scope detection, at minimum, and realistically
tense and modality with it. That is a different kind of component with a
different failure mode, and it is not proposed here. They are also the benign
direction of error: the person writing them is not having a good day either, and
a panel of helplines is not a harmful thing to put in front of them. Left as
they are, deliberately, and they should not be "fixed" by weakening the four
patterns that catch them.

**The fifth was accepted knowingly.** Bare `hang myself` already collided with
the same idiom, so `hanging myself` adds the more common inflection of a problem
that predates it rather than a new one. The asymmetry runs opposite to the
bereavement case: missing `ive been thinking about hanging myself` is far worse
than showing a panel to someone using a workplace idiom, who is not a vulnerable
person being mishandled. Approved as built.

### Recall: 100% against the list's own vocabulary, 2/24 against held-out paraphrase

The main corpus scores 58/58, but that number is close to meaningless on its own
— the phrases were built from the list, so it largely measures that the regexes
compile. The honest number is a second corpus of 24 first-person intent phrases
written *without* consulting the list:

```
held-out intent phrases: 24
matched : 2
MISSED  : 22   (held-out recall 0.083)
```

Up from 0/24. The two now caught are `i want to kill my self` (the normalise
rule) and `thinking of jumping off the bridge` (the inflection). Still missed:
`im going to kms`, `i want to unalive myself`, `i want to stop existing`, `ive
been counting my pills`, `i wrote a note for my mum`, `i have a plan and a
date`, `life isnt worth living`, `im going to jump`.

**This gap was deliberately not chased further.** The module header already says
the list will miss things and prioritises precision, so 2/24 is not a
contradiction of the design — it is the size of the thing the header describes
in words, recorded as a number. Closing it properly needs real NLP, the same
conclusion as the negation cases and for the same reason: the remaining misses
are semantic, not lexical. The structural coverage — the helpline strip in the
footer of every route including 404s — is doing the great majority of the work
here. The keyword panel is a nudge on a narrow set of phrasings, and should
never be described, internally or in copy, as detection.

One held-out miss is worth naming because it is mechanical rather than semantic:
`ive been hurting my self again`. The `my self` rule works and rejoins it
correctly, but `hurting myself` is not a listed gerund, so it falls into the
same inflection gap that `hanging myself` and `jumping off` were just added to
close. Not fixed, since chasing inflections one probe at a time is how a
precision-first list quietly becomes a recall-first one.

---

## 6. Verified vs still unverified

**Now verified, which was not before the outage lifted:**

- The suite runs green against a real Postgres, and it is now the whole suite.
- The migration applies to an empty schema, and is a no-op on an initialised one.
- `vent_events` is exactly `id, mood, created_at` — asserted in `VentSchemaIT`
  and confirmed independently in the running compose database.
- The mood CHECK constraint rejects a direct out-of-enum insert.
- The full three-container stack builds, starts, self-reports healthy, serves
  both vent routes, records releases, counts them, and shuts down clean.

**Still unverified:**

- **`/vent` has not been used by a person.** Every check above is `curl` and
  `psql`. Nobody has typed into the textarea, picked a mood chip, pressed the
  button, or seen `/vent/released`. Whether the release *feels* like a release
  is the actual acceptance criterion for this phase and it remains unmet.
- **`CrisisPanel` has never been seen rendering.** Its trigger is unit-tested
  through `containsCrisisLanguage`; the panel's own appearance, wording in
  place, and whether it reads as help rather than alarm are unobserved.
- **The Hinglish patterns still have no native-speaker review.** Unchanged from
  when they were written — the §5 fixes were English-only and did not touch
  them. The bare-`suicide` question that was on this review's agenda is now
  closed, but it has been replaced by a narrower one: whether the first-person
  carrier-verb rule that fixed the English nouns transfers to Hinglish, or
  whether transliteration variance defeats it.
- **Rate limiting was verified at the unit level only** — `VentControllerIT`
  covers the 31st-request 429, but no concurrent or multi-client test has run,
  and the limiter is still in-memory and per-instance (phase 9).
- **No load, soak, or restart-persistence testing.** The compose database uses a
  named volume but nothing has confirmed the count survives a `down`/`up`
  without `-v`.
- Keyboard, screen reader, Lighthouse: still not run, unchanged since phase 2.

---

## 7. Still open

Carried from phase 3a §5, with movement:

1. **`/vent` no longer 404s.** Closed by this phase.
2. **`/support` and `/login` still 404.** Phases 8 and 5.
3. **Contact address is still a placeholder** — `lib/contact.ts` line 20, flag
   on line 24.
4. **Helpline re-verification cadence** still undecided.
5. **Privacy policy still needs legal review** — and now has a concrete subject,
   since the site began writing rows to a database.
6. **Safety list — partly closed in this pass.** Of the four items originally
   raised in §5, three are done: the bare `suicide`/`overdose` nouns are gone
   and the bereavement false positive with them, the `my self` spacing gap is
   closed in `normalise()`, and the inflection coverage is now consistent
   across `hang`, `jump` and the `…ing` forms. Precision went 0.894 → 0.921
   with zero false negatives.

   **Still open, and still needing a native speaker:** the Hinglish set. It was
   not touched in this pass, and §5's fixes have now added a second question
   for that same review — whether the first-person carrier-verb rule that
   replaced the bare English nouns has a Hinglish equivalent, or whether
   transliteration variance makes it unworkable there.

7. **New — the four remaining false positives are not keyword work.**
   `i dont want to kill myself…`, `i used to want to die but not anymore`,
   `i am not suicidal…`, `i would never hurt myself`. Every one is negation or
   past tense, and every phrase they fire on is a phrase the list *should*
   contain, so no amount of adding, removing or reshaping entries reaches them.
   Closing them needs real NLP — negation scope detection at minimum,
   realistically tense and modality too — which is a different component with a
   different failure mode and is not proposed. Recorded so that a future pass
   does not mistake them for a list problem and weaken the four patterns that
   catch them. The same conclusion applies to the 22 remaining held-out recall
   misses, for the same reason: what is left is semantic, not lexical.

---

# Phase 5 — Auth backend

Completed 2026-09-05. Backend only; the login and register pages are phase 6.

## 1. Status

Registration, password login, JWT access tokens, refresh-token rotation with
reuse detection, logout, `/me`, Google OAuth2, and 5/min rate limiting are all
implemented and tested against real Postgres.

The suite went from **26 tests to 106** (40 surefire, 66 failsafe), all passing.

The constraint that governed the phase held: `/api/v1/vent/**` is still public,
`vent_events` is unchanged, and there is now a test that fails the build if
either stops being true.

---

## 2. The rule this phase was written around

Three mechanisms, because a comment saying "do not authenticate the vent
endpoints" is not one.

**`/api/v1/vent/**` stayed in `PUBLIC_PATHS`.** That line was not edited. The
diff on `SecurityConfig` adds paths; it removes none.

**`ModuleBoundaryArchitectureTest`** — new, three rules. No class in `..vent..`
may depend on `..auth..`; no class in `..auth..` may depend on `..vent..`; and a
narrower third rule naming the specific mistake section 4 warns about, a
cross-module JPA association. Both directions are separate tests so a failure
says which way the boundary was crossed. The existing
`VentRuleArchitectureTest` guards what a vent type may *contain*; this guards
what the two modules may *see*.

The reasoning is in the class comment and is worth repeating: rule 2.2 does not
die by someone deciding to require login. It dies by degrees — an import, then
an `Optional<UserId>` parameter "just for analytics", then a nullable column —
and at no point does anyone believe they have changed the product. The first
step is an import, so that is what fails.

**`VentRemainsAnonymousIT`** — seven tests, none of which authenticate. Release
and stats with no credentials; release with no body and no credentials; both
with no account existing in the system at all; and release with a *forged*
bearer token, which must be ignored rather than rejected, so a stale token in
someone's browser cannot 401 them out of the one part of the site that is
unconditionally available.

Two of its tests go further than reachability. One signs in fully and then
releases, and asserts that `vent_events` still has exactly `id, mood,
created_at` — the runtime consequence of the schema rule, not just the schema.
The other asserts the refresh cookie's `Path` is `/api/v1/auth`, so a browser
never attaches it to a vent request. A cookie scoped to `/` would be an ambient
identifier on every anonymous release.

---

## 3. Three bugs found by the tests, not by review

All three passed code review in my own head and failed the moment they ran.
Recorded because each is a plausible thing to reintroduce.

### 3.1 CITEXT was silently case-*sensitive*

`emailIsCaseInsensitive` failed with a 500. The column is `CITEXT` and the
unique index is genuinely case-insensitive, but a Spring Data derived query
`findByEmail(String)` is not: the JDBC driver sends the parameter typed as
VARCHAR, and Postgres resolves `citext = varchar` by coercing the citext side
*down* to `text` and comparing case-sensitively.

The failure mode is nasty. The lookup says the address is free, the application
inserts, and the unique index — which *is* case-insensitive — rejects it. A
duplicate registration in different case became a 500 instead of the shared
201, which is both a crash and an account-existence oracle.

Fixed with a native query casting the parameter: `WHERE email = CAST(:email AS
citext)`. That restores citext's own equality operator and still uses the unique
index. `findByEmailIgnoreCase` would have been the other obvious fix and is
worse — it generates `upper(email)` and cannot use the index at all.

`@Column(columnDefinition = "citext")` was separately required on the entity, or
Hibernate's `ddl-auto: validate` refuses to start the whole application,
expecting `varchar(255)`.

### 3.2 Reuse detection revoked the family and then rolled it back

The one that would have mattered in production. `RefreshTokenService.rotate`
detects a spent token, revokes every token in the family, and throws
`RefreshTokenReuseException`. Both of those are inside `@Transactional`, so the
throw marked the transaction rollback-only and **undid the revocation**. The
caller got a clean 401 and the stolen family stayed live. Reuse detection was
decorative.

Fixed with `noRollbackFor = RefreshTokenReuseException.class` on both
`RefreshTokenService.rotate` and `DefaultAuthService.refresh` — both, because
with `REQUIRED` propagation they share one physical transaction and either
marking it is enough to lose the write.

`reuseRevokesTheFamily` fails if either annotation is removed. It is exactly the
sort of annotation that looks like noise during a tidy-up.

### 3.3 The Google-optional design failed at the first hurdle

The plan said `client-id: ${GOOGLE_CLIENT_ID:}` in `application.yml` would leave
the registration unconfigured. It does not. Spring Boot's
`OAuth2ClientProperties.validate()` runs over every *declared* registration and
throws `"Client id of registration 'google' must not be empty"`. So the empty
default guaranteed the precise startup failure the requirement existed to
prevent — no `GOOGLE_CLIENT_ID`, no application, password login down with it.

The registration has to be genuinely absent, not present and empty, and YAML
cannot express that. Added
`GoogleOAuth2EnvironmentPostProcessor`, which injects the
`spring.security.oauth2.client.registration.google.*` properties at startup only
when both `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` are non-blank. That also
keeps the friendly variable names rather than making an operator set
`SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID`.

`OAuth2AbsentConfigIT` had to change too, for the same reason: blanking the
properties with `@TestPropertySource` *declares* an empty registration and trips
the same validation. It now removes the injected property source with a context
initializer, which also keeps the test honest on a machine that has real Google
credentials in its environment.

---

## 4. Decisions

### Hashing — Argon2id, m=19456 t=2 p=1

OWASP's first recommended Argon2id configuration, spelled out in
`PasswordEncoderConfig` rather than inherited from
`defaultsForSpringSecurity_v5_8()`, because a library default changing
underneath stored password hashes is discovered late.

**Why not BCrypt-12: BCrypt silently truncates at 72 bytes.** The policy allows
64+ characters. 64 ASCII characters fit, so a quick reading says BCrypt is fine
— but 64 characters of Devanagari is roughly 192 bytes, and everything past the
72nd would be ignored, so two different long passphrases would hash
identically with nothing reporting a problem. `PasswordPolicyTest` has a test
named `multiByteIsNotTruncated` that exists solely to record this.

Costs ~40-60ms per hash, which is the point on the login path and is why the
suite is slower from this phase on.

### Password policy

Minimum 12. Maximum 200 — a bound on work per request, far above the 64 the
brief required. No composition rules at all; there is a test,
`noClassRequirements`, whose only job is to fail if someone adds one.

Blocklist of ~110 common passwords, case-insensitive. Most entries are shorter
than 12 and are already caught by the length rule — the ones that earn their
place are the long entries, because a 25-character passphrase looks strong and
is worthless if it is `correcthorsebatterystaple`. The test suite separates
those two cases rather than pretending the blocklist catches the short ones.

Rejects a password containing the email local part **or the display name**
(case-insensitive, minimum 3 characters — "Jo" would otherwise reject any
password containing "jo").

### Registration — option A, always 201

Duplicate and new registrations return the same status and byte-identical
bodies. `duplicateDoesNotLeak` compares the two responses directly.

The Argon2 hash runs on the duplicate path too, and is discarded. Skipping it
would make the duplicate path tens of milliseconds faster, which is readable
over a network and restores exactly the oracle the shared response removes. The
same trick is used on login: an unknown address still pays for a hash.

A duplicate registration also does not overwrite the existing password —
`duplicateDoesNotOverwriteCredentials` exists because "register over an existing
account" must not be an unauthenticated password reset.

**Phase 6 requirement, recorded so it is not lost:** the registration success
screen must read *"If that address is new, your account is ready. If you already
had one, sign in instead"* with a link to `/login`. The string is
`RegistrationResponse.SHARED_MESSAGE` and is already returned by the API. This
is the agreed mitigation for the one real cost of option A — a returning user
who has forgotten their account otherwise gets a success message and no way
forward. Without a mail transport there is no better answer.

### First admin

`APP_ADMIN_BOOTSTRAP_EMAILS`, comma-separated, applied by `AdminBootstrap` at
startup to accounts that **already exist**. Register → set the variable →
restart. It never creates an account, so the variable is not a credential and
leaking it grants nothing.

`UserAccount`'s private constructor takes no role parameter and hardcodes
`USER`, so no call site can create an admin — a stronger guarantee than
remembering not to pass one. `RegisterRequest` has no role field. The single
mutation that can produce an ADMIN is `assignRole`, called only from
`AdminBootstrap`.

Rejected: seeding an admin in a migration (a committed password hash), and any
promote endpoint (a self-service path to ADMIN).

### Rate limiting — the limiter did need generalising

`ClientIpRateLimiter` hard-coded 30/min. It now takes a `RateLimitPolicy` enum
(`RELEASE` 30/min, `AUTH` 5/min) and keys buckets on **policy + hashed IP**.

Keying on both is not incidental. A shared bucket would let failed sign-ins
consume someone's ability to vent, which rule 2.2 forbids. There is a test for
it — `ventingSurvivesAnExhaustedAuthBucket` exhausts the auth limit and then
releases successfully.

AUTH covers register, login and refresh. Not logout (someone who cannot sign out
because they signed in too often is the worse outcome) and not `/me`.

### 401 vs 403 — the phase 2a item, closed

`GlobalExceptionHandler.handleAccessDenied` now inspects the `SecurityContext`:
null, unauthenticated, or `AnonymousAuthenticationToken` → 401; otherwise 403.

Only the advice needed it. The filter chain was already correct —
`ExceptionTranslationFilter` routes anonymous callers to the entry point (401)
and authenticated ones to the access-denied handler (403). What reaches the
advice is an `AccessDeniedException` thrown inside the dispatcher, by
`@PreAuthorize`, where that routing has already happened.

### Tokens and the cookie

Access token: HS256, 15 minutes, response body only, carries `sub` (user id) and
`role` and deliberately no email or display name — those change, and a stale
name rendered in a UI is tedious to trace. `/me` is one query and always current.

Refresh token: 32 bytes of `SecureRandom`, base64url, **cookie only**, 30 days.
`HttpOnly`, `Secure`, `SameSite=Strict`, `Path=/api/v1/auth`.

Stored as SHA-256, not Argon2. It is looked up by value on every refresh so the
transform must be deterministic, and that is safe here in a way it is not for a
password: there is no dictionary to run against 256 bits of entropy. Both
choices are in `RefreshTokenService`'s class comment so they do not read as
inconsistent.

**`SameSite=Strict` is correct only while frontend and backend share a
registrable domain.** Port and subdomain do not affect same-site; a different
registrable domain does. If they ever split, this must become `SameSite=None;
Secure`. Recorded now rather than discovered in production.

Own `JwtAuthenticationFilter` rather than `oauth2ResourceServer(jwt)`: the
resource-server filter installs its own entry point and answers with the RFC
6750 `WWW-Authenticate` form, which is not the section 6 error shape. Doing the
decode ourselves keeps every rejection in the application routing through
`SecurityErrorHandler`.

### Module boundary

Public from `auth`: `AuthService`, the DTOs, `UserRole`, and
`JwtAuthenticationFilter`. Package-private: `UserAccount`, `RefreshToken`, both
repositories, `DefaultAuthService`, `JwtService`, `RefreshTokenService`,
`PasswordPolicy`, `AdminBootstrap`, `GoogleSignInHandler`, `AuthController`.

`JwtAuthenticationFilter` is the one thing crossing the line, because
`SecurityConfig` lives in `config` and must install it. It is module
infrastructure, not an entity or repository, so section 4 is satisfied — but it
is the exception and is flagged as such.

Auth's exceptions are rendered by `AuthExceptionHandler` inside the `auth`
package rather than by `GlobalExceptionHandler`, so `common.web` does not import
this module's types. `InvalidCredentialsException` and
`RefreshTokenReuseException` produce byte-identical responses.

### Email verification

Column exists, defaults false, nothing is gated on it, and `/me` reports it.

For it to mean anything: a mail transport (none exists, and adding one brings
deliverability, bounce handling and a provider dependency); a table of
single-use signed verification tokens with expiry, which is a third auth table;
a `GET /api/v1/auth/verify` endpoint; a resend path with its own rate limit; and
a decision about *what* requires a verified address. That last is the real
question and it is a product one — gating feedback submission is defensible,
gating login is not, and gating anything about venting is forbidden by rule 2.2.

---

## 5. Verification actually run

| Check | Result |
|---|---|
| `mvnw verify` | **PASS** — 106 tests, 0 failures, 0 errors |
| Test count went **up**, not just green | **PASS** — 26 → 106 (surefire 13 → 40, failsafe 13 → 66) |
| The new IT classes actually executed | **PASS** — all 8 named in the failsafe output |
| Compiles under `-Xlint:all -Werror` | **PASS** — zero warnings |
| Flyway V2 applies to real Postgres | **PASS** |
| `citext` extension available | **PASS** — probed in `postgres:16-alpine` before it was designed in |
| Argon2id actually in use | **PASS** — stored hash asserted to start `$argon2id$` |
| ArchUnit boundary, both directions | **PASS** — 3 rules |
| `/api/v1/vent/**` anonymous | **PASS** — 7 tests, none authenticated |
| No auth response leaks a secret | **PASS** — 6 endpoints swept, incl. error bodies |
| Context starts with no Google config | **PASS** |
| `docker compose build` (all three) | **PASS** |
| `docker compose up` — all three | **PASS** — db, backend, frontend all healthy |
| `/`, `/vent`, `/vent/released`, `/about` through the stack | **PASS** — 200 |
| Register → login → `/me` through the stack | **PASS** — 201, 200, 200 |
| Google entry point with no credentials | **PASS** — 404, app otherwise fine |
| Flyway V1 **and** V2 applied in the running stack | **PASS** |
| `vent_events` still exactly 3 columns, live | **PASS** |
| Vent release over HTTP with no auth | **PASS** — 204 |
| Vent release over HTTP with a *forged* token | **PASS** — 204, token ignored |
| Registration duplicate is byte-identical, live | **PASS** — `cmp` on the two bodies |
| Login case-insensitive over HTTP | **PASS** — registered `Real@Example.com`, logged in as `REAL@example.com` |
| Cookie attributes on the wire | **PASS** — `Path=/api/v1/auth; Max-Age=2592000; Secure; HttpOnly; SameSite=Strict` |
| Rotation → reuse → whole family dead, live | **PASS** — verified in the database, see below |
| Raw refresh token absent from storage | **PASS** — 0 rows match the issued value |

### The reuse path, end to end over HTTP

Not just asserted in a test. Against the running stack:

```
refresh #1 with token A   -> 200, new token B, body contains no token
reuse    token A          -> 401
use      token B          -> 401     <- the legitimate client, correctly locked out
```

And in the database afterwards, showing the blast radius is exactly one family:

```
 family_id                            | tokens | revoked
--------------------------------------+--------+---------
 7a515e9d-...  (an earlier login)     |      1 |       0
 74d2ad1b-...  (the compromised one)  |      2 |       2
```

### The frontend container, and a false alarm worth recording

On the first attempt `frontend` stayed in `Created` and failed to bind:

```
Error response from daemon: ports are not available: exposing port TCP
0.0.0.0:3000 ... bind: Only one usage of each socket address ... is normally permitted.
```

A `node.exe` (PID 5020) on the host held port 3000 — a leftover dev server,
unrelated to this phase. It was left running rather than killed. This was
initially written up as "three-container bring-up not verified".

**That entry was wrong and is corrected here.** The port later freed and the
full stack came up healthy on all three containers, and everything above was
then re-run against it: the four frontend routes, an anonymous vent release, and
a complete register/login//me cycle. Recorded rather than quietly edited,
because the sequence matters — a port conflict on the host looks exactly like a
broken compose file for as long as it lasts, and the difference is only visible
by retrying.

The refresh cookie as observed through the full stack:

```
Set-Cookie: hhf_refresh=...; Path=/api/v1/auth; Max-Age=2592000;
            Secure; HttpOnly; SameSite=Strict
```

`GET /oauth2/authorization/google` returns 404 with no credentials configured,
which is the intended shape: the route is simply not mapped, rather than mapped
and broken.

### The leakage sweep

`AuthResponseLeakageIT` checks six responses — login, register, refresh, `/me`,
a failed login, an unauthenticated `/me` — against a list of forbidden
substrings (`password_hash`, `token_hash`, `$argon2`, `refresh_token`,
`google_id`, and casing variants), plus the actual stored hash and the actual
issued refresh token pulled from the database and the cookie.

It also locks the shape of both response records by reflection.
`AccessTokenResponse` gaining a `refreshToken` component is the specific
serialisation mistake that would put the token in a body, and it fails there
rather than in review.

---

## 6. NOT verified

- ~~**Google sign-in has never been executed.**~~ **Executed 2026-09-06.** A
  real sign-in was completed once the two defects below were fixed: Google
  authenticated, `GoogleSignInHandler` created the account and linked the Google
  id, the refresh cookie was issued, and the browser was redirected to
  `/auth/callback`. The account row exists with `has_google = t`, and
  `APP_ADMIN_BOOTSTRAP_EMAILS` then promoted it to `ADMIN` on restart. The
  create-and-link path is no longer unexercised code. Three branches of the
  handler still are — linking to an existing *password* account, the
  unverified-email rejection, and a returning sign-in — and the cookie was set
  but never replayed, because the page that would replay it is phase 6. Full
  record in "Google sign-in, executed end to end" at the end of this log.
- **No frontend has called any of this.** Every test is MockMvc or curl. Cookie
  behaviour in a real browser — `SameSite=Strict` on the actual cross-port XHR
  from `localhost:3000`, whether `Secure` over plain HTTP behaves as expected —
  is reasoned from the spec, not observed. The attributes on the wire are
  confirmed; how a browser *acts* on them is not.
- **No browser has loaded any of it.** The frontend routes return 200 and the
  backend serves the whole auth cycle through the compose network, but that is
  `curl` against both. `/vent` rendering correctly, and the cookie actually
  being stored and replayed by a browser under `SameSite=Strict`, remain
  unobserved — and the frontend has no auth code yet anyway, which is phase 6.
- **Timing equalisation is argued, not measured.** The Argon2 hash runs on both
  registration paths and on unknown-address login, which removes the large
  difference. No statistical timing test was run, and smaller differences
  certainly remain — a database lookup that misses is not free.
- **The rate limiter is still per-instance and in-memory**, and still keyed on
  `getRemoteAddr()` with `X-Forwarded-For` untrusted. Under `docker compose`
  every request appears to come from the Docker gateway, so **all users share
  one bucket** — meaning the 5/min auth limit is effectively global in the
  compose deployment. Unchanged from phase 4 and still phase 9's problem, but it
  matters more now that it guards login.
- **`refresh_tokens` is never pruned.** One row per refresh, forever. Expired
  and revoked rows accumulate. `idx_refresh_tokens_expires_at` exists to support
  a cleanup job that does not exist.
- **The JWT secret has a committed default.** `application.yml` falls back to a
  base64 value that is in the repository. ~~An install that forgets
  `APP_JWT_SECRET` starts successfully on a public secret; phase 9 should refuse
  to boot on it outside the `local` profile.~~ **Closed 2026-09-06** —
  `JwtSecretGuard` refuses to start on that value unless the `local` profile is
  active. The default remains, which is what keeps a fresh clone and the suite
  working.
- **No concurrency testing.** Two simultaneous refreshes with the same token
  race on the reuse check. The unique index on `token_hash` prevents duplicate
  rows, but which request wins and whether the loser's family is revoked is
  unspecified and untested.
- **Load, soak, and token-expiry-over-time** are untested. Nothing has waited 15
  minutes to watch an access token actually expire; the TTL is asserted as a
  number in the response.
- **`@PreAuthorize` has no admin endpoint to guard yet.** The 401/403 fix is
  tested through the filter chain, not through method security, because nothing
  is annotated yet. Phase 7 brings the first one.

---

## 7. Environment variables introduced

```
APP_COOKIE_SECURE=true                  # false only for LAN-IP without TLS
APP_ADMIN_BOOTSTRAP_EMAILS=             # comma-separated; promotes existing accounts only
GOOGLE_CLIENT_ID=                       # optional
GOOGLE_CLIENT_SECRET=                   # optional
APP_OAUTH2_SUCCESS_REDIRECT=http://localhost:3000/auth/callback
```

`APP_JWT_SECRET`, `APP_JWT_ACCESS_TOKEN_TTL` and `APP_JWT_REFRESH_TOKEN_TTL`
existed in `.env.example` since phase 1 and are now actually read.

**Renamed:** `GOOGLE_OAUTH_CLIENT_ID` / `GOOGLE_OAUTH_CLIENT_SECRET` →
`GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET`. The old names were phase-1
placeholders nothing ever read; the new ones match the labels in the Google
Cloud Console.

### Getting Google credentials

Google Cloud Console → APIs & Services → Credentials → Create credentials →
OAuth client ID → **Web application**.

- Authorised redirect URI: `http://localhost:8080/login/oauth2/code/google`
- Authorised JavaScript origin: `http://localhost:3000`

Both variables must be set or the registration is not created at all.

---

## 8. Still open

Carried forward, with movement:

1. **`/support` and `/login` still 404.** `/login` is phase 6, and the backend
   it will call now exists.
2. **Contact address is still a placeholder** — `lib/contact.ts` line 20.
3. **Helpline re-verification cadence** still undecided.
4. **Privacy policy still needs legal review**, and the subject has grown: the
   site now stores email addresses, display names and password hashes.
5. **Safety-list review** (phase 4 §5) — the Hinglish set still needs a native
   speaker.
6. **New — phase 6 must render the registration message from §4** alongside a
   link to `/login`. This is the agreed mitigation for non-enumerating
   registration and the API already returns the exact string.
7. ~~**New — Google sign-in is unexercised.** First real sign-in should be
   treated as testing, not as usage.~~ **Closed 2026-09-06** — that first sign-in
   has been done, and was treated as testing. See the end of this log.
8. **New — refresh token pruning** has no job. Phase 9.
9. ~~**New — the committed JWT secret default** should stop being accepted
   outside the `local` profile. Phase 9.~~ **Closed** — not in phase 9, in a
   standalone fix immediately after this phase. See the section below.

---

# Security fix — the committed JWT secret is refused outside `local`

**Date:** 2026-09-06
**Closes:** Phase 5 §8 item 9. Nothing else in this change.

## 1. What was wrong

`application.yml` gave `app.auth.jwt-secret` a working default, and that default
is in the repository. It is long enough for HS256, so nothing downstream
objected: an install that never set `APP_JWT_SECRET` did not fail loudly, it
started and signed tokens with a key that is public on GitHub.

The consequence is not "weak crypto". Anyone who can read the source can mint a
valid access token for any user id with `role` set to `ADMIN` — no password, no
database, no request to the server, and nothing in a log to separate the forged
token from a real one. Phase 5 recorded this as a deliberate trade for local
ergonomics and deferred it to phase 9. It was worth pulling forward, because the
window is "any deployment that happens before phase 9", and the person who makes
that deployment is exactly the person who would not notice.

## 2. The fix

`config/JwtSecretGuard` — an `InitializingBean` that compares the bound value of
`app.auth.jwt-secret` against the committed default and throws
`IllegalStateException` unless the `local` profile is active. Startup message:

```
app.auth.jwt-secret is still the development default from application.yml, and
the active profile is not 'local'. That default is committed to the repository,
so anyone who can read the source can forge an access token for any account,
including an ADMIN one. Set APP_JWT_SECRET to a secret of your own:
openssl rand -base64 48
```

The default stays. A fresh clone still works, because `docker-compose.yml`
already defaulted `SPRING_PROFILES_ACTIVE` to `local` — that was true before this
change and is what made the profile the right signal to key on.

Three decisions worth recording:

- **Profile, not a "is this production" guess.** There is no reliable answer to
  that question at startup. The profile is set deliberately by an operator, and
  the failure direction is the safe one: an install with *no* profile set — the
  same install that forgot `APP_JWT_SECRET` — is refused rather than trusted.
- **The check is on the value, not on where it came from.** Copying the default
  into `APP_JWT_SECRET`, an `.env` file or a secret manager gets the same
  refusal. The secret is public wherever it is typed.
- **The default is duplicated in Java**, because YAML cannot reference a
  constant. That duplication is the failure mode this fix could have shipped
  with: change the YAML default and the guard goes on comparing against a string
  nothing uses, silently guarding nothing. `JwtSecretGuardTest` reads
  `application.yml` off the classpath and asserts the two still match.

`PostgresTestBase` now declares `@ActiveProfiles("local")`. The suite runs on the
committed default, so this states what a test run is — a local install — instead
of giving every test class a secret of its own. It also means every
`@SpringBootTest` exercises the accepting branch: if the guard were wrong about
it, nothing would boot.

## 3. Verification actually run

`./mvnw verify` — **66 tests, 0 failures**, including the 4 new ones. The IT logs
show `The following 1 profile is active: "local"`, so the accepting branch is
genuinely exercised rather than assumed.

Unit tests alone would only prove the method throws, not that anything calls it,
so all three cases were also run against the packaged jar and a real Postgres:

| Profile | Secret | Result |
| --- | --- | --- |
| `prod` | committed default | **exit 1**, `BeanCreationException` carrying the message above |
| `prod` | `openssl rand -base64 48` | exit 0, `Started HeadHeartFreesApplication` |
| `local` | committed default | exit 0, `Started HeadHeartFreesApplication` |

One thing the jar run showed that the unit test could not: the datasource and
Flyway initialise *before* this bean, so on a host with an unreachable database
the DB error is what appears first. The application does not start either way, so
the secret is never in use, but the message is not always the first failure in
the log.

## 4. Also changed

- `README.md` — the no-Docker path is now
  `SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run`. Without the profile it
  would now refuse to start, and a documented command that fails on a fresh
  clone is its own bug.
- `.env.example` and the comments in `application.yml` / `AuthProperties` — they
  described the hazard and pointed at phase 9. They now describe the guard.

## 5. Still open

Phase 5 §8 stands otherwise, minus item 9. Nothing here touches items 1–8.

Two limits of this fix, stated rather than implied:

1. **It rejects one known string.** A deployment that sets `APP_JWT_SECRET` to
   `password` passes. Entropy is not checked; `JwtService` still only enforces
   the 256-bit length HS256 requires.
2. **`local` is an honour system.** An operator who sets
   `SPRING_PROFILES_ACTIVE=local` on a public server gets exactly what they
   asked for. The guard raises the floor from "silently insecure by default" to
   "insecure only on purpose"; it is not an authorisation boundary.

---

# Phase 5 defect found after the fact — the auth variables never reached the container

**Date:** 2026-09-06
**Kind:** Phase 5 defect, found after that phase was signed off. Two causes, one
symptom.

## 1. The symptom

`docker compose exec backend env | grep GOOGLE` returned nothing, and
`GET /oauth2/authorization/google` returned 404. Google sign-in had never been
reachable through compose.

## 2. First cause — the variables were never plumbed through compose

`docker-compose.yml` lists the backend's environment explicitly, and phase 5
added its variables to `.env.example` and to the application without adding them
to that list. Everything phase 5 introduced was therefore inert under compose:

| Variable | What compose was actually doing |
| --- | --- |
| `APP_JWT_SECRET` | ignored; backend ran on the committed default |
| `APP_JWT_ACCESS_TOKEN_TTL` | ignored; 15m default |
| `APP_JWT_REFRESH_TOKEN_TTL` | ignored; 30d default |
| `APP_COOKIE_SECURE` | ignored; `true` default |
| `APP_ADMIN_BOOTSTRAP_EMAILS` | ignored; no account could ever be promoted |
| `GOOGLE_CLIENT_ID` / `_SECRET` | ignored; no Google registration |
| `APP_OAUTH2_SUCCESS_REDIRECT` | ignored; default callback |

`.env.example` documented all of them, the application read all of them, and
nothing in between carried them. The phase-5 log's "environment variables
introduced" section (§7) described a contract that only held for someone running
the jar directly.

The `APP_ADMIN_BOOTSTRAP_EMAILS` row is worth naming separately: the documented
sequence for getting an ADMIN account — register, set the variable, restart — did
nothing at all under compose, and the failure is silent, because the bootstrap
promotes accounts that exist and says nothing when the list is empty.

**Fixed** by adding all eight to the backend service. `APP_JWT_SECRET` uses the
null form (`APP_JWT_SECRET:`) rather than `${APP_JWT_SECRET:-}`, because an unset
variable interpolates to the empty string and an empty string is a *set* property
to Spring: it would override the default in `application.yml` and the application
would refuse to start with "app.auth.jwt-secret is not set". The null form passes
a value through from `.env` or the shell and leaves the variable absent when there
is none. Both behaviours were confirmed with a throwaway compose file before the
real one was touched:

```
with .env:     NULLFORM_VAR=from-dotenv   EMPTYDEFAULT_VAR=from-dotenv
without .env:  (NULLFORM_VAR absent)      EMPTYDEFAULT_VAR=
```

`GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` default to empty, deliberately:
blank means the registration is never created and the app starts with password
login only, which is what `OAuth2AbsentConfigIT` pins.

## 3. Second cause — the post-processor was registered in a file Spring never reads

Adding the variables was necessary and not sufficient: with all of them present
in the container, `/oauth2/authorization/google` still 404'd.

`GoogleOAuth2EnvironmentPostProcessor` was registered in
`META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports`.
That is the modern-looking form and it is read for `AutoConfiguration` only.
Spring Boot loads `EnvironmentPostProcessor` implementations through
`META-INF/spring.factories` — its own `spring-boot-3.5.16.jar` registers
`ConfigDataEnvironmentPostProcessor` and the rest exactly that way.

So the class had never been loaded. Not under compose, not from the jar, not in
the test suite, not once since phase 5. The registration it exists to create was
never created, and every line of its javadoc described behaviour that could not
happen.

**Fixed** by replacing the `.imports` file with `META-INF/spring.factories`.

### Why the suite did not catch it

`OAuth2AbsentConfigIT` was the only test touching any of this, and it asserts
that no `ClientRegistrationRepository` bean exists. That was true — and it stayed
true with credentials configured, for the wrong reason. A negative test with no
positive counterpart passes for a class that is never loaded.

`OAuth2ConfiguredIT` is the missing half: with `GOOGLE_CLIENT_ID` and
`GOOGLE_CLIENT_SECRET` set as inlined test properties, the registration exists,
`/oauth2/authorization/google` redirects to `accounts.google.com`, and the
authorization URL requests `openid email profile` — the three scopes
`GoogleSignInHandler` needs, which a default-scoped registration would not have
requested and which would have failed at the callback rather than here. It uses a
fake client id on purpose: Spring builds the URL without contacting Google, so a
fake proves the plumbing exactly as well as a real credential and keeps the test
independent of what a developer has exported.

It was checked against the bug: with the `.imports` file restored, all three of
its tests fail, the first with "empty here means
GoogleOAuth2EnvironmentPostProcessor did not run".

## 4. Verification actually run

`./mvnw verify` — **69 tests, 0 failures** (66 before this change, plus the three
new ones).

Then against the real stack, `docker compose up -d --build backend`:

| Check | Result |
| --- | --- |
| `docker compose exec backend env` | all eight variables present |
| `GET /oauth2/authorization/google` | **302** to `https://accounts.google.com/o/oauth2/v2/auth`, with `scope=openid email profile` and `redirect_uri=http://localhost:8080/login/oauth2/code/google` |
| `-e APP_JWT_SECRET=too-short` | refuses to start: "must decode to at least 256 bits for HS256; got 72 bits" — proof the variable now reaches the property rather than merely existing |
| `GOOGLE_CLIENT_ID`/`_SECRET` forced empty | container healthy, `/oauth2/authorization/google` 404, `/api/v1/auth/me` 401 — the `OAuth2AbsentConfigIT` state, unchanged |

The third row is the one that distinguishes "the variable is in the container"
from "the application is using it". Presence in `env` proves only the former.

## 5. Still open

1. ~~**Google sign-in past the redirect is still unexercised.**~~ **Closed the
   same day.** A real sign-in went all the way through `GoogleSignInHandler`;
   the account it created is in the database. Recorded in full in the next
   section.
2. ~~**`APP_ADMIN_BOOTSTRAP_EMAILS` has never promoted an account under
   compose.**~~ **Closed the same day** — it promoted that Google-created
   account to `ADMIN` on restart, the first time the documented sequence has
   ever run under compose. It could not have worked before this entry's fix,
   which is what makes it evidence that the fix landed.
3. **No test asserts that `docker-compose.yml` carries what `.env.example`
   documents.** Both files are edited by hand and this defect is precisely their
   drifting apart. A check that every `APP_*`/`GOOGLE_*` key in `.env.example`
   appears in the compose backend service would have caught it on the day it
   landed.

---

# Google sign-in, executed end to end

**Date:** 2026-09-06
**Closes:** phase 5 §6 first bullet, phase 5 §8 item 7, and items 1 and 2 of the
compose-defect entry above. No code changed — this records a run.

## 1. What was observed

One real sign-in, in a browser, against the developer's localhost Google OAuth
client, through the compose stack:

1. `GET /oauth2/authorization/google` redirected to Google, and **Google
   authenticated the person** — the consent screen was reached and completed,
   which is the step that had never happened before.
2. The backend **created a local account** from the profile. This is the
   `linkOrCreate` → *create* branch: no row matched the Google id and none
   matched the email.
3. The **Google id was linked** to that account.
4. The **refresh cookie was issued** on the redirect response.
5. The browser was **redirected to `/auth/callback`**, which **404s**. That is
   phase 6's page and it does not exist yet. The redirect itself is correct
   behaviour, and — worth stating — the 404 is the frontend's, not a failure of
   the sign-in.
6. The database showed **one row, `has_google = t`, `role = USER`**. Sign-in
   through a provider grants no privilege, which is what that `USER` confirms.
7. `APP_ADMIN_BOOTSTRAP_EMAILS`, set to that address, **promoted the account to
   `ADMIN` on restart** — under compose, for the first time. The documented
   register-set-restart sequence had never once run to completion before.

Point 7 is also independent evidence for the previous entry: that variable could
not reach the container until it was added to `docker-compose.yml`, so a
successful promotion is only possible on the fixed file.

## 2. What this does not cover

The handler has four paths. One of them has now run. The others have not, and
should not be described as working:

- **Linking to an existing password account.** `linkOrCreate` falls back to
  `findByEmail` and calls `linkGoogle` on the match. The account here was
  created fresh, so that branch did not execute. It is the branch the javadoc
  worries about — the one that prevents a duplicate row and the CITEXT
  constraint turning a first Google sign-in into an unexplained 500 — and it
  remains argued rather than observed. Registering with a password first, then
  signing in with the same address through Google, is the test.
- **A returning sign-in.** `findByGoogleId` returning a match, rather than
  creating, has not been exercised. Signing in a second time covers it.
- **The unverified-email rejection.** `?error=unverified_email` requires a Google
  account with an unverified address, which the sign-in used did not have.
- **The cookie being replayed.** It was set. Nothing has sent it back:
  `/auth/callback` 404s, so `POST /api/v1/auth/refresh` from a browser has not
  happened. Whether a browser stores and replays it under `SameSite=Strict`
  across the `localhost:3000` → `localhost:8080` boundary is still reasoned from
  the spec, exactly as phase 5 §6 says. Phase 6 is what finally tests it.

And the run used the **developer's** OAuth client, which only works on localhost
and which HANDOVER §4 says the developer deletes at handover. The owner's own
client, on the real domain, is a different registration with different redirect
URIs — HANDOVER §3.4 keeps that as a launch item for good reason.

---

# Phase 6 — Auth frontend

**Date:** 2026-09-07
Completed. Register, sign in, Google, session restore, avatar menu, one
protected route. One backend change, approved in advance and no more.

## 1. Status

| Piece | State |
|---|---|
| `/login`, `/register` | built, with the shared registration message |
| `/auth/callback` | built — it was the 404 the first Google sign-in landed on |
| `/account` | built; the phase's only protected route |
| Session restore, rotation, sign-out | built |
| Avatar menu | built, hand-rolled, no Radix |
| Soft arrival prompt | **built and cut.** §4 below |
| Backend | one line: `Retry-After` exposed across origins, plus its test |

`mvnw verify` — 116 tests, 0 failures (was 106).
`npm test` — 38 tests, 6 files, 0 failures. First frontend tests in the project.
`docker compose up --build` — clean, all three services healthy.

## 2. The rule this phase was written around

Rule 2.2: venting requires no account. The way that breaks in a phase like this
is not a gate — a gate would be caught in review, and the rule is written down in
three places. It breaks because a provider added for the navbar holds the render
while it asks the server who is signed in, and `/vent` stalls for three hundred
milliseconds on a bad connection, invisibly, for the person least able to wait.

So `SessionProvider` renders its children immediately and unconditionally. There
is no branch in which it returns a spinner, a skeleton or null. Only components
that show session-dependent UI read `status`, and the only one that changes
shape is the navbar.

`VentComposer.session.test.tsx` holds it. The refresh in that file is not slow
and does not fail — it is a promise that never settles, which is what a train
tunnel actually looks like — and the composer has to accept a mood, accept
typing, surface the crisis panel and complete a release straight through it. One
test also asserts that an anonymous visitor makes **no** auth request at all, and
one walks every request the page made looking for the typed sentence, on the
grounds that a rule 2.1 leak would arrive as an analytics call or an error
report rather than as an extra field on `/release`.

## 3. Decisions

### 3.1 The session hint cookie

`hhf_session_hint=1`. Non-httpOnly, `SameSite=Lax`, thirty days, and **it is a
boolean, not a credential** — the file says so at the top and the point is worth
repeating here so nobody has to open it to find out. Forging it buys exactly one
`POST /refresh` that 401s. The real credential is the httpOnly `hhf_refresh`
cookie that script cannot read. Nothing anywhere trusts this value to decide who
someone is; it decides only whether a request is worth making.

Without it, every page load by every visitor calls `/refresh` to find out
whether there is a session. That endpoint shares a 5/min per-IP bucket with
login and register, and behind Docker — or any proxy not yet forwarding the
client address, which is the state described in HANDOVER §10.3 — every visitor
is one IP. Five anonymous page loads a minute would exhaust the bucket real
sign-ins need. The site would be denying service to itself, and worse the more
traffic it got.

With it, a browser that has never signed in makes no auth request whatsoever.
That is asserted in two separate test files.

The lifetime can drift from the server's `APP_JWT_REFRESH_TOKEN_TTL`, which the
frontend cannot read. Both directions are harmless and self-correcting: a hint
that outlives the token costs one call that 401s and clears it; a token that
outlives the hint means signing in again. Neither is a security property, which
is the point.

### 3.2 What a failed refresh is allowed to conclude

| Response | Meaning | Result |
|---|---|---|
| 401 | the session is genuinely gone | sign out, clear the hint |
| 429 | we were not allowed to ask | keep state, retry once |
| network error | we could not ask | keep state, retry once |

Only a 401 is the server saying "this person is not signed in". The obvious
simplification — one `catch`, sign out, done — means a rate limiter having a
busy minute silently ends somebody's session, and on the shared-IP deployment
above the busy minute is not hypothetical.

When a restore fails twice for a reason that is not a 401, the status settles to
`anonymous` (the navbar has to offer a way in) but the **hint is kept**, because
nothing ever established that the session was over and the next page load should
try again rather than write the person off. When a *renewal* fails twice, the
live session is left exactly as it is and another attempt is scheduled — the
access token may well still be valid, and `authFetch` will find out otherwise.

The retry delay comes from `Retry-After`, capped at sixty seconds. Six tests
cover this table, including one that asserts the retry does not fire a second
early: retrying at twenty-nine seconds against a header that said thirty spends
a token the bucket has not refilled and earns another 429.

### 3.3 The one backend change

```java
config.setExposedHeaders(List.of(HttpHeaders.RETRY_AFTER));
```

A browser will not let script read a response header on a cross-origin response
unless the server names it. `Retry-After` was already on the wire of every 429 —
`GlobalExceptionHandler` sets it, and `AuthRateLimitIT` asserts it — and
`ApiError.retryAfterSeconds` in the frontend was nonetheless `null` in a browser,
always, on precisely the responses the field exists for. Shipping a field that is
permanently null is worse than not having one.

`CorsExposedHeadersIT` asserts it three ways: on an ordinary cross-origin
response, on the real 429 after exhausting the bucket, and — while there — that
the preflight for `/refresh` still echoes the origin with
`Allow-Credentials: true` rather than a wildcard, since the whole session
restore depends on that.

`AuthRateLimitIT` asserts the header is **sent**; this asserts it can be
**read**. Both are needed. MockMvc reads response headers directly and would
never notice the difference, which is exactly how this was invisible until now.

### 3.4 Vitest, Testing Library, jsdom. No Playwright.

First tests in `frontend/`. `npm test`, 38 of them across six files.

A browser-driving suite would test more and would need a running backend, a
database and a browser download in CI — a large amount of machinery for a phase
whose new logic is one provider and three forms. jsdom cannot tell us how any of
this looks. It can tell us what it does, and what it does is where the rules
live. What that leaves unobserved is §7, and one item there is significant.

### 3.5 The return destination

`hhf_return_to`, five minutes, cleared on read. A cookie rather than a query
parameter only because Google sign-in leaves this origin entirely — browser to
backend to Google to backend to `/auth/callback` — and nothing in React state
survives that. Ordinary same-origin sign-in uses `?next=` instead, because a URL
that says where it will send you is easier to reason about than a cookie you
cannot see.

Both paths go through one sanitiser, and it is re-run on read as well as write:
the cookie is writable by script, so validating only on write puts the guard on
the wrong side of the trust boundary. "Starts with a slash" is not the check —
`//evil.example` and `/\evil.example` both start with one and both resolve to
another host, which is how a return-to becomes a phishing hop that begins with
your own domain. Fourteen cases in `return-to.test.ts`.

### 3.6 The restoring state never renders a signed-out navbar

While `status` is `restoring` the navbar's right-hand corner renders a reserved
gap and nothing else, and the mobile panel renders one fewer item. It never
renders "Sign in" and then swaps it for an avatar.

On most sites that flicker is a rendering wart. Here the signed-out state is a
claim about somebody's account, and "you have been logged out" is a frightening
thing to read on a page you opened because you were already having a bad day. A
blank gap says nothing, which is the honest thing to say before the answer
arrives. The width is reserved so the wordmark and nav do not slide.

`Navbar.test.tsx` asserts no element anywhere in the header contains the text
"sign in" while a refresh is pending, then resolves it and asserts the account
menu appears without that text ever having been rendered. `RequireAuth` follows
the same rule for the same reason — it must not redirect while restoring, or a
signed-in person opening `/account` from a bookmark gets bounced to a form
asking them to sign in to the account they are already in.

### 3.7 Hand-rolled avatar menu

Phase 2 deferred "Radix, for the avatar dropdown only" to this phase. It is
still not installed. Every other primitive is hand-rolled, the navbar already
hand-rolls a disclosure with the same escape-and-restore behaviour, and what
Radix would have brought — roving focus, `role="menu"` semantics, outside-click
and escape — is about fifty lines. A second or third menu is the moment to
reconsider.

`AccountMenu.test.tsx` covers what that decision took on: open puts focus on the
first item, arrows move and wrap, Escape closes *and returns focus to the
trigger* — the half that gets forgotten, and without which Escape drops a
keyboard user at the top of the document. `role="menu"` is a promise to a screen
reader that these things happen; a menu that announces itself that way and then
does not behave that way is worse than an unadorned list of links.

The focus code reads its items from the DOM rather than collecting them into a
ref array by index. The array version was written first and was wrong: the
indices shift when the ADMIN entry appears, leaving a stale detached node at the
end of the list, and the arrow keys then move focus to nothing. One of the tests
exists specifically for the three-item admin case.

No avatar image. Nobody uploads one, Google's `picture` claim is deliberately
not stored, and an `<img>` pointing at `googleusercontent.com` would tell Google
which pages of this site a person is looking at. Two letters on a clay wash
instead.

### 3.8 What is deliberately absent

- **No "forgot password" link.** There is no mail transport, so a reset link has
  nowhere to send anything. A link to a page saying "not available yet" is worse
  than no link: someone locked out would follow it and then have to work out for
  themselves that there is no way back.
- **No "your email is unverified" notice on `/account`.** The column exists,
  `/me` reports it, nothing depends on it. A warning with no button that could
  resolve it is a permanent complaint about something the person cannot fix.
- **No delete-account button.** There is no endpoint. `/account` links to
  `/contact` and says a person will do it, which is true.
- **No password change.** Same reason as the missing reset.

All four go in when email does. They are one feature, not four.

## 4. The soft arrival prompt — built, then cut

PROJECT_BRIEF.md §7 asks for "a dismissible soft prompt on first visit only,
remembered via a cookie", that "must not cover the page, must not block `/vent`,
and must have an obvious close control". It was built to that spec: one line in
ordinary document flow, no overlay, no backdrop, no animation, cookie written on
render so ignoring it was as final as closing it, and excluded from `/vent`,
`/vent/released`, `/crisis-resources` and the auth pages.

Then it was looked at, and there were only two places to put it:

- **Above the footer**, where it is not intrusive and is also below the fold on
  every page it can appear on — and where it would sit directly on top of the
  crisis helpline strip. The last thing before those numbers should not be an
  account prompt.
- **Below the navbar**, where it would actually be read, and where it is the
  first thing a first-time visitor sees, before the site has said what it is.

Neither is good, and that was the argument for cutting it until the deciding
fact turned up: **the home page already says this, better, as one of its four
promises.**

> "You do not sign up to write. There is no email box between you and the page.
> Accounts exist later for people who want to leave feedback under a name, and
> never for venting."

And `HowItWorks` step one opens "No account, no email, no waiting." The prompt
could only repeat, in a smaller voice and a worse position, a message the home
page already makes as a headline. So it is not shipped, `ArrivalPrompt.tsx` is
deleted rather than left commented out, and `layout.tsx` carries a note saying
where the argument is. Reinstating it needs a reason that promise block does not
already cover.

The brief is not wrong to ask for it. It asked before the home page existed.

## 5. Two concurrency bugs found while building, not by review

Both come from the same fact: **refresh tokens rotate, and presenting a spent
one is how the backend detects theft.** `RefreshTokenService` revokes the entire
family and every session from that sign-in ends. So two overlapping refreshes
send the same cookie, and the second looks exactly like an attacker replaying a
stolen token — the punishment for a race is a surprise sign-out.

Verified over HTTP against the running stack rather than argued: replaying a
spent token returns 401, and the token that had legitimately replaced it is 401
too. The family really is gone.

1. **Concurrent refreshes.** The scheduled renewal, a 401 retry inside
   `authFetch`, and the visibility-change catch-up can all fire at once.
   `SessionProvider` now deduplicates: callers share one in-flight attempt, and
   the first caller's mode decides what happens on failure.
2. **StrictMode on `/auth/callback`.** `reactStrictMode: true`, so effects run
   twice in development on the same instance. Without a guard ref the second run
   replays the cookie the first had just spent and signs the person straight back
   out — a bug that appears only in development and looks exactly like Google
   sign-in being broken.

Neither was in the plan. Both are in the code with the reasoning attached.

## 6. Verification actually run

| Check | Result |
|---|---|
| `./mvnw verify` | **PASS** — 116 tests, 0 failures, 0 errors (44 unit, 72 IT) |
| `npm test` | **PASS** — 38 tests, 6 files |
| `npm run typecheck` | **PASS** — clean |
| `npm run lint` | **PASS** — clean |
| `npm run build` | **PASS** — 15 routes, all prerendered static |
| `docker compose up -d --build` | **PASS** — db, backend, frontend all healthy |

Against the running compose stack, over HTTP:

| Check | Result |
|---|---|
| `/login`, `/register`, `/auth/callback`, `/account`, `/vent`, `/` | all **200** |
| `/auth/callback` no longer 404s | **closed** — it was the phase-5 landing failure |
| register, then the shared 201 message | byte-for-byte the string the UI renders |
| login, then `Set-Cookie: hhf_refresh` | `Path=/api/v1/auth; Secure; HttpOnly; SameSite=Strict` |
| refresh, then rotation | new token issued, old one replaced |
| replaying the spent token | **401**, and the rotated token is **401** too — the family is revoked |
| 429 after six logins | `Retry-After: 3` **and** `Access-Control-Expose-Headers: Retry-After` |
| the same 429's CORS headers | `Allow-Origin` echoed once, `Allow-Credentials: true`, no wildcard |
| `/me` with a bearer token | 200, and the body is exactly `UserSummary` — no hash, no `googleId` |
| `/me` with no token | 401 |
| `POST /vent/release` with no account and no cookie | **204** |
| the server-rendered HTML of `/` | contains **no** "Sign in" — the neutral restoring state is what actually ships, not just what the tests assert |
| `/login` with `NEXT_PUBLIC_GOOGLE_SIGN_IN` unset | no Google button, password form intact |

## 7. NOT verified

Stated plainly, because two of these are things this phase was supposed to
close.

1. **No browser was driven.** That was the decision in §3.4 and it is the right
   one for the logic, but it means everything below is reasoned rather than
   observed.

2. **The refresh cookie has still not been replayed by a real browser.** The
   phase 5 log says "Phase 6 is what finally tests it" about whether a
   `SameSite=Strict` cookie set on `localhost:8080` comes back on an XHR from
   `localhost:3000`. The page that would do it now exists and 200s. It has not
   been opened in a browser. **curl has no concept of SameSite**, so the HTTP
   checks in §6 prove the server side and prove nothing about this. Port does not
   affect same-site, so it should work — which is exactly what phase 5 already
   said, and saying it again is not evidence. This is the first thing to try, and
   it is one page load.

3. **Google sign-in has not been run through the new callback page.** The
   handler's other three branches — linking to an existing password account, a
   returning sign-in, the unverified-address rejection — remain exactly as
   unexercised as HANDOVER §3.4 records. Linking to an existing password account
   is still the one worth testing deliberately.

4. **Nothing has been looked at.** No responsive check at the breakpoints, no
   visual review, no screen reader, no automated a11y run. The keyboard
   behaviour in the avatar menu is written and unit-tested for structure, not
   driven by hand.

5. **`NEXT_PUBLIC_GOOGLE_SIGN_IN` is duplicated configuration.** The failure
   modes are visible rather than silent — set without the backend configured and
   the button 404s; unset with it configured and the button is absent while
   password sign-in works. It exists because the backend has no endpoint saying
   which providers are configured, and adding one was outside the single backend
   change approved for this phase. Phase 7 should add
   `GET /api/v1/auth/providers` and delete this variable.

6. **`npm audit` reports 2 vulnerabilities (1 high) in `postcss`**, reached
   through `next`. Pre-existing, not introduced here, and the fix is Next 16 — a
   major upgrade, and phase 9's call.

## 8. Environment variables introduced

| Variable | Default | Notes |
|---|---|---|
| `NEXT_PUBLIC_GOOGLE_SIGN_IN` | `false` | Draws the Google button. Build time — `docker compose build frontend`. Must agree with `GOOGLE_CLIENT_ID`. See §7.5. |

Added to `.env.example`, `docker-compose.yml` and `frontend/Dockerfile`.

Two browser cookies are now written by the frontend, and neither is a
credential: `hhf_session_hint` (a boolean, 30 days) and `hhf_return_to` (a
same-origin path, 5 minutes, cleared on use). `hhf_refresh` remains the
backend's, httpOnly, and unreadable from here.

## 9. Still open

Carried forward, with movement:

1. ~~**`/login` still 404s.**~~ **Closed** — `/login`, `/register`,
   `/auth/callback` and `/account` all exist and serve.
2. ~~**Phase 6 must render the registration message from phase 5 §4.**~~
   **Closed** — `/register` renders `RegistrationResponse.message` verbatim with
   a link to `/login`, and `RegisterForm.test.tsx` fails if either is dropped.
3. **`/support` still 404s.** Phase 8.
4. **Contact address is still a placeholder** — `lib/contact.ts` line 20.
   `/account` now links to `/contact` for account removal, so the placeholder is
   on one more path than it was.
5. **Helpline re-verification cadence** still undecided.
6. **Privacy policy still needs legal review.**
7. **Safety-list review** — the Hinglish set still needs a native speaker.
8. **Refresh token pruning** has no job. Phase 9.
9. **New — the browser-side checks in §7.** Items 2 and 3 there are one browser
   session's work and should be done before phase 7 builds on this.
10. **New — `GET /api/v1/auth/providers`**, to delete
    `NEXT_PUBLIC_GOOGLE_SIGN_IN`. Phase 7.
11. **New — `/admin/feedback` is linked from the avatar menu for ADMIN accounts
    and does not exist yet.** Phase 7 builds it. The link is there now because
    the role already exists and an admin with no route to their own queue is a
    link somebody adds badly later.

---

# `/login` drew no Google button — the third wiring defect of the same class

## 1. The symptom

`/login` rendered straight into the Email field. No Google button, no divider,
on a stack where the backend was fully configured and a real Google sign-in had
already completed through it — the one recorded two sections above.

## 2. Cause

Not a missing component. `LoginForm.tsx` line 90 draws the button and the
divider together behind `GOOGLE_SIGN_IN_ENABLED`, which is
`process.env.NEXT_PUBLIC_GOOGLE_SIGN_IN === "true"`, inlined at build time.

Pulled out of the running container, the shipped chunk read:

```js
6940:(e,t,n)=>{ ... let a=!1, s="".concat(r.JR,"/oauth2/authorization/google"); ... }
```

`a` is `GOOGLE_SIGN_IN_ENABLED`, baked `false`.

This time `docker-compose.yml`, `frontend/Dockerfile` and `.env.example` were
all correct — the phase 6 work had plumbed the variable properly, as a
`build.args` entry rather than an `environment` one. The break was one step
further out: the operator's gitignored `.env` never got the key. It was added to
`.env.example` during phase 6, *after* that `.env` was written from an earlier
copy, so compose fell through to `${NEXT_PUBLIC_GOOGLE_SIGN_IN:-false}` and
baked the default.

Diffing key sets, it was the only key in `.env.example` absent from `.env`.

## 3. The third instance, and what closes the class

| # | Phase | Documented | Wired into compose | Set in `.env` | Found by |
|---|---|---|---|---|---|
| 1 | 5 | yes | **no** | yes | loading a page |
| 2 | 5 | yes | **no** | yes | an admin promotion that never happened |
| 3 | 6 | yes | yes | **no** | loading a page |

Same shape every time: hand-edited files that have to agree, with nothing
holding them together, and the disagreement surfacing in a browser rather than
in the suite. Phase 5 §5 item 3 already named the missing check. This entry
builds it.

`EnvExampleComposeDriftTest` (backend, `mvn test`, no Spring context, ~0.1s)
asserts both directions: every `APP_*`, `GOOGLE_*` and `NEXT_PUBLIC_*` key in
`.env.example` reaches its compose service, and compose interpolates no such key
that `.env.example` fails to document. The routing table is part of the
assertion — `NEXT_PUBLIC_*` must land in the frontend's **`build.args`**, not its
`environment`, because a value placed under `environment` would be accepted by
compose, would appear in `docker compose exec frontend env`, and would still be
missing from the bundle the browser downloads.

It lives in the backend suite for the dull reason that snakeyaml is already on
the classpath there and nothing else in the repo runs on every build. There is
still no CI; when there is, this is one of the things it must run.

**It was checked against all three bugs**, because a drift test that has never
failed is the vacuously-passing negative test phase 5 §3 warned about:

| Compose mutated to | Result |
| --- | --- |
| `APP_*`/`GOOGLE_*` stripped from backend `environment` (the phase-5 state) | fails, names `APP_CORS_ALLOWED_ORIGINS` first |
| `NEXT_PUBLIC_GOOGLE_SIGN_IN` removed from `build.args` | fails, names it and the service |
| the same key moved to the frontend's `environment` | **still fails** — the near-miss that would have looked right in review |
| unmodified | passes |

What it cannot do is read `.env`, which is gitignored and rightly absent from
CI — and instance 3 lived exactly there. What it closes for that case is
narrower but real: `.env.example` stays a complete template, so an operator
diffing against it sees the missing key.

`GoogleSignInGate.test.tsx` holds the other half — the component gate itself,
on both forms, for `"true"` / `"false"` / unset / `"1"`. Under vitest
`process.env.NEXT_PUBLIC_*` is an ordinary runtime lookup, which is what makes
`vi.stubEnv` work there and why it proves nothing about a real bundle. The two
tests are deliberately split along that line. Checked against a broken gate:
hardcoding `GOOGLE_SIGN_IN_ENABLED = true` fails 6 of its 8 tests.

## 4. Verification actually run

| Check | Result |
| --- | --- |
| `mvn test` (backend) | **46 tests, 0 failures** — 44 before, plus the two new ones |
| `npm test` (frontend) | **46 tests, 0 failures** — 38 before, plus the eight new ones |
| `npm run typecheck` / `npm run lint` | clean |
| `docker compose build frontend` with `.env` set to `true` | chunk `page-c87f5ae72fc5c2bb.js`, gate baked **`let a=!0`** |
| `docker build --build-arg NEXT_PUBLIC_GOOGLE_SIGN_IN=false` | chunk `page-bc469799636594a0.js`, gate baked **`let a=!1`** |
| running container after `docker compose up -d frontend` | serves `page-c87f5ae72fc5c2bb.js`, `let a=!0`, healthy |

The false-build chunk hash is byte-identical to the one the broken container was
serving before the fix, which is what makes the A/B a controlled one rather than
two builds that merely differ: the same content addresses to the same hash, so
the flag is demonstrably the only input that changed.

**Not verified in a browser.** `/login` renders its form client-side inside a
`Suspense` boundary, so the served HTML carries the fallback either way and
`curl` cannot see the button. A jsdom hydration harness was tried and abandoned
— it failed to hydrate Next 15 / React 19 at all, producing no email field
either, so it was evidence of nothing. The bundle A/B plus the component tests
cover both halves of the mechanism, but nobody has yet watched the button appear
on screen. That is one page load and should be the first thing done next.

## 5. Environment variables

No new variable. `NEXT_PUBLIC_GOOGLE_SIGN_IN` was already in `.env.example`,
`docker-compose.yml` and `frontend/Dockerfile`, all correct and all with the
build-time note. Added in this entry:

- `.env` — `NEXT_PUBLIC_GOOGLE_SIGN_IN=true`, the actual fix.
- `frontend/.env.local` — so `npm run dev` draws the button too. Gitignored by
  the root `.gitignore`'s `.env.*`, which has no slash and so matches at any
  depth. It is a **fourth** copy of this setting, and the drift test cannot see
  it either; that is an argument for §6, not against the file.

## 6. Still open

1. **`GET /api/v1/auth/providers` deletes this whole class for Google.** Carried
   from phase 6 §9 item 10, now with a third instance behind it. The flag exists
   only because the frontend cannot ask the backend what is configured. One
   endpoint removes `NEXT_PUBLIC_GOOGLE_SIGN_IN` from `.env`, `.env.example`,
   `docker-compose.yml`, `frontend/Dockerfile` and `frontend/.env.local` at
   once, and makes both new tests unnecessary rather than merely passing.
2. **Watch `/login` in a browser.** See §4.
3. **There is still no CI.** Both suites pass locally and neither runs anywhere
   else. The drift test is only as good as the thing that invokes it.

---

# A custom header on `/refresh` and `/logout`

## 1. The change

`POST /api/v1/auth/refresh` and `POST /api/v1/auth/logout` now require
`X-Requested-With: fetch`. Without it: `403`, code `CSRF_HEADER_REQUIRED`, and
nothing rotated, spent or revoked.

Those two are the only endpoints whose sole credential is the `hhf_refresh`
cookie, which the browser attaches by itself. Everything else either carries its
credential in the body (`/register`, `/login`) or requires a Bearer token script
had to attach deliberately (`/me`). A cross-site `<form>` cannot set a header,
and setting one from script must first clear a CORS preflight against the origin
allowlist — so the requirement is the check.

Four files carry it: `CsrfHeaderFilter`, its installation and the CORS
`allowedHeaders` entry in `SecurityConfig`, and one line in `lib/api.ts`.

## 2. A correction to the premise this was requested under

**The refresh cookie is `SameSite=Strict`, not `Lax`.** `RefreshCookie` line 67,
unchanged since phase 5, and the phase 5 log records it at three separate
points. Only `hhf_session_hint` is `Lax`, and that is the literal string `"1"` —
not a credential, and `session-hint.ts` says so at length.

So the attack as described — a malicious page causing a signed-in visitor's
browser to call `/refresh` — **does not currently work in any current browser**.
The cookie is not attached cross-site at all. This change did not close an open
hole.

It is still worth having, for one specific reason. `RefreshCookie`'s own comment
and PROJECT_BRIEF §5 both record that `SameSite=Strict` is conditional: the day
the frontend and backend move to different registrable domains it must become
`SameSite=None; Secure`. On that day the described attack becomes real and
severe, and the only thing standing in front of it is this filter. Adding it
afterwards means shipping the hole first and remembering later. It is a second,
independent lock on a door whose first lock is documented as temporary.

**There is also no "phase 6 CSRF item" to close.** The request said to note that
this closes one; the log has none. Searching every phase for `csrf`,
`cross-site` and `forgery` returns exactly one hit — a line in the phase 1 file
tree reading "CSRF off". What this change does relate to is phase 6 §7 item 2,
which remains open and is not closed by this: whether a `SameSite=Strict` cookie
survives the cross-port XHR has still never been observed in a browser. That
question is now less load-bearing than it was, because the header no longer
depends on the answer — but it is not answered.

## 3. Decisions

**403 with its own code, not 401.** A client has to distinguish "you forgot the
header" from "your credentials are bad": the first is a caller bug, the second
means sign in again. Conflating them sends someone to re-authenticate over a
missing header, or retries forever over a dead session. `CsrfHeaderIT` asserts
both codes on the same endpoint in the same test, so they cannot silently merge.

**Not a `@Component`.** Spring Boot auto-registers beans of type `Filter` into
the plain servlet chain, which runs before Spring Security's and therefore
before its `CorsFilter`. A rejection from out there carries no CORS headers, so
a browser would see an opaque CORS failure instead of the section 6 body. The
filter is constructed by `SecurityConfig` and installed in exactly one place.

**Anchored to `JwtAuthenticationFilter`, not to `UsernamePasswordAuthenticationFilter`.**
Two `addFilterBefore` calls against the same anchor leave their relative order
incidental. Anchoring the second to the first states it: the header is checked
before any token is parsed.

**`ApiErrorWriter` extracted.** `SecurityErrorHandler` already existed to render
filter-chain rejections in the section 6 shape, and its own comment says both
its responses live in one class so they "cannot drift apart". This filter is a
third such rejection. Rather than hand-roll a third copy of the serialisation,
it moved to one component both use. `SecurityErrorHandler`'s behaviour is
unchanged; only the copy is gone.

**The header is sent on every frontend request, not just the two.** The
alternative is a per-path rule in `api.ts` that has to stay in step with
`CsrfHeaderFilter`, which is the drift this repo keeps paying for. Sending it
everywhere costs nothing: `/api/v1/vent/**` neither requires it nor rejects it,
so rule 2.2 is untouched and any other client still works with no ceremony.

## 4. Verification actually run

`mvn verify` — **46 unit + 81 integration, 0 failures** (72 integration before;
`CsrfHeaderIT` adds 9). `npm test` 46 passed, typecheck and lint clean.

The guard was checked against its own removal, not merely watched to pass:

| Mutation | Result |
| --- | --- |
| `isGuarded` forced to `false` | 3 of 9 fail — both rejection tests and the distinct-code test |
| `X-Requested-With` dropped from `allowedHeaders` | `preflightAllowsTheHeader` fails: the **preflight itself** 403s, which is precisely the "unreachable from a browser, fine from curl" failure |
| unmodified | 9 pass |

Then against the running stack, `docker compose up -d --build backend frontend`:

| Check | Result |
| --- | --- |
| `POST /refresh` without the header | **403** `CSRF_HEADER_REQUIRED` in the section 6 shape |
| `POST /logout` without the header | **403** `CSRF_HEADER_REQUIRED` |
| both with the header | **200** / **204** |
| preflight, `Access-Control-Request-Headers: x-requested-with` | **200**, `Access-Control-Allow-Headers: x-requested-with, content-type` |
| `POST /vent/release`, no header at all | **204** |
| `GET /vent/stats`, no header at all | **200** |
| full lifecycle with `Origin: http://localhost:3000` — register, login, refresh, refresh, logout | 201 / 200 / 200 / 200 / 204 |
| refresh after logout | **401 `UNAUTHORIZED`** — distinct from the 403, on the wire |
| `X-Requested-With":"fetch"` in the shipped frontend bundle | present in 3 chunks |

The rejection responses carry no `Set-Cookie`, and `CsrfHeaderIT` asserts the
refused call left the token spendable by refreshing successfully with the same
cookie afterwards. A guard that refused the request *after* consuming the token
would hand an attacker the forced-rotation outcome while looking like it worked.

## 5. NOT verified

**No browser was driven.** The request asked for sign-in, refresh-on-reload and
sign-out to be confirmed in a real browser, and that has not happened — the same
gap as the previous two entries, for the same reason: nothing here can drive
one, and the jsdom harness tried last time cannot hydrate Next 15 / React 19.

What was done instead is the full lifecycle over HTTP with the browser's own
`Origin` header, plus proof that the header is baked into the shipped bundle.
That exercises the server side of every request a browser would make and the
client side of none of them. Specifically still unobserved: that the preflight
and the real request pair up correctly in a browser, and — carried from phase 6
§7 item 2 — that the `SameSite=Strict` cookie is replayed cross-port at all.
Three page loads would close both.

## 6. Still open

Unchanged from the previous entry: the providers endpoint, the browser pass, and
the absence of any CI. This change adds nothing new to that list.

---

# Phase 7 — Feedback

**Completed:** 2026-09-07
**Scope:** PROJECT_BRIEF.md §9 row 7 — submission, public list, moderation
endpoints, admin queue UI. Closes the `/admin/feedback` 404 the avatar menu has
linked to since phase 6.

---

## 1. The rule this phase could have broken

Feedback is stored. Vent text is not. Nothing may connect the two.

There is no foreign key, no shared identifier, and no column in `feedback` that
refers to `vent_events`. `ModuleBoundaryArchitectureTest` now fails the build if
either package so much as imports a type from the other, in either direction.

**The non-obvious half is the timestamp.** The form lives on `/vent/released`,
so in the ordinary case a submission lands a minute or two after a release. Two
tables with full-precision `created_at` would pair them for anyone holding a
dump — a release at 14:32:10 and a note at 14:33:45, at this site's traffic, is
one person. So `feedback.created_at` is **truncated to the hour on write**, and
the public endpoint returns no timestamp at all.

Verified on the running stack: a note submitted at 10:22 stored as
`2026-09-07T10:00:00Z`.

The cost is real and was accepted deliberately: "submitted 3 minutes ago" does
not exist, and two notes in the same hour have no defined order between them.
`moderated_at` keeps full precision — it records a moderator acting on their own
queue, not a visitor arriving, so there is nothing to correlate it against.

## 2. Decisions

**Message floor is 10, not 20.** The instruction suggested 20. "Thank you." is
ten characters and "This helped a lot." is nineteen, and on this page those are
the likeliest honest answers. A floor that rejects them is set against the
users.

**`<3` is allowed.** Markup is rejected rather than sanitised, as instructed,
but the rule is tag-shaped `<` only — a bracket followed by a letter, slash,
bang or question mark — plus HTML entities, so `&lt;script&gt;` cannot be
smuggled past for something downstream to decode. Rejecting every `<` would have
been simpler and would have failed people writing the most common affectionate
thing there is, on a site about feelings.

**Location refuses commas.** One place, 60 characters, one line, with the
placeholder (`Mumbai`) teaching the format. The comma rule is what stops the
field becoming an address assembled next to somebody's name on a public page.
Both name and location carry a notice, beside the fields rather than in the
preamble, saying they are published — a form field is assumed private until it
says otherwise, and someone filling one in has stopped reading the intro.

**The display name is prefilled in the browser, not on the server.** It keeps
`feedback` free of any dependency on `auth` (§4), and it means renaming an
account later cannot rewrite a name already published under the old one. It also
makes "signed in does not mean attributed" structural: an empty field posts an
anonymous note from an authenticated request, and the row still records
`user_id` so a removal request can be matched.

**Reversals are last-write, not history.** `moderated_at` and `moderated_by` are
overwritten on each decision. **If reversals ever need a history rather than a
last write, that is a `feedback_moderation_events` table**, appended to per
decision, and this is the note saying so. Today the queue answers "who published
this, and when", which is what the Community Guidelines removal promise
requires.

## 3. The first `@PreAuthorize`, and a finding about testing it

`AdminFeedbackController` carries the project's first method security, closing
the phase 5 open item. `AdminFeedbackAuthorisationIT` pins all three outcomes:
anonymous **401**, authenticated USER **403**, ADMIN **200**.

The instruction also asked for a test asserting an anonymous
`GET /api/v1/admin/feedback` returns 401 "so the ordering can't silently
regress". That test was written — and **it does not do that job**, which was
found by trying it rather than assuming it.

The exact regression was simulated: `/api/v1/**` added to `PUBLIC_PATHS` *and*
the admin matcher moved below it. **All six tests still passed.** They passed
because `@PreAuthorize` caught the request after the filter chain let it
through. That is the belt-and-braces design working correctly, and it is
genuinely reassuring — one mistake is not a breach. It also means no
request-level test can tell the two layers apart, so none can notice when one
silently stops contributing.

`AdminPathsAreNotPublicTest` closes it properly: a plain unit test asserting
that no pattern in `PUBLIC_PATHS` matches an admin URL, whatever order the
matchers are declared in. Against the simulated regression it fails with
`Public path "/api/v1/**" matches admin URL "/api/v1/admin/feedback"`. Both
locks are now independently guarded.

## 4. The moderation screen

It shows unfiltered writing from strangers on a mental-health site, and whoever
reviews it may read a lot in one sitting. One at a time by default: a compact
list of `<details>` rows, each showing a rating and a one-line clamped preview,
with the full note only when opened. Nothing polls, nothing animates in, nothing
arrives mid-read.

**The helplines are worded for the moderator**, not for the person who wrote in.
The visitor-facing framing would be worse than useless here because it implies
the moderator could pass them on, and they cannot — submissions carry no contact
details of any kind, by design. The panel says that plainly: you cannot reply,
some of this will stay with you, that is an ordinary response to the work, and
these lines are open to you too.

## 5. Verification actually run

`mvn verify` — **48 unit + 106 integration, 0 failures** (46 + 81 before).
`npm test` — **59 passing** (49 before). `npm run build`, `npm run lint`,
`npm run typecheck` all clean; 19 static pages, `/voices` and `/admin/feedback`
both generated.

> `./mvnw` cannot run: `.mvn/wrapper/` holds only `maven-wrapper.properties`,
> with no `maven-wrapper.jar`, so the wrapper dies with a classworlds launcher
> error. `mvn` from the PATH was used instead. **The wrapper is broken for
> anyone cloning this repo** and phase 9 should fix it.

Guards were checked against their own removal rather than watched to pass:

| Mutation | Result |
| --- | --- |
| `/api/v1/**` public + admin matcher moved below it | `AdminPathsAreNotPublicTest` fails, naming the pattern; the six IT tests do **not** (see §3) |
| accessible names removed from the rating radios | 3 of 6 `RatingInput` tests fail |

Then against the running stack, `docker compose up -d --build`:

| Check | Result |
| --- | --- |
| anonymous submission | **201**, stored PENDING, `user_id` null |
| does it appear on `/voices`? | **no** — `items: []` |
| approve as ADMIN | **200**, status APPROVED, `moderated_by` set to the moderator's id |
| does it appear now? | **yes**, carrying only rating/message/name/location |
| reject | **200** — gone from `/voices`, row retained in the queue as REJECTED |
| signed-in USER on the admin endpoints | **403 FORBIDDEN**, not 401 |
| anonymous on the admin endpoints | **401 UNAUTHORIZED** |
| 4th submission in an hour | **429 RATE_LIMITED** with `Retry-After` |
| `/vent`, `/vent/released`, `/voices` signed out | **200** each |
| `POST /vent/release` with no account and no header | **204**; stats **200** |
| `created_at` as stored | `2026-09-07T10:00:00Z` for a 10:22 submission — truncation confirmed |

The ADMIN used the documented bootstrap path — register, set
`APP_ADMIN_BOOTSTRAP_EMAILS`, restart — and the log line
`Promoted account f6cb6d35… to ADMIN` confirms it. **The variable was reverted
to empty afterwards, but `mod@example.com` remains ADMIN in the local dev
database**, because the promotion is persisted and clearing the variable does
not demote. That account and the test rows are dev-only artifacts.

## 6. NOT verified

1. **No browser was driven, again.** This is the fourth phase in a row carrying
   this gap and it is now the largest thing wrong with this project's process.
   Everything below follows from it.
2. **No breakpoint check at any width.** 320, 375, 414, 768, 1024, 1440 and 1920
   are all unverified. Highest risk: the two-across name/location row at its
   `sm` threshold, the queue row's truncated preview at 320, and the star row
   with 44px tap targets at 320, where five of them plus padding is most of the
   viewport.
3. **The rating control has never been operated by a real keyboard or a real
   screen reader.** `RatingInput.test.tsx` proves it is built from named native
   radios in a fieldset — the structure that makes the announcement correct, and
   the thing a button row fails — but jsdom has no accessibility tree and no
   real focus model. The arrow-key test exercises Testing Library's synthetic
   events, not the browser's radio-group behaviour. This is exactly the failure
   the instruction called out, and it remains open.
4. **No visual review.** Nobody has looked at `/voices`, the invitation on
   `/vent/released`, or the queue.
5. **The empty state has been seen only as JSON.** `/voices` returns 200 and the
   list is empty; nobody has watched the page render that.
6. **Concurrent moderation is untested.** Two moderators PATCHing the same row
   race, and last-write-wins is assumed rather than asserted.
7. **`npm audit` still reports the pre-existing postcss advisories** reached
   through `next`. Unchanged by this phase; phase 9's call.

## 7. Still open

1. **The browser pass**, carried forward and now four phases old. Items 2–5
   above are one session's work.
2. **`GET /api/v1/auth/providers`** to delete `NEXT_PUBLIC_GOOGLE_SIGN_IN`.
3. **No CI.** Both suites pass locally and nothing runs them anywhere else.
4. **`./mvnw` is broken** — missing wrapper jar. See §5.
5. **A moderation history table**, if reversals ever need more than a last
   write. See §2.
6. `/support` still 404s (phase 8). The contact address is still a placeholder.
   Helpline re-verification cadence still undecided. Privacy policy still needs
   legal review. The Hinglish safety list still needs a native speaker. Refresh
   token pruning still has no job.

---

# Phase 8 — Support

**Completed:** 2026-09-07
**Scope:** PROJECT_BRIEF.md §9 row 8 — the `/support` page, the footer link, the
About section. **No endpoint and no table**; see §1 below. Closes the last 404
in the build.

---

## 1. `donation_intents` was removed rather than built

The brief specified a fifth table and `POST /api/v1/donations/intent`. Both are
gone from §5 and §6, each with a dated amendment.

The decisive argument is the flow of funds. There is no payment provider in
this project and one cannot be added by a developer — taking money in India
needs the owner's bank account, PAN and KYC — so **nothing could ever close an
intent row**. It is permanently write-only: a figure somebody typed, kept
forever, reconciled against nothing, in a project that truncates feedback
timestamps to the hour and refuses to store an IP address at all.

Two supporting reasons, recorded because they are the ones that would come back
if this is ever revisited:

**It could not be built honestly.** The only truthful interface over an endpoint
that records an intent and takes no money is one that says so — at which point
nobody uses it and the table collects an empty set. The alternative is a form
that returns 201 to someone who typed ₹500 and believes they have given
something. There is no version that is both used and honest.

**It guaranteed a rewrite.** `amount_minor`/`currency`/`status`/`provider_ref`
is a sketch of what a provider hands you, and Razorpay, UPI intent flows and
Stripe each have different reconciliation models and status vocabularies.
Whatever was built now would be replaced wholesale on the day a provider was
chosen — a migration to add it and a second to undo it.

`backend/.../donation/package-info.java` is **kept and empty**, with the reason
in its javadoc, so the decision is visible where someone would go looking for
the code. §4's repo layout comment now reads `# empty — see the §5 note`.

## 2. The payment method: two lines, and why that matters

`frontend/src/lib/support.ts` mirrors `lib/contact.ts`:

```
line 30   export const SUPPORT_UPI_ID = "REPLACE-ME@example.invalid";
line 41   export const SUPPORT_PAYMENT_IS_PLACEHOLDER = true;
```

**Both must change in the same edit**, and they fail in opposite directions if
only one does. A real ID with the flag still `true` hides the payment details,
so nobody can give anything. A cleared flag with the placeholder ID still there
publishes an invalid address — and money sent to a valid-but-wrong VPA reaches
a stranger, so that direction loses somebody's money rather than merely
failing. Neither is visible in a one-line diff, which is why both are pinned by
tests.

Nothing was guessed at: no UPI ID, no bank account, no Razorpay key, no
donation link. The `upi://` deep link is derived from the constant rather than
written separately, so there is no second copy to go stale, and it carries **no
`am=` parameter** — that would prefill a figure in the payment app, which is a
suggested amount arriving through the back door.

## 3. The notice is worded for a visitor

Not "payment not configured", which is a status line about the software. The
person reading it came here meaning to give and needs to know that they cannot,
that they have not done anything wrong, and that nothing is owed:

> **There is no way to give anything yet**
>
> No payment method has been set up, so nothing can be sent to this site at the
> moment — and that is fine. Everything here works and stays free regardless. If
> you came to this page meaning to help, the thought is the part that was going
> to be worth anything anyway.

No apology and no "coming soon" — one is theatre, the other is a promise about a
date nobody has set. While the flag is `true` **no payment details render at
all**, so a half-configured page cannot read as a working one.

## 4. What the page does not do

Absent by instruction and by judgement, each with a comment at the point where
someone would add it: progress bar, goal, total raised, donor count, urgency or
scarcity, deadline, suggested amounts, tiers, rewards, memberships, badges.

**And no figures.** The costs are named as categories — a server, a domain, a
database — and the page says out loud that no number is given because there is
not a verified one to give. An invented "under ₹2,000 a year" would break the
promise the opening paragraph makes two sentences earlier. The comment in the
file says a number here needs to come from a real invoice.

Guilt was the likeliest way to get this subtly wrong, including its gentle
registers — "if you can spare it", "every little helps", "only if it has been
useful". Each turns not giving into a small failure. `SupportPage.test.tsx`
asserts against those phrases specifically.

## 5. Reconciling the two entry points

The About section previously listed what support pays for, and the new page said
the same thing in different words — two copies of one claim, which is how they
drift until the site contradicts itself about money. `/support` is now the
single source of truth for the detail; `DonationSection` says only enough to
explain its link.

The released page's line — "This runs on a small server and stays free. You can
help pay for it if you want to, and nothing changes if you do not" — was checked
against the new copy and still reads true. It was not changed.

## 6. Verification actually run

`mvn verify` **48 unit + 106 integration, 0 failures**. `npm test` **74
passing** (67 before, 7 new). `npm run build`, `lint`, `typecheck` clean.

The support tests were checked against their own removal rather than watched to
pass:

| Mutation | Result |
| --- | --- |
| payment details rendered regardless of the flag | 2 tests fail |
| "if you can spare it, every little helps" added to the copy | the forbidden-phrase test fails |
| unmodified | 7 pass |

Then against the running stack, `docker compose up -d --build`:

| Check | Result |
| --- | --- |
| `/support` | **200** |
| footer "Support this space" | resolves — `href="/support"` present, target 200 |
| About → "How to support this space" | resolves, target 200 |
| the placeholder notice in the served HTML | present: "There is no way to give anything yet", "and that is fine" |
| `REPLACE-ME@example.invalid` in the served HTML | **absent** — 0 occurrences |
| `<title>` and meta description | "Support this space · HeadHeartFreeS", real description |
| **every route in the app** | 15 routes, **all 200** |
| `/nope` as a control | **404**, so the check above is not vacuous |
| linked-but-missing routes, diffed across the whole source | **none** |

**This was the last 404.** Every internal `href` in the codebase now has a
matching route, verified by diffing all links against all pages rather than by
clicking.

## 7. NOT verified

1. **No browser was driven — fifth phase running.** The standing gap.
2. **No breakpoint check** at 320, 375, 414, 768, 1024, 1440 or 1920. This page
   is static prose with no interactive control and one link, which makes it the
   lowest-risk page in the project for that gap — but it is still unverified,
   and the `<code>` block holding the UPI ID at 320px is the one element worth
   a look, since a long VPA could overflow.
3. **No visual review.** Nobody has looked at the page.
4. **The configured state has only been rendered in jsdom.** The tests cover it
   by mocking the module, but nobody has set a real UPI ID and seen the payment
   block on a real page — because doing so would require inventing an
   identifier, which was the one thing ruled out.
5. **The `upi://` link has never been opened by a payment app.** It is
   well-formed per the UPI deep-link spec and carries `pa`, `pn` and `cu`, but
   whether a given Indian payment app accepts it is untested and untestable
   without a real VPA.
6. **`npm audit`** still reports the pre-existing postcss advisories via `next`.

## 8. Still open

1. **The browser pass**, now five phases old and the largest process gap in the
   project.
2. **The owner must supply a payment method** — HANDOVER §2.7, two lines.
3. **The owner must resolve the legal and tax position** before enabling
   payments — HANDOVER §2.8: individual vs registered entity, income tax
   treatment, FCRA if funds ever arrive from outside India, and 80G. **Nothing
   on the page states or implies any tax status, and nothing may be added that
   does** without the registration behind it.
4. **A QR code** is the natural next step once a real UPI ID exists, generated
   from the `upi://` link the page already builds. Not added now, because a QR
   encoding an invalid address is worse than none.
5. **No CI.** Both suites pass locally and nothing runs them anywhere else.
6. **`./mvnw` is broken** — missing wrapper jar. Carried from phase 7.
7. `GET /api/v1/auth/providers` to delete `NEXT_PUBLIC_GOOGLE_SIGN_IN`. The
   contact address is still a placeholder. Helpline re-verification cadence
   still undecided. Privacy policy still needs legal review. The Hinglish safety
   list still needs a native speaker. Refresh token pruning still has no job.

---

# Phase 9 — Hardening

**Completed:** 2026-09-08
**Scope:** PROJECT_BRIEF.md §9 row 9 — security headers, CORS lockdown, OpenAPI
restriction, GitHub Actions, Lighthouse, accessibility audit. Plus the proxy-aware
rate limiter, refresh-token cleanup, and making the documentation true.

**This is the last build phase.** §11 is the production-readiness summary, and it
is the section to read before deciding whether to deploy.

---

## 1. The CSP, and the decision that was deliberately not taken

### What shipped

```
default-src 'self';
script-src  'self' 'unsafe-inline';
style-src   'self';
img-src     'self' data:;
font-src    'self';
connect-src 'self' <API origin>;
frame-ancestors 'none';
base-uri 'none'; object-src 'none'; form-action 'self'
```

Built from what the app actually loads, checked first rather than assumed:

- **~13 inline `<script>` blocks per page** carrying the RSC flight payload
  (`self.__next_f.push`). Not optional, and their content changes per page and
  per build, so hashes are impractical.
- **Zero `<style>` tags, zero `style=` attributes, zero `style={{}}` in source**
  across every route. So `style-src 'self'` carries **no `'unsafe-inline'`**.
  Most Next applications cannot say that and it is worth not giving up.
- The paper-grain overlay is a `data:` SVG in a CSS `background-image`, which is
  governed by `img-src`, not `style-src` — hence `img-src 'self' data:`.
- `next/font` self-hosts, so `font-src 'self'` with nothing from Google.
- The frontend and backend are **different origins**, so `connect-src` names the
  API explicitly. Omitting it blocks every request, and does so silently from
  the user's side.

### Why `'unsafe-inline'` on script-src, and what would reverse it

**A per-request nonce with `'strict-dynamic'`, set from middleware, is the
stronger policy.** This section exists so that decision can be reversed
knowingly rather than rediscovered.

**What the nonce would buy.** With `'unsafe-inline'`, a script tag injected into
the page executes. With a nonce, it does not — the browser runs only scripts
carrying the per-request value, so an injection that reaches the DOM is inert.
That is the single largest weakness in the policy above, and it is not a
theoretical one in general.

**Why it was not taken here.** Next opts every page out of static generation
when a nonce is used, because the value must differ per request. This site is
almost entirely static: 19 prerendered routes, most of them prose. The cost is
losing that, plus HTML CDN caching, plus middleware running on every request
that the eventual owner would have to understand and maintain.

Against that, the XSS path the nonce defends **does not currently exist in this
application**:

- no `dangerouslySetInnerHTML` anywhere in the codebase
- React escapes every interpolation, and all user content is rendered as text
- the backend rejects markup at the boundary (`NoHtml`), so a tag cannot be
  stored in the first place
- the only user-generated content that reaches a public page — feedback — passes
  human moderation before it is published

**Reverse this decision the moment any of those stops being true.** Concretely:
a rich-text field, an embed, any third-party script, any HTML rendered from user
input, or dropping moderation from the publish path. At that point the nonce is
worth its cost, and the work is a `middleware.ts` that generates a value per
request and sets the header — Next wires it into its own inline scripts from
there.

### Verified, not assumed

A **`Content-Security-Policy-Report-Only` pass ran first**, on a build made for
the purpose, with a `securitypolicyviolation` listener installed before each
document loaded so parse-time violations were caught and not just later ones.
Across **14 routes: 0 violations**, and an explicit cross-origin `fetch` to the
API returned **200**, which is the one thing a page load cannot demonstrate on
its own. The policy was then enforced and the same audit re-run against the
shipped build: **0 violations again**.

Then the flows, because a blocked request fails silently: signing in through the
real form, landing signed in, the avatar menu rendering, and a reload on
`/account` still signed in with the address shown — that last one exercises
refresh-on-load, the call most likely to be blocked by a wrong `connect-src`.

## 2. The other headers

| Header | Value | Why |
|---|---|---|
| `X-Content-Type-Options` | `nosniff` | Standard; no cost. |
| `X-Frame-Options` | `DENY` | Redundant with `frame-ancestors` on current browsers, kept for older ones. |
| `Referrer-Policy` | `no-referrer` | See below. |
| `Permissions-Policy` | camera, microphone, geolocation, payment, USB, MIDI, sensors, autoplay, display-capture, `browsing-topics` all `()` | The app uses none of them, so each is free to give up. `browsing-topics` opts out of interest inference, which a mental-health site should not participate in by default. |
| `Strict-Transport-Security` | **off** | `ENABLE_HSTS`, default false. Never sent over plain HTTP locally. |

**`no-referrer`, not the browser default.** The site loads no third-party
resources at all and has exactly **one** outbound link — `findahelpline.com` on
`/crisis-resources`. That single link is the whole argument:
`strict-origin-when-cross-origin` would still tell that site the visitor came
from here, and the pages someone reads on this domain (`/vent`,
`/crisis-resources`) are precisely the ones that should not appear in anybody
else's logs. Nothing here needs a referrer, so nothing is lost.

**HSTS is one-way.** A browser that has seen it refuses plain HTTP to the host
for `max-age` — one year as configured — and there is no way to retract it. Its
primary home is the TLS terminator; the flag exists so a deployment without one
is not left without the header. It goes on **last**, after TLS is confirmed
working. HANDOVER §3.7 says so in the place the owner will actually read.

## 3. Locked down

**Swagger UI and `/v3/api-docs` are off unless the `local` profile is active.**
They are a machine-readable map of every endpoint, its request schema and its
validation rules, served to anyone who asks, and the application warned about
this on every startup until now.

`ApiDocsDisabledOutsideLocalIT` pins it, and needed its own boot to do so: every
other test in this project runs under `@ActiveProfiles("local")` because
`JwtSecretGuard` refuses the committed development secret anywhere else — so the
entire suite had only ever observed the configuration where these are **on**.
That test starts the app the way a deployment does, with a real secret and a
non-`local` profile, and asserts 404 on both plus 200 on the real health
endpoint, so it cannot pass by failing to start.

**Actuator exposes nothing** (`management.endpoints.web.exposure.include: ""`).
`/api/v1/health` is a hand-written controller and is what the compose healthcheck
uses. `env` and `configprops` would have published which configuration and which
secrets are set.

**CORS: the app now refuses to start** on a wildcard origin, a schemeless origin,
a trailing slash or a blank entry. The subtle case is the reason: with Spring's
origin *patterns*, a wildcard **reflects the caller's origin back and allows
credentials**, which is a working cross-origin read of an authenticated API from
any site the pattern admits. This configuration uses `setAllowedOrigins`, which
does not interpret patterns, so the same value would instead silently match
nothing and break the frontend. One failure mode is a hole and the other is an
outage; both are invisible from outside, so it fails at startup where somebody
is watching.

**`/design-system` is deleted**, as §7 said it would be. It existed to build the
design system against, shipped no product surface, and an internal tool left on
a public origin is a page nobody maintains and everybody can read.

## 4. The rate limiter behind a proxy

Keyed on `getRemoteAddr()`, so under Docker every request appeared to come from
the gateway and all users shared one bucket — one person could exhaust the login
limit for everybody.

`ClientAddressResolver` honours `X-Forwarded-For` **only when the immediate peer
is listed in `app.rate-limit.trusted-proxies`**, and reads the chain **from the
right**, skipping trusted hops.

Both halves matter and they fail in opposite directions:

- Ignoring the header entirely: everyone behind the proxy shares one bucket — a
  denial of service against real users.
- Trusting it from anyone: **the limiter stops existing**, because a caller sends
  a different value per request and gets a fresh bucket each time.

Reading from the right is the second half of that. `X-Forwarded-For` is appended
to by each hop, so everything left of the entry *our* proxy added is
client-supplied — a caller sends a header with fabricated entries and the proxy
appends rather than replaces. Taking the leftmost value, which is the common
implementation, is exactly the bypass.

**The list is empty by default**, which reproduces the old behaviour precisely:
no header is ever honoured. The safe failure is the default; the owner must name
their proxy deliberately (HANDOVER §3.6).

**Buckets remain per instance.** They live in this process's heap, so two
backends behind a load balancer enforce the limit twice over and a caller gets N
times the allowance. **This holds for a single backend only.** A shared store
(Bucket4j supports Redis) is what changes that, and it is not done.

## 5. Refresh token cleanup — 30 days past expiry

`refresh_tokens` gained a row per refresh and nothing removed them; one active
person refreshing every fifteen minutes produces roughly 35,000 rows a year.

`RefreshTokenCleanup` runs hourly, deleting rows whose token expired more than
one refresh-TTL ago. The window is **derived from `APP_JWT_REFRESH_TOKEN_TTL`**
(30 days by default) rather than hardcoded, so lengthening the TTL lengthens
retention with it. A row therefore survives its full life plus another 30 days —
up to 60 in total.

**Why not just delete on rotation:** spent rows are the mechanism, not debris.
Rotation marks the old token revoked; if that row is later presented, two parties
hold tokens from one sign-in and the family is revoked. Delete it and the same
presentation is an unknown token — a plain 401, theft undetected, the thief's own
token still live. The retention window is a security parameter.

**Two clauses, and the second is the one easy to omit.** Expired-and-revoked is
the obvious case. Expired-and-*never*-revoked is the ending of every abandoned
session, which is the common ending — a rule of "expired AND revoked" would keep
those forever and the table would grow exactly as before. Deleting them is safe:
an expired token is refused on its expiry whether or not a row is found.

What it never touches is a row **revoked but not yet expired**, which is exactly
the population reuse detection reads. The predicate is on `expiresAt` alone, so
those are outside it by construction, and `RefreshTokenCleanupIT` asserts all
four cases.

Every instance runs the job; the delete is idempotent so a race is harmless. Any
job added here that is **not** idempotent needs leader election first.

## 6. Dependencies

`npm audit`: **2 advisories (1 high) in postcss**, reachable only as a transitive
dependency of `next` — XSS via an unescaped `</style>` in stringify output, and
arbitrary `.map` file read via an attacker-controlled `sourceMappingURL`.

**Both are build-time paths.** postcss runs when Tailwind compiles CSS during
`npm run build`, over CSS this repository controls. No user input reaches it, and
**it is not in the runtime image at all** — the standalone bundle contains
compiled CSS, not the compiler.

**Not fixed, deliberately.** `npm audit fix --force` installs **Next 16**, a
major upgrade that changes routing, caching and build behaviour. Doing that in
the final phase, with no time left to find what it broke, would trade a
contained build-time advisory for an uncontained runtime risk. It is written up
in HANDOVER §3.8 as something the owner inherits, with what it takes to close.

Backend: no dependency-check plugin was added. Spring Boot 3.5.x manages the
dependency versions and none are pinned below their managed version. **This is
weaker than a real scan and is stated as such** — no CVE database was consulted
for the Java side.

## 7. Accessibility — 100 across every route

Lighthouse (which bundles axe) over **all 14 routes**: **accessibility 100,
zero failures**, everywhere. Nothing needed fixing.

**What that does not mean.** An automated pass checks what a machine can check —
contrast ratios, names on controls, landmark structure, heading order, ARIA
validity. It cannot check whether the result is usable. These remain
**untested**, and they are the ones that matter most on this product:

- **Screen reader flow.** No screen reader has ever been run over this site.
  Axe confirms the rating control is a named radiogroup in a fieldset; it cannot
  confirm what NVDA or VoiceOver actually announces when a person arrives at it.
- **The star rating with a real screen reader.** Called out in the phase 7 log as
  the specific thing automated tools would miss, and it is still true.
- **Focus order in the moderation queue.** `<details>` rows that expand, a filter
  tab pair, and buttons that disappear after a decision. Axe sees a valid tree;
  whether the focus lands somewhere sensible after a row's button vanishes has
  not been observed.
- **Keyboard path through the vent flow** end to end, with an on-screen keyboard
  on a real phone, which §8 explicitly asked for and which nothing here can do.

## 8. Lighthouse

| Route | Performance | Accessibility | Best practices | SEO |
|---|---|---|---|---|
| `/` | **88** | **100** | **100** | **100** |
| `/vent` | **89** | **100** | **100** | **100** |
| `/voices` | **87** | **100** | **100** | **100** |

Performance sits in the high 80s on the same handful of items on every route:
render-blocking CSS, `legacy-javascript` (Next's own polyfill bundle), and LCP.
**Nothing was done about them.** Each fix is either a Next internal or would mean
removing something real — the self-hosted font, the stylesheet — and the
instruction was not to chase 100 by removing what matters. A 100 bought that way
would be a worse site with a better number.

## 9. CI — built, not confirmed

**There was no workflow.** The brief listed one under Infra from phase 1 and it
was never created; PHASE_LOG has carried "no CI" as open since phase 7. So this
phase wrote it rather than verifying it.

`.github/workflows/ci.yml`, four jobs: **backend** (`mvn verify`), **frontend**
(lint, typecheck, test, build), **wiring** (the env/compose drift check as its
own named job), **docker** (`docker compose build`).

The thing it is built against is a workflow that appears to run and does not —
the same class of bug as everything else this project has found:

- **`mvn verify`, not `mvn test`.** Failsafe runs the `*IT` classes: 114 of the
  180 backend tests. `mvn test` runs surefire only and would skip every
  integration test while printing BUILD SUCCESS.
- **Explicit count guards.** The backend job fails if fewer than 100 integration
  tests ran; the wiring job fails if the drift report is missing or has fewer
  than 2 assertions. If discovery silently breaks, the count collapses and the
  build goes red instead of green-on-nothing.
- **No `continue-on-error`, no `|| true`**, no step that swallows an exit code.
  Verified by parsing the YAML rather than by reading it.

**It has never executed.** There is no remote to push to from here, so the
workflow is syntactically valid, structurally checked, and **unrun**. The first
push will be its first real test, and that is the honest status. The count guards
are what make a false green unlikely, not impossible.

> **Superseded 2026-09-08.** It has now run. Three jobs passed and the wiring
> job failed — it invoked `surefire:test` directly, which compiles nothing, so
> on a fresh runner there were no test classes to find. See the addendum at the
> end of this log. The sentence above about the first push being the real test
> turned out to be the most accurate line in the section.

## 10. The rule, verified end to end in a browser

The last check, and the one the whole project rests on. Not a unit test: nine
phases have added auth, sessions, feedback, moderation and headers on top of that
page, and the question was whether any of it leaked in.

A real browser, signed out, cookies cleared. A sentinel string typed into the
textarea the way a person types — native value setter plus a bubbling `input`
event, because React ignores a direct `.value` assignment. Then Release. **Every**
network request recorded: method, URL, headers, body.

| Check | Result |
|---|---|
| Requests carrying the sentinel, in any URL, body or header | **0 of 25** |
| The release call's actual body | `{"mood":null}` |
| `vent_events` rows | 7 → 8 |
| The new row, every column | `id=8, mood=NULL, created_at=2026-09-08 14:02:10+00` |
| Columns that exist on the table | `id`, `mood`, `created_at` — there is nowhere for text to go |
| Sentinel in backend / frontend / db logs | **0 / 0 / 0** |
| Sentinel anywhere in the `feedback` table | **0** |

The text reached the release page and the component holding it unmounted. It was
never sent, and there is no column it could have been stored in.

## 11. Production readiness — read this before deploying

### Ready

- **Rule 2.1 holds**, verified end to end in a browser on the final build.
- **Security headers** enforced and verified against a real browser across 14
  routes, with the auth flow working under them.
- **Authentication**: bcrypt, short-lived access tokens held in memory only,
  httpOnly `SameSite=Strict` refresh cookie, rotation with reuse detection and
  family revocation, a CSRF header on the two cookie-authenticated POSTs.
- **Authorisation**: `@PreAuthorize` plus a path rule, each independently tested;
  401 and 403 correctly distinguished.
- **Input**: bean validation throughout, markup rejected at the boundary, one
  error shape, rate limits on auth, vent and feedback.
- **The endpoint map, actuator and the design-system page** are all closed.
- **Accessibility**: 100 on every route, zero automated failures.
- **Suites**: 180 backend (66 unit + 114 integration), 74 frontend. All green,
  run through `./mvnw verify` and `npm run build/lint/typecheck/test`.

### Not ready, and what it would take

1. **Two placeholders make the site partly non-functional.** The contact address
   (`lib/contact.ts` line 20, flag line 24) and the payment method
   (`lib/support.ts` line 30, flag line 41). Both show honest notices while
   unset, so nothing lies to a visitor — but `/contact` cannot receive mail and
   `/support` cannot receive money. **Two lines each.**
2. **`APP_RATE_LIMIT_TRUSTED_PROXIES` must be set** once there is a reverse
   proxy, or every user behind it shares one login bucket.
3. **The launch blockers in HANDOVER §3 are unchanged and are not developer
   decisions**: legal review of the privacy policy, a native Hindi speaker on the
   crisis keyword list, helpline numbers re-verified, and — if payments are ever
   enabled — the tax and FCRA position.
4. **Single instance only.** Rate-limit buckets are per process and the cleanup
   job has no leader election. Two backends behind a load balancer will not
   enforce limits correctly.
5. **CI has run twice.** Run #1: the wiring job could not run at all. Run #2:
   the wiring job passed, and the frontend *guard* failed a run in which all 74
   tests passed. Both defects were in the checking apparatus, not the code it
   checks; both are fixed and the re-run is pending. See §9 and the two addenda
   at the end of this log.
6. **The postcss advisory is inherited.** Build-time only; closing it is a Next
   16 upgrade. §6 and HANDOVER §3.8.
7. **No screen reader has ever been used on this site.** §7.
8. **No load, soak or failure testing.** Nothing has run under concurrency,
   nothing has waited for an access token to expire in real time, and the
   two-simultaneous-refresh race noted in phase 5 is still unexercised.
9. **No backups, no monitoring, no alerting, no log aggregation.** None of it was
   in scope for any phase and none of it exists. A deployment without at least
   database backups is one disk from losing every account and every published
   note.

### The honest summary

**The application is sound and the security work is real.** What is missing is
almost entirely operational: backups, monitoring, a proxy configuration, a CI run,
and a handful of decisions only the owner can make.

**It should not go public today.** It should go public after items 1–3 are done,
with backups configured, and with someone watching it for the first week. Item 9
is the one that would hurt most and is the least interesting to fix.

Nothing in this log is worded to make the project sound more finished than it is.
Where something was verified, it says how. Where it was not, it says so.

---

## 12. Corrections to earlier entries

Earlier sections are left exactly as written — they record what was true when
written. These are the corrections, collected rather than edited in.

### 12.1 `./mvnw` is NOT broken. The phase 7 log was wrong.

Phase 7 §5 recorded that the Maven wrapper "cannot run" and that
`.mvn/wrapper/` was missing its jar, and phase 8 carried it forward as an open
item. **Both were wrong, and the error was mine.**

`maven-wrapper.properties` declares `distributionType=only-script`, which needs
no jar: the script downloads Maven itself. The classworlds error I hit was a
local Git Bash environment problem — a `MAVEN_HOME` pointing at something
stale — and it reproduced with plain `mvn` in that shell too, which should have
told me the repository was not at fault.

From PowerShell, `./mvnw.cmd -B verify` downloads Maven 3.9.11 and runs the full
suite green. **Nothing needs fixing and phase 9 changed nothing here.** Anyone
who read those entries and went looking for a missing jar was sent on an errand
that did not exist.

### 12.2 Items closed by this phase

| Recorded open in | Item | Status |
|---|---|---|
| Phase 2 §7 | postcss advisories deferred to phase 9 | **Assessed, not fixed** — build-time only; closing it is a Next 16 upgrade. §6, HANDOVER §3.8 |
| Phase 5 §5 | `@PreAuthorize` had no endpoint to guard | Closed in phase 7 |
| Phase 5 | Swagger served in production | **Closed** — local profile only, pinned by a test |
| Phase 5 | Rate limiter shares one bucket behind Docker | **Closed** — trusted-proxy resolution. Owner must set the address (§4) |
| Phase 6 §7 | `SameSite=Strict` cookie never replayed by a real browser | **Closed** — sign-in and refresh-on-reload driven in Chrome (§1) |
| Phase 6 §9 | Refresh token pruning has no job | **Closed** (§5) |
| Phase 7 §6 | No browser was driven — carried five phases | **Closed** — headless Chrome over CDP; every claim in this entry that says "verified in a browser" was |
| Phase 7 §6 | No breakpoint or visual check | **Partly closed** — phase 8 measured the footer at six widths; a full visual review across all routes still has not happened |
| Phase 7 §7 | No CI | **Written, never executed** (§9) |
| Phase 8 §6 | `/support` payment block only ever rendered in jsdom | **Still open** — it needs a real UPI ID, which is the owner's |
| Phase 8 §7 | `./mvnw` broken | **Withdrawn** — see 12.1 |
| All phases | Accessibility never audited | **Closed** — 100 on 14 routes, with the limits in §7 |

### 12.3 Still open, and now permanently the owner's

Screen reader testing (§7), load and concurrency testing, backups and
monitoring, the two placeholders, the trusted-proxy address, and the HANDOVER §3
launch blockers. None of these are code problems and none can be closed from
inside this repository.

---

# Phase 9 addendum — CI ran, and the drift check was the job that could not run

**2026-09-08, after the first push.** Three jobs green; **wiring** failed.

## What broke

```
- name: Drift check
  run: mvn -B -Dtest=EnvExampleComposeDriftTest -DfailIfNoSpecifiedTests=false surefire:test
```

Two defects in one line.

**1. `surefire:test` compiles nothing.** Invoking a plugin goal directly runs
only that goal, skipping the lifecycle phases before it. On a fresh runner
`target/test-classes` does not exist, so surefire had nothing to find. It passed
here for the worst possible reason: a `target/` left populated by an earlier
build. The job was reading the output of a compilation that CI never performed.

**2. The suppression property was misspelled and had never taken effect.**
Surefire reads `-Dsurefire.failIfNoSpecifiedTests`, not
`-DfailIfNoSpecifiedTests`. The error message says so explicitly — and that
message had never been seen, because the condition it guards had never been
reached locally.

Reproduced on a deliberately emptied `target/`:

```
[INFO] No tests to run.
[ERROR] No tests matching pattern "EnvExampleComposeDriftTest" were executed!
        (Set -Dsurefire.failIfNoSpecifiedTests=false to ignore this error.)
```

## The fix

```
run: mvn -B -Dtest=EnvExampleComposeDriftTest test
```

The `test` lifecycle phase compiles main and test sources itself, so there is no
prior phase to remember. Verified on the same emptied `target/`: 79 main and 36
test sources compiled, 2 drift assertions run, exit 0, and the report the guard
step reads present with `tests="2"`.

**The suppression property is gone entirely rather than corrected.** Neither
spelling belongs there. If that class is ever renamed or deleted, this job
*should* fail rather than pass having run nothing — which is the whole point of
the job.

## The same class of mistake, elsewhere

Two more found by auditing every job for anything assuming prior state:

- **`bc` in the backend guard.** `paste -sd+ - | bc` assumes bc is installed. It
  is on GitHub's runners, and it is absent from the shell this was written in —
  a guard that fails because its own arithmetic is missing is the same
  assumption in a different coat. Replaced with `awk`, exercised against the
  real failsafe reports: counts 114.
- **The frontend suite ran twice.** `npm test`, then `npm test` again inside the
  guard purely to grep the output — doubling the slowest step and giving a flaky
  suite two chances to disagree with itself. Now one run through `tee`, with a
  count backstop. Exercised against real output: parses 74.

The other jobs are clean. `mvn verify` and `docker compose build` are whole
lifecycles, and the frontend scripts were re-run with `.next` deleted to confirm
none of them depends on a previous build.

## What this says

**The check written to catch configuration drift was the one job misconfigured**,
and it failed in exactly the way this project keeps finding: something that
appeared to run and did not. It had "passed" locally every time, on state a
fresh machine does not have.

Phase 9 §9 recorded that CI was "written, never executed" and that "the first
push will be its first real test". It was, and it found a real defect on the
first attempt — which is the argument for the count guards, since a wrong
`-Dtest` pattern would otherwise have produced a green job that ran nothing.

**Status change:** CI has now executed. Three jobs pass; the fourth failed,
was diagnosed, fixed, and the fix verified against a reproduced clean-runner
state — but **the corrected wiring job has not itself run on CI yet**. It will
on the next push. Phase 9 §11 item 5 moves from "never run" to "ran once, one
job fixed, re-run pending".

---

# Phase 9 addendum II — CI ran again, and the guard failed the run it was there to vouch for

The wiring fix worked: the drift job passed on run #2. The frontend job failed
instead, and **the tests were not the problem**. The log read:

```
 Test Files  12 passed (12)
      Tests  74 passed (74)
...
Error: Process completed with exit code 1
```

Seventy-four passing tests, then exit 1. **The guard written to prove the tests
ran was itself what failed the run.**

## What broke

The step was one script: run the suite through `tee`, then grep the saved output
to prove the suite was not empty.

```bash
set -euo pipefail
npm test 2>&1 | tee test-output.txt
...
count=$(grep -oE 'Tests +[0-9]+ passed' test-output.txt | grep -oE '[0-9]+' | head -1)
echo "Frontend tests run: ${count:-0}"
if [ "${count:-0}" -lt 50 ]; then ...
```

Two candidates: `pipefail` taking a non-zero status from the `npm test | tee`
pipeline, or the count parse missing and the `-lt 50` branch firing. It was
neither, quite. Reproduced locally rather than reasoned about:

```
$ npm test 2>&1 | tee test-output.txt >/dev/null; echo "PIPESTATUS=[${PIPESTATUS[@]}]"
PIPESTATUS=[0 0]
```

The pipeline is clean, so that candidate is out. And the `-lt 50` branch is out
too, on the evidence of the CI log itself: **that branch prints two lines**
(`Frontend tests run: 0` and an `::error::` annotation) and the log contains
neither. The run died *before* the `echo`.

It died on the assignment. `grep` exits 1 when it matches nothing; under
`set -o pipefail` that becomes the pipeline's status; under `set -e` a failing
command substitution in an assignment kills the script — silently, with no
diagnostic, because the `echo` explaining the count is on the *next* line.

The reason it matched nothing is that the guard read text written for a human.
On a runner, vitest detects CI and colours its output. Reproduced with
`FORCE_COLOR=1`, the summary line is not what the regex expects:

```
$ FORCE_COLOR=1 npm test 2>&1 | grep -a Tests | cat -v
^[[2m      Tests ^[[22m ^[[1m^[[32m74 passed^[[39m^[[22m^[[90m (74)^[[39m
```

`Tests +[0-9]+ passed` cannot match `Tests \e[22m \e[1m\e[32m74 passed`. Locally,
where output is not a TTY and colour stays off, the same guard parses 74 and
passes — which is why it was written with confidence and shipped.

Isolated to be certain the mechanism is the assignment and not the comparison:

```
[plain summary]           reached echo, count=[74]   exit=0
[ANSI-coloured summary]   (no output)                exit=1
[no match at all]         (no output)                exit=1
```

## The fix

Stop grepping console output. Assert on a machine-readable report, which is what
the backend and wiring guards already do — they read failsafe and surefire XML,
and the frontend guard was the only one parsing text meant for a person.

The suite still runs once, now emitting both reporters, and **with no pipe** so
the step's exit code is vitest's own:

```yaml
- name: npm test
  run: npm test -- --reporter=default --reporter=json --outputFile.json=test-results.json
```

The guard is its own step reading that report through `node` (guaranteed
present — `setup-node` ran four steps earlier), and **every branch prints why it
failed before it exits**. The old one could exit 1 having printed nothing, which
is precisely what made it expensive to diagnose.

## Verification

Both steps extracted verbatim from `ci.yml` and executed, with `FORCE_COLOR=1`
and `CI=true` set — the exact condition that failed on the runner:

```
### STEP: npm test (FORCE_COLOR=1, as on a runner)
JSON report written to .../test-results.json
npm test step exit=0

### STEP: Guard
Frontend tests run: 74 (74 passed, 0 failed)
guard step exit=0
```

Every guard branch exercised against a crafted report:

| Condition | Output | Exit |
|---|---|---|
| report missing | `::error::Vitest wrote no JSON report - the suite did not run.` | 1 |
| report corrupt | `::error::Vitest JSON report is unreadable: ...` | 1 |
| discovery collapsed to 3 tests | `Frontend tests run: 3` + `::error::Only 3 ... expected at least 50.` | 1 |
| report format changed | `::error::Vitest report has no numTotalTests - ... this guard needs updating.` | 1 |
| real green suite | `Frontend tests run: 74 (74 passed, 0 failed)` | 0 |

And the guard has not been softened into uselessness — a genuinely failing test
still fails the job at the test step, before the guard is reached:

```
$ # with one deliberately failing test added
npm test step exit with a failing test = 1
```

No `|| true`, no `continue-on-error`, and no swallowed exit code was added; the
file's own standing rule against them still holds.

## What this says

The previous addendum's lesson was "something that appeared to run and did not."
This one is its mirror: **something that ran, and the check on it said otherwise.**

A guard that turns a green suite red is worse than no guard, because it spends
the credibility the guards exist to build — the next red frontend job is now a
little more likely to be read as "the guard again" than as a real failure. Two
specific things caused it, both worth keeping:

1. **The guard was coupled to human-readable output.** ANSI colour is a
   presentation detail that changes with the environment, and the guard's
   correctness depended on it. The machine-readable report is a contract; the
   console summary never was.
2. **`set -euo pipefail` makes a non-matching `grep` fatal and silent.** The
   diagnostic was written on the line *after* the one that could die. A guard
   must print its reason before it can exit, not after.

Both failures so far have been in the checking apparatus rather than the code it
checks, and both passed locally for environment-specific reasons — a populated
`target/`, and a terminal without colour. That is now a pattern worth naming:
**the guards need reproducing under runner conditions, not just running.**

**Status change:** run #2 — wiring fixed and green, frontend guard fixed here.
The corrected frontend job has not itself run on CI yet. Phase 9 §11 item 5 moves
to "ran twice, two guard defects fixed, re-run pending".


---

# Post-phase-9 — the contact address is real

Date: 2026-09-10. Not a phase. One placeholder closed and the documentation
around it made true.

## 1. What changed

`frontend/src/lib/contact.ts` now holds a real address:

```
export const CONTACT_EMAIL = "headheartfrees@gmail.com";
export const CONTACT_EMAIL_IS_PLACEHOLDER = false;
```

Both lines moved in the same edit, which is the whole point of the pair. The
file's header comment was rewritten too: it was a set of instructions for
filling in a blank, addressed to someone who no longer exists. It now says what
the values are and what breaks if the two ever disagree.

`/contact` itself was not restyled and its copy was not rewritten. The only
change to `app/contact/page.tsx` is a stale comment that described the address
as a placeholder.

Two documentation cross-references were corrected in passing, because both had
just become false:

- `lib/support.ts` said its flag's polarity matched `CONTACT_EMAIL_IS_PLACEHOLDER`
  "so the two placeholders read the same way". There is one placeholder now.
- `HANDOVER.md`'s header still said "Phases 1-6 complete" while its own §3.5
  recorded phase 9 as done on 2026-09-08.

**The payment placeholder in `lib/support.ts` is untouched and still open.** It
is a different decision belonging to the owner (HANDOVER §2.7, §2.8), and
nothing here brings it closer.

## 2. The four checks

Not reasoned from the source. `/contact` was rendered and asserted against.

| # | Check | Result |
|---|---|---|
| 1 | The "not live yet" notice is gone from `/contact` | Pass — the `caution` callout does not render, and no "will not arrive" text is in the document |
| 1 | The mailto is correct | Pass — `mailto:headheartfrees@gmail.com?subject=HeadHeartFreeS%20enquiry`, and the address is no longer struck through |
| 2 | HANDOVER no longer lists the address as something the owner must supply | Pass — §2.5 is struck through and marked SUPPLIED, and records that the account belongs to the project |
| 3 | HANDOVER carries the note about what the inbox costs the person reading it | Pass — new §2.5.1 |
| 4 | `/contact` still says it is not a crisis line and is not watched around the clock | Pass — the `important` callout, "not monitored around the clock", "nobody is watching it overnight" |
| 4 | `/contact` still links to `/crisis-resources` | Pass — two links, one in the crisis callout and one under "What this inbox cannot do" |

Checks 1 and 4 are now a test rather than an observation:
`src/app/contact/page.test.tsx`, four cases. It was written to run the checks
and kept because check 4 is the kind of thing a later tidy-up removes without
anyone noticing — a callout that reads as boilerplate to someone who does not
know why it is there.

Suite: **78 frontend tests, 13 files, all passing** (was 74/12). `npm run
typecheck` and `npm run lint` clean. The backend was not touched and was not
run; the env/compose drift job is unaffected, since no environment variable is
involved — the address is a compiled-in constant, deliberately.

## 3. The inbox is a job, and HANDOVER now says so

New §2.5.1, "What comes with the inbox".

The address being real changes something that no test covers. Until today
nobody could write to this site. Now they can, and on a mental health site some
of what arrives will be hard to read — eventually somebody will write to it in
real distress, or write something that stays with the reader after the tab is
closed. Handing over a password without saying that is handing over a job while
describing it as a credential.

So the section is written to the person who will open the inbox, not as a
warning attached to a config item. It says three things: that difficult mail is
what a public address on this site means rather than a sign of something going
wrong; that `/contact`'s "not a crisis line" framing is the honest answer to a
message that cannot be answered in time, and must not be softened into a
promise; and that the helplines on `/crisis-resources` are open to the reader
too.

That last one is deliberately the same note the moderation queue already
carries — `ModeratorSupport` in `components/sections/ModerationQueue.tsx`, whose
header explains why the moderator-facing wording had to differ from the
visitor-facing one. The two roles are the same shape: a person reading
unfiltered writing from strangers, with no way to reply. If the inbox and the
queue end up with different people, the section says both should have read it.

## 4. Why §2.5 was struck through rather than deleted

Deleting it would renumber §2.6 through §2.8, and those numbers are load-bearing
outside the file: `frontend/src/lib/support.ts` cites "HANDOVER.md section 2.7",
and this log cites §2.7 and §2.8 in the phase 8 entry. Renumbering would
silently redirect all of them.

The document already had the idiom — §3.5 is a struck-through heading recording
closed phase 9 blockers — so this follows it. §4's revocation table gains a row
saying the inbox transfers rather than being revoked, which is the one thing
about it that differs from every other credential in that table.


---

# Post-phase-9 — social links in the footer

Date: 2026-09-10. Not a phase. A four-icon row added to the footer's bottom
bar, and a third owner-supplied placeholder created in the process.

## 1. Read this part first

**The four links are the developer's personal accounts, they are live, and
nothing on the rendered page says they are temporary.**

That last clause is the whole entry. This project already had two placeholders
and both were built to fail loudly:

| | `CONTACT_EMAIL` | `SUPPORT_UPI_ID` | `SOCIAL_LINKS` |
|---|---|---|---|
| Value while unconfigured | obviously fake | obviously fake | **real and working** |
| Page shows a notice | yes | yes | **no** |
| Visitor can tell | yes | yes | **no** |
| Failure if forgotten | nobody can write in | nobody can give | **visitors are sent to a stranger's profiles, indefinitely** |

The notice is missing on purpose rather than by oversight. A footer captioned
"these profiles belong to the developer" would read as noise to a visitor and
would not make anyone click less; there is no honest visitor-facing wording for
"this link works, but it is the wrong person's". So the guard is entirely in
places a visitor never sees — the header of `frontend/src/lib/social.ts`,
HANDOVER §2.9, and a row in HANDOVER §4's revocation table.

The consequence is worth naming plainly: **this is the placeholder most likely
to survive handover**, because it is the only one whose survival breaks
nothing. The other two stop the site doing something it advertises. This one
just quietly keeps working, for the wrong person, on every page.

`SOCIAL_LINKS_ARE_PLACEHOLDER` exists as a single thing to grep for. It is
wired to no behaviour, and the file says so rather than implying it guards
something.

## 2. What was built

`frontend/src/lib/social.ts` — the four URLs, the `SocialIconName` union, and
the flag. One file, so the handover edit is one file.

`Footer.tsx` — `SocialRow` and `SocialIcon`, plus a restructured bottom bar.

**No icon library was added.** Four glyphs did not justify a dependency, and
they are hand-drawn to the spec the rest of the site already uses: `0 0 24 24`,
`fill="none"`, 1.75 stroke, round caps and joins, `currentColor`, `h-5 w-5` —
the same numbers as the navbar menu button and the account chevron, so the
footer introduces no second icon weight.

**No brand logos.** The LinkedIn, Facebook and GitHub marks are trademarks with
usage terms attached. The glyphs here are metaphors instead: a briefcase, two
figures, a git branch, a globe.

**The cost of that, stated because it is a real one:** a briefcase is not as
instantly readable as the LinkedIn mark. Recognition is carried by the
accessible name and by `title`, which puts the same word in a hover tooltip.
The icon narrows the guess and the name settles it — but a sighted mouse-free
visitor scanning the footer will not identify these as fast as brand marks, and
that is the trade the trademark constraint buys.

Colour is `text-ink-soft` with `hover:text-(--color-text-accent)`, matching the
support line beside it. No hex, no rgb, nothing outside the token set; the SVGs
inherit through `currentColor`.

## 3. Why the icons are not at the right edge

The brief was "copyright on one side, icons on the other". They are opposite
the copyright — but at the **start** of the second track, not at the far right,
and they share that track with "Support this space".

The footer's own comment explains why. This bottom bar used to be
`justify-between`, which put a measured 507px hole in the middle of the footer
at 1440. The fix was a fixed-measure two-track grid and, more to the point,
abandoning the far edge as somewhere to anchor things. Sending the icons to the
right edge of a 544px track would have rebuilt exactly that hole one row lower,
with the support line and the icons at opposite ends of it.

So the support line and the icon row sit together, 32px apart, opposite the
copyright. This is a deviation from the letter of the brief and it is
deliberate.

## 4. Verified in a real browser at five widths

Not reasoned from the classes. Chrome over CDP against `next dev`, five
viewports, geometry read off the live DOM with `getBoundingClientRect`.

| Width | Layout | Icon row | Right edge | Overflow |
|---|---|---|---|---|
| 320 | stacked | y=3961, below the support line at y=3905 | 180 of 320 | none |
| 375 | stacked | y=3854, below the support line at y=3798 | 180 of 375 | none |
| 768 | 2-col grid, cell stacks | y=2677, below the support line at y=2621 | 572 of 768 | none |
| 1440 | inline | y=2845, same row as the support line | 1057 of 1440 | none |
| 1920 | inline | y=2845, same row as the support line | 1297 of 1920 | none |

- **All four targets measured exactly 44x44 at every width.**
- `document.scrollWidth === window.innerWidth` at all five. No horizontal
  scroll anywhere.
- Every anchor carried `target="_blank"`, `rel="noopener noreferrer"`, an
  `aria-label` naming the platform, and an `aria-hidden="true"` SVG.

**The stack point is `lg`, not `md`.** At `md` the second grid cell is 336px
and the support line plus four 44px targets measures ~338px — a wrap at exactly
the width where the grid first goes two-up. Stacking at `lg` avoids relying on
a 2px margin.

On mobile the `-ml-3` cancels the first target's own 12px of padding, so the
glyphs align with the copyright text above them instead of sitting indented.
It is dropped at `lg`, where the row goes inline and the offset would eat into
the gap.

No dark theme exists in this project, so the tokens have one rendering path and
there was no second palette to check.

## 5. Tests

`Footer.social.test.tsx`, two cases, 80 frontend tests total (was 78).

jsdom has no layout, so the geometry above is not and cannot be a test — it was
measured in a browser and is recorded here instead. What the test holds is the
part a reviewer cannot see on the rendered page: the accessible names, the
`rel`/`target` pair, the `aria-hidden` glyph, and that the row renders from
`lib/social.ts` rather than from URLs pasted into the JSX. That last one
matters for handover specifically — a hardcoded URL would survive editing the
file that HANDOVER §2.9 tells the owner to edit.

`npm run typecheck` and `npm run lint` clean.

## 6. Placeholder count

Two open, one closed:

| | State |
|---|---|
| `CONTACT_EMAIL` | closed earlier today |
| `SUPPORT_UPI_ID` | open — owner's decision, HANDOVER §2.7 and §2.8 |
| `SOCIAL_LINKS` | **open, live, and invisible to visitors** — HANDOVER §2.9 |

---

# Post-phase-9 — footer surface and typographic rhythm

Date: 2026-09-10. Not a phase. Two changes: the footer moves to its own
surface, and the site gets vertical rhythm it did not have. **No palette
change.** Not one value in globals.css §1 was touched; everything below is made
out of tokens that already existed.

## 1. The footer sits on `surface-sunk`

`<footer>` was `bg-surface` — the same colour as the page above it, so the site
did not end so much as stop. It is now `bg-surface-sunk`.

**The crisis strip gains presence rather than losing it**, which was the risk
worth checking. The strip is `surface-raised` and was a 1.07:1 step off the
`surface` around it — barely a boundary. Against `surface-sunk` that step is
1.17:1. The strip itself was not touched.

### Measured, not asserted

Every text element on the new surface, read off the rendered DOM via
`getComputedStyle` in headless Chrome — composited colours, not the hex values
in the stylesheet:

| Element | Token | on `surface` (before) | on `surface-sunk` (after) | AA needs | |
|---|---|---|---|---|---|
| Support links | `ink` | 15.34 | **14.05** | 4.5 | AAA |
| "A place to put it down." / "Support" | `ink` | 15.34 | **14.05** | 4.5 | AAA |
| Brand paragraph | `ink-soft` | 9.30 | **8.52** | 4.5 | AAA |
| Copyright, 13px | `ink-soft` | 9.30 | **8.52** | 4.5 | AAA |
| "Support this space", 15px | `ink-soft` | 9.30 | **8.52** | 4.5 | AAA |
| Social icon strokes | `ink-soft` | 9.30 | **8.52** | 3.0 graphic | pass |
| Every hover state | `clay-deep` | 6.03 | **5.52** | 4.5 | AA |
| Support-column hairline | `clay` | 4.14 | **3.79** | 3.0 graphic | pass |
| Top border | `rule` | 1.32 | **1.20** | — | decorative |
| Support-line underline | `rule-strong` | 1.75 | **1.60** | — | decorative |

**`ink-faint` was the token at risk and it is not exposed here.** It measures
4.12:1 on `surface-sunk`, under the 4.5 AA wants for normal text. The only
`ink-faint` in `Footer.tsx` is inside the crisis strip, which stays on
`surface-raised` where it is 4.82:1. Checked before the change, not after.

The one number that drops without a floor to clear is `rule-strong` at 1.60, a
decorative underline. The link it decorates is identified by its `ink-soft`
text at 8.52:1, not by that rule, so SC 1.4.11 is not in play.

## 2. Rhythm

### 2.1 The section rule is now a component

`ui/SectionRule` — the 1px, 40px clay hairline. The motif already existed and
was hand-copied in six places at two different widths. It is now one component,
applied to every `h2`-level section heading on Home, About, Support, Contact,
Crisis Resources, Community Guidelines and Privacy.

**Side effect worth naming:** the hero's and Promises' rules were `w-12` (48px)
and are now `w-10` (40px), because unifying the motif means picking one width
and 40px is what the crisis strip and the footer column already used. Nobody
asked for the hero's rule to shrink; it is a consequence of consolidating, and
it is one prop away from being reversible.

`CrisisStrip`, `CrisisPanel`, `/vent`, `/vent/released` and `/auth/callback`
still draw the span by hand. Those files were deliberately not opened: the
crisis surfaces are not to be restyled and the vent pages stay quiet. The
markup is byte-identical, so there is nothing to see.

### 2.2 Long-form headings use a pseudo-element

`/privacy` and `/community-guidelines` hand `Prose` bare `<h2>` tags — fourteen
of them. Threading a component through each would put decoration markup into
the copy, so `Prose` grows a `::before` rule on `h2` instead. Empty `content`,
so there is nothing for a screen reader to announce.

### 2.3 Ledes read as standfirsts

`PageHeader`'s lede and the hero's: `body-lg`/`ink-soft` becomes
`h4`/`leading-relaxed`/`ink`. Measured on the rendered page: **18px,
line-height 29.25px, 15.34:1** — up from 17px at 28.56px and 9.30:1.

`leading-relaxed` is deliberate. The `h4` token carries 1.36 because it was
scaled for a heading, and `/support`'s lede runs sixty words; at 1.36 that is a
wall. 1.625 puts the leading within 0.7px of what `body-lg` had, so the
paragraph gains size and weight without the rhythm tightening under it.

### 2.4 The home page alternates, and the phasing is not arbitrary

```
navbar          surface
hero            surface-raised
How it works    surface
Promises        surface-raised
Closing         surface
crisis strip    surface-raised
footer          surface-sunk
```

Every seam is a step. **The hero had to start on `raised` for that to work.**
Phasing it the other way — hero on `surface` — lands `raised` against `raised`
at the crisis strip and flattens the one boundary that is not allowed to go
soft. Contrast on the hero improves rather than degrading: ink 15.34 to 16.45,
ink-soft 9.30 to 9.97.

**Only the home page alternates.** The seven static pages are a single
`max-w-3xl` column, not full-bleed sections; banding them would mean
re-wrapping every section and redoing the padding and measure on each. They get
their rhythm from the hairlines instead. This was a scoping decision taken with
the owner before building, not a shortfall discovered afterwards.

### 2.5 One word of colour in the hero

`Somewhere to put it down.` — the final word in `clay-deep`.

`clay-deep`, not `clay`. At `text-display` the word renders at 53.26px, so WCAG
treats it as large text needing 3.0, and `clay` would clear that at 4.14:1. But
globals.css §1 holds that clay is never a text colour anywhere on this site,
and one hero is not a reason to make a standing rule conditional. Measured on
the page: **#93472B on #FFFCF7, 6.46:1.**

It is the only coloured word in the type on the home page. The other two
clay-deep elements there — the `01/02/03` step numbers and "Go to the vent" —
were already there.

## 3. Verification

### 3.1 Contrast, every route

A CDP probe walked every rendered text element on 16 routes, resolved each
one's composited background by walking ancestors, and computed the WCAG 2.x
ratio from the painted colours.

**371 text elements measured across 16 routes. One below AA:**

```
2.49:1  17px #9A958C on #EFEAE1  <button> "Release & Let Go"
```

That is the disabled release button on `/vent` with an empty textarea. It is
`--color-disabled-text`, which globals.css documents as deliberately failing
because a disabled control must read as unavailable, and which WCAG exempts. It
is unchanged by this work and was 2.49:1 before it.

### 3.2 Lighthouse

Accessibility category, production build, headless Chrome, every route:

| | |
|---|---|
| 100 | `/` `/about` `/voices` `/support` `/contact` `/crisis-resources` `/community-guidelines` `/privacy` `/vent` `/vent/released` `/login` `/register` `/account` `/auth/callback` `/admin/feedback` |
| no score | `/nope-404` |

**The 404 route cannot be Lighthouse-scored.** It returns
`ERRORED_DOCUMENT_REQUEST` — Lighthouse refuses to audit any page served with a
404 status. That is a tooling limitation and not a finding, but it does mean
the "100 across every route" claim has a hole in it, so the hole was filled
another way.

### 3.3 axe-core, including the route Lighthouse would not take

axe-core 4.10.2 injected over CDP, `wcag2a wcag2aa wcag21a wcag21aa`:

**0 violations across all 16 routes, `/nope-404` included.** 23–27 rules passed
per route. One `incomplete` on `/vent` — `color-contrast` on the same disabled
button, which axe flags for human review rather than as a violation.

### 3.4 Everything else

`npm test` 80 passed, 14 files. `npm run typecheck` clean. `npm run lint`
clean. Rendered and read at 1440 and 375.

**One build note:** `next build` first failed with `PageNotFoundError` on five
routes. It was a stale `.next` left by a dev server killed mid-write, not a
code fault — `rm -rf .next` and it built clean. Worth knowing, because the
error names application routes and reads like a broken import.

## 4. What was not done

- **No gradient, no new colour, no saturated accent, no shadow on a card.**
  §8's list is intact.
- **No palette change.** globals.css §1 is byte-identical.
- **The crisis strip is not restyled.** Its surface, type and spacing are as
  they were; what changed is the surface beneath it.
- **`/vent` and `/vent/released` were not opened.**
---

# Post-phase-9 — a security header broke every client component, and /vent was where it showed

**Reported as:** typing in the vent composer does not update state. The text
appears in the box, the counter stays at `0 / 2000`, and "Release & Let Go"
never enables.

**Actual cause:** the phase 9 Content-Security-Policy. It applies to
`next dev`, it does not permit `eval`, and React Fast Refresh needs `eval`.
The client bundle stopped booting three commits ago and **nothing on the site
has hydrated in development since**. `/vent` is simply the page where a dead
client bundle is impossible to mistake for a working page.

Production was never affected, and nothing shipped was ever wrong.

## 1. The instruction was a misdiagnosis, and it pointed at a clean path

The task came with a hypothesis attached, and it is worth writing down because
it was wrong in an instructive way:

> This was working before the design changes. Check whether anything in that
> work touched VentComposer or its parent — a section wrapper added around it,
> a component boundary changed, a "use client" boundary moved. Wrapping a
> client component's parent in a new server component is a plausible cause.

It *is* a plausible cause. It is the right instinct for the symptom, and the
timing genuinely did point at the design work. It was also a path with nothing
wrong in it, and following it would have meant reading `VentComposer.tsx`,
`Textarea.tsx` and `app/vent/page.tsx` looking for a defect none of them had.

The author who filed it was reasoning from the last thing that changed. That is
usually right and here it was not, because **the design commits were the last
thing that changed but not the last thing that broke**. The break was three
commits older and had been invisible the whole time.

The lesson is not "ignore the reporter's hypothesis". It is that a hypothesis
is a place to look first, not a conclusion to confirm — and that the cheapest
way to settle it is to check what the suspected commits actually touched before
reading any of the files they didn't.

## 2. Ruling out the design work took one command

```
$ git log --name-only --oneline -3
5d2990c feat(design): sink the footer, and give the type some rhythm
  PHASE_LOG.md, app/about, app/contact, app/crisis-resources, app/support,
  layout/Footer.tsx, sections/ClosingAction.tsx, sections/DonationSection.tsx,
  sections/Hero.tsx, sections/HowItWorks.tsx, sections/PageHeader.tsx,
  sections/Promises.tsx, ui/Prose.tsx, ui/SectionRule.tsx
c352603 feat(footer): a social row, ...
  layout/Footer.social.test.tsx, layout/Footer.tsx, lib/social.ts
4deedb6 feat(contact): supply the real address, ...
  app/contact/page.test.tsx, app/contact/page.tsx, lib/contact.ts, lib/support.ts
```

No `VentComposer.tsx`. No `ui/Textarea.tsx`. No `app/vent/page.tsx`. No
`app/layout.tsx`. No wrapper was added around the composer, no component
boundary moved, and no `"use client"` directive was touched in any of the
three. The named hypothesis was dead in one command, before a single component
file was opened.

That the design phase did not go near `/vent` is not an inference. **It is
written at the bottom of this very log**, as the last line of the previous
entry:

> - **`/vent` and `/vent/released` were not opened.**

Which is also the answer to "how did this survive three commits". The design
work was careful and scoped, said so, and stayed inside its scope. It reviewed
its pages in a real browser at 1440 and 375 — and every one of those pages is
static server-rendered content that looks *identical* whether React hydrates or
not. The one page whose correctness depends on hydration is the one page the
phase deliberately did not open.

## 3. `hasReactPropsKey: []` — what actually identified it

Reproduced in real Chrome over CDP against `next dev` before changing anything.
The check that settled it was not the counter or the button. It was asking the
textarea whether React knew about it at all:

```js
const ta = document.querySelector('textarea');
Object.keys(ta).filter(k => k.startsWith('__react'))
```

React attaches `__reactFiber$…`, `__reactProps$…` and `__reactEvents$…` to
every host node it mounts. Result on the broken dev server:

```json
{
  "hydrationMarkers": {
    "textareaFound": true,
    "hasReactPropsKey": [],
    "valueAttrPresent": false
  },
  "afterTyping": {
    "domValue": "hello there",
    "counterText": "0 / 2000",
    "buttonDisabled": true
  },
  "consoleAndErrors": [
    { "kind": "exception",
      "text": "EvalError: Evaluating a string as JavaScript violates the following Content Security Policy directive because 'unsafe-eval' is not an allowed source of script: script-src 'self' 'unsafe-inline' ... at .../@next/react-refresh-utils/dist/runtime.js at __webpack_require__ (webpack.js) at __webpack_exec__ (main-app.js)" }
  ]
}
```

An empty `hasReactPropsKey` is not a component with a broken handler. It is a
component that **was never mounted**. That single field converts the question
from "which line of VentComposer is wrong" — a question with no answer — into
"why did React not run", which has exactly one candidate sitting in the console
next to it.

This distinction is worth keeping, because the two states are visually
identical and behave identically to a person typing:

| | React mounted, handler broken | React never mounted |
|---|---|---|
| Text appears in box | yes | yes |
| Counter frozen at `0 / 2000` | yes | yes |
| Button never enables | yes | yes |
| `__reactFiber$` on the node | **present** | **absent** |

Without the last row there is nothing to tell them apart from the outside, and
every instinct points at the component — which is precisely where the reported
hypothesis pointed, and precisely where nothing was wrong.

The mechanism, once named: `eval` is called by the Fast Refresh runtime *during
the execution of a webpack module factory* in `main-app.js`. It is not a
deferred callback that fails in isolation. It throws synchronously inside the
chunk's own evaluation, so the chunk never finishes, so the app never boots,
so **no client component anywhere on the site hydrates**. The navbar, the
account menu, every form — all inert. They just have nothing to display that
would reveal it.

## 4. Why `/vent` and only `/vent`

Server HTML renders normally; CSP blocks the script, not the response. So every
page looks right. The site degrades to what it would be with JavaScript
disabled, and almost all of this site is honest static content that survives
that.

`/vent` does not. A textarea with no React `value` controlling it keeps whatever
is typed into it, because that is what a plain HTML textarea does. So the box
accepts writing and appears to work, while the state the counter and the button
read from stays `""` forever. Someone writes into it and the page refuses to
acknowledge a word of it.

Rule 2.1 says the text in that box never leaves the browser. It never did — the
bug was strictly worse than a leak in one specific way: **it made the product
look like it was ignoring people.** That is the failure mode this site exists to
not have.

## 5. Production was never broken

Verified rather than assumed, on the same commit, same repro:

```
$ next build && next start
{ "hasReactPropsKey": ["__reactFiber$…","__reactProps$…","__reactEvents$…"],
  "afterTyping": { "domValue": "hello there",
                   "counterText": "11 / 2000",
                   "buttonDisabled": false },
  "consoleAndErrors": [] }
```

Fast Refresh is development-only, so the `eval` that trips the policy is not in
a production bundle. Nothing that reached a user was ever affected. The blast
radius was the development environment, for three commits.

## 6. The fix

`next.config.ts` now builds the policy through an exported
`contentSecurityPolicy(mode, apiOrigin)`. Development gets three additive
relaxations and production gets none:

| directive | production | development |
|---|---|---|
| `script-src` | `'self' 'unsafe-inline'` | `+ 'unsafe-eval'` — Fast Refresh |
| `style-src` | `'self'` | `+ 'unsafe-inline'` — dev injects stylesheets |
| `connect-src` | `'self' <api>` | `+ ws: wss:` — HMR socket |

Everything the policy is actually for is unchanged in both:
`frame-ancestors 'none'`, `object-src 'none'`, `base-uri 'none'`,
`form-action 'self'`, and the API origin named explicitly. Developing against
it stays representative.

**The production string is byte-for-byte what it was**, checked two ways rather
than by reading the diff — out of the build manifest, and off the wire:

```
$ python -c "...routes-manifest.json..."
PROD CSP: default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self'; img-src 'self' data:; font-src 'self'; connect-src 'self' http://localhost:8080; frame-ancestors 'none'; base-uri 'none'; object-src 'none'; form-action 'self'
IDENTICAL TO PRE-FIX: True

$ curl -sD - localhost:3333/vent | grep -i ^content-security
Content-Security-Policy: default-src 'self'; script-src 'self' 'unsafe-inline'; ...
```

`'unsafe-eval'` in a shipped policy would hand an injected string the ability to
become code, which is most of what the rest of that header is written to
prevent. The dev/prod split is the whole point of the change and the thing most
likely to be undone by someone simplifying it later, so it is pinned by test
rather than by comment.

## 7. Why the existing tests passed, and what was added

`VentComposer.session.test.tsx` types into this exact textarea and asserts the
release button is enabled. It passed throughout. It was never wrong:

- it mounts `VentComposer` directly through Testing Library — no HTTP, no
  document served with headers, no hydration step;
- **jsdom does not implement CSP at all**;
- `next.config.ts` was not imported by any test in the suite.

The defect lived entirely in the delivery layer. **No component test could have
caught it, however thorough** — the component was correct, and a test that
mounts the component tests the one thing that was working. This is the useful
generalisation: a suite made only of component tests cannot see any fault in how
the components are delivered, and will report full health while the site is
down.

Two tests added, doing different jobs:

**`frontend/next.config.test.ts`** — the one that covers the actual defect. It
pins the production policy as one exact string, so a development relaxation
cannot be appended without failing, and asserts development permits the `eval`
Fast Refresh needs. Verified to fail in both directions rather than assumed to:

```
# with the pre-fix single policy restored
× in development > permits the eval that React Fast Refresh needs
× in development > permits the injected stylesheets and the HMR socket
× relaxes nothing outside development
Tests  3 failed | 4 passed

# with 'unsafe-eval' leaked into production
× in production > is exactly the policy that was verified in a browser
× in production > never permits eval
× relaxes nothing outside development
Tests  3 failed | 4 passed
```

It sits beside `next.config.ts` rather than under `src/`, because that is where
someone editing the CSP will look. That is what the second `include` entry in
`vitest.config.mts` is for.

**`VentComposer.composing.test.tsx`** — the composer's own contract, which was
only ever asserted incidentally. The counter was asserted **nowhere** in the
suite; the session test types in order to release and checks the button on the
way to clicking it. Now explicit: typing moves the counter and enables the
button, deleting takes both back, whitespace alone does not offer release, and
a prompt starter counts. Its header says plainly that it could not have caught
the outage, so nobody reads it later as coverage it does not provide.

## 8. Verification

- Real Chrome over CDP, `next dev`, `/vent`: fibers present, `11 / 2000`,
  button enabled, no CSP errors.
- Same, `next build` + `next start` from a clean `.next`: identical, zero
  console output.
- `/`, `/voices`, `/login`, `/crisis-resources` in dev: no CSP violations.
  (`/voices` logs a CORS/network error because the backend is not running
  locally — unrelated, and present before this change.)
- `npm test` 92 passed, 16 files. `npm run typecheck` clean. `npm run lint`
  clean.

One note on method, since section 6 of the phase 1 entry set the precedent: an
earlier `next start` held its port after being stopped, and the first "verified
in production" run hit that stale pre-fix server. The number it returned was
right for the wrong reason. It was caught, the port was cleared, and the run
above is against a freshly built and freshly served bundle. A verification that
silently tests the previous build is worth less than no verification, because it
is believed.

## 9. What this says

**A header is application behaviour.** `headers()` was reviewed as a security
artefact and reasoned about entirely in terms of what an attacker could do with
it. What it could do to `next dev` was not considered, and it turned the
development environment off. Config that varies by environment needs to be
tested in each environment it varies in — which, before this, meant a test file
the suite had no route to at all.

**"It worked before X" locates the last change, not the first fault.** The
report was filed against the design commits because those were what moved. They
were three commits too recent. The correction was cheap — one `git log
--name-only` against the named suspects — and it should come *before* reading
the files a hypothesis points at, not after failing to find anything in them.

**Ask whether the component ran before asking what it did wrong.** A component
that never mounted and a component with a dead handler present identically.
`Object.keys(node).filter(k => k.startsWith('__react'))` separates them in one
line and costs nothing. It should be the first thing checked whenever a React
UI is inert but the DOM looks correct — the alternative is auditing correct code
indefinitely.

**A green suite described a site that did not run.** Ninety-two component tests
passed against a build where no component mounted. That is not a gap in any one
test; it is the shape of a suite with no layer beneath the components. This
entry adds one file at that layer. It is thin, and it is the only thing standing
between the next CSP edit and another silent three-commit outage.

**The failure landed on `/vent`.** Everything else degraded to static content
and looked fine. The product degraded to a box that takes someone's words and
does not react to them, and it did so while every promise on the page about
those words remained true. `/vent` should be opened in a real browser as part of
verifying *any* change to headers, layout, providers or the root of the tree —
not because it is fragile, but because it is the only page on this site that
tells the truth about whether the client is alive.

---

# Post-phase-9 — TOTP as a second factor on admin sign-in

The admin account is the only one that can read the moderation queue — every
note a stranger submitted, published or not. It was protected by a password.
Now it is protected by a password and a six-digit code.

**The rule first, because it is the one that matters.** Nothing in this phase
touches the vent path. `/vent` and `/vent/released` require no account, and
`/api/v1/vent/**` takes no credential. `VentUnaffectedByTotpIT` is a second
file alongside `VentRemainsAnonymousIT`, written against the specific things
this phase introduced — an account that owes a factor, a ticket that looks like
a token, a login endpoint that now sometimes refuses to issue a session — and it
never authenticates successfully anywhere. The browser run releases a vent
signed out, twice, at both ends of the flow.

---

## 1. Three things found before writing any code, and what each cost

The instruction asked for proposals before implementation. Reading the existing
auth module first turned up three problems that were not in the brief, and two
of them would have made the feature decorative.

### 1.1 Google sign-in never touches the login endpoint

`GoogleSignInHandler.onAuthenticationSuccess` issues a refresh-token family and
redirects the browser. It does not call `AuthService.login`. A second factor
bolted onto `/api/v1/auth/login` alone would therefore have held for everyone
except somebody who clicked the Google button — which is to say, it would have
held for everyone not trying to avoid it.

This is reachable rather than theoretical: `linkGoogle` attaches a Google
identity to an existing password account on first Google sign-in, so an admin
created the documented way (register, promote via `APP_ADMIN_BOOTSTRAP_EMAILS`)
acquires a Google route the moment they use it once.

Two ways to close it. **Rejected:** put a challenge ticket in the redirect URL.
It works, and it puts a live credential into browser history and into every
proxy log between here and the person — a real weakening next to the password
path, where the ticket never leaves memory. **Taken, on the owner's decision:**
refuse Google sign-in for ADMIN accounts outright.

The refusal is not a dead end. It redirects to `/login?error=admin_password_required`,
which renders a sentence saying admin accounts use a password and a code. It
also writes nothing: the Google identity is not linked and no account is
created, so the refusal is repeatable and reversible — remove the role and
Google works again. `AdminGoogleSignInRefusedIT` covers all of it, including the
case that matters most in practice: a user who signs in with Google, is *later*
promoted, and must lose that route.

Ordinary users are untouched.

### 1.2 A refresh cookie outlives the requirement by thirty days

`app.auth.refresh-token-ttl` is `P30D`. An admin signed in when this shipped
holds a cookie minted before the requirement existed, and a refresh endpoint
that simply rotated it would keep issuing admin access tokens for a month
without a code ever being presented. "Required for admins" would have been true
of new sign-ins and false of every session that already existed — which, on the
day it ships, is the only session there is.

So `DefaultAuthService.refresh` revokes the family and refuses when the account
is ADMIN and owes a factor. One extra sign-in, once. It is placed *after*
`rotate()` deliberately: rotation has already spent the presented token, so this
cannot be worked around by presenting it again — that path is reuse detection,
which revokes the family too.

`AdminRefreshRequiresTotpIT` builds the exact sequence: a session obtained as an
ordinary USER, the promotion applied afterwards, then a refresh. It also asserts
the revocation is *conditional* — an enrolled admin refreshes normally, or they
would be signed out every fifteen minutes forever.

### 1.3 Access tokens carried no token-type claim

This is the one that would have made the whole feature a no-op, and it is worth
stating plainly because the failure is invisible.

`JwtService` now signs three things with one key: an access token, a challenge
ticket, and an enrolment ticket. All three are HS256 JWTs, same issuer, user id
in `sub`. Without something separating them they are **interchangeable** — and a
challenge ticket accepted as a Bearer token means the second factor is bypassed
using the ticket the server hands out, for free, immediately after a correct
password.

The feature would have looked entirely correct from outside. `/login` would have
refused to issue a session. Every test about codes would have passed. And the
moderation queue would have opened to anybody who read the ticket out of the
first response.

The fix is one mandatory `typ` claim and one `filter` in
`JwtAuthenticationFilter`. `TotpTicketIsNotAnAccessTokenIT` presents each ticket
type as a Bearer token, plus a token with no claim at all and one with an
unrecognised claim, and asserts refusal — with a control that a real access
token still works, so the class cannot pass against a filter that rejects
everything.

**A separate signing key per token type was considered and not taken.** It is
stronger in principle and it is a second secret for the operator to generate,
set and never lose, guarding against a mistake that one required claim and one
filter check already prevent.

---

## 2. The decisions, and what was rejected

### Who it applies to

**Required for ADMIN, optional for USER.** A USER account grants the ability to
put a display name on a note that a human then moderates. The admin account
grants the queue. Forcing enrolment on somebody who signed up to leave one
piece of feedback is friction with no corresponding benefit.

`TotpService.isRequiredFor` is the single source of that policy. `UserAccount.toSummary`
calls it rather than testing the role itself, so there is one place to change
and not two.

### The enrolment gap

**Forced enrolment at sign-in. No grace period.**

A correct password on an ADMIN account with no second factor returns an
*enrolment ticket*, not a session. That ticket authorises exactly two calls —
`/auth/totp/setup` and `/auth/totp/enable` — and enabling returns the session
they came for, so enrolling and signing in are one step.

Why not a grace period: a grace period is a window in which the protection does
not exist, and it ends on a date nobody is watching. The lockout it is meant to
prevent still happens, just later and by surprise. Why not "issue the session,
then nag": that is precisely the thing the phase exists to prevent.

Verified against a real deploy, not reasoned about — see §6.

### Recovery

**Ten codes, ten characters each, shown once, Argon2id-hashed.**

The alphabet is Crockford-style with `I`, `L`, `O`, `U`, `0` and `1` removed —
those are the characters people transcribe wrongly, and a backup code is read at
the worst possible moment.

Argon2id rather than the SHA-256 used for `refresh_tokens.token_hash`. The
argument that justifies a bare digest there — 256 bits from `SecureRandom`, so
there is no dictionary — does not survive the drop to ~50 bits, where SHA-256 is
days of GPU work against a leaked table. A deterministic lookup is not needed
either: the user id is always known when a code is verified, so the ten rows are
fetched and compared one at a time. That linear scan is affordable *because* the
lockout ladder caps how often it can happen.

Consumed rows are marked, not deleted, so "how many are left" is answerable
honestly on `/account` — and the count is surfaced there with a warning below
three, because otherwise somebody who has spent eight of ten has no signal at
all until the day it matters.

Regeneration is gated on a current code and replaces all ten. Old rows are
deleted rather than marked, so the sheet somebody just threw away is not still
a way in.

### Storage, and the honest answer

Two tables, `user_totp` and `user_totp_backup_code`, rather than columns on
`users`. `users` is read on every `/me` and every refresh and has no business
carrying a credential through those reads; a missing row is an unambiguous "not
enrolled" where nullable columns would be three states pretending to be two; and
the recovery procedure becomes a `DELETE` against a table with one purpose
rather than an `UPDATE` whose `WHERE` could damage the account it is meant to
rescue.

**The secret is encrypted at rest with AES-256-GCM**, keyed by
`APP_TOTP_ENCRYPTION_KEY`, with the owner's UUID as additional authenticated
data so a ciphertext cannot be moved between rows. Guarded at startup exactly as
`JwtSecretGuard` guards the JWT secret.

What that buys, stated the way it is stated in `TotpSecretCipher` and in
HANDOVER §7:

- It **does** separate "somebody holds the database" from "somebody holds the
  database *and* the application's environment". A dump, a stolen backup, a read
  replica, a cloud console, SQL injection — all yield ciphertext.
- It **does not** protect against anybody who has the running application. Root
  on the box, or read access to its environment, decrypts every row. The key and
  the data are in the same place at runtime, necessarily, because the
  application has to verify a code.

Without it, the honest sentence would be: *a base32 string in `user_totp` is a
permanent second factor for whoever reads it, and a database backup in the wrong
bucket is a complete bypass.* Confirmed in the running system: the column holds
`v1:`-prefixed ciphertext, checked by `psql` against a live enrolment.

The cost is a second unrecoverable secret, and it is documented as one.

### Rate limiting, and why the IP limit is the weaker half

A new `RateLimitPolicy.TOTP` at 10/min reuses `ClientIpRateLimiter` as
instructed — its own bucket, not `AUTH`, following the reasoning already in that
enum: somebody fumbling a six-digit code must not lose the allowance that lets
them sign in at all.

**But per-IP limiting is close to worthless here** and the code says so.
HANDOVER §10.3 records that behind Docker every visitor shares one bucket, which
makes it either evadable from a second address or a nuisance to everyone. The
limit that holds is per account, on the `user_totp` row.

Five consecutive failures lock the second factor for 15 minutes; the next five
for 30; then 60; then 24 hours. **The counter is not reset when a lock expires**
— only by a success. That one line is the whole defence:

| | guesses/day | even odds of a hit |
|---|---|---|
| Reset on expiry | 480 | ~1 year |
| Compounding (built) | 5 | ~180 years |

Six digits with a ±1 window means three live codes, so a guess lands with
probability 3×10⁻⁶. A flat "five per fifteen minutes" sounds strict and is a
speed bump. `UserTotpLockoutTest` walks the ladder with time as an argument,
because the property is about hours and cannot be observed through MockMvc
without sleeping.

**The lock is not a usable denial of service**, and this is load-bearing: the
counter is only reachable after a correct password, because that is the only
thing that produces a challenge ticket. Anybody who can lock the admin out for a
day already knows the admin's password. Somebody who knows only the email
address cannot reach it at all.

### Replay

`last_used_step` on the row, and a code is refused when its step is **≤** the
last accepted one.

Stricter than "the same code cannot be used twice", deliberately, and this is
what RFC 6238 §5.2 recommends. It closes the actual attack, which is not reuse
of the code you just watched somebody type — it is replay of the code from the
step *before*, still live because of the drift window. A shoulder-surfer's
window is 90 seconds, not 30.

`TotpSignInIT.anOlderCodeInsideTheWindowIsRefused` tests exactly that: a
never-presented code from the previous step, which the drift window accepts and
which only the spent-step rule can refuse, with a control proving the next step
still works.

### Clock drift

**±1 step. 90 seconds total, three live codes.** RFC 6238 §5.2 says at most one,
and the reason is arithmetic — each extra step is two more live codes. ±2 would
be a 67% larger target in exchange for tolerating a phone a full minute out,
which fails against every other TOTP site too and whose real fix is its own
clock settings. `TotpProperties` refuses to start above 2 rather than letting a
misconfiguration be discovered in a log nobody reads.

The replay rule pulls the same way: a wider window is a longer
capture-and-replay opportunity.

### The library

**`com.eatthepath:java-otp:1.0.0`.** MIT, released 2026-08-18, ~20KB, and
**zero compile or runtime dependencies** — every `<dependency>` in its published
POM is test scope. It does one thing: derive a code from a key and an instant
over `javax.crypto.Mac`.

Rejected: `dev.samstevens.totp` — last released November 2020, and pulls zxing
plus commons-codec behind it. `com.atlassian:onetime` — maintained, but Kotlin,
so `kotlin-stdlib` enters a Java-only runtime image for one algorithm.

Two things the library does not do:

**Base32** is written here, ~25 lines, against the RFC 4648 §10 test vectors.
The algorithm is never reimplemented — `java-otp` takes a `SecretKey` built from
raw bytes and base32 is needed only for the string a person types and the
`otpauth://` URI. It is an alphabet substitution with no secret and no timing
sensitivity, and it fails loudly: a wrong table or bit order produces a secret
whose codes never match, caught by the first scan. That is a different class of
risk from getting a truncation offset quietly wrong.

**QR encoding** is `io.nayuki:qrcodegen:1.8.0`. Its last release is April 2022
and **that is not rot.** QR is a frozen ISO specification (ISO/IEC 18004) and
this is a complete implementation of it with no dependencies and no upstream
moving underneath it. There is nothing to track. Judge it on whether it still
encodes the spec — it does — rather than on the date. This paragraph exists so
nobody later flags the date as neglect and swaps it for something worse.

It sits on the backend rather than the frontend for two reasons: the frontend
has zero runtime dependencies today and that is worth keeping, and the
production CSP is `style-src 'self'` with no `'unsafe-inline'`, which a
client-side QR component is one inline style away from breaking. The server
returns an SVG `data:` URI, which the existing `img-src 'self' data:` already
covers, so no policy changed. Rendering into `<img>` also means the SVG never
becomes live DOM and there is no `dangerouslySetInnerHTML` anywhere in this
feature.

---

## 3. Two bugs the tests found, and neither was in the test

### 3.1 The attempt limiter was decorative

`TotpAttemptLimitIT.theSequenceStops` expected a 429 on the sixth attempt and
got a sixth 401. `aCorrectCodeIsRefusedWhileLocked` got a *successful sign-in*.

Every failure path records something and then throws: `failAndThrow` increments
the counter, sets the lock, saves, and raises `InvalidTotpCodeException`. Under
the default rollback rules **that throw undoes the save in the same
transaction**. The caller got a 401, the counter went back to zero, and the
limit counted to five forever without arriving.

Six digits behind a limit that does not count is a million guesses at whatever
rate the network allows.

This is the identical trap `RefreshTokenService.rotate` already documents for
reuse detection, in the same module, with a comment explaining it. It was read
during this phase and the lesson still had to be relearned from a red test. The
fix is `noRollbackFor` on three methods in `TotpService` and four in
`DefaultAuthService` — both layers, because with `REQUIRED` propagation they
share one physical transaction and either marking it rollback-only loses the
write.

**An existing comment warning about a trap does not prevent the trap.** The
thing that caught it was a test that asserted the *sixth* attempt behaves
differently from the fifth. A test that only checked "a wrong code is refused"
would have passed against a limiter that never limited.

### 3.2 `maxLength={6}` ate a digit out of every pasted code

`CodeInput` had `maxLength={6}` and an `onChange` that strips non-digits. Paste
`123 456` — seven characters, and how most authenticators and password managers
render a code — and the browser clips the **raw** value to `123 45` before the
filter runs. Result: `12345`, a disabled button, and nothing on screen
explaining why.

Caught by a unit test that pasted rather than typed. Typing never reproduces it.
The cap now lives in `sanitiseCode`, applied *after* separators are removed, and
a comment says why `maxLength` must not come back.

---

## 4. What was built

**Backend** — all inside `com.headheartfrees.auth`, so the existing module
boundary rules cover it with no edit. `ModuleBoundaryArchitectureTest` gains a
rule asserting no TOTP type may exist outside `auth` (bar the two configuration
bindings), plus a check that the rule is not passing vacuously against a renamed
set.

```
POST /auth/login          → AUTHENTICATED + session
                          → TOTP_REQUIRED + ticket, no session, no cookie
                          → TOTP_ENROLMENT_REQUIRED + ticket
POST /auth/login/totp     {ticket, code}   → session
POST /auth/totp/setup     session or ticket → {manualKey, otpauthUri, qrDataUri}
POST /auth/totp/enable    {code}            → {backupCodes[10], session?}
POST /auth/totp/backup-codes {code}         → {backupCodes[10]}
POST /auth/totp/disable   {password, code}  → 204   (refused for ADMIN)
GET  /auth/me                               + totpEnabled, totpRequired, backupCodesRemaining
```

`LoginOutcome` is a sealed interface and `AuthController` switches over it
exhaustively, so a fourth outcome cannot be added without the switch failing to
compile. Neither challenge case carries a session, a user, or an access token —
the fields are absent, not null.

The three public paths (`/login/totp`, `/totp/setup`, `/totp/enable`) are
`permitAll` because the caller legitimately has no session yet. They are not
unauthenticated: each demands a ticket issued only after a correct password,
five-minute life, refused as a Bearer token.

Disabling requires the password **and** a code. The threat is somebody else
holding the session — and a session is exactly what they have; without the
password, turning off the factor would be the one move a hijacked session could
make to render itself permanent. It also revokes every session on every device.

**Error copy.** One message for every failure of the code step — wrong code,
expired ticket, forged ticket, replayed code, unknown backup code, spent backup
code, account not enrolled. `TotpSignInIT.aBadTicketIsIndistinguishableFromABadCode`
asserts the responses are byte-identical but for the timestamp.

The one deliberate exception is the lock: 429 with `Retry-After`. It discloses
nothing — anybody seeing it already passed the password check — and an admin
whose correct code is silently refused, with no indication of when that stops,
has no way to tell a lockout from a broken authenticator and will keep trying,
which extends the lock.

**Frontend** — still zero runtime dependencies. The code step is a state change
on `/login`, not a route: a `/login/code` route would have to carry the ticket
in a query string (browser history, proxy logs) or in storage (survives the
tab). Held in a `useState`, a reload loses it, and losing it is correct — the
person re-enters a password they know.

One labelled input, not six boxes. Six boxes need a label per box or none at
all, fight `one-time-code` autofill, break paste, and steal focus. `inputMode="numeric"`,
`autoComplete="one-time-code"`, and a filter that makes any paste land as six
digits.

`alt=""` on the QR, deliberately: it is a picture of the string printed beside
it, and an alt of "QR code" announces a dead end. The manual key is not a
fallback — it is the same secret, as selectable text.

---

## 5. Tests

Backend **93 unit + 166 integration, all passing**. Frontend **107 across 18
files**, lint and typecheck clean.

Every item the instruction listed, and where it lives:

| Requirement | Test |
|---|---|
| Correct password, no code → no session | `TotpSignInIT.correctPasswordAloneIssuesNoSession` |
| Correct password, wrong code → no session | `TotpSignInIT.wrongCodeIssuesNoSession` |
| Correct password, correct code → succeeds | `TotpSignInIT.correctCodeSucceeds` |
| The same code cannot be used twice | `TotpSignInIT.aCodeCannotBeReplayed` |
| Attempts are limited, and the limit stops the sequence | `TotpAttemptLimitIT` (5 tests) |
| Backup codes work once each, then do not | `TotpBackupCodeIT` (5 tests) |
| Enabling requires a verified code first | `TotpEnrolmentIT.enablingRequiresAVerifiedCode` |
| A USER without TOTP signs in normally | `TotpSignInIT.ordinaryUserIsUntouched` |
| **`/api/v1/vent/**` works with no credentials** | **`VentUnaffectedByTotpIT` (7 tests)** |

The first assertion in `TotpSignInIT` checks an *absence* three ways — the
status, the missing body fields, and the missing `Set-Cookie` — because an
absence is easy to break by accident and easy to miss in review.

**A test-fixture note worth reading before the next change.** Enrolment verifies
a code, and a verified code spends its step. A sign-in in the next thirty
seconds derives the *same* step and is correctly refused. `TotpTestSupport.signInCodeFor`
takes the next step's code, and where a test needs two operations it signs in
with a backup code (which spends no step) rather than a TOTP code. This is real
behaviour, not a test artefact — somebody who enrols and immediately signs in on
a second device waits for the digits to roll over — and it is written down so
the next person does not read it as a bug.

---

## 6. Verified in a browser, against the real stack

`docker compose up --build`, Chromium via Playwright, production CSP enforced.
Playwright was installed in a scratch directory, not into the project: nothing
was added to `package.json`.

**On authenticator apps.** No phone was involved. Codes came from two
independent RFC 6238 implementations that share no code with `java-otp` —
`pyotp` 2.10.0 for the API-level run, and a twelve-line implementation written
against the spec for the browser run — cross-checked against each other on a
fixed secret to make sure agreement was not two identical mistakes. That
exercises the same path an authenticator app takes: decode the published
`manualKey`, derive a code, present it. **It is not the same as scanning the QR
with Google Authenticator on a handset, and that remains unverified** — see §8.

### API-level, 35 checks

Register → sign in → enrol → enable → sign in with a code → replay refused →
backup codes → limit → `/vent`. All passed. The lockout sequence came out as
`[401, 401, 401, 401, 401, 429, 429]` — the sixth attempt, exactly as designed.

### The deploy-day scenario, run as a deploy

Not simulated. An account was registered, signed in as an ordinary USER (holding
a real refresh cookie), then promoted by restarting the backend with
`APP_ADMIN_BOOTSTRAP_EMAILS` set — the documented sequence from HANDOVER §6, and
the exact thing that happens on deploy:

```
AdminBootstrap : Promoted account bed19adb-… to ADMIN via APP_ADMIN_BOOTSTRAP_EMAILS
DefaultAuthService : Account bed19adb-… must enrol a second factor before signing in
DefaultAuthService : Revoking sessions for admin account bed19adb-…: it holds no
                     second factor, so this refresh family predates the
                     requirement and must not outlive it
```

The pre-promotion cookie was refused, and refused again — revoked, not merely
declined once. The password still reached a setup screen. Enrolling issued the
session and the moderation queue opened.

### Browser, 32 checks

`/vent` first and last. The vent counter is asserted to track typing, because
the phase 9 addendum records 92 component tests passing against a build where
nothing mounted — a textarea holds text on its own, and only React state proves
the client booted.

Enrolment from `/account`: QR painted from a `data:` URI under the production
CSP (measured bounding box, not just a `src` attribute), manual key selectable,
a code typed *with a space in it* landing as six digits, ten backup codes shown
once. Sign-in in two steps, a wrong code showing the API's own sentence and
clearing the field, then a correct one. Cookie attributes checked on the wire:
`httpOnly`, `SameSite=Strict`, `Path=/api/v1/auth` — the last is what keeps it
off `/vent`. A backup code signing in and the count dropping to 9. The admin
enrolment screen mid-sign-in with no session cookie, and the backup codes
staying on screen rather than being swept away by a redirect.

Zero CSP violations, zero uncaught page errors.

### The open tab across a deploy — and the first attempt at it proved nothing

The instruction was to verify, not assert, that adding `typ` costs an open tab
nothing visible.

**The first attempt was wrong and is recorded because the mistake is easy to
repeat.** It signed in, closed the browser, restarted the backend, reopened the
profile — and everything passed with *no 401 ever happening*. A fresh page load
starts with an empty in-memory access token, sees the session-hint cookie, and
refreshes on mount. It demonstrated the ordinary page-load path, not the one
under test. A green result that exercises the wrong code path is worse than a
red one.

The second attempt never reloads the page: one process, one tab, signed in,
backend restarted from inside the run, then a single ordinary click. The
network from that one click:

```
401 /auth/totp/setup      ← the token the tab was holding
200 /auth/refresh         ← SessionProvider, unprompted
200 /auth/totp/setup      ← the retry
```

The setup screen appeared, under ten seconds, nothing on screen mentioning an
error or a sign-out, no redirect to `/login`, no uncaught errors. **9 of 9
passed.**

**What stands in for the old build, stated plainly.** The previous backend could
not be rebuilt — this phase forbids git commands, so the old tree cannot be
checked out. Instead the backend was restarted with a different
`APP_JWT_SECRET`, which makes every previously-minted access token unverifiable
while leaving refresh tokens — random strings hashed in the database, not JWTs —
completely valid. That is the same state the `typ` claim creates. The mechanism
under test, 401 → refresh → retry in an already-open tab, is the real thing; the
cause of the 401 is a stand-in.

### The recovery SQL was run, not just written

The procedure in HANDOVER §6 was executed against a live enrolled admin:
`DELETE 1`, `DELETE 10`, `UPDATE 3`. The next sign-in returned
`TOTP_ENROLMENT_REQUIRED` with no session and **no restart**, which is what the
documentation claims. Documented SQL that has never been run is a guess.

---

## 7. Documentation

HANDOVER gains: what two-step sign-in means for the admin and what the first
sign-in after deploy looks like; that Google is refused on admin accounts and
why; where backup codes come from and that they are shown once; the lockout
recovery procedure as literal, tested SQL; and a "two secrets you must not lose"
section carrying the honest account of what encryption at rest does and does not
buy.

§4 gains the line the instruction asked for: **removing the developer's address
from `APP_ADMIN_BOOTSTRAP_EMAILS` is a separate act from adding the owner's.**
The variable is a list. An address left in it is re-promoted on every restart,
and removing it does not demote an account that is already ADMIN — so the SQL
and the verifying `SELECT` are there too, with the instruction to restart and
run the SELECT again.

`.env.example` and `docker-compose.yml` gained the five `APP_TOTP_*` variables.
`APP_TOTP_ENCRYPTION_KEY` takes the null form in compose for the same reason
`APP_JWT_SECRET` does: an empty string is a *set* property to Spring and would
override the default, breaking a fresh clone. `EnvExampleComposeDriftTest`
passes.

---

## 8. NOT verified

- **No real authenticator app on a real phone.** Codes came from two
  independent RFC 6238 implementations, cross-checked. The QR was rendered and
  measured in a browser but **never photographed by a handset camera**. What
  that leaves open is narrow and real: the `otpauth://` URI's label and
  parameters are built to the de-facto Key Uri Format and were not confirmed
  against how any particular app files an entry. Scan it once with Google
  Authenticator before handover.
- **No iOS or Android browser.** The code field's `inputMode="numeric"` and
  `autoComplete="one-time-code"` are exactly the attributes those platforms read
  to offer a code from a notification, and neither was observed doing it.
- **The 24-hour end of the lockout ladder was never waited out.** The ladder is
  unit-tested with time as an argument; nothing sat through a real one.
- **No screen reader.** The QR's `alt=""` and the manual key beside it are
  reasoned about, not heard. Same limitation as HANDOVER §10.6.
- **Argon2id over ten backup codes was not load-tested.** A verification is up
  to ten hashes at ~19 MiB each; bounded by the lockout ladder, and never
  measured under concurrency.
- **No Google round trip.** The admin refusal is covered by driving
  `GoogleSignInHandler` directly with a stubbed `OAuth2User`, because the thing
  under test is the decision it makes after Google has said yes. The real flow
  needs the owner's credentials — HANDOVER §3.4 already carries that.
- **Rate limiting is still per-instance and in-memory** (HANDOVER §10.3). The
  per-account lock is in the database and does not share that weakness, which is
  the reason it is the half that matters.

---

## 9. What this says

**Read the module before proposing the design.** The three findings in §1 cost
about an hour of reading and two of them were the difference between a control
and the appearance of one. None would have been found by reading the
instruction, and the Google path in particular would have shipped looking
perfect.

**A comment warning about a trap does not prevent the trap.** `noRollbackFor` is
documented at length in this very module, was read during this phase, and the
attempt limiter was still written without it. What caught it was a test
asserting the sixth attempt differs from the fifth. Write the test that
distinguishes "counted" from "counting".

**Green can mean the wrong code path ran.** The first open-tab check passed with
no 401 in it. Assert that the thing you are recovering from actually happened —
both browser scripts now check for the 401 they depend on, and the
ArchUnit rule checks it matched something.

**Absences need more than one assertion.** "No session" is the entire feature,
and it is checked as a status, a missing body field, and a missing `Set-Cookie`
— because any one of those could come back on its own.

**Say which half of a security property you have.** Encryption at rest here
separates a database compromise from a full compromise, and nothing more. Saying
"the secret is encrypted" and stopping would leave the next person believing
something untrue at the moment it matters.

---

# Post-phase-9 — the accent retoned to the logo's maroon, and `clay` renamed to `accent`

The client supplied `frontend/public/logo.jpeg` and asked for the site accent to
match it. Maroon is clay's neighbour on the wheel — both warm, both earthy — so
this is a retone inside one family, not a foreign colour arriving.

## 1. Sampled, not estimated

The logo is a JPEG, so single pixels are unreliable. Method: classify by HSV hue
band, erode each mask by 1px to drop the anti-aliased fringe, average over a
named rectangle, and take the 10th/90th luminance percentiles of the eroded
stroke core for the endpoints. Image is 1380x752.

**Maroon**

| | Hex | Region |
|---|---|---|
| darkest | `#521221` | pure-maroon stroke core, darkest 10% (n=1066) |
| **signature** | **`#5E1D2C`** | head-profile outline, x490-560 y250-395 (n=2297) |
| corroborating | `#5F1D2D` | pure-maroon median, whole mark (n=10662) |
| corroborating | `#581928` | "HH" wordmark glyphs, x405-610 y462-566 (n=8335) |
| lightest | `#82404A` | stroke core, lightest 10% |

Three independent regions land within 1-2 units of `#5E1D2C`. That is the
identity maroon and it became `--hhf-accent`.

**Champagne gold**

| | Hex | Region |
|---|---|---|
| darkest | `#AA8871` | wing tip, transitioning toward maroon |
| **signature** | **`#D8BD9E`** | hair arc gold segment, x560-660 y185-240 (n=307) |
| corroborating | `#D6BA9C` | wing barbs mid, x740-860 y250-330 (n=1344) |
| median | `#CCAF93` | pure-gold mask, whole mark (n=5236) |
| lightest | `#EED6B4` | stroke core, lightest 10% |

**Charcoal** `#50504F` ("FreeS" glyphs, n=10297); tagline darker at `#41413F`.
**Background** `#F5F2EE`, consistent across three corners (n=14400 each). The
`#FEFEFE` at the centre is the vignette glow, not the base.

## 2. What gold can and cannot do

Gold on bone measures **1.64:1**. That fails the 4.5 text bar *and* the 3.0
graphic bar. The brief proposed a gold hairline as a legitimate use at 3:1; it
is not one, and the correction matters more than the suggestion.

| Use | Ratio | |
|---|---|---|
| ink on gold band | **9.37:1** | PASS AAA - the one role it was given |
| ink on gold-wash | **11.95:1** | PASS AAA |
| gold on accent | **6.93:1** | PASS - the logo's own pairing |
| gold on bone | 1.64:1 | FAIL - fails text *and* graphics |
| white on gold | 1.80:1 | FAIL - illegible, not merely sub-AA |
| gold-deep on bone | 2.96:1 | FAIL - still short of 3:1 |

So gold is a **background that carries ink**, never a foreground on bone. The
hairline (`band-rule`, gold-deep) is permitted on exactly the terms `rule`
(1.32:1) already uses — decorative, conveys nothing, never bounds a control. It
is not a 3:1 graphic element; it misses.

Gold was deliberately **not darkened to make it pass**. The darkest gold present
in the logo is gold-deep, and it still fails 3:1 while already reading brown. A
gold dark enough to carry white text is not the logo's colour.

**Gold is defined and documented but not yet adopted by any component.** Where a
gold band belongs is a design decision, not a retone; the tokens and the rule
are in place for whoever makes it.

## 3. Hover had to reverse direction

Clay was light enough that a darker hover read clearly — dE~12.8 between states.
Every maroon in the logo is 6.9-13:1 on bone, and darkening from `#5E1D2C`
compresses to **dE 6.6**: the state stops being perceptible, which is the whole
purpose of a state. Hover now **lifts** to `#7A2E3D` (dE 9.6) and active still
deepens to `#4C1523`, so rest/hover/active stay mutually distinguishable.

## 4. Re-measured off the rendered DOM

Production build, headless Chrome 1440x1000, computed style with forced
pseudo-states, backgrounds flattened through the ancestor chain. Three items
cross-checked against screenshot pixels.

| | Measured | Bar | |
|---|---|---|---|
| white label on accent fill | **12.44:1** | 4.5 | PASS |
| accent fill vs page (graphic) | 12.16:1 | 3.0 | PASS |
| button hover — label on fill | 9.19:1 | 4.5 | PASS |
| button hover — fill vs page | 8.98:1 | 3.0 | PASS |
| button active — label on fill | 14.60:1 | 4.5 | PASS |
| button active — fill vs page | 14.26:1 | 3.0 | PASS |
| hero accent word (64px, was clay-deep) | 12.16:1 | 3.0 | PASS |
| link text at rest | 9.30:1 | 4.5 | PASS |
| link underline at rest (ink-faint by design) | 4.50:1 | 3.0 | PASS |
| link hover — text | 11.34:1 | 4.5 | PASS |
| link hover — underline | 11.34:1 | 3.0 | PASS |
| footer link at rest | 9.97:1 | 4.5 | PASS |
| footer link hover | 16.45:1 | 4.5 | PASS |
| navbar link hover | 15.34:1 | 4.5 | PASS |
| SectionRule hairline vs surface | 11.34:1 | 3.0 | PASS |
| navbar current-page rule (`::after`) | 11.34:1 | 3.0 | PASS |
| footer hairline | 12.16:1 | 3.0 | PASS |
| selected chip — ink on accent-wash | 13.42:1 | 4.5 | PASS |
| selected chip — border vs page | 11.34:1 | 3.0 | PASS |
| unselected chip hover — text | 16.45:1 | 4.5 | PASS |
| focus ring vs page background | **6.72:1** | 3.0 | PASS |

Maroon is darker than clay, so almost everything improved. The button label went
4.54 -> 12.44; the hero word 6.03 -> 12.16; the chip border 4.14 -> 11.34.

**Three items sit under 3:1 and none is a regression.** Each is satisfied by a
different mechanism, and each measured the same or better before the retone:

- **focus ring vs accent fill, 1.81:1.** `outline-offset: 2px` puts a 2px gap of
  page background between fill and ring, so the adjacent colour is bone, where
  the ring is 6.72:1. Against clay this was 1.51:1 — the retone improved it.
- **selected chip wash vs page, 1.14:1.** Chip.tsx carries the state on the
  border (11.34:1) and ink + font-medium; the wash is reinforcement. clay-tint
  was 1.13:1.
- **ghosted hero watermark, 1.63:1.** Decorative, `aria-hidden`.

**The watermark is why pixels were sampled.** `getComputedStyle` returns
`oklab(0.337378 0.0937479 0.0162755 / 0.25)` unresolved for
`color-mix(in oklab, ...)`, and reading those numbers as RGB gives a plausible,
wrong 1.83:1. The rendered pixel at the densest stroke is `#D7C4C4` ->
**1.63:1**. Screenshot sampling also confirmed the button fill renders as
literally `#5E1D2C` across 91.8% of its pixels.

## 5. The standing rule was reversed, not inherited

globals.css carried: *"`clay` is never a text colour"* — true at 4.14:1. Maroon
measures **11.34:1**, clearing AA by 2.5x and AAA outright. The prohibition
described a colour no longer in the palette, so it is gone rather than carried
forward. `--color-text-accent` now points at `accent` itself instead of a
darkened variant, and Hero.tsx's comment explaining the workaround was rewritten
to record that the constraint was lifted.

## 6. Audits

- **Lighthouse accessibility: 100 on all 15 scorable routes.** `/nope-404`
  returns `ERRORED_DOCUMENT_REQUEST` as before — Lighthouse refuses any page
  served 404. Tooling limitation, not a finding, and covered below.
- **axe-core 4.10.2 over CDP, wcag2a/2aa/21a/21aa: 0 violations across all 16
  routes, `/nope-404` included.** 21-27 rules passed per route. **0 incomplete**
  — the `color-contrast` incomplete previously flagged on `/vent` no longer
  appears.
- `npm test` 107 passed / 18 files. `typecheck` clean. `lint` clean. Build clean.

## 7. Did the raw-layer-only edit hold?

**For colour, yes — completely.** Every hex lives in `--hhf-*`. No component
gained a colour value. The only hex outside globals.css is Google's brand mark
in GoogleButton.tsx, which is correct.

**Two things still forced component edits, and only one was expected.**

The rename `clay` -> `accent` touched 37 files. That was the approved choice and
it is identifier churn, not colour churn — `accent` is role-named, so a third
retone will not need a third sweep.

The unexpected one: **measured contrast ratios were duplicated as prose in six
components** — Footer, Hero, SectionRule, Spinner, styles.ts, Chip. A hue change
invalidated all of them, and they had to be re-measured and rewritten by hand:
`4.14`, `6.03`, `3.79`, `5.52`, `1.37`, `6.46` were all quietly wrong the moment
the raw layer changed. The colour tokens have a single source of truth; the
*numbers describing them* do not. That is the discipline gap this phase found.

## 8. One thing renaming exposed

`@theme inline` had `--color-clay: var(--color-accent)` — safe, because the two
layers used different names. Renaming collapsed them onto one name and turned it
into exactly the self-reference bug documented at the top of the file: Tailwind
emits `--color-accent` into `:root` beside the semantic declaration, and it only
resolves because of source order. The five accent entries and the two gold ones
now read `--hhf-*` directly, like the surface and status entries already did.
The `ink`/`rule` entries still point at the semantic layer and remain safe,
because those Tailwind names differ from the semantic names they read.

## 9. Not done, deliberately

The logo itself is untouched. The JPEG cannot go on the site — opaque off-white
background, strokes below a pixel at navbar size, and a wordmark reading
"HHFreeS" where the site says "HeadHeartFreeS". It needs tracing to SVG by a
designer. The exhale mark stays in place.

---

# Post-phase-9 — the traced logo: cleaned, measured, and kept out of the navbar

The client supplied `HHFreeS_logo_vector.svg`, an auto-trace of the JPEG. The
brief was explicit that it might not be usable and that saying so was part of
the job. It is not usable at navbar size. It is now on `/about` and `/login`
only, inside a circle, and the exhale mark keeps the navbar and the favicon.

## 1. Cleaning

| | Before | After |
|---|---|---|
| File size | 14,297 B | **4,603 B** (-68%) |
| Background rect | opaque `#f7f5f2` | removed |
| Paths / fills | 9 | 8 |
| Points | 1,332 | 1,100 |
| viewBox | `0 0 1000 544` | `0 0 291 222` |

Cleaned file is `public/logo-mark.svg`; the supplied original is untouched at
`public/HHFreeS_logo_vector.svg` (it arrived named `.svg.svg`, renamed once).

**The wordmark was not in one path.** The charcoal path (`rgb(80,80,79)`,
y 342-438) was pure wordmark and was dropped whole. But the darkest maroon path
spanned y 96-410 — it held the two "H" glyphs *and* mark geometry in a single
`d` attribute. Deleting it would have taken the face profile with it. It was
split into subpaths, the two glyphs dropped (232 points), the rest kept. Anyone
re-doing this from the original should know the fills do not map to elements.

SVGO with `floatPrecision: 1` and `removeViewBox: false` did the rest.

## 2. It does not work at navbar size

Rendered at 24, 32 and 40px in headless Chrome at 1x and inspected, not assumed.

**The face profile is gone.** Not degraded — absent. No nose, no lip, no chin
at any of the three sizes; it reads as a loop with a leaf. Ink coverage in the
face region:

| | 160px | 40px | 32px | 24px |
|---|---|---|---|---|
| face region ink | 24.3% | 37.5% | 43.2% | **53.6%** |
| whole mark ink | 17.6% | 26.9% | 30.0% | **34.4%** |

Coverage doubling as the mark shrinks is what filling-in looks like when you
count it: the gaps that *make* a profile are the first thing to close.

**The eight bands never read as a gradient.** At 160px they read as dashes —
each stroke is a chain of disconnected blobs rather than a line. At navbar size
they average into a pale mauve and the deep maroon never renders at all:

| mean ink colour | 160px | 40px | 32px | 24px |
|---|---|---|---|---|
| traced logo | `#A78280` 3.11:1 | `#C5ADAA` 1.93:1 | `#C9B3B0` 1.81:1 | `#D0BDB9` **1.64:1** |
| exhale mark (current) | `#53545A` 6.87:1 | 4.45:1 | 4.45:1 | **3.65:1** |

The exhale mark holds 2x the presence at every size and its coverage is
scale-stable (16.8% → 22.9%) where the trace nearly doubles. That is the whole
argument for leaving the navbar alone.

**Faceting** from the straight-line tracing is visible at 160px and not at
navbar size, only because nothing is.

**Two further findings.** On an accent fill the mark half-disappears: its
darkest band `#581928` against `--hhf-accent` `#5E1D2C` is **1.07:1**, because
the accent token was sampled from this very logo last phase. It can never sit
on an accent surface. And on every light surface the three lightest bands are
1.27-1.91:1, so part of the mark is always near-invisible — it can only ever be
decorative, and is `aria-hidden` accordingly.

## 3. The circle

Requested as a framing treatment. **It frames the mark; it does not fix it.**
Legibility is a property of the trace, and the circle changes none of it. If a
properly drawn vector is ever commissioned, the circle is an independent design
decision that can be kept or dropped on its own merits.

**What fills it.** Nothing bone-ish can define the circle — measured against the
surfaces it sits on, every candidate is invisible:

| fill | vs surface | vs raised | vs sunk |
|---|---|---|---|
| bone-raised | 1.07 | 1.00 | 1.17 |
| bone | 1.00 | 1.07 | 1.09 |
| bone-sunk | 1.09 | 1.17 | 1.00 |
| accent-wash | 1.14 | 1.23 | 1.05 |
| gold-wash | 1.28 | 1.38 | 1.18 |
| gold | 1.64 | 1.75 | 1.50 |

Going darker to make the disc visible costs the artwork: on a gold fill the
lightest bands drop to 1.18:1 and the wing dissolves. So `surface-raised` was
chosen for two reasons that are not about the disc being visible — it is
closest to the artwork's own `#f7f5f2` ground, and it gives the dark structural
bands their best contrast of any candidate (12.95:1, against 12.08 on bone and
11.06 on sunk).

**The border is what makes the circle a circle.** `band-rule` (gold-deep
`#AA8871`) — the token added last phase for decorative hairlines, which until
now had no use. It is the logo's own colour, sampled from the wing tip.

Measured off the rendered page, not computed: the page renders `#F4F1EC` rather
than the flat `#F7F4EF` token because of the paper-grain overlay, so these are
true numbers.

| | /about (160px) | /login (128px) |
|---|---|---|
| circle fill vs page | 1.10:1 | 1.10:1 |
| **border ring vs page** | **2.77:1** | **2.60:1** |
| border ring vs fill | 3.05:1 | 2.86:1 |

The flat token is 2.96:1 on bone; the rendered ring measures lower because a
1px curved border loses some of itself to antialiasing, and more at the tighter
128px curvature. **This is below 3:1 and is not claimed otherwise.** It is
decorative and `aria-hidden`, so SC 1.4.11 does not apply — the same footing as
`rule` (1.32:1) and `rule-strong` (1.75:1), both already documented as
decorative-only, and the ring is materially stronger than either. `accent` at
11.34:1 was rejected: a dark maroon ring reads as a heavy badge and competes
with the mark it frames.

The mark is contained, never cropped — at 0.66 of the diameter its half-diagonal
is 0.427x against the circle's 0.5, leaving ~7% breathing room. A crop at this
aspect would clip the wing tip.

## 4. The CSP nearly shipped this broken, and the DOM measurement caught it

The first version sized the circle with `style={{ width: diameter }}`. It built
clean, typechecked clean, and the served HTML contained
`style="width:160px;height:160px"`.

**It rendered at 108x82px.** In the browser, `el.getAttribute('style')` returned
the string and `el.style.width` returned empty — the browser refused to parse
it. `next.config.ts` ships `style-src 'self'` with no `'unsafe-inline'` in
production and documents "zero `style=` attributes and zero `style={{}}` in
source across every route" as a property worth defending. A grep confirmed the
new component was the only `style=` in the entire codebase; the invariant had
held until this change broke it.

**Development relaxes the directive** (`'unsafe-inline'` in dev), so it looks
correct locally and is wrong once deployed. Nothing in the build, the types or
the tests catches it. It was caught only by measuring the rendered element
rather than trusting the emitted markup — which is the same reason the last
phase measured contrast off the DOM instead of the stylesheet.

The size is now a variant (`md` | `lg`) resolving to literal Tailwind classes
Tailwind can see at build time. The `<img>` keeps `width`/`height` **attributes**
— presentational markup, not CSS, so `style-src` does not touch them, and they
reserve the box. Measured CLS is **0** on both routes.

## 5. Placement

`LogoMark` is a new component alongside `Logo`, not a replacement. `/about` at
160px, `/login` at 128px, both `aria-hidden` via the `title={null}` pattern
`Logo` already uses — the page heading names the place, and announcing the brand
twice helps nobody. The SVG is referenced rather than inlined: 4.6KB that cannot
use `currentColor` buys nothing in the bundle, and `img-src 'self'` covers it.
`next/image` is declined for the same reason as in TotpSetup — the optimiser
does not process SVG, so it would return identical bytes through a wrapper.

## 6. Audits

- **Lighthouse accessibility 100 on all 15 scorable routes**; `/nope-404`
  unscorable as always (`ERRORED_DOCUMENT_REQUEST`).
- **axe-core: 0 violations, 0 incomplete, across all 16 routes** including
  `/nope-404`. A new graphic element is a new contrast surface, and it added
  nothing — because it is correctly marked decorative.
- 107 tests pass, typecheck clean, lint clean, build clean, CLS 0.

**One build note, repeating a trap already in this log:** rebuilding while a
`next start` server held `.next` corrupted the output — `Cannot find module
'./331.js'`, `__webpack_modules__[moduleId] is not a function`, and 500s on
routes that were fine minutes earlier. Kill the server *before* `rm -rf .next`.
It is the same stale-`.next` failure recorded in Phase 9 and it presents as
broken code rather than as a stale build.

## 7. Honest assessment

**The file is not good enough, and it is shipped only where its faults are least
visible.** At 160px it is presentable. It is not good: the strokes are dashed
rather than continuous, the curves are 1,100 straight segments, and the gradient
is eight flat bands that read as posterisation under any scrutiny. It reads as a
degraded scan of a logo, which is what it is.

A proper redraw would fix, in order of how much it matters:

1. **Real curves.** Bezier paths instead of 1,100 line segments chasing pixel
   boundaries. This alone removes the faceting and most of the file size.
2. **One continuous stroke per element.** The head, the heart and each wing barb
   should be single stroked paths, not chains of filled polygons. That is what
   makes the strokes dashed and it is why they break up at every size.
3. **A real gradient, or 2-3 deliberate flat colours.** Eight posterised bands
   are an artefact of tracing a JPEG, not a design decision.
4. **A profile that survives to 24px** — drawn thicker, with fewer inflections,
   optically corrected for small sizes rather than scaled down from large.

With 1-4 the mark could plausibly take the navbar and this component's minimum
size could drop. Until then the exhale mark holds it, and that is the right
outcome rather than a compromise.

---

# 2026-09-13 — the traced logo replaces the wordmark in the navbar, against recommendation

## 0. The plain statement

**The mark in the navbar is below the legibility this log already measured for
it, and it ships anyway at the client's request.**

The previous entry measured this file at navbar size and recommended against it.
Nothing has changed that assessment and nothing here revises it. At the 32px
that shipped, the face profile is gone — no nose, lip or chin — and what remains
reads as a loop with a leaf. The client was shown the renders and the numbers
and chose it. It is built as well as this file can be built; the file is the
limit, not the implementation.

## 1. What is in the navbar

The cleaned mark (`public/logo-mark.svg`, 4.6KB) at 32px, then "HHFreeS" as
**live text** in the display serif. Not the traced lettering from inside the
SVG — those glyphs were split out during cleaning and stay out. Live text is
crisp at any zoom, restyles with the type scale, and is selectable and
translatable; traced lettering is none of those.

No tagline in the navbar: "Empty Head & Free the Heart" would set around 3px
tall in a 64px header. It moved to the footer brand block, beneath the mark,
where it measures 24px tall and is `ink-soft` at 8.52:1 on surface-sunk.

## 2. Size: 24, 32 and 40px measured, 32 chosen

Rendered in the real navbar and measured off the pixels, isolating the mark from
the text:

| mark height | ink coverage | mean ink | darkest 5% | vs wordmark cap |
|---|---|---|---|---|
| 24px | 38.0% | 1.82:1 | 5.40:1 | 1.9x |
| 32px | 35.0% | 1.97:1 | 7.41:1 | 2.5x |
| 40px | 32.1% | **2.12:1** | **8.59:1** | 3.2x |

**40px is measurably the best on every legibility metric**, which is expected:
this file's failure mode is scale-dependent, so bigger is monotonically better
for it. The gain is not linear — 24→32 buys +2.01 on the darkest band, 32→40
only +1.18 — and at 40px the mark stands 3.2x the wordmark's cap height, reads
mark-heavy, and leaves 12px clearance in the 64px header.

32px was chosen: it takes the larger share of the available gain and balances
against the wordmark. 24px was ruled out — at 1.82:1 mean it reads as a smudge
beside the text and adds nothing.

**None of the three recovers the face profile.** Choosing between them is
choosing how large an illegible mark is, not whether it becomes legible.

## 3. The name conflict, chosen not missed

**Decided 2026-09-13.** The navbar now reads **HHFreeS**. The footer, page
titles, browser tab, metadata and all body copy continue to read
**HeadHeartFreeS**. The client was told and chose this.

Nothing else was renamed, deliberately. Both names are inventoried here so that
if this is ever revisited the work is already scoped:

- **"HHFreeS" as user-visible text: exactly one place** —
  `components/layout/Navbar.tsx`, in `NavBrand`. Other occurrences in that file
  and in `ui/Logo.tsx` are comments.
- **"HeadHeartFreeS": 20 files** — `app/layout.tsx` (metadata title template),
  `app/icon.svg` (favicon aria-label), `components/layout/Footer.tsx`,
  `components/ui/Logo.tsx` (the `Logo` default `title`), the page files for
  `/`, `/about`, `/community-guidelines`, `/contact`, `/crisis-resources`,
  `/login`, `/privacy`, `/register`, `/support`, plus `lib/api.ts`,
  `lib/contact.ts`, `lib/support.ts` and four test files.

Reconciling would mean changing the metadata title template, which changes every
browser tab and every search result. That is a client decision, not a cleanup.

## 4. The exhale mark after this change

It leaves the navbar. It is **not** dead — it still appears:

- **`app/icon.svg` — the favicon.** Unchanged, and it stays: this is the one
  mark that works at 16px, which the traced file cannot.
- **`components/layout/Footer.tsx:94`** — the footer brand block, at 32px.
- **`components/sections/Hero.tsx:105`** — the ghosted hero watermark at
  `text-accent/25`.
- **`components/ui/MoodIcon.tsx`** borrows its line language, by reference.

**`Wordmark` became dead code and was deleted.** The navbar was its only caller.
It is removed rather than left unused on purpose: it rendered the exhale mark
plus "HeadHeartFreeS", so anyone reaching for it would have reintroduced a third
brand lockup at exactly the moment there are already two names in play. A note
in `Logo.tsx` records what it was and why it went. `Logo` itself is untouched.

## 5. Mobile: the brief's fallback was not needed

The brief allowed hiding the wordmark below `sm` if mark plus text would not fit
beside the menu button. Measured, it fits:

| width | brand link | wordmark | clearance to menu button | h-overflow |
|---|---|---|---|---|
| 320 | 129x40 | shown | 115px | none |
| 375 | 129x40 | shown | 170px | none |
| 414 | 129x40 | shown | 209px | none |
| 768 | 129x40 | shown | nav row | none |

At 320px the brand is 129px and the menu button 44px inside 288px of usable
width. **The wordmark therefore shows at every width.**

The responsive version was built first and rejected on evidence. Hiding the
visible text below `sm` requires a second `sr-only` copy for the accessible
name, and the accessibility tree confirmed the result: the link's name came back
as **"HHFreeSHHFreeS"**. `textContent` and `sr-only` do not respect `display:
none` the way the two-span assumption needed. One always-visible element that is
always in the accessibility tree cannot desync, so that is what ships.

Verified from the CDP accessibility tree, not from markup:
`role=link, name="HHFreeS"` at both 320px and 1440px, and the mark
`ignored=true, reason=ariaHiddenElement`. **The brand is announced once.**

## 6. CLS

Six routes, 1440x900, `layout-shift` entries with `hadRecentInput` excluded:

| route | CLS |
|---|---|
| `/about` | **0** |
| `/login` | **0** |
| `/vent` | **0** |
| `/support` | **0** |
| `/` | 0.021 |
| `/voices` | 0.084 |

**No shift entry on any route has a source inside `<header>`.** Every source was
checked: on `/` it is the hero block reflowing (a section, the watermark svg,
two text blocks, all shifting ~29px vertically); on `/voices` it is the footer
being repositioned as the list loads. Both are pre-existing and both are under
the 0.1 "good" threshold.

The footer tagline was cleared as a cause: the footer settles at 762px on `/`,
`/about`, `/voices` and `/support` alike, with the tagline present and 24px tall
on each — and the two routes that carry the tagline with no client-loaded
content measure exactly 0.

The mark contributes no shift because its size is `width`/`height`
**attributes**. This is the same CSP constraint recorded in the previous entry:
`style-src 'self'` silently drops `style={{ width }}`, which builds clean,
typechecks clean and renders at the wrong size. Attributes are presentational
markup, so the directive does not touch them, and they reserve the box before
the file arrives. No inline styles were added anywhere in this change.

## 7. Audits

- **Lighthouse accessibility 100 on all 15 scorable routes**; `/nope-404`
  unscorable as always (`ERRORED_DOCUMENT_REQUEST`).
- **axe-core: 0 violations, 0 incomplete, across all 16 routes** including
  `/nope-404`. 21-27 rules passed per route.
- 107 tests / 18 files pass, typecheck clean, lint clean, build clean.

**The stale-`.next` trap bit twice more in this phase**, both times presenting
as something else. Once the running server held port 3000 so a fresh `npm start`
died with `EADDRINUSE` and the old process kept serving the previous build — the
navbar rendered the old exhale wordmark for three screenshots before the cause
was found. Kill every listener on 3000 and confirm the port is free *before*
rebuilding; `netstat -ano | grep :3000` can show more than one PID.

## 8. What would change this

Nothing about this implementation. The mark needs redrawing — real curves
instead of 1,100 straight segments, one continuous stroke per element instead of
eight posterised bands, and a face profile drawn thick enough with few enough
inflections to survive to 32px. The previous entry lists these in priority
order. When a proper vector exists, the navbar is the first place it should go,
and `MARK_H` in `NavBrand` is the one number to revisit.
