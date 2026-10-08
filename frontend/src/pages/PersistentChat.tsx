import { useEffect, useRef, useState } from "react";
import { AlertTriangle, Copy, MessageSquare, Send, ShieldCheck } from "../components/icons";
import Logo from "../components/Logo";
import { Button } from "../components/ui";
import type { AuthUser } from "../context/AuthContext";
import type { ChatMessage, Conversation, ConversationSummary, Source } from "../types/chat";
import { createConversation, deleteConversation, getConversation, listConversations, sendConversationMessage } from "../api/client";

const SELECTED_CONVERSATION_KEY = "edutrust.selectedConversation";

interface Props { user: AuthUser | null; logout: () => void; navigate: (path: string) => void; }

function sourceText(source: Source) { return `${source.documentTitle || "Institution document"} · Page ${source.pageNumber}${source.version ? ` · ${source.version}` : ""}`; }

function TrustInfo({ message }: { message: ChatMessage }) {
  if (message.faithfulnessStatus === "NOT_FOUND") return <p className="mt-3 rounded-lg bg-amber-50 px-3 py-2 text-sm text-amber-800">No supporting information found.</p>;
  if (!message.faithfulnessStatus && message.faithfulnessScore == null) return null;
  const score = message.faithfulnessScore == null ? null : Math.round(message.faithfulnessScore * 100);
  return <div className={`mt-3 rounded-lg px-3 py-2 text-sm ${message.faithfulnessStatus === "VERIFIED" ? "bg-green-50 text-green-800" : "bg-amber-50 text-amber-800"}`}><div className="font-medium">{message.faithfulnessStatus === "VERIFIED" ? "✓ Trusted" : "⚠ Could not verify"}</div>{score != null && <div>Faithfulness: {score}%</div>}</div>;
}

function AssistantMessage({ message }: { message: ChatMessage }) {
  return <div className="flex items-start gap-3"><div className="w-8 h-8 rounded-full bg-teal-100 text-teal-700 flex items-center justify-center shrink-0"><ShieldCheck size={17} /></div><div className="max-w-2xl rounded-2xl rounded-tl-sm bg-white border border-slate-200 px-5 py-4 shadow-sm"><p className="text-sm leading-relaxed whitespace-pre-wrap">{message.content}</p>{message.sources?.length ? <div className="mt-4 border-t border-slate-100 pt-3"><p className="text-xs font-semibold text-slate-500 mb-2">Sources</p>{message.sources.map((source, index) => <p key={`${source.documentTitle}-${source.pageNumber}-${index}`} className="text-xs text-slate-600">{sourceText(source)}</p>)}</div> : null}{message.conflictDetected && <p className="mt-3 text-xs text-slate-600">Information was reconciled from multiple documents.{message.selectedDocument ? ` Selected: ${message.selectedDocument}.` : ""}</p>}<TrustInfo message={message} /><button className="mt-3 text-slate-400 hover:text-slate-600" title="Copy answer" onClick={() => navigator.clipboard?.writeText(message.content)}><Copy size={14} /></button></div></div>;
}

