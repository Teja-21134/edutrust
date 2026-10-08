import { FormEvent, useState } from "react";
import { useNavigate } from "react-router-dom";
import Login from "./Login";
import { useAuth } from "../context/AuthContext";

export default function LoginPage() {
  const { login, emailLogin } = useAuth();
  const navigate = useNavigate();
  const [state, setState] = useState<"default" | "error" | "loading">("default");
  const [mode, setMode] = useState<"email" | "admin">("email");

  async function handleLogin(email: string, password: string, event?: FormEvent) {
    event?.preventDefault();
    setState("loading");
    try {
      const result = mode === "email" ? await emailLogin(email) : await login(email, password);
      if (!result.ok) { setState("error"); return; }
      navigate(result.user?.role === "ADMIN" ? "/upload" : "/chat", { replace: true });
    } catch {
      setState("error");
    }
  }

  return <Login state={state} mode={mode} onModeChange={(nextMode) => { setMode(nextMode); setState("default"); }} onLogin={handleLogin} />;
}
