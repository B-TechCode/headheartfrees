# Handover — HeadHeartFreeS

Everything the owner of this project needs to run it, and everything that has to
change hands. Written to be read by someone who did not build it.

Last updated: 2026-09-06. Phases 1–5 complete; see `PHASE_LOG.md` for the full
build record and `PROJECT_BRIEF.md` for the design decisions behind it.

---

## 1. The one rule that must survive any change

**Vent text never leaves the browser.** It is not sent, not logged, not stored,
not "saved then deleted." There is no API endpoint that can receive it.

This is not a preference. It is the promise the site makes on every page, it is
the reason people would use it, and the codebase enforces it with automated
tests that fail the build:

- `VentRuleArchitectureTest` — no free-text field can exist in the vent domain
- `VentSchemaIT` — `vent_events` must have exactly `id`, `mood`, `created_at`
- `ModuleBoundaryArchitectureTest` — auth and vent cannot see each other
- `VentRemainsAnonymousIT` — the vent endpoints work with no account at all

If any of those fail after a change, the fix is almost never to relax the rule.
Whoever maintains this next should read `PROJECT_BRIEF.md` §2 before touching
anything in the `vent` package.

---

## 2. What the owner must provide before this goes live

Nothing here can be supplied by the developer, because each item is either a
credential that should belong to the owner or a decision only they can make.

### 2.1 A domain name

Needed for the production URLs, the Google OAuth configuration, and TLS.

### 2.2 Google OAuth credentials — the owner's own

The development credentials used while building only work on `localhost` and
cannot be used in production. New ones must be created under the owner's Google
account. Section 5 has the steps.

### 2.3 A JWT signing secret

Generate with:

```
openssl rand -base64 48
```

Set it as `APP_JWT_SECRET`. **The application will refuse to start without it**
outside the `local` profile — deliberately, because the development default is
committed to this repository and anyone who can read the source could otherwise
forge an admin session.

### 2.4 Database credentials

A PostgreSQL 16 instance, and a user and password for it. Not the development
defaults.

### 2.5 A contact email address

Currently a placeholder. It is one line:

```
frontend/src/lib/contact.ts  line 20
export const CONTACT_EMAIL = "REPLACE-ME@example.invalid";
```

And a flag on line 24 to set to `false` once the real address is in. Until then
the Contact page shows a visible notice that the address is not live, so nobody
emails a void and waits.

### 2.6 A decision on who moderates feedback

The feedback wall is reviewed before anything is published (Phase 7). Someone
has to do that reviewing, and that person needs an admin account. See §6.

---

## 3. What must happen before a public launch

These are not optional polish. Each one is a real exposure.

### 3.1 Legal review of the privacy policy

`/privacy` was written to be accurate rather than to be boilerplate, and it is
accurate as far as the developer could determine. It has **not** been reviewed
by a lawyer. The site stores email addresses, display names and password hashes,
and it serves users in India and potentially elsewhere, so India's DPDP Act and
the GDPR are both plausibly in scope.

### 3.2 A native Hindi speaker reviews the crisis keyword list

`frontend/src/lib/safety.ts` contains a small set of Hindi and Hinglish phrases
used to decide when to surface helplines beside the writing box. The English set
was tested and measured. The Hinglish set was not, because the developer could
not judge its false-positive rate — and casual Hinglish uses hyperbolic
references to dying far more often than English does.

A list that fires on ordinary sad writing teaches people to ignore it, and then
it is not there in the moment it matters.

### 3.3 Crisis helpline numbers re-verified

Every number on `/crisis-resources` was verified against the operating
organisation's own website when written. **One number in the original design
turned out to be wrong** — it had been widely republished but no longer appeared
on the operator's site.

These numbers change. They should be re-checked before launch and on a schedule
afterwards — quarterly is reasonable. All of them live in one file,
`frontend/src/lib/safety.ts` and `frontend/src/lib/helplines.ts`, precisely so
this is a single job.

A wrong crisis number is worse than no crisis number.

### 3.4 Google sign-in re-tested on the owner's own credentials

**A real Google sign-in has now been completed** (2026-09-06), which it never had
been before that date. Google authenticated, the backend created the account,
linked the Google id, issued the refresh cookie and redirected to
`/auth/callback`; the account row exists with Google linked, and
`APP_ADMIN_BOOTSTRAP_EMAILS` then promoted it to `ADMIN` on restart. So the path
works, and the "treat the first sign-in as testing" warning has been served.

Two reasons this stays on the pre-launch list:

