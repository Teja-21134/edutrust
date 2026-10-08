import { useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import Chat from "./Chat";
import { askQuestion, getApiErrorMessage } from "../api/client";
import { useAuth } from "../context/AuthContext";

type ChatState = "empty" | "loading" | "answer" | "noAnswer" | "error";

export interface ChatExchange {
  id: number;
  question: string;
  state: Exclude<ChatState, "empty">;
  answer?: string;
  sources: { documentTitle: string; pageNumber: number }[];
  timeMs: number | null;
  errorMessage?: string;
  response?: Record<string, unknown>;
}

export default function ChatPage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const nextMessageId = useRef(0);
  const [question, setQuestion] = useState("");
  const [messages, setMessages] = useState<ChatExchange[]>([]);

  async function ask(value: string) {
    const trimmed = value.trim();
    if (!trimmed) return;

    const id = nextMessageId.current++;
    setQuestion("");
    setMessages((current) => [...current, {
      id,
      question: trimmed,
      state: "loading",
      sources: [],
      timeMs: null,
    }]);

    try {
      const result = await askQuestion(trimmed);
      setMessages((current) => current.map((message) => message.id === id ? {
        ...message,
        state: result.answered ? "answer" : "noAnswer",
        answer: result.answer,
        sources: result.sources ?? [],
        timeMs: result.timeMs ?? null,
        response: result,
      } : message));
    } catch (requestError) {
      const errorMessage = getApiErrorMessage(requestError, requestError instanceof Error ? requestError.message : "Something went wrong while getting your answer. Please try again.");
      setMessages((current) => current.map((message) => message.id === id ? { ...message, state: "error", errorMessage } : message));
    }
  }

  return <Chat messages={messages} inputValue={question} onInputChange={setQuestion} onAsk={ask} onLogout={() => { logout(); navigate("/login"); }} user={user} />;
}
