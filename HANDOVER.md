# Handover — HeadHeartFreeS

Everything the owner of this project needs to run it, and everything that has to
change hands. Written to be read by someone who did not build it.

Last updated: 2026-09-10. Phases 1–9 complete; see `PHASE_LOG.md` for the full
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

### 2.5 ~~A contact email address~~ — SUPPLIED 2026-09-10

Nothing to do here. The address is `headheartfrees@gmail.com`, it is set in
`frontend/src/lib/contact.ts`, and the "not live yet" notice on `/contact` is
gone because `CONTACT_EMAIL_IS_PLACEHOLDER` is now `false`.

**The account belongs to the project, not to a person.** It is not anyone's
personal mailbox, so it changes hands with the site: at handover the password
and any recovery details go to the new owner, and whoever held it before loses
access. Nobody has to rewrite the site, reprint anything, or ask visitors to
use a different address.

The kept-numbering here is deliberate: sections 2.7 and 2.8 are referred to by
`frontend/src/lib/support.ts` and by `PHASE_LOG.md`, so this heading stays
rather than renumbering the rest.

### 2.5.1 What comes with the inbox

Worth reading before you take the password, and worth passing on to whoever
takes it after you.

This is a public address on a mental health site, so some of what arrives will
be hard to read. Most of it will be ordinary — a broken link, a wrong number, a
question about privacy. Some of it will not be. Sooner or later somebody will
write to this inbox in real distress, or write something that stays with you
after you have closed the tab. That is not a sign anything has gone wrong with
the site; it is what having a public address on a site like this one means.

Two things make it survivable. The first is knowing what the inbox can and
cannot do — `/contact` says plainly that it is not a crisis line and is not
watched overnight, and that page is the honest answer to a message you cannot
answer in time. Do not let it drift into promising more. The second is that the
helplines on `/crisis-resources` are open to you as well. They are not only for
the people writing in. If something you read here sits with you, that is an
ordinary response to reading it, and those numbers are yours to call.

The moderation queue carries the same note for the person reviewing feedback
(`ModeratorSupport` in `frontend/src/components/sections/ModerationQueue.tsx`),
for the same reason. If the inbox and the queue end up with different people,
both of them should have read this.

### 2.6 A decision on who moderates feedback

The feedback wall is reviewed before anything is published (Phase 7). Someone
has to do that reviewing, and that person needs an admin account. See §6.

### 2.7 A payment method — and there are TWO lines to change

`/support` is complete apart from the owner's payment identifier. It is one
line:

```
frontend/src/lib/support.ts  line 30
export const SUPPORT_UPI_ID = "REPLACE-ME@example.invalid";
```

**And a second line, which is easy to miss:**

```
frontend/src/lib/support.ts  line 41
export const SUPPORT_PAYMENT_IS_PLACEHOLDER = true;
```

Set the real ID and change that flag to `false` **in the same edit**. They fail
in opposite directions if only one is done: a real ID with the flag still `true`
leaves the notice up and the payment details hidden, so nobody can give
anything; clearing the flag without a real ID publishes an invalid address, and
money sent to a valid-but-wrong VPA reaches a stranger.

While the flag is `true` the page carries a visible notice saying there is no
way to give anything yet, worded for a visitor rather than as a status message,
and renders no payment details at all. That notice is the reason a
half-configured page cannot quietly read as a working one.

**A UPI ID needs no integration** — no SDK, no API key, no provider account, no
webhook. The ID and a link are the entire mechanism, which is why there is no
payment configuration anywhere else in this repository. A scannable QR code is
the natural next step once a real ID exists; it can be generated from the same
`upi://` link the page already builds.

This is the owner's identifier and their decision. It was not guessed at.

### 2.8 A decision on the legal and tax position of accepting money

**Resolve this before enabling payments, not after.** None of it is a
developer's call and none of it has been assumed anywhere in the code or the
copy:

- **Who receives the money.** An individual, or a registered entity — a
  society, trust or Section 8 company. The answer changes everything below.