1. **It was the developer's OAuth client**, which only works on localhost and
   which the developer deletes at handover (section 4). The owner's own client
   under the owner's own Google account (section 2.2) is a different
   registration with different redirect URIs, and a wrong redirect URI is the
   usual way this breaks. Sign in once on the real domain after switching.
2. **Three branches of the handler still have not run.** Linking Google to an
   *existing password account* — register with a password first, then sign in
   with Google using the same address — is the one worth testing deliberately,
   because it is what stops one person ending up with two accounts. A second
   sign-in with the same Google account, and the rejection of an unverified
   Google address, are the other two.

The redirect lands on `/auth/callback`, which 404s until the frontend for it
exists. That page is Phase 6's; the 404 is the missing page, not a broken
sign-in.

### 3.5 The remaining Phase 9 items

`PHASE_LOG.md` carries a running list. The significant ones: security headers,
CORS lockdown for the production domain, disabling the Swagger UI in production,
rate limiting behind a real reverse proxy (currently all users share one bucket
behind Docker), a cleanup job for expired refresh tokens, and an accessibility
audit.

---

## 4. What stays with the developer, and what to revoke

At handover the owner should assume the developer retains nothing, and verify it:

| Item | Action |
|---|---|
| Developer's Google OAuth client | Developer deletes it. It only worked on localhost. |
| Developer's local `.env` | Never committed; developer deletes it. |
| GitHub repository access | Transfer ownership or remove the developer as a collaborator. |
| Any deployment or database access | Rotate credentials after handover, regardless of trust. |

Rotating credentials at handover is normal practice and is not a statement about
anyone. It is simply how you make the boundary real.

---

## 5. Setting up Google sign-in in production

The application needs exactly two environment variables. **No code changes.**
If either is missing the site runs normally with no Google option, and password
sign-in is unaffected.

### Steps

1. Sign in to `console.cloud.google.com` with the account that should own this.
2. Create a project.
3. Open **Google Auth Platform** (formerly APIs & Services → Credentials).
4. Configure the consent screen: **External** user type, app name, support email,
   developer contact email.
5. Go to **Clients** → **Create client** → **Web application**.
6. Set, substituting the real domain:
   - Authorised JavaScript origin: `https://yourdomain.com`
   - Authorised redirect URI: `https://api.yourdomain.com/login/oauth2/code/google`
   - The redirect URI must point at the **backend**, and must match exactly —
     including `https`, no trailing slash.
7. Copy the client ID and client secret. The secret is shown once and is awkward
   to retrieve afterwards.
8. Set them on the server:
   ```
   GOOGLE_CLIENT_ID=...
   GOOGLE_CLIENT_SECRET=...
   APP_OAUTH2_SUCCESS_REDIRECT=https://yourdomain.com/auth/callback
   ```
9. **Publish the consent screen.** While it is in testing mode only explicitly
   listed test users can sign in. Basic email and profile scopes do not require
   Google's verification review.

---

## 6. Creating the first admin account

There is deliberately no way to make yourself an admin through the website. No
endpoint anywhere accepts a role, and the code that creates accounts cannot
produce an admin. That is intentional: a self-service path to admin is a
self-service path for an attacker.

The sequence is:

1. Register normally through the site with the address that should be admin.
2. Set `APP_ADMIN_BOOTSTRAP_EMAILS` to that address on the server.
   Comma-separated for several.
3. Restart the backend.

On startup the application promotes that **already existing** account. It never
creates one, so the variable is not a credential — setting it to an unregistered
address does nothing at all.

To undo, remove the address from the variable and run:

```sql
UPDATE users SET role = 'USER' WHERE email = 'them@example.com';
```

---

## 7. Environment variables

Copy `.env.example` to `.env` and fill it in. `.env` is gitignored and must never
be committed.

### Required in production

| Variable | Notes |
|---|---|
| `APP_JWT_SECRET` | `openssl rand -base64 48`. App refuses to start without it. |
| `SPRING_DATASOURCE_URL` | Real database, not the compose default |
| `SPRING_DATASOURCE_USERNAME` | |
| `SPRING_DATASOURCE_PASSWORD` | |
| `APP_CORS_ALLOWED_ORIGINS` | The real frontend origin. No wildcard — the refresh cookie needs credentialed CORS. |
| `NEXT_PUBLIC_API_BASE_URL` | **Baked in at build time.** Changing it needs a frontend rebuild, not a restart. |

### Optional

