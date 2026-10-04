import { useState } from "react";
import { useNavigate } from "react-router-dom";
import Chat from "./Chat";
import { askQuestion, getApiErrorMessage } from "../api/client";
import { useAuth } from "../context/AuthContext";

type ChatState = "empty" | "loading" | "answer" | "noAnswer" | "error";

export default function ChatPage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [state, setState] = useState<ChatState>("empty");
  const [question, setQuestion] = useState("");
  const [answer, setAnswer] = useState("");
  const [sources, setSources] = useState<{ documentTitle: string; pageNumber: number }[]>([]);
  const [timeMs, setTimeMs] = useState<number | null>(null);
  const [errorMessage, setErrorMessage] = useState("");

  async function ask(value: string) {
    const trimmed = value.trim();
    if (!trimmed || state === "loading") return;
    setQuestion(trimmed); setState("loading"); setErrorMessage("");
    try {
      const result = await askQuestion(trimmed);
      setAnswer(result.answer); setSources(result.sources ?? []); setTimeMs(result.timeMs ?? null);
      setState(result.answered ? "answer" : "noAnswer");
    } catch (requestError) {
      setErrorMessage(getApiErrorMessage(requestError, requestError instanceof Error ? requestError.message : "Something went wrong while getting your answer. Please try again."));
      setState("error");
    }
  }

  return <Chat state={state} question={question} answer={answer} sources={sources} timeMs={timeMs} errorMessage={errorMessage} inputValue={question} onInputChange={setQuestion} onAsk={ask} onLogout={() => { logout(); navigate("/login"); }} user={user} />;
}