export default function PersistentChat({ user, logout, navigate }: Props) {
  const [conversations, setConversations] = useState<ConversationSummary[]>([]);
  const [conversation, setConversation] = useState<Conversation | null>(null);
  const [question, setQuestion] = useState("");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const endRef = useRef<HTMLDivElement>(null);

  async function loadConversation(id: string) {
    try { const loaded = await getConversation(id) as Conversation; setConversation({ ...loaded, messages: [...(loaded.messages ?? [])].sort((a, b) => (a.createdAt ?? "").localeCompare(b.createdAt ?? "")) }); sessionStorage.setItem(SELECTED_CONVERSATION_KEY, id); }
    catch (requestError) { setError(requestError instanceof Error ? requestError.message : "Could not load this conversation."); }
  }

  async function refresh(preferredId?: string) {
    const list = await listConversations() as ConversationSummary[]; setConversations(list);
    const id = preferredId || sessionStorage.getItem(SELECTED_CONVERSATION_KEY) || list[0]?.id;
    if (id && list.some((item) => item.id === id)) await loadConversation(id); else setConversation(null);
  }

  useEffect(() => { refresh().catch(() => setError("Could not load your conversations.")).finally(() => setLoading(false)); }, []);
  useEffect(() => { endRef.current?.scrollIntoView({ behavior: "smooth" }); }, [conversation?.messages.length, busy]);

  async function newChat() {
    if (busy) return; setBusy(true); setError(null);
    try { const created = await createConversation() as Conversation; setConversations((current) => [created, ...current]); setConversation({ ...created, messages: [] }); sessionStorage.setItem(SELECTED_CONVERSATION_KEY, created.id); }
    catch { setError("Could not create a new chat."); } finally { setBusy(false); }
  }

  async function ask() {
    const text = question.trim(); if (!text || !conversation || busy) return; setQuestion(""); setBusy(true); setError(null);
    const pending: ChatMessage = { id: `pending-${Date.now()}`, role: "USER", content: text, pending: true }; setConversation((current) => current ? { ...current, messages: [...current.messages, pending] } : current);
    try { const response = await sendConversationMessage(conversation.id, text); setConversation((current) => current ? { ...current, messages: [...current.messages.filter((message) => !message.pending), response.userMessage, response.assistantMessage] } : current); await refresh(conversation.id); }
    catch { setError("Something went wrong while getting your answer. Please try again."); setConversation((current) => current ? { ...current, messages: current.messages.filter((message) => !message.pending) } : current); } finally { setBusy(false); }
  }

  async function deleteChat(id: string) {
    if (!window.confirm("Delete this conversation?")) return;
    setError(null);
    try {
      await deleteConversation(id);
      setConversations((current) => current.filter((item) => item.id !== id));
      if (conversation?.id === id) {
        setConversation(null);
        sessionStorage.removeItem(SELECTED_CONVERSATION_KEY);
      }
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Could not delete this conversation.");
    }
  }

  function handleLogout() { sessionStorage.removeItem(SELECTED_CONVERSATION_KEY); logout(); navigate("/login"); }
  return <div className="h-screen flex bg-slate-50 text-slate-900">
    <aside className="w-72 shrink-0 bg-white border-r border-slate-200 flex flex-col">
      <div className="p-5 border-b border-slate-100"><Logo /></div>
      <div className="p-4"><Button className="w-full" onClick={newChat} loading={busy}>+ New Chat</Button></div>
      <div className="px-3 flex-1 overflow-y-auto">
        <p className="px-2 pb-2 text-xs font-semibold uppercase tracking-wide text-slate-400">Conversations</p>
        {!conversations.length && !loading && <p className="px-2 text-sm text-slate-500">No conversations yet.</p>}
        {conversations.map((item) => <div key={item.id} className={`w-full flex items-center gap-1 rounded-lg px-1 py-1 mb-1 ${conversation?.id === item.id ? "bg-blue-50 text-blue-900" : "text-slate-600 hover:bg-slate-50"}`}>
          <button onClick={() => loadConversation(item.id)} className="min-w-0 flex-1 flex items-center gap-2 rounded-md px-2 py-1.5 text-left text-sm"><MessageSquare size={15} /><span className="truncate">{item.title}</span></button>
          <button type="button" title="Delete conversation" aria-label={`Delete ${item.title}`} onClick={() => deleteChat(item.id)} className="shrink-0 rounded p-1.5 text-slate-400 hover:bg-red-50 hover:text-red-600">🗑</button>
        </div>)}
      </div>
      <div className="p-4 border-t border-slate-100"><div className="text-xs text-slate-500 truncate mb-3">{user?.email}</div><button onClick={handleLogout} className="text-sm text-slate-600 hover:text-slate-900">Logout</button></div>
    </aside>
    <main className="flex-1 min-w-0 flex flex-col">
      <header className="h-16 shrink-0 bg-white border-b border-slate-200 flex items-center px-6"><h1 className="font-semibold truncate">{conversation?.title ?? "New Chat"}</h1></header>
      {error && <div className="mx-6 mt-4 rounded-lg bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700 flex items-center gap-2"><AlertTriangle size={16} />{error}</div>}
      {loading ? <div className="flex-1 flex items-center justify-center text-sm text-slate-500">Loading conversations...</div> : <div className="flex-1 overflow-y-auto p-6"><div className="max-w-3xl mx-auto flex flex-col gap-6">
        {conversation?.messages.length ? conversation.messages.map((message) => message.role === "USER" ? <div key={message.id}>
          <div className="flex justify-end"><div className="max-w-lg rounded-2xl rounded-br-sm bg-blue-900 text-white px-4 py-3 text-sm">{message.content}</div></div>
          {message.pending && <div className="mt-4 flex items-start gap-3"><div className="w-8 h-8 rounded-full bg-teal-100 text-teal-700 flex items-center justify-center shrink-0"><ShieldCheck size={17} /></div><div className="rounded-2xl rounded-tl-sm bg-white border border-slate-200 px-5 py-4 shadow-sm"><div className="flex items-center gap-1.5" aria-label="Thinking"><span className="loading-dot">●</span><span className="loading-dot">●</span><span className="loading-dot">●</span></div></div></div>}
        </div> : <AssistantMessage key={message.id} message={message} />) : <div className="flex-1 text-center py-24"><ShieldCheck size={36} className="mx-auto text-teal-600" /><h2 className="mt-4 text-xl font-semibold">Ask about your institution's documents</h2><p className="mt-2 text-sm text-slate-500">Get answers with sources and verification information.</p></div>}
        <div ref={endRef} /></div></div>}
      <div className="shrink-0 border-t border-slate-200 bg-slate-50 p-4"><div className="max-w-3xl mx-auto flex items-end gap-3 rounded-xl bg-white border border-slate-200 p-3 shadow-sm"><textarea value={question} onChange={(event) => setQuestion(event.target.value)} onKeyDown={(event) => { if (event.key === "Enter" && !event.shiftKey) { event.preventDefault(); ask(); } }} disabled={!conversation || busy} rows={1} placeholder="Ask anything..." className="flex-1 resize-none outline-none text-sm min-h-6" /><button disabled={!conversation || busy || !question.trim()} onClick={ask} className="w-10 h-10 rounded-full bg-blue-900 disabled:bg-slate-300 flex items-center justify-center"><Send size={16} color="white" /></button></div></div>
    </main>
    <style>{`@keyframes loading-dot-pulse { 0%, 80%, 100% { opacity: .35; transform: translateY(0); } 40% { opacity: 1; transform: translateY(-2px); } } .loading-dot { color: #64748b; font-size: 10px; animation: loading-dot-pulse 1.4s infinite ease-in-out; } .loading-dot:nth-child(2) { animation-delay: .2s; } .loading-dot:nth-child(3) { animation-delay: .4s; }`}</style>
  </div>;
}
