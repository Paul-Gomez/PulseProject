import { HttpError } from "./errors";
import type { TokenPair } from "./types";

const ACCESS_KEY = "pulse.accessToken";
const REFRESH_KEY = "pulse.refreshToken";

export const tokens = {
  get access() {
    return typeof window === "undefined" ? null : localStorage.getItem(ACCESS_KEY);
  },
  get refresh() {
    return typeof window === "undefined" ? null : localStorage.getItem(REFRESH_KEY);
  },
  store(pair: TokenPair) {
    localStorage.setItem(ACCESS_KEY, pair.accessToken);
    localStorage.setItem(REFRESH_KEY, pair.refreshToken);
  },
  clear() {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
  },
};

// Called when the session can't be recovered (refresh token rejected).
let onSessionExpired: () => void = () => {};
export function setSessionExpiredHandler(handler: () => void) {
  onSessionExpired = handler;
}

// If several requests get a 401 at the same time they all wait for this one refresh.
let refreshInFlight: Promise<boolean> | null = null;

async function refreshTokens(fetchImpl: typeof fetch): Promise<boolean> {
  const refreshToken = tokens.refresh;
  if (!refreshToken) return false;
  refreshInFlight ??= fetchImpl("/api/v1/auth/refresh", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ refreshToken }),
  })
    .then(async (res) => {
      if (!res.ok) return false;
      tokens.store((await res.json()) as TokenPair);
      return true;
    })
    .catch(() => false)
    .finally(() => (refreshInFlight = null));
  return refreshInFlight;
}

/**
 * fetch wrapper for the Pulse API: adds the access token, parses JSON, throws HttpError,
 * and on a 401 refreshes the token pair once and repeats the request.
 */
export async function api<T>(path: string, init: RequestInit = {}, fetchImpl: typeof fetch = fetch): Promise<T> {
  const isAuthCall = path.startsWith("/auth/");

  const send = () => {
    const headers = new Headers(init.headers);
    if (init.body && !(init.body instanceof FormData)) headers.set("Content-Type", "application/json");
    const token = tokens.access;
    if (token && !isAuthCall) headers.set("Authorization", `Bearer ${token}`);
    return fetchImpl(`/api/v1${path}`, { ...init, headers });
  };

  let response = await send();
  if (response.status === 401 && !isAuthCall) {
    if (await refreshTokens(fetchImpl)) {
      response = await send();
    } else {
      tokens.clear();
      onSessionExpired();
    }
  }

  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new HttpError(response.status, body);
  }
  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}

export const json = (body: unknown): RequestInit["body"] => JSON.stringify(body);