- **Income tax treatment.** Money received by an individual and money received
  by a registered nonprofit are taxed differently. Donations to an individual
  are generally income.
- **FCRA.** If funds ever arrive from outside India, the Foreign Contribution
  (Regulation) Act applies and registration is required *in advance*. A UPI ID
  posted on a public page is reachable from anywhere, so this is not
  hypothetical the moment the page goes live.
- **80G and similar.** Only certain registered entities can offer tax-deductible
  receipts.

**Nothing on `/support` states or implies any tax status, and nothing may be
added that does.** No "tax-deductible", no "80G", no "registered charity", no
receipt language. Adding any of those without the registration behind it is a
misrepresentation to the person giving, and the page is written so there is no
half-true sentence to extend. If the status is later established, the copy can
say so — with the registration number.

### 2.9 Social links — and these are the developer's own accounts

**This is the one placeholder a visitor cannot see, and the only one that is
live.** The footer of every page currently links to four accounts belonging to
the person who built this site.

All four are in one file:

```
frontend/src/lib/social.ts  lines 47-60
export const SOCIAL_LINKS = [
  LinkedIn   line 50   https://www.linkedin.com/in/aakashprasadchaurasiya/
  Facebook   line 55   https://www.facebook.com/aakash.chaurasiya.232668/
  GitHub     line 58   https://github.com/B-TechCode
  Portfolio  line 59   https://www.aakashchaurasiya.com.np/
]
```

And a flag beside them:

```
frontend/src/lib/social.ts  line 71
export const SOCIAL_LINKS_ARE_PLACEHOLDER = true;
```

Replace the four URLs with the project's own accounts and set the flag to
`false`. If the project has no such accounts, **delete the entries** — an empty
`SOCIAL_LINKS` renders no row and breaks nothing.

**Why this one needs deliberate attention.** The other two placeholders in this
section protect themselves. `CONTACT_EMAIL` and `SUPPORT_UPI_ID` are visibly
fake strings, and while their flags are `true` the pages carrying them show a
notice telling the visitor the thing is not connected yet. Forget either one
and the site says so, out loud, to everybody.

These are real, working links, and **nothing on the rendered page marks them as
temporary.** The flag above changes nothing on screen — deliberately, because
a footer captioned "these profiles belong to the developer" would be strange to
a visitor and would not stop anyone clicking. So this file's header comment,
this section, and §4 are the entire safety net.

The failure mode is therefore quiet and open-ended: a site handed over with
this file untouched sends its visitors to a stranger's LinkedIn, from the
footer of every page, for as long as it stays up. Nobody will report it,
because to a visitor it looks like it is working.

**Check `frontend/src/lib/social.ts` at handover even if nothing prompts you.**

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

The redirect lands on `/auth/callback`. **That page now exists** (Phase 6) and
serves; the 404 recorded above is closed.

What it has not done is run. Nobody has completed a Google sign-in *through* the
new page, so the step where the browser replays the refresh cookie to
`POST /api/v1/auth/refresh` — the thing the page exists for — is still reasoned
from the spec rather than observed. `curl` has no concept of `SameSite`, so the
HTTP-level checks in the phase 6 log do not cover it. **This is one page load
and should be the first thing anyone does with this build.**

### 3.5 ~~The remaining Phase 9 items~~ — DONE 2026-09-08

Every item that stood here is closed. For the record, and because this section
previously listed them as blockers:

| Was blocking | Now |
|---|---|
| Security headers | CSP, `nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer`, `Permissions-Policy`. Verified in a real browser across 14 routes, 0 violations. |
| CORS lockdown | `APP_CORS_ALLOWED_ORIGINS`; the app **refuses to start** on a wildcard, a schemeless origin or a trailing slash. |
| Swagger UI in production | `/v3/api-docs` and Swagger UI are off unless the `local` profile is active. Pinned by a test that boots on a non-local profile. |
| Rate limiting behind a proxy | `X-Forwarded-For` honoured only from `APP_RATE_LIMIT_TRUSTED_PROXIES`, read right-to-left. **You must set this** — see 3.6. |
| Refresh token cleanup | Hourly job, 30 days past expiry. |
| Accessibility audit | Lighthouse/axe over all 14 routes: **100, zero failures**. |

