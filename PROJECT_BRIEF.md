# HeadHeartFreeS — Project Brief

> Place this file at the repository root. It is the single source of truth for the build.
> Read it fully before writing any code.

---

## 1. What this is

HeadHeartFreeS is an anonymous emotional-release web app. A person arrives, types
whatever is weighing on them, presses **Release & Let Go**, and the words are gone.
No therapy, no advice, no AI reply. Just a place to exhale.

An earlier version was generated from a Figma export and hardcoded. We are rebuilding
it properly: production-grade frontend and backend, written from scratch.

---

## 2. Non-negotiable rules

These are correctness requirements, not preferences. Violating any of them is a bug.

1. **Vent text NEVER leaves the browser.** Not sent, not logged, not "saved then
   deleted." No backend endpoint may accept a body containing vent content. The text
   lives in React state and is cleared on release. If you find yourself writing a DTO
   with a `content` or `text` field for the vent domain, stop — that is the mistake.
2. **Venting requires no account.** `/vent` is fully public. Auth is optional and must
   never block access to it.
3. **Do not run git commands.** No `git add`, `commit`, `push`, or `branch`. At the end
   of each phase, stop and print a suggested conventional-commit message. The developer
   commits and pushes manually.
4. **No secrets in the repo.** Only `.env.example` with placeholder values is committed.
5. **Build one phase at a time.** Complete the phase, print the commit message, then
   stop and wait. Do not run ahead.

---

## 3. Tech stack

### Backend
- Java 21, Spring Boot — latest supported 3.x GA (currently 3.5.16), Maven
- Spring Web, Spring Security 6, Spring Data JPA, Validation
- PostgreSQL 16, Flyway for migrations
- JWT (jjwt) — short-lived access token + refresh token in an httpOnly cookie
- Google OAuth2 login (Spring Security OAuth2 Client)
- Bucket4j for rate limiting
- springdoc-openapi for Swagger UI
- JUnit 5, MockMvc, Testcontainers

### Frontend
- Next.js 15 (App Router), TypeScript (strict), Tailwind CSS
- shadcn/ui as a base, but heavily restyled — see §8
- Framer Motion for transitions, honouring `prefers-reduced-motion`
- react-hook-form + zod for forms

### Infra
- Multi-stage Dockerfiles for both apps
- docker-compose: postgres, backend, frontend
- GitHub Actions workflow for build + test (no deploy)

---

## 4. Repository layout

```
headheartfrees/
├── PROJECT_BRIEF.md
├── README.md
├── .gitignore
├── .env.example
├── docker-compose.yml
├── backend/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/headheartfrees/
│       ├── HeadHeartFreesApplication.java
│       ├── common/       # exceptions, base DTOs, error handler, utils
│       ├── config/       # security, CORS, OpenAPI, rate limit, Jackson
│       ├── auth/         # user, registration, login, JWT, OAuth2
│       ├── feedback/     # feedback submission + moderation
│       ├── vent/         # anonymous counter ONLY
│       └── donation/     # donation intents
└── frontend/
    ├── Dockerfile
    └── src/
        ├── app/          # App Router routes
        ├── components/   # ui/ (primitives), layout/, sections/
        ├── lib/          # api client, auth, validators
        └── styles/
```

### Module boundary rule (this is the "microservice-ready" part)

Each backend domain package exposes **only** a public service interface and DTOs.
No class in `feedback` may import an entity or repository from `auth`; it references
users by ID only. No cross-module JPA relationships — use IDs, not `@ManyToOne` across
domains. Follow this and any module can later be lifted into its own service with a
gateway in front, without rewriting business logic.

---

## 5. Data model

Only three tables store anything. There is deliberately no table for vent content.

```sql
users
  id UUID PK, email CITEXT UNIQUE NOT NULL, password_hash TEXT NULL,
  google_id TEXT NULL UNIQUE, display_name TEXT, role TEXT DEFAULT 'USER',
  email_verified BOOLEAN DEFAULT false, created_at, updated_at

feedback
  id UUID PK, user_id UUID NULL FK, display_name TEXT NULL, location TEXT NULL,
  rating SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
  message TEXT NOT NULL, status TEXT DEFAULT 'PENDING', created_at

vent_events
  id BIGSERIAL PK, mood TEXT NULL, created_at
  -- counter only. no content, no user_id, no IP.

donation_intents
  id UUID PK, user_id UUID NULL, amount_minor BIGINT, currency TEXT,
  status TEXT, provider_ref TEXT NULL, created_at
```

All schema changes go through Flyway migrations in `src/main/resources/db/migration`.

---

## 6. API surface

```
GET    /api/v1/health                 → { status, version, time }   (public, liveness)

POST   /api/v1/auth/register          { email, password, displayName }
POST   /api/v1/auth/login             { email, password }
POST   /api/v1/auth/refresh           (refresh cookie)
POST   /api/v1/auth/logout
GET    /api/v1/auth/me                (authenticated)
GET    /oauth2/authorization/google    Google sign-in entry point

POST   /api/v1/vent/release           { mood?: string }   → NO TEXT FIELD
GET    /api/v1/vent/stats             → { totalReleases, countries }

POST   /api/v1/feedback               { rating, message, displayName?, location? }
GET    /api/v1/feedback               → approved feedback, paginated, public
PATCH  /api/v1/admin/feedback/{id}    { status }          → ADMIN role only

POST   /api/v1/donations/intent       { amountMinor, currency }
```

Rate limits: 5/min on auth endpoints per IP, 3/hour on feedback per IP,
30/min on vent release. Return `429` with a `Retry-After` header.

