import { Send, Copy, ThumbsUp, ThumbsDown, AlertTriangle, ShieldCheck } from "../components/icons";
import Navbar from "../components/Navbar";
import { Button, SourceChip, ShieldCheckAvatar, Alert } from "../components/ui";
import type { AuthUser } from "../context/AuthContext";

type ChatState = "empty" | "loading" | "answer" | "noAnswer" | "error";

interface ChatProps {
  state?: ChatState;
  onNavigate?: (page: string) => void;
  inputValue?: string;
  onInputChange?: (value: string) => void;
  onAsk?: (question: string) => void;
  onLogout?: () => void;
  user?: AuthUser | null;
  question?: string;
  answer?: string;
  sources?: { documentTitle: string; pageNumber: number }[];
  timeMs?: number | null;
  errorMessage?: string;
}

function SuggestionCard({ text, onClick }: { text: string; onClick?: () => void }) {
  return (
    <button
      className="flex-1 text-left p-4 rounded-xl bg-white transition-all hover:shadow-md"
      style={{
        border: "1px solid #E2E8F0",
        boxShadow: "0 1px 3px rgba(0,0,0,0.04)",
        color: "#0F172A",
      }}
      onClick={onClick}
    >
      <p className="text-sm leading-relaxed">{text}</p>
    </button>
  );
}

function UserBubble({ text }: { text: string }) {
  return (
    <div className="flex justify-end">
      <div
        className="max-w-lg px-4 py-3 text-sm leading-relaxed"
        style={{
          backgroundColor: "#1E3A8A",
          color: "#FFFFFF",
          borderRadius: "16px 16px 4px 16px",
        }}
      >
        {text}
      </div>
    </div>
  );
}

function TypingIndicator() {
  return (
    <div className="flex items-start gap-3">
      <ShieldCheckAvatar />
      <div
        className="px-4 py-3"
        style={{
          backgroundColor: "#FFFFFF",
          border: "1px solid #E2E8F0",
          borderRadius: "4px 16px 16px 16px",
          boxShadow: "0 1px 3px rgba(0,0,0,0.04)",
        }}
      >
        <div className="flex items-center gap-1.5 mb-1.5">
          {[0, 1, 2].map((i) => (
            <div
              key={i}
              className="w-1.5 h-1.5 rounded-full"
              style={{
                backgroundColor: "#CBD5E1",
                animation: `bounce 1.2s ease-in-out ${i * 0.2}s infinite`,
              }}
            />
          ))}
        </div>
        <p className="text-xs" style={{ color: "#64748B" }}>
          Searching documents and preparing your answer...
        </p>
      </div>
    </div>
  );
}

function AnswerBubble({ answer, sources, timeMs }: { answer: string; sources: { documentTitle: string; pageNumber: number }[]; timeMs?: number | null }) {
  return (
    <div className="flex items-start gap-3">
      <ShieldCheckAvatar />
      <div
        className="flex-1 max-w-2xl px-5 py-4"
        style={{
          backgroundColor: "#FFFFFF",
          border: "1px solid #E2E8F0",
          borderRadius: "4px 16px 16px 16px",
          boxShadow: "0 1px 4px rgba(0,0,0,0.04)",
        }}
      >
        <p className="text-sm leading-relaxed" style={{ color: "#0F172A" }}>
          {answer}
        </p>

        <hr className="my-3" style={{ borderColor: "#E2E8F0" }} />

        <div className="flex flex-wrap items-center gap-2 mb-3">
          <span className="text-xs font-medium" style={{ color: "#64748B" }}>
            Sources
          </span>
          {sources.map((source) => <SourceChip key={`${source.documentTitle}-${source.pageNumber}`} text={`${source.documentTitle} - Page ${source.pageNumber}`} />)}
        </div>

        {timeMs !== null && timeMs !== undefined && <p className="text-xs" style={{ color: "#94A3B8" }}>{timeMs} ms</p>}

        {/* Reserved area for future: verification badge / confidence score */}
        <div
          className="rounded-lg px-3 py-2 flex items-center gap-2"
          style={{ backgroundColor: "#F8FAFC", border: "1px dashed #E2E8F0" }}
        >
          <span className="text-xs" style={{ color: "#CBD5E1" }}>
            — Verification badge · Confidence score (reserved)
          </span>
        </div>

        <div className="flex items-center gap-2 mt-3">
          <button className="p-1.5 rounded transition-colors hover:bg-gray-50" style={{ color: "#94A3B8" }}>
            <Copy size={14} />
          </button>
          <button className="p-1.5 rounded transition-colors hover:bg-gray-50" style={{ color: "#94A3B8" }}>
            <ThumbsUp size={14} />
          </button>
          <button className="p-1.5 rounded transition-colors hover:bg-gray-50" style={{ color: "#94A3B8" }}>
            <ThumbsDown size={14} />
          </button>
        </div>
      </div>
    </div>
  );
}

function NoAnswerBubble() {
  return (
    <div className="flex items-start gap-3">
      <ShieldCheckAvatar />
      <div
        className="max-w-xl px-5 py-4 flex items-start gap-3"
        style={{
          backgroundColor: "#FEF3C7",
          border: "1px solid #FDE68A",
          borderRadius: "4px 16px 16px 16px",
        }}
      >
        <AlertTriangle size={16} className="shrink-0 mt-0.5" style={{ color: "#D97706" }} />
        <p className="text-sm leading-relaxed" style={{ color: "#92400E" }}>
          I could not find this information in the available documents. Please
          contact the concerned department for clarification.
        </p>
      </div>
    </div>
  );
}

