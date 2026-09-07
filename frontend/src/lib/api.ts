/**
 * Thin fetch wrapper for the HeadHeartFreeS API.
 *
 * Rule 2.1 (PROJECT_BRIEF.md): vent text never leaves the browser. Nothing in
 * this module - now or later - may accept or forward what a person wrote in the
 * vent textarea. The release call sends an optional mood label and nothing else.
 *
 * NEXT_PUBLIC_API_BASE_URL is inlined into the bundle at build time. Changing it
 * requires a rebuild, not a restart.
 */

export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

/** The single error shape every endpoint returns (PROJECT_BRIEF.md section 6). */
export interface ApiErrorBody {
  timestamp: string;
  status: number;
  code: string;
  message: string;
  path: string;
  fieldErrors?: Record<string, string>;
}

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly fieldErrors: Record<string, string>;
  /** Seconds to wait, parsed from `Retry-After` on a 429. */
  readonly retryAfterSeconds: number | null;

  constructor(
    status: number,
    code: string,
    message: string,
    fieldErrors: Record<string, string> = {},
    retryAfterSeconds: number | null = null,
  ) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
    this.fieldErrors = fieldErrors;
    this.retryAfterSeconds = retryAfterSeconds;
  }
}

export interface ApiRequestOptions {
  method?: "GET" | "POST" | "PATCH" | "PUT" | "DELETE";
  /** JSON-serialisable request body. */
  body?: unknown;
  signal?: AbortSignal;
  /**
   * Short-lived access token for the `Authorization` header.
   *
   * Deliberately a single named option rather than an open `headers` bag. The
   * only header this application ever needs to vary is this one, and a general
   * escape hatch is how a caller eventually attaches something it should not -
   * on this codebase, the thing it must not attach is anything derived from
   * what someone wrote in the vent box (rule 2.1).
   *
   * Callers should not read this from anywhere durable. `SessionProvider`
   * holds the token in memory for the life of the tab and passes it here; it
   * is never in localStorage or a cookie.
   */
  accessToken?: string | null;
  /**
   * Set `false` for a request that must not be cancellable.
   *
   * When false, any `signal` passed in is **dropped** before it reaches
   * `fetch`, so nothing tied to a component lifetime can abort the request
   * partway.
   *
   * This is forward-looking rather than a fix for a live bug: nothing in this
   * codebase currently constructs an `AbortController` or passes a signal, so
   * today there is nothing to drop. It exists because the two calls that use
   * it have the same shape — the component that starts them unmounts as a
   * direct result of them succeeding — and both fail badly and silently if
   * cut short:
   *
   * - **logout**: the client looks signed out while the refresh token stays
   *   live on the server.
   * - **feedback submission**: the note silently vanishes. Someone who has
   *   just written something and pressed send is told it worked, and it is
   *   nowhere.
   *
   * A submission that errors is recoverable. One that evaporates is not, and
   * on this site it happens directly after the person let something go.
   */
  abortable?: boolean;
}

/**
 * Calls the API and returns the parsed JSON body.
 *
 * Credentials are included on every request so the browser attaches the
 * httpOnly refresh cookie. That cookie is scoped to `/api/v1/auth`, so it rides
 * along on the auth calls that need it and on nothing else - in particular not
 * on `/api/v1/vent/**`, where an ambient identifier is precisely what rule 2.2
 * is written against.
 *
 * @throws {ApiError} on any non-2xx response.
 */
export async function apiFetch<T>(
  path: string,
  options: ApiRequestOptions = {},
): Promise<T> {
  const { method = "GET", body, signal, accessToken, abortable = true } = options;

  // Dropped rather than merely ignored. If a caller ever passes an
  // effect-scoped signal to a request marked unabortable, the correct outcome
  // is that the request completes, not that it quietly inherits a cancellation
  // it asked to be exempt from.
  const effectiveSignal = abortable ? signal : undefined;

  // Sent on every request, from here and nowhere else.
  //
  // The backend requires it on POST /api/v1/auth/refresh and /logout: those are
  // state-changing and authenticated only by the httpOnly refresh cookie, which
  // the browser attaches by itself, so a cross-site page could otherwise cause
  // one. A cross-site <form> cannot set a header, and setting this one from
  // script costs a CORS preflight against the origin allowlist.
  //
  // It rides along on the unguarded calls too, deliberately. The alternative is
  // a per-path rule here that has to stay in step with CsrfHeaderFilter, and
  // the cost of sending it everywhere is nothing: /api/v1/vent/** neither
  // requires it nor rejects it, so rule 2.2 is untouched and any other client
  // still works with no ceremony.
  const headers: Record<string, string> = {
    Accept: "application/json",
    "X-Requested-With": "fetch",
  };
  if (body !== undefined) {
    headers["Content-Type"] = "application/json";
  }
  if (accessToken) {
    headers.Authorization = `Bearer ${accessToken}`;
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    method,
    credentials: "include",
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
    ...(effectiveSignal ? { signal: effectiveSignal } : {}),
  });

  if (!response.ok) {
    throw await toApiError(response);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

async function toApiError(response: Response): Promise<ApiError> {
  const retryAfter = Number.parseInt(response.headers.get("Retry-After") ?? "", 10);
  const retryAfterSeconds = Number.isNaN(retryAfter) ? null : retryAfter;

  let parsed: Partial<ApiErrorBody> = {};
  try {
    parsed = (await response.json()) as Partial<ApiErrorBody>;
  } catch {
    // Non-JSON error page (proxy timeout, for instance). Fall through to the
    // generic message below.
  }

  return new ApiError(
    response.status,
    parsed.code ?? "UNKNOWN",
    parsed.message ?? `Request failed with status ${response.status}`,
    parsed.fieldErrors ?? {},
    retryAfterSeconds,
  );
}