### 3.6 Set the trusted proxy address once you have a reverse proxy

This is the one Phase 9 control that is **deliberately left switched off**,
because only the owner knows their topology.

`APP_RATE_LIMIT_TRUSTED_PROXIES` is empty by default. Empty means
`X-Forwarded-For` is ignored and every caller behind the proxy shares one
rate-limit bucket — so one person hitting the login limit locks out everybody.
Set it to the proxy's own address and per-user limits start working.

**Do not set it to a wildcard, and do not trust the header from anything else.**
The app refuses a wildcard at startup. An unconditionally trusted
`X-Forwarded-For` is worse than ignoring it: it is client-supplied, so anyone
who wants to defeat the limiter sends a different value on every request.

### 3.7 Turn HSTS on last, and understand that it is one-way

`ENABLE_HSTS` is `false`, and the primary place for the header is your TLS
terminator rather than the app.

**Enabling it is close to irreversible for the length of `max-age`** — one year
as configured. A browser that has seen the header refuses plain HTTP to this
host until it expires, and there is no way to reach into someone's browser and
retract it. So: get TLS working, confirm it, leave it working for a while, and
only then turn this on. If a certificate problem happens afterwards, visitors
are locked out rather than degraded.

It is a **build-time** flag: `headers()` is resolved when Next builds, so
changing it needs `docker compose build frontend`, not a restart.

### 3.8 A dependency advisory you are inheriting

`npm audit` reports 2 advisories (1 high) in **postcss**, reachable only as a
transitive dependency of `next`:

- XSS via an unescaped `</style>` in stringify output
- Arbitrary `.map` file read via an attacker-controlled `sourceMappingURL`

**Both are build-time paths, not runtime ones.** postcss runs when Tailwind
compiles CSS during `npm run build`, on CSS this repository controls. Neither
advisory is reachable by a visitor: no user input reaches postcss, and postcss
is not in the runtime image at all — the standalone server bundle contains
compiled CSS, not the compiler.

**What it would take to close:** `npm audit fix --force` installs **Next 16**, a
major version upgrade. That was deliberately not done in the final phase of the
project, because a major Next upgrade changes routing, caching and build
behaviour and there would be no time left to find what it broke. It is a
contained, well-understood piece of work for whoever picks this up next, and it
should be done deliberately with the suite green before and after.

---

## 4. What stays with the developer, and what to revoke

At handover the owner should assume the developer retains nothing, and verify it:

| Item | Action |
|---|---|
| Developer's Google OAuth client | Developer deletes it. It only worked on localhost. |
| Developer's local `.env` | Never committed; developer deletes it. |
| GitHub repository access | Transfer ownership or remove the developer as a collaborator. |
| The contact inbox (`headheartfrees@gmail.com`) | **Transfers, not revoked.** It is the project's account, not a person's — hand over the password and recovery details, then change the password and remove any other recovery address or device still attached. See §2.5. |
| Any deployment or database access | Rotate credentials after handover, regardless of trust. |
| **The developer's address in `APP_ADMIN_BOOTSTRAP_EMAILS`** | **Remove it.** Adding the owner's address is only half the job: the variable is a list, and an address left in it is re-promoted to ADMIN on every restart. Doing one and forgetting the other leaves the developer with permanent admin access — and therefore the moderation queue — on a site they no longer run. Removing the address does not demote an existing admin, so run the SQL below as well. |
| Developer's social and portfolio links in the site footer | **Still live on every page.** These are the developer's personal accounts, not the project's, and unlike the other placeholders nothing on screen says so. Replace or delete the four URLs in `frontend/src/lib/social.ts` — see §2.9. This is the item most likely to be missed, because nothing breaks if it is. |

