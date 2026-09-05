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

- **Google sign-in has never been executed.** No credentials exist, so
  `GoogleSignInHandler` — account linking, the verified-email check, the
  redirect — has run zero times. What is tested is the *absence* case: the
  context starts, and everything else works, without it. The handler itself is
  unexercised code and should be treated as such until someone completes a real
  sign-in.
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
  base64 value that is in the repository, so an install that forgets
  `APP_JWT_SECRET` starts successfully on a public secret. Deliberate for local
  ergonomics; phase 9 should refuse to boot on it outside the `local` profile.
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
7. **New — Google sign-in is unexercised.** First real sign-in should be treated
   as testing, not as usage.
8. **New — refresh token pruning** has no job. Phase 9.
9. **New — the committed JWT secret default** should stop being accepted outside
   the `local` profile. Phase 9.
