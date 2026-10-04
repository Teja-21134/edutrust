import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { getAuthStorageKey, login as loginRequest, setUnauthorizedHandler } from "../api/client";

export type UserRole = "ADMIN" | "STUDENT";
export interface AuthUser { token: string; email: string; name: string; role: UserRole; }
interface AuthContextValue {
  user: AuthUser | null;
  login: (email: string, password: string) => Promise<{ ok: boolean; message?: string; user?: AuthUser }>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(() => {
    const stored = sessionStorage.getItem(getAuthStorageKey());
    if (!stored) return null;
    try { return JSON.parse(stored) as AuthUser; } catch { sessionStorage.removeItem(getAuthStorageKey()); return null; }
  });

  function logout() {
    sessionStorage.removeItem(getAuthStorageKey());
    setUser(null);
  }

  useEffect(() => {
    setUnauthorizedHandler(logout);
    return () => setUnauthorizedHandler(null);
  }, []);

  async function login(email: string, password: string) {
    const result = await loginRequest(email, password);
    if (!result.ok || !result.user) return { ok: false, message: result.message };
    setUser(result.user);
    sessionStorage.setItem(getAuthStorageKey(), JSON.stringify(result.user));
    return { ok: true, user: result.user };
  }
  const value = useMemo(() => ({ user, login, logout }), [user]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used inside AuthProvider");
  return context;
}
