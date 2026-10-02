import { useState } from "react";
import { useNavigate } from "react-router-dom";
import Chat from "./Chat";
import { askQuestionMock } from "../api/client";
import { useAuth } from "../context/AuthContext";

type ChatState = "empty" | "loading" | "answer" | "noAnswer" | "error";

export default function ChatPage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [state, setState] = useState<ChatState>("empty");
  const [question, setQuestion] = useState("");

  async function ask(value: string) {
    const trimmed = value.trim();
    if (!trimmed || state === "loading") return;
    setQuestion(trimmed); setState("loading");
    const result = await askQuestionMock(trimmed);
    setState(result.type === "answer" ? "answer" : result.type === "no-answer" ? "noAnswer" : "error");
  }

  return <Chat state={state} inputValue={question} onInputChange={setQuestion} onAsk={ask} onLogout={() => { logout(); navigate("/login"); }} user={user} />;
}