Every error response uses one consistent shape:
`{ timestamp, status, code, message, path, fieldErrors? }`

---

## 7. Frontend routes and flows

```
/                 Home
/vent             The writing space (public, no auth)
/vent/released    Thank-you + feedback form + optional deeper share
/about            Mission, principles, and the donation section
/support          Donation page
/login, /register Auth pages
/admin/feedback   Moderation queue (ADMIN only)
```

### Navbar
`Home · Vent · About` on the left of centre, and on the right either
**Sign in** or the user's avatar menu. **Donate and Feedback are removed from the
navbar and from the footer's Navigate column** — the old design had them and they
must not reappear.

### The release flow — build this exactly

1. `/vent` — optional mood chips, optional prompt starters, a large textarea with a
   2000-character counter, and a `Release & Let Go` button that stays disabled until
   there is text.
2. On click: fire `POST /api/v1/vent/release` with `{ mood }` **only**. Clear the
   textarea. Navigate to `/vent/released`.
3. `/vent/released` — the thank-you message, then the feedback form (rating + message,
   name and location optional). This posts to `/api/v1/feedback` and **is** stored.
4. Below the feedback form, a quiet secondary option: *"Want to tell us more about what
   you're going through?"* linking to a longer optional form. This is separate from
   feedback and its content is not stored in our database.
5. One soft line about supporting the project. Not a banner, not a modal.

### Login prompt on arrival
A dismissible soft prompt on first visit only, remembered via a cookie. It must not
cover the page, must not block `/vent`, and must have an obvious close control.

### Client-side crisis detection
Because vent text never reaches the server, safety handling must run in the browser.
Maintain a keyword list in `lib/safety.ts`. If the textarea content matches, render a
calm inline panel above the button surfacing the helplines (Tele-MANAS 14416,
Vandrevala Foundation 9999666555, iCall 9152987821 Mon-Sat 10am-8pm, Crisis Text Line:
text HOME to 741741). Do not block submission, do not send anything anywhere, do not
use alarming language.

> **Corrected 2026-09-04.** This section previously listed Vandrevala Foundation as
> 1860-2662-345. That number does not appear on the foundation's own contact page,
> free-counselling page or FAQ, all of which list only +91 9999 666 555. It was
> replaced everywhere it appeared. Lead with Tele-MANAS: it is 24/7, free,
> government-run, available in 20 languages, and 14416 is a short code that is easier
> to recall in distress. Never imply a line is open when it is not — iCall is
> Mon-Sat 10am-8pm and must always be shown with its hours.

---

## 8. Design direction

The brief is "premium and human." A reviewer must not be able to tell this was built
with AI assistance. Avoid every one of these tells:

- Default shadcn radii and drop shadows repeated on every card
- Indigo/violet gradients, glassmorphism, neon accents
- Emoji standing in for icons (the old mood chips did this — replace with custom
  line icons)
- Perfect symmetry everywhere and identical vertical rhythm between all sections
- Inter for everything
- Generic marketing copy ("Empower your journey", "Unlock your potential")

Instead: a deliberate serif/sans pairing, a faint paper-grain overlay, an asymmetric
hero, one accent colour used sparingly, and real spacing decisions.

Define all colour as CSS custom properties in `globals.css` and reference them through
the Tailwind theme — never hardcode hex in components. The palette will be supplied;
scaffold with neutral placeholders and make it a one-file swap.

### Responsiveness
Mobile-first. Verify at 320, 375, 414, 768, 1024, 1440, 1920. Test the vent textarea
with an on-screen keyboard open on iOS — that is where this kind of layout usually
breaks. Tap targets ≥ 44px. Use `dvh`, not `vh`.

### Accessibility
WCAG 2.1 AA. Real semantic landmarks, visible focus rings, labelled inputs, keyboard
paths through every flow, contrast checked against the final palette.

---

## 9. Build phases

Complete one phase, print a suggested commit message, then stop.

| # | Phase | Contents |
|---|-------|----------|
| 1 | Scaffold | Repo layout, `pom.xml`, `.gitignore`, `.env.example`, both Dockerfiles, `docker-compose.yml`, `README.md`, health endpoint, Next.js app booting |
| 2 | Design system | CSS variables, Tailwind theme, typography scale, buttons, inputs, cards, navbar, footer, logo component, grain overlay |
| 3 | Static pages | Home, About, fully responsive across all breakpoints |
| 4 | Auth backend | User entity, Flyway migration, register/login, JWT, refresh rotation, Google OAuth2, `/me`, rate limiting, tests |
| 5 | Auth frontend | Login, register, Google button, session handling, avatar menu, protected routes, soft arrival prompt |
| 6 | Vent flow | `/vent` page, mood chips, counter, crisis detection, release endpoint, `/vent/released` |
| 7 | Feedback | Submission, public list, moderation endpoints, admin queue UI |
| 8 | Donation | `/support` page, footer link, About section, donation intent endpoint |
| 9 | Hardening | Security headers, CORS lockdown, OpenAPI docs, GitHub Actions, Lighthouse, a11y audit |

---

## 10. Definition of done for every phase

- Compiles with zero warnings; TypeScript strict passes; ESLint clean
- New backend logic has tests; `mvn verify` passes
- `docker compose up` still works end to end
- No hardcoded secrets, no hardcoded colours in components
- Responsive check at all listed breakpoints
- Rule §2.1 re-verified: no vent text anywhere near the server

---

## Start here

Read this brief, then build **Phase 1 only**. Before writing code, print a short plan
of the files you intend to create and wait for approval.
