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
}

/**
 * Calls the API and returns the parsed JSON body.
 *
 * Credentials are included on every request so the httpOnly refresh cookie is
 * sent once auth exists (phase 4).
 *
 * @throws {ApiError} on any non-2xx response.
 */
export async function apiFetch<T>(
  path: string,
  options: ApiRequestOptions = {},
): Promise<T> {
  const { method = "GET", body, signal } = options;

  const response = await fetch(`${API_BASE_URL}${path}`, {
    method,
    credentials: "include",
    headers: body === undefined ? { Accept: "application/json" } : {
      Accept: "application/json",
      "Content-Type": "application/json",
    },
    body: body === undefined ? undefined : JSON.stringify(body),
    ...(signal ? { signal } : {}),
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
