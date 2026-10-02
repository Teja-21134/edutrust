import { createContext, useContext, useMemo, useState } from "react";
import { loginMock } from "../api/client";

export type UserRole = "ADMIN" | "STUDENT";
export interface AuthUser { email: string; name: string; role: UserRole; }
interface AuthContextValue {
  user: AuthUser | null;
  login: (email: string, password: string) => Promise<{ ok: boolean; message?: string }>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  async function login(email: string, password: string) {
    const result = await loginMock(email, password);
    if (!result.ok || !result.user) return { ok: false, message: result.message };
    setUser(result.user);
    return { ok: true };
  }
  const value = useMemo(() => ({ user, login, logout: () => setUser(null) }), [user]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used inside AuthProvider");
  return context;
}
