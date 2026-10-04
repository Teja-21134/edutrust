import { FormEvent, useState } from "react";
import { useNavigate } from "react-router-dom";
import Login from "./Login";
import { useAuth } from "../context/AuthContext";

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [state, setState] = useState<"default" | "error" | "loading">("default");

  async function handleLogin(email: string, password: string, event?: FormEvent) {
    event?.preventDefault();
    setState("loading");
    const result = await login(email, password);
    if (!result.ok) { setState("error"); return; }
    navigate(result.user?.role === "ADMIN" ? "/upload" : "/chat", { replace: true });
  }

  return <Login state={state} onLogin={handleLogin} />;
}