After editing `APP_ADMIN_BOOTSTRAP_EMAILS`, demote the developer's account and
confirm who is left:

```sql
-- Demote the developer.
UPDATE users SET role = 'USER' WHERE email = 'developer@example.com';

-- Then check. This should list the owner's address and nothing else.
SELECT email, role FROM users WHERE role = 'ADMIN';
```

Restart the backend afterwards and run the SELECT once more. If the developer's
address is still in the variable, the restart puts it straight back, and the
second SELECT is what tells you.

Rotating credentials at handover is normal practice and is not a statement about
anyone. It is simply how you make the boundary real.

---

## 5. Setting up Google sign-in in production

The backend needs exactly two environment variables, and the frontend needs one
more. **No code changes.** If the backend pair is missing the site runs normally
with no Google option, and password sign-in is unaffected.

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
   Then tell the frontend to draw the button, and **rebuild it**:
   ```
   NEXT_PUBLIC_GOOGLE_SIGN_IN=true
   docker compose build frontend
   ```
   This is a second setting rather than one because the backend has no endpoint
   yet that says which sign-in providers it has, so the frontend cannot ask.
   Getting it wrong is visible, not silent: set here without the backend
   configured and the button leads to a 404; left `false` with the backend
   configured and there is simply no button, while password sign-in works. A
   restart will not pick it up — `NEXT_PUBLIC_*` values are baked in at build
   time.
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

### The admin account requires two-step sign-in

**An admin signs in with an email, a password, and a six-digit code from an
authenticator app.** This is not optional and cannot be turned off from inside
the site. It applies to ADMIN accounts only; ordinary users may turn it on if
they want it, and most will not.

The admin account is the only one on this site that can read the moderation
queue — every note a stranger submitted, including the ones never published. A
password on its own is not enough for that.

**What happens the first time an admin signs in after this was deployed,
including the account you were just promoted:**

1. Email and password, as before.
2. Instead of the site, a setup screen appears: a QR code, the same key written
   out as text, and a box for a code.
3. Scan the QR with any authenticator app — Google Authenticator, 1Password,
   Bitwarden, Aegis. Or type the key in by hand if the camera will not play.
4. Enter the six digits the app shows. **Nothing is switched on until a real
   code has worked**, so a mistyped key fails here where you can see it rather
   than at your next sign-in where it would lock you out.
5. Ten backup codes appear. Read the next section before clicking past them.
6. Clicking through signs you in. You do not have to sign in again.

**Nobody is locked out by this.** The password still works and still reaches
that screen. The only thing it no longer reaches on its own is the moderation
queue.

One consequence worth knowing: **any session an admin had open before this was
deployed is ended.** The first time such a session tries to renew itself the
server revokes it and asks for a fresh sign-in. That is deliberate — without it,
a browser signed in before the change would keep working for thirty days without
ever meeting the requirement.

### Google sign-in does not work on an admin account

Deliberately. Google is a second, separate route to a session that never touches
the password form, so an admin signing in through it would be using one factor
and the requirement would mean nothing.

An admin who clicks the Google button is returned to `/login` with a message
saying admin accounts use a password and a code. Nothing is broken and nothing
is written to the account — if the role is ever removed, Google sign-in simply
works again.

Ordinary users are unaffected: Google works for them exactly as it always did.

### Backup codes — where they come from, and that they are shown once

Ten codes are generated at the moment two-step sign-in is turned on, and shown
on the screen immediately after. **That is the only time they exist anywhere but
in your own hands.** They are hashed the way passwords are hashed before they
reach the database, and there is no page, no endpoint and no support request
that can show them to you again.

Each one signs you in once, in place of a code from the app, and is then dead.

Save them somewhere you can reach **without this site and without your phone**:
a password manager, or paper in a drawer. If you close that screen without
saving them, generate a new set from `/account` while your authenticator still
works — that page shows how many you have left and replaces all ten at once.

### If an admin is locked out — the recovery procedure

Reach for this when the phone is gone **and** the backup codes are gone. It is
the only way back, and it needs database access, which means it needs the owner
or whoever operates the server.

