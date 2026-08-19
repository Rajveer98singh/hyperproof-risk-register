import { afterEach, describe, expect, it, vi } from "vitest";
import { request, type ApiError } from "./client";

function jsonResponse(status: number, body: unknown): Response {
  return {
    ok: status >= 200 && status < 300,
    status,
    statusText: "",
    json: async () => body,
  } as Response;
}

afterEach(() => vi.restoreAllMocks());

describe("api client", () => {
  it("retries a GET on 5xx then succeeds", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse(500, { detail: "boom" }))
      .mockResolvedValueOnce(jsonResponse(200, { ok: true }));
    vi.stubGlobal("fetch", fetchMock);

    const result = await request<{ ok: boolean }>("/risks");

    expect(result).toEqual({ ok: true });
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });

  it("parses a ProblemDetail 400 into fieldErrors and does not retry", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      jsonResponse(400, {
        status: 400,
        detail: "Validation failed",
        fieldErrors: { likelihood: "must be between 1 and 5" },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    await expect(request("/risks", { method: "POST", body: {} })).rejects.toMatchObject({
      status: 400,
      fieldErrors: { likelihood: "must be between 1 and 5" },
    } as Partial<ApiError>);
    expect(fetchMock).toHaveBeenCalledTimes(1); // POST is not retried
  });
});
