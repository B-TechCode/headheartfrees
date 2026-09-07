# HeadHeartFreeS

A quiet place to put down what you are carrying. You type what is weighing on you,
press **Release & Let Go**, and the words are gone. No therapy, no advice, no AI reply.

> **The one rule that shapes everything else:** vent text never leaves the browser.
> It is not sent, not logged, and not "saved then deleted." No API endpoint accepts it.
> The release call records an optional mood label and a timestamp — nothing more.
> See [PROJECT_BRIEF.md](PROJECT_BRIEF.md) §2.1, which is the source of truth for this build.

---

## Status

Phase 6 of 9 — **auth, front and back**. You can register, sign in with a
password or with Google, and stay signed in across a reload; the vent flow still
requires no account and never will. The phase table lives in the brief (§9),
which was reordered — the vent flow moved from 6 to 4 and auth moved back,
because `/vent` needs no account and nothing was gated behind auth.

| Phase | | |
|---|---|---|
| 1 | Scaffold | ✅ done |
| 2 | Design system | ✅ done |
| 3 | Static pages | ✅ done |
| 4 | Vent flow | ✅ done |
| 5 | Auth backend | ✅ done |
| 6 | Auth frontend | ✅ done |
| 7 | Feedback | next |
| 8–9 | Donation, hardening | pending |

## Stack

**Backend** — Java 21, Spring Boot 3.5.16 (latest 3.x GA at time of scaffolding),
Maven, Spring Web, Spring Security 6, springdoc-openapi.

**Frontend** — Next.js 15 (App Router), React 19, TypeScript strict, Tailwind CSS v4.

JWT and the Google OAuth2 client are deliberately **not** on the classpath yet.
Persistence (JPA, PostgreSQL driver, Flyway) and Bucket4j arrived in phase 4. Each is added in
the phase that first uses it.

**As of phase 4 the backend requires Postgres to start.** `vent_events` is the
first table, Flyway owns the schema, and Hibernate validates its mapping against
it on boot. JWT (jjwt) and the Google OAuth2 client are still deferred to phase 5.

## Prerequisites

- JDK 21 or newer (built and tested on 22; the bytecode target is 21)
- Node.js 22+
- Docker with Compose v2, if you want the containerised path

Maven is not required — use the bundled `./mvnw` wrapper.

## Quick start

### With Docker (everything at once)

```bash
docker compose up --build
```

Every value has a local-development default, so this works on a fresh clone with no
`.env` file. Then:

- Frontend — http://localhost:3000
- API health — http://localhost:8080/api/v1/health
- Swagger UI — http://localhost:8080/swagger-ui.html

`docker compose` sets `SPRING_PROFILES_ACTIVE=local` for you. That profile is what
permits the development `app.auth.jwt-secret` default: the secret is committed, so
the backend **refuses to start** on it under any other profile and tells you to run
`openssl rand -base64 48` and set `APP_JWT_SECRET`. Anything that is not a
developer's own machine therefore needs a real secret before it will boot.