Connect to the database — under compose:

```
docker compose exec db psql -U headheartfrees -d headheartfrees
```

Confirm which account you mean before changing anything:

```sql
SELECT u.id, u.email, u.role,
       t.confirmed_at IS NOT NULL AS has_two_step,
       (SELECT count(*) FROM user_totp_backup_code b
         WHERE b.user_id = u.id AND b.used_at IS NULL) AS unused_backup_codes
FROM users u
LEFT JOIN user_totp t ON t.user_id = u.id
WHERE u.email = 'them@example.com';
```

Then remove the second factor for that account, and only that account:

```sql
-- Removes the enrolment. The account keeps its password, its role, and
-- everything else. The WHERE clause is a subquery on the email rather than a
-- pasted UUID so there is nothing to mistype.
DELETE FROM user_totp
 WHERE user_id = (SELECT id FROM users WHERE email = 'them@example.com');

-- The backup codes belonged to the enrolment that no longer exists.
DELETE FROM user_totp_backup_code
 WHERE user_id = (SELECT id FROM users WHERE email = 'them@example.com');

-- And end every session, on every device. The person locked out is not
-- necessarily the only person who has been trying.
UPDATE refresh_tokens SET revoked_at = now()
 WHERE revoked_at IS NULL
   AND user_id = (SELECT id FROM users WHERE email = 'them@example.com');
```

No restart is needed. The account is now back to where it was before enrolment:
the next sign-in with the password lands on the setup screen, a new QR is
generated, and a new set of backup codes is issued.

**Two warnings.**

*Do not* try to "fix" this by editing `user_totp` — setting `confirmed_at` to
null, clearing `failed_attempts`, and so on. The row holds a secret that only
the lost phone knows. Deleting it is the operation; anything else leaves an
enrolment nobody can satisfy.

*Do* treat the need for this as worth asking about. A legitimate lockout is a
lost phone. It is also what an attacker who has the password would ask for.

---

## 7. Environment variables

Copy `.env.example` to `.env` and fill it in. `.env` is gitignored and must never
be committed.

### Required in production

| Variable | Notes |
|---|---|
| `APP_JWT_SECRET` | `openssl rand -base64 48`. App refuses to start without it. **Keep a copy — it cannot be regenerated.** |
| `APP_TOTP_ENCRYPTION_KEY` | `openssl rand -base64 32` — exactly 32 bytes, not 48. Encrypts the stored two-step secrets. App refuses to start on the committed default outside the `local` profile. **Keep a copy. See the warning below.** |
| `SPRING_DATASOURCE_URL` | Real database, not the compose default |
| `SPRING_DATASOURCE_USERNAME` | |
| `SPRING_DATASOURCE_PASSWORD` | |
| `APP_CORS_ALLOWED_ORIGINS` | The real frontend origin. No wildcard — the refresh cookie needs credentialed CORS. |
| `NEXT_PUBLIC_API_BASE_URL` | **Baked in at build time.** Changing it needs a frontend rebuild, not a restart. It also feeds the CSP's `connect-src`, so a wrong value blocks every API call in the browser. |
| `APP_RATE_LIMIT_TRUSTED_PROXIES` | Your reverse proxy's address. Empty means everyone behind it shares one rate-limit bucket. See 3.6. |

### Optional

