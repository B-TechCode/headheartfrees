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
    ...overrides,
  };
}

/** The body of a successful login or refresh. */
export function aSession(overrides: Record<string, unknown> = {}) {
  return {
    accessToken: "header.payload.signature",
    tokenType: "Bearer",
    expiresIn: 900,
    user: aUser(),
    ...overrides,
  };
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
