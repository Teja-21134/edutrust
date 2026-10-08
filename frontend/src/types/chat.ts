export type MessageRole = "USER" | "ASSISTANT";

export interface Source {
  documentTitle: string;
  pageNumber: number;
  version?: string | null;
}

export interface ChatMessage {
  id: string;
  role: MessageRole;
  content: string;
  sources?: Source[];
  faithfulnessStatus?: string | null;
  faithfulnessScore?: number | null;
  conflictDetected?: boolean | null;
  conflictResolution?: string | null;
  selectedDocument?: string | null;
  selectedVersion?: string | null;
  createdAt?: string;
  pending?: boolean;
  error?: string;
}

export interface ConversationSummary {
  id: string;
  title: string;
  createdAt: string;
  updatedAt: string;
}

export interface Conversation extends ConversationSummary {
  messages: ChatMessage[];
}

export interface ChatResponse {
  conversationId: string;
  userMessage: ChatMessage;
  assistantMessage: ChatMessage;
  answer: {
    answer: string;
    answered: boolean;
    sources: Source[];
    timeMs: number;
    faithfulnessStatus: string;
    faithfulnessScore: number;
    conflictDetected: boolean;
    conflictResolution: string;
    selectedDocument: string | null;
    selectedVersion: string | null;
  };
}
