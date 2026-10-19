import { beforeEach, describe, expect, it, vi } from "vitest";
import { api, setSessionExpiredHandler, tokens } from "./api";
import { HttpError } from "./errors";

// Minimal browser globals for code that reads localStorage.
const store = new Map<string, string>();
Object.assign(globalThis, {
  window: globalThis,
  localStorage: {
    getItem: (key: string) => store.get(key) ?? null,
    setItem: (key: string, value: string) => store.set(key, value),
    removeItem: (key: string) => store.delete(key),
  },
});

const reply = (status: number, body?: unknown) =>
  new Response(body === undefined ? null : JSON.stringify(body), { status });

describe("api client", () => {
  beforeEach(() => {
    store.clear();
    tokens.store({ accessToken: "old-access", refreshToken: "refresh-1" });
  });

  it("sends the access token to the API", async () => {
    const fetchMock = vi.fn(async () => reply(200, { ok: true }));

    await api("/workspaces", {}, fetchMock);

    const [url, init] = fetchMock.mock.calls[0] as unknown as [string, RequestInit];
    expect(url).toBe("/api/v1/workspaces");
    expect(new Headers(init.headers).get("Authorization")).toBe("Bearer old-access");
  });

  it("refreshes the tokens after a 401 and repeats the request once", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(reply(401))
      .mockResolvedValueOnce(reply(200, { accessToken: "new-access", refreshToken: "refresh-2" }))
      .mockResolvedValueOnce(reply(200, { id: "1" }));

    const result = await api<{ id: string }>("/users/me", {}, fetchMock);

    expect(result).toEqual({ id: "1" });
    expect(fetchMock.mock.calls[1][0]).toBe("/api/v1/auth/refresh");
    const retry = fetchMock.mock.calls[2][1] as RequestInit;
    expect(new Headers(retry.headers).get("Authorization")).toBe("Bearer new-access");
    expect(tokens.refresh).toBe("refresh-2");
  });

  it("ends the session when the refresh token is rejected too", async () => {
    const expired = vi.fn();
    setSessionExpiredHandler(expired);
    const fetchMock = vi.fn().mockResolvedValueOnce(reply(401)).mockResolvedValueOnce(reply(401)).mockResolvedValue(reply(401));

    await expect(api("/users/me", {}, fetchMock)).rejects.toBeInstanceOf(HttpError);
    expect(expired).toHaveBeenCalled();
    expect(tokens.access).toBeNull();
  });

  it("exposes the backend error code", async () => {
    const fetchMock = vi.fn(async () => reply(409, { code: "CHANNEL_NAME_TAKEN", message: "taken" }));

    await expect(api("/workspaces/1/channels", { method: "POST" }, fetchMock)).rejects.toMatchObject({
      status: 409,
      body: { code: "CHANNEL_NAME_TAKEN" },
    });
  });
});
