// Thin typed fetch wrapper.
// - Parses RFC 9457 ProblemDetail error bodies into a typed ApiError (incl. field errors).
// - Retries idempotent GETs with exponential backoff on transient (5xx / network) failures.

export interface ApiError extends Error {
  status: number;
  detail: string;
  fieldErrors?: Record<string, string>;
}

function makeApiError(status: number, detail: string, fieldErrors?: Record<string, string>): ApiError {
  const err = new Error(detail) as ApiError;
  err.name = "ApiError";
  err.status = status;
  err.detail = detail;
  err.fieldErrors = fieldErrors;
  return err;
}

const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));

async function parseError(res: Response): Promise<ApiError> {
  try {
    const body = await res.json();
    return makeApiError(
      res.status,
      body.detail ?? body.title ?? res.statusText,
      body.fieldErrors,
    );
  } catch {
    return makeApiError(res.status, res.statusText || "Request failed");
  }
}

interface RequestOptions {
  method?: string;
  body?: unknown;
  retries?: number; // number of retry attempts for transient failures (GET only)
}

export async function request<T>(path: string, opts: RequestOptions = {}): Promise<T> {
  const method = opts.method ?? "GET";
  const maxRetries = method === "GET" ? opts.retries ?? 2 : 0;

  let attempt = 0;
  // Retry loop: only for transient failures on safe (GET) requests.
  for (;;) {
    try {
      const res = await fetch(`/api${path}`, {
        method,
        headers: opts.body ? { "Content-Type": "application/json" } : undefined,
        body: opts.body ? JSON.stringify(opts.body) : undefined,
      });

      if (res.status === 204) {
        return undefined as T;
      }
      if (!res.ok) {
        // 5xx is transient and worth retrying; 4xx is the caller's fault and is not retried.
        if (res.status >= 500 && attempt < maxRetries) {
          attempt++;
          await sleep(150 * 2 ** (attempt - 1));
          continue;
        }
        throw await parseError(res);
      }
      return (await res.json()) as T;
    } catch (e) {
      // Network error (fetch threw): retry if we still can.
      if (e instanceof Error && e.name !== "ApiError" && attempt < maxRetries) {
        attempt++;
        await sleep(150 * 2 ** (attempt - 1));
        continue;
      }
      throw e;
    }
  }
}