| Variable | Default | Notes |
|---|---|---|
| `GOOGLE_CLIENT_ID` | unset | Both must be set or Google is simply unavailable |
| `GOOGLE_CLIENT_SECRET` | unset | |
| `APP_OAUTH2_SUCCESS_REDIRECT` | localhost | Where Google sends the browser afterwards |
| `APP_ADMIN_BOOTSTRAP_EMAILS` | empty | See §6 |
| `NEXT_PUBLIC_GOOGLE_SIGN_IN` | `false` | Draws the Google button on the frontend. **Must agree with `GOOGLE_CLIENT_ID`.** Build time, like every `NEXT_PUBLIC_*` value. Set it `true` when the backend has Google credentials, and rebuild the frontend. |
| `APP_COOKIE_SECURE` | `true` | Leave true. False only for a LAN IP with no TLS. |
| `APP_JWT_ACCESS_TOKEN_TTL` | `PT15M` | |
| `APP_JWT_REFRESH_TOKEN_TTL` | `P30D` | |
| `APP_TOTP_ISSUER` | `HeadHeartFreeS` | The name an authenticator app files the entry under |
| `APP_TOTP_CHALLENGE_TTL` | `PT5M` | How long a half-finished sign-in stays usable before the code must be entered |
| `APP_TOTP_DRIFT_STEPS` | `1` | 30-second steps accepted either side of now. **Leave it at 1.** Each extra step adds two more simultaneously valid codes and multiplies the guessing surface; the app refuses to start above 2. |
| `APP_TOTP_BACKUP_CODE_COUNT` | `10` | Recovery codes issued at enrolment |

### The two secrets you must not lose

`APP_JWT_SECRET` and `APP_TOTP_ENCRYPTION_KEY` are both generated once and kept
forever. Neither can be worked out from anything else, and neither can be
regenerated without consequences.

**Losing `APP_TOTP_ENCRYPTION_KEY` means every enrolled account must enrol
again.** Backup codes still work — they are hashed rather than encrypted — and
past those, the recovery SQL in §6 is what is left. Back it up wherever you back
up the database password.

**What that key does and does not protect, stated plainly.** It encrypts the
two-step secret at rest with AES-256. Somebody who obtains *only* the database —
a dump, a stolen backup, a read replica, a cloud console, a SQL injection —
gets ciphertext and nothing they can use. Somebody who has the database **and**
the application's environment can decrypt every secret and generate valid codes
for any account, indefinitely and silently. That includes anyone with root on
the server or read access to its environment variables. The key separates those
two situations. It is not a vault, and it is worth knowing which one you are
defending against before relying on it.

Without it — that is, if the secrets were stored as plain text — a database
backup left in the wrong bucket would be a complete bypass of two-step sign-in
for every account in it.

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

### If `npm run dev` serves a site where nothing works, read this before debugging a component

**`next.config.ts` sends a Content-Security-Policy, and `headers()` applies to
`next dev` exactly as it applies to a production build.** The dev server is not
the same application as a build: it runs React Fast Refresh, whose runtime calls
`eval`. So the policy is built per environment, and the development branch adds
three things production does not get — `'unsafe-eval'` on `script-src`,
`'unsafe-inline'` on `style-src`, and `ws: wss:` on `connect-src`.

**Remove `'unsafe-eval'` from the development branch and the entire client
bundle stops booting.** The `eval` call happens while a webpack module factory
in `main-app.js` is still executing, so the throw kills the chunk rather than
one feature. No client component anywhere on the site hydrates.

**It does not present as a CSP problem. It presents as a component bug**, and
usually as a bug in whatever page you happen to open — which is why this note is
here and not in the security section. The server HTML still renders, so every
page looks right; the site quietly degrades to what it would be with JavaScript
switched off, and most of this site is static content that survives that. `/vent`
does not: a textarea with no React `value` controlling it keeps typed text the
way plain HTML does, so the box accepts writing while the character counter sits
at `0 / 2000` and "Release & Let Go" never enables. It reads exactly like a
broken `onChange` handler in `VentComposer`. `VentComposer` is never mounted.

This happened, on 10 September 2026, and cost three commits before anyone
noticed. See the last entry in [PHASE_LOG.md](PHASE_LOG.md) for the full account.

**Confirming it takes one line in the browser console.** Ask a DOM node whether
React knows about it:

```js
Object.keys(document.querySelector('textarea')).filter(k => k.startsWith('__react'))
```

React attaches `__reactFiber$…`, `__reactProps$…` and `__reactEvents$…` to every
host node it mounts. An empty array means the component was never mounted, and
you should stop reading component source and look at the console for a CSP
`EvalError` from `@next/react-refresh-utils`. A non-empty array means React is
running and the fault really is in the component.