| Variable | Default | Notes |
|---|---|---|
| `GOOGLE_CLIENT_ID` | unset | Both must be set or Google is simply unavailable |
| `GOOGLE_CLIENT_SECRET` | unset | |
| `APP_OAUTH2_SUCCESS_REDIRECT` | localhost | Where Google sends the browser afterwards |
| `APP_ADMIN_BOOTSTRAP_EMAILS` | empty | See §6 |
| `APP_COOKIE_SECURE` | `true` | Leave true. False only for a LAN IP with no TLS. |
| `APP_JWT_ACCESS_TOKEN_TTL` | `PT15M` | |
| `APP_JWT_REFRESH_TOKEN_TTL` | `P30D` | |

---

## 8. Running it

### Locally, everything at once

```
docker compose up --build
```

Frontend at `http://localhost:3000`, API at `http://localhost:8080`, Swagger UI
at `http://localhost:8080/swagger-ui.html`. Works on a fresh clone with no `.env`
because compose sets `SPRING_PROFILES_ACTIVE=local`, which is what permits the
development JWT secret.

### Locally, without Docker

```
docker compose up -d db

cd backend
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run

cd frontend
npm install
npm run dev
```

Postgres is required — the backend will not start without it.

### Tests

```
cd backend
./mvnw verify
```

Needs Docker running; the integration tests use Testcontainers to run against a
real PostgreSQL 16.

---

## 9. Where things are

```
headheartfrees/
├── PROJECT_BRIEF.md    The design. Read §2 before changing anything.
├── PHASE_LOG.md        What was built, what was verified, what was not.
├── HANDOVER.md         This file.
├── backend/            Java 21, Spring Boot, PostgreSQL
│   └── src/main/java/com/headheartfrees/
│       ├── auth/       Accounts, JWT, refresh rotation, Google sign-in
│       ├── vent/       An anonymous counter. Never content.
│       ├── feedback/   Phase 7
│       ├── donation/   Phase 8
│       ├── common/     Error handling, rate limiting
│       └── config/     Security, CORS, OpenAPI
└── frontend/           Next.js 15, TypeScript, Tailwind
    └── src/
        ├── app/        Pages
        ├── components/ ui/ primitives, layout/, sections/
        ├── lib/        API client, helplines, safety list, contact
        └── styles/     globals.css — the only place colour is defined
```

Two files deserve particular care:

**`frontend/src/lib/helplines.ts`** — the single source of truth for every crisis
number on the site. It exists because these numbers drifted once already.

**`frontend/src/lib/safety.ts`** — the keyword list that decides when helplines
appear beside the writing box. Its header explains why it is deliberately narrow
and what it will and will not catch. Read that before editing it.

---

## 10. Known limitations, stated plainly

Not a list of bugs. These are things that are true about the software as
delivered, and someone should know them before deciding what to do next.

1. **No email is sent, at all.** There is no mail transport. That means no
   password reset, no email verification, and no way to tell a returning user
   that they already have an account. The `email_verified` column exists and
   nothing depends on it.

2. **Registration deliberately cannot tell you an email is taken.** Registering
   an address that already exists returns the same response as a new one. This
   prevents anyone testing whether a given person has an account here — which,
   for a mental health site, is disclosive on its own. The cost is that someone
   who has forgotten their account gets a success message. The wording addresses
   both cases, but a password reset is the real fix, and that needs email.

3. **Rate limiting is per-instance and in-memory.** Behind Docker every request
   appears to come from the same address, so all users currently share one
   bucket. Needs a trusted-proxy configuration and ideally a shared store.

4. **Refresh tokens are never pruned.** One row per refresh, kept forever. Needs
   a scheduled cleanup job.

5. **Nothing has been load tested.** No idea how it behaves under real traffic.

6. **No accessibility audit has been run.** The site was built to WCAG 2.1 AA
   and the colour contrast was measured, but no screen reader testing and no
   automated audit have been performed.

7. **The site has no users, and says so.** There are no visitor counts, no
   testimonials and no satisfaction figures anywhere, because there are none
   that would be true. **Do not add invented ones.** Publishing fabricated
   testimonials is deceptive advertising under India's Consumer Protection Act
   and the FTC's endorsement rules, and for this product specifically it would
   destroy the one thing it runs on. Real numbers become available in Phase 7,
   when the feedback wall goes live.

---

## 11. Questions worth asking before extending this

Anyone adding a feature should be able to answer these:

- Does it cause vent text to be transmitted, stored or logged? If so, stop.
- Does it require an account in order to vent? If so, stop.
- Does it add a column to `vent_events`? If so, stop.
- Does it claim something about usage or outcomes that is not measurably true?
  If so, stop.

The tests will catch the first three. Nothing catches the fourth except judgment.
