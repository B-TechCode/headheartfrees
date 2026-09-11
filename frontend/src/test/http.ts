import { vi } from "vitest";

/**
 * Enough of `Response` for `apiFetch` to work with.
 *
 * `apiFetch` reads exactly four things — `ok`, `status`, `headers.get(...)` and
 * `json()` — so this is a complete stand-in for its purposes without pulling in
 * a fetch-mocking library. If `apiFetch` ever reads a fifth, these tests fail
 * loudly rather than quietly passing against a fiction.
 */
export function jsonResponse(
  status: number,
  body: unknown,
  headers: Record<string, string> = {},
): Response {
  const lower = new Map(Object.entries(headers).map(([k, v]) => [k.toLowerCase(), v]));
  return {
    ok: status >= 200 && status < 300,
    status,
    headers: { get: (name: string) => lower.get(name.toLowerCase()) ?? null },
    json: async () => body,
  } as unknown as Response;
}

export function noContent(): Response {
  return jsonResponse(204, null);
}

/**
 * The shape of a 429 as the API actually sends it, `Retry-After` included.
 *
 * The header is only readable in a real browser because `SecurityConfig`
 * exposes it across origins; including it here keeps these tests honest about
 * what the client is entitled to see.
 */
export function tooManyRequests(retryAfterSeconds: number): Response {
  return jsonResponse(
    429,
    {
      timestamp: new Date().toISOString(),
      status: 429,
      code: "RATE_LIMITED",
      message: "Too many requests.",
      path: "/api/v1/auth/refresh",
    },
    { "Retry-After": String(retryAfterSeconds) },
  );
}

export function unauthorized(path = "/api/v1/auth/refresh"): Response {
  return jsonResponse(401, {
    timestamp: new Date().toISOString(),
    status: 401,
    code: "UNAUTHORIZED",
    message: "Invalid email or password.",
    path,
  });
}

/** A `UserSummary` with the fields the UI reads. */
export function aUser(overrides: Record<string, unknown> = {}) {
  return {
    id: "9f1d1f2a-0000-4000-8000-000000000001",
    email: "person@example.com",
    displayName: "Ada",
    role: "USER",
    emailVerified: false,
    createdAt: "2026-01-05T09:30:00Z",
    // The default is an ordinary USER with no second factor, because that is
    // the ordinary case and the one most likely to regress unnoticed.
    totpEnabled: false,
    totpRequired: false,
    backupCodesRemaining: 0,
    ...overrides,
  };
}

/**
 * The body of a successful refresh - a bare `AccessTokenResponse`.
 *
 * `/refresh` still returns this shape. `/login` and `/login/totp` wrap it, see
 * `anAuthenticatedLogin` below.
 */
export function aSession(overrides: Record<string, unknown> = {}) {
  return {
    accessToken: "header.payload.signature",
    tokenType: "Bearer",
    expiresIn: 900,
    user: aUser(),
    ...overrides,
  };
}

/** `POST /auth/login` when nothing further is owed. */
export function anAuthenticatedLogin(sessionOverrides: Record<string, unknown> = {}) {
  return { status: "AUTHENTICATED", session: aSession(sessionOverrides) };
}

/**
 * `POST /auth/login` when the account holds a second factor.
 *
 * Note what is NOT here: no `session`, no `accessToken`, no `user`. The key is
 * absent rather than null, which is exactly how the API sends it - so a client
 * that tried to read a session off this would get `undefined` rather than
 * something half-shaped.
 */
export function aTotpChallenge(ticket = "ticket.for.the.code.step") {
  return { status: "TOTP_REQUIRED", ticket, ticketExpiresIn: 300 };
}

/** `POST /auth/login` for an ADMIN that has never enrolled. */
export function anEnrolmentChallenge(ticket = "ticket.for.enrolment") {
  return { status: "TOTP_ENROLMENT_REQUIRED", ticket, ticketExpiresIn: 300 };
}

/** The single generic 401 every failure of the code step returns. */
export function invalidTotpCode(path = "/api/v1/auth/login/totp"): Response {
  return jsonResponse(401, {
    timestamp: new Date().toISOString(),
    status: 401,
    code: "INVALID_TOTP_CODE",
    message:
      "That code is not valid. Check your authenticator app and that your phone's clock is " +
      "set automatically, then try again.",
    path,
  });
}

/**
 * Installs a `fetch` stub and hands back the mock.
 *
 * Nothing is routed by default: an unexpected call rejects with a message
 * naming the URL, so a component that starts making requests it should not is a
 * failure rather than a silent `undefined`.
 */
export function stubFetch(handler: (url: string, init: RequestInit) => Response | Promise<Response>) {
  const fetchMock = vi.fn(async (input: unknown, init: RequestInit = {}) =>
    handler(String(input), init),
  );
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

/** Every URL that was requested, in order. */
export function requestedUrls(fetchMock: ReturnType<typeof stubFetch>): string[] {
  return fetchMock.mock.calls.map((call) => String(call[0]));
}
