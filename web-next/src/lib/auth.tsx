"use client";

import { useRouter } from "next/navigation";
import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { api, json, setSessionExpiredHandler, tokens } from "./api";
import type { TokenPair, User } from "./types";

interface AuthContextValue {
  user: User | null;
  /** false until we know whether there is a stored session (avoids flashing the login page). */
  ready: boolean;
  login(email: string, password: string): Promise<void>;
  register(data: { email: string; username: string; password: string; displayName: string }): Promise<void>;
  logout(): void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const router = useRouter();
  const [user, setUser] = useState<User | null>(null);
  const [ready, setReady] = useState(false);

  const loadUser = useCallback(async () => {
    setUser(await api<User>("/users/me"));
  }, []);

  useEffect(() => {
    setSessionExpiredHandler(() => {
      setUser(null);
      router.replace("/login");
    });
    // localStorage only exists in the browser, so the session is restored after hydration.
    const restore = tokens.access
      ? api<User>("/users/me").then(setUser).catch(() => tokens.clear())
      : Promise.resolve();
    restore.finally(() => setReady(true));
  }, [loadUser, router]);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      ready,
      async login(email, password) {
        tokens.store(await api<TokenPair>("/auth/login", { method: "POST", body: json({ email, password }) }));
        await loadUser();
      },
      async register(data) {
        tokens.store(await api<TokenPair>("/auth/register", { method: "POST", body: json(data) }));
        await loadUser();
      },
      logout() {
        const refreshToken = tokens.refresh;
        if (refreshToken) {
          api("/auth/logout", { method: "POST", body: json({ refreshToken }) }).catch(() => undefined);
        }
        tokens.clear();
        setUser(null);
        router.replace("/login");
      },
    }),
    [user, ready, loadUser, router],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used inside <AuthProvider>");
  return context;
}