**When you change the CSP** — and you will, for a font, an analytics script or an
embed — add what you need to the production directives and leave the development
relaxations alone. `next.config.test.ts` pins both halves: the production policy
as one exact string, so a development relaxation cannot leak into a shipped
header, and the development `'unsafe-eval'`, so this outage cannot come back
silently. If it fails, it is telling you which of those two you just did.

**Never add `'unsafe-eval'` to production.** It hands an injected string the
ability to become code, which is most of what that header exists to prevent.

### Tests

```
cd backend
./mvnw verify        # 180 tests. Needs Docker: Testcontainers runs a real
                     # PostgreSQL 16.

cd frontend
npm test             # 92 tests. Vitest + Testing Library + jsdom. No browser,
                     # no backend, no Docker. One file, next.config.test.ts,
                     # is not a component test: it pins the CSP above.
npm run typecheck
npm run lint
```

The frontend suite is small and specific. Most of it exists to hold one
property: **the session must never gate the page.** `/vent` needs no account, and
the way that breaks is not a deliberate gate but a provider that withholds the
render for a few hundred milliseconds while it asks the server who is signed in.
`VentComposer.session.test.tsx` leaves a refresh permanently unresolved and then
writes and releases straight through it. If that file starts failing, read it
before changing it.

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
        ├── components/ ui/ primitives, layout/, sections/, auth/
        ├── lib/        API client, helplines, safety list, contact
        │   └── auth/   Session provider, hint cookie, return-to, Google
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

8. **The site uses two names, and this was chosen on 2026-09-13.** The navbar
   reads **HHFreeS**. The footer, page titles, browser tab, metadata and all
   body copy read **HeadHeartFreeS**. The client was shown this and chose it; it
   is not an oversight and it was not missed in review.

   Both names are inventoried so the work is already scoped if it is revisited:

   - **"HHFreeS" as user-visible text appears in exactly one place** —
     `frontend/src/components/layout/Navbar.tsx`, in the `NavBrand` component.
   - **"HeadHeartFreeS" appears in 20 files** — `app/layout.tsx` (the metadata
     title template), `app/icon.svg`, `components/layout/Footer.tsx`,
     `components/ui/Logo.tsx` (the `Logo` default `title`), the page files for
     `/`, `/about`, `/community-guidelines`, `/contact`, `/crisis-resources`,
     `/login`, `/privacy`, `/register` and `/support`, plus `lib/api.ts`,
     `lib/contact.ts`, `lib/support.ts` and four test files.

   Reconciling in either direction means editing the metadata title template,
   which changes every browser tab and every search result for the site. That is
   a client decision rather than a tidy-up, which is why it has not been done.

9. **The navbar logo is below the legibility it was measured against, and ships
   anyway at the client's request.** The supplied logo is an auto-trace of a
   JPEG. At the 32px it runs at in the navbar, the face profile is gone and mean
   ink contrast is 1.97:1, where the exhale mark it replaced held 4.45:1. The
   client was shown the renders and the measurements and chose it. PHASE_LOG
   (2026-09-13) has the full numbers at 24, 32 and 40px.

   The fix is a properly drawn vector, not a code change: real curves instead of
   1,100 straight line segments, one continuous stroke per element instead of
   eight posterised colour bands, and a face profile drawn to survive small
   sizes. When one exists, `MARK_H` in `NavBrand` is the single number to
   revisit. **The favicon is unaffected** — `app/icon.svg` is still the exhale
   mark, which is the only mark here that works at 16px.

---

## 11. Questions worth asking before extending this

Anyone adding a feature should be able to answer these:

- Does it cause vent text to be transmitted, stored or logged? If so, stop.
- Does it require an account in order to vent? If so, stop.
- Does it add a column to `vent_events`? If so, stop.
- Does it claim something about usage or outcomes that is not measurably true?
  If so, stop.

The tests will catch the first three. Nothing catches the fourth except judgment.