function ErrorBubble({ onRetry, message }: { onRetry?: () => void; message?: string }) {
  return (
    <div className="flex items-start gap-3">
      <div
        className="w-8 h-8 rounded-full flex items-center justify-center shrink-0"
        style={{ backgroundColor: "#FEF2F2" }}
      >
        <ShieldCheck size={16} style={{ color: "#DC2626" }} />
      </div>
      <div
        className="max-w-xl px-5 py-4 flex flex-col gap-3"
        style={{
          backgroundColor: "#FEF2F2",
          border: "1px solid #FCA5A5",
          borderRadius: "4px 16px 16px 16px",
        }}
      >
        <p className="text-sm" style={{ color: "#DC2626" }}>
          {message || "Something went wrong while getting your answer. Please try again."}
        </p>
        <Button variant="secondary" size="sm" onClick={onRetry}>
          Retry
        </Button>
      </div>
    </div>
  );
}

function ChatInput({ value = "", onChange, onAsk }: { value?: string; onChange?: (value: string) => void; onAsk?: (value: string) => void }) {
  function handleKeyDown(event: React.KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === "Enter" && !event.shiftKey) { event.preventDefault(); onAsk?.(value); }
  }
  return (
    <div
      className="rounded-xl p-3 flex items-end gap-3"
      style={{
        backgroundColor: "#FFFFFF",
        border: "1px solid #E2E8F0",
        boxShadow: "0 2px 8px rgba(15,23,42,0.06)",
      }}
    >
      <textarea
        className="flex-1 resize-none text-sm outline-none leading-relaxed"
        placeholder="Ask a question about your institution's documents..."
        value={value}
        onChange={(event) => onChange?.(event.target.value)}
        onKeyDown={handleKeyDown}
        rows={1}
        style={{
          color: "#0F172A",
          backgroundColor: "transparent",
          minHeight: "24px",
          maxHeight: "120px",
        }}
      />
      <button
        className="w-10 h-10 rounded-full flex items-center justify-center shrink-0 transition-colors"
        style={{ backgroundColor: "#1E3A8A" }}
        onClick={() => onAsk?.(value)}
      >
        <Send size={16} color="white" />
      </button>
    </div>
  );
}

export default function Chat({ state = "empty", question = "", answer = "", sources = [], timeMs, errorMessage, inputValue = "", onInputChange, onAsk, onLogout, user }: ChatProps) {
  return (
    <div className="min-h-screen flex flex-col" style={{ backgroundColor: "#F8FAFC" }}>
      <Navbar
        role={user?.role === "ADMIN" ? "admin" : "student"}
        activeTab="chat"
        userName={user?.name}
        onLogout={onLogout}
      />

      <div className="flex-1 flex flex-col overflow-hidden">
        <div className="flex-1 overflow-y-auto scroll-container">
          <div className="max-w-3xl mx-auto px-6 py-8 flex flex-col gap-6">

            {/* EMPTY state */}
            {state === "empty" && (
              <div className="flex flex-col items-center justify-center py-20 gap-8">
                <div
                  className="w-16 h-16 rounded-2xl flex items-center justify-center"
                  style={{ backgroundColor: "#CCFBF1" }}
                >
                  <ShieldCheck size={32} style={{ color: "#0D9488" }} />
                </div>
                <div className="text-center">
                  <h2 className="text-2xl font-semibold mb-2" style={{ color: "#0F172A" }}>
                    Ask about your institution's documents
                  </h2>
                  <p className="text-base" style={{ color: "#64748B" }}>
                    Get answers with the exact source and page number.
                  </p>
                </div>
                <div className="flex gap-3 w-full">
                  <SuggestionCard text="What is the minimum attendance required?" onClick={() => onInputChange?.("What is the minimum attendance required?")} />
                  <SuggestionCard text="When is the last date to pay semester fees?" onClick={() => onInputChange?.("When is the last date to pay semester fees?")} />
                  <SuggestionCard text="What are the exam malpractice rules?" onClick={() => onInputChange?.("What are the exam malpractice rules?")} />
                </div>
              </div>
            )}

            {/* LOADING state */}
            {state === "loading" && (
              <>
                <UserBubble text={question} />
                <TypingIndicator />
              </>
            )}

            {/* ANSWER state */}
            {state === "answer" && (
              <>
                <UserBubble text={question} />
                <AnswerBubble answer={answer} sources={sources} timeMs={timeMs} />
              </>
            )}

            {/* NO ANSWER state */}
            {state === "noAnswer" && (
              <>
                <UserBubble text={question} />
                <NoAnswerBubble />
              </>
            )}

            {/* ERROR state */}
            {state === "error" && (
              <>
                <UserBubble text={question} />
                <ErrorBubble message={errorMessage} onRetry={() => onAsk?.(question)} />
              </>
            )}

          </div>
        </div>

        {/* Fixed input bar */}
        <div
          className="shrink-0 border-t py-4"
          style={{ borderColor: "#E2E8F0", backgroundColor: "#F8FAFC" }}
        >
          <div className="max-w-3xl mx-auto px-6 flex flex-col gap-2">
            <ChatInput value={inputValue} onChange={onInputChange} onAsk={onAsk} />
            <p className="text-xs text-center" style={{ color: "#94A3B8" }}>
              Answers are generated only from uploaded institutional documents.
            </p>
          </div>
        </div>
      </div>

      <style>{`
        @keyframes bounce {
          0%, 100% { transform: translateY(0); opacity: 0.5; }
          50% { transform: translateY(-4px); opacity: 1; }
        }
      `}</style>
    </div>
  );
}