To override anything, `cp .env.example .env` and edit it. Note that
`NEXT_PUBLIC_API_BASE_URL` is baked into the frontend bundle at **build** time — see
[Environment](#environment) below.

### Without Docker

```bash
# Terminal 1 — backend on :8080
cd backend
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run

# Terminal 2 — frontend on :3000
cd frontend
npm install
npm run dev
```

**Postgres is required for the backend from phase 4 onward.** The quickest way to
get one is to start just the database from compose and leave the two applications
running on the host:

```bash
docker compose up -d db
```

The datasource defaults in `application.yml` point at `localhost:5432` with the
compose credentials, so nothing else needs configuring. The frontend still runs
standalone against whatever `NEXT_PUBLIC_API_BASE_URL` points to.

## Layout

```
headheartfrees/
├── PROJECT_BRIEF.md      # source of truth — read before changing anything
├── docker-compose.yml    # postgres + backend + frontend
├── backend/
│   ├── Dockerfile        # 3 stages: build → layer extract → temurin JRE runtime
│   ├── pom.xml
│   └── src/main/java/com/headheartfrees/
│       ├── common/       # shared DTOs, error handling, health endpoint
│       ├── config/       # security, CORS, OpenAPI
│       ├── auth/         # phase 5
│       ├── feedback/     # phase 7
│       ├── vent/         # phase 4 — counter only, never content
│       └── donation/     # phase 8
└── frontend/
    ├── Dockerfile        # 3 stages: deps → build → standalone runtime
    └── src/
        ├── app/          # App Router
        ├── components/   # ui/ primitives, layout/, sections/
        ├── lib/          # api client, auth, validators
        └── styles/       # globals.css — the only place colour is defined
```

Each backend domain package carries a `package-info.java` stating its boundary. The
short version: a domain exposes a service interface and DTOs only, and refers to other
domains by ID. No cross-module JPA relationships, no importing another domain's
entities. That is what makes any module liftable into its own service later.

## Commands

| | Backend (`backend/`) | Frontend (`frontend/`) |
|---|---|---|
| Run | `SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run` | `npm run dev` |
| Build | `./mvnw clean package` | `npm run build` |
| Test | `./mvnw verify` | `npm test` (Vitest, jsdom) |
| Types | — | `npm run typecheck` |
| Lint | compiler runs `-Xlint:all -Werror` | `npm run lint` |

## Environment

Copy `.env.example` to `.env`. It is grouped by the phase that starts using each
variable; anything marked unused is documented ahead of time so the file stays the
single deployment reference. `.env` is gitignored — only the example is committed.

**`NEXT_PUBLIC_API_BASE_URL` is inlined at build time.** Next.js substitutes
`NEXT_PUBLIC_*` values into the JavaScript bundle when it compiles, so the running
container never reads them. Changing where the frontend points means rebuilding:

```bash
docker compose build --build-arg NEXT_PUBLIC_API_BASE_URL=https://api.example.com frontend
```

Restarting the container with a new environment variable will not change the baked-in
value. The Dockerfile takes it as an `ARG` with a localhost default.

## Browser support

Tailwind CSS v4 is used in its CSS-first mode: design tokens live in `@theme` inside
[frontend/src/styles/globals.css](frontend/src/styles/globals.css), with no
`tailwind.config.js`. That keeps the palette swap a genuine one-file change, which the
brief (§8) asks for.

The trade-off is v4's browser floor, which is **Safari 16.4+, Chrome 111+, Firefox
128+** (Tailwind v4 depends on `@property` and `color-mix()`). Those shipped in March
2023, March 2023 and July 2024 respectively, and Firefox 128 is the current ESR base.
Older browsers will render the site unstyled rather than broken.

For this audience — a consumer web app being built in 2026, primarily reached on
current mobile browsers — that floor is acceptable and was accepted deliberately. If a
meaningful share of traffic later turns out to sit below it, the escape hatch is
Tailwind v3 with a JS config, which costs one file and a token rewrite, not a rebuild.

## Accessibility and responsiveness

Targets are WCAG 2.1 AA, mobile-first layouts verified at 320 / 375 / 414 / 768 / 1024
/ 1440 / 1920, `dvh` rather than `vh`, and tap targets of at least 44px. The
reduced-motion media query is already honoured globally in `globals.css`. Real
verification starts in phase 2, when there is something to look at.

## Conventions

- **No colour is hardcoded in a component.** Every value is a CSS custom property in
  `globals.css`, surfaced to Tailwind through `@theme`.
- **No secrets in the repo.** Only `.env.example`, with placeholders.
- **Git is the developer's.** The build never runs git commands; each phase ends with a
  suggested conventional-commit message to run by hand.
- **One phase at a time**, per the brief's phase table.

## Known scaffold noise

Spring Boot logs a generated security password on startup. That is the default
in-memory user from `spring-boot-starter-security`, which backs off automatically once
phase 4 introduces a real `UserDetailsService`. It is unusable in the meantime — the
filter chain has no form login and no HTTP basic — and no action is needed.

`springdoc` also warns that `/v3/api-docs` and `/swagger-ui.html` are enabled by
default. Disabling them for production is part of phase 9 hardening.
