import axios from "axios";

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "http://localhost:8080",
  headers: { Accept: "application/json" },
});

const AUTH_STORAGE_KEY = "edutrust.auth";
let unauthorizedHandler = null;

export function setUnauthorizedHandler(handler) {
  unauthorizedHandler = handler;
}

export function getAuthStorageKey() {
  return AUTH_STORAGE_KEY;
}

apiClient.interceptors.request.use((config) => {
  const stored = sessionStorage.getItem(AUTH_STORAGE_KEY);
  if (stored) {
    try {
      const { token } = JSON.parse(stored);
      if (token) config.headers.Authorization = `Bearer ${token}`;
    } catch {
      sessionStorage.removeItem(AUTH_STORAGE_KEY);
    }
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const isLoginRequest = error.config?.url?.includes("/api/auth/login") || error.config?.url?.includes("/api/auth/email");
    if (error.response?.status === 401 && !isLoginRequest) unauthorizedHandler?.();
    return Promise.reject(error);
  },
);

function backendMessage(error, fallback) {
  return error.response?.data?.message || fallback;
}

export async function login(email, password) {
  try {
    const { data } = await apiClient.post("/api/auth/login", { email, password });
    return { ok: true, user: { token: data.token, name: data.name, email: data.email, role: data.role } };
  } catch (error) {
    if (error.response?.status === 401) return { ok: false, message: "Invalid email or password" };
    throw error;
  }
}

export async function emailLogin(email) {
  try {
    const { data } = await apiClient.post("/api/auth/email", { email });
    return { ok: true, user: { token: data.token, name: data.name, email: data.email, role: data.role } };
  } catch (error) {
    if (error.response?.status === 400) return { ok: false, message: "Please enter a valid email address" };
    if (error.response?.status === 403) return { ok: false, message: "Please use administrator login for this account" };
    throw error;
  }
}

export async function uploadDocument(payload, onProgress) {
  const formData = new FormData();
  formData.append("file", payload.file);
  formData.append("title", payload.title);
  formData.append("department", payload.department);
  formData.append("docType", payload.docType);
  formData.append("academicYear", payload.academicYear);
  formData.append("version", payload.version);
  formData.append("docDate", payload.docDate);
  if (payload.authority) formData.append("authority", payload.authority);
  const { data } = await apiClient.post("/api/documents", formData, {
    onUploadProgress: (event) => {
      if (event.total) onProgress?.(Math.round((event.loaded * 100) / event.total));
    },
  });
  return data;
}

export async function listDocuments() {
  const { data } = await apiClient.get("/api/documents");
  return data;
}

export async function deleteDocument(id) {
  await apiClient.delete(`/api/documents/${id}`);
}

export async function askQuestion(question) {
  try {
    const { data } = await apiClient.post("/api/ask", { question }, { timeout: 300000 });
    return data;
  } catch (error) {
    if (error.response?.status === 503) {
      throw new Error(backendMessage(error, "The answer service is not available. Please try again."));
    }
    throw error;
  }
}

export async function createConversation(title) {
  const { data } = await apiClient.post("/api/conversations", title ? { title } : {});
  return data;
}

export async function listConversations() {
  const { data } = await apiClient.get("/api/conversations");
  return data;
}

export async function getConversation(id) {
  const { data } = await apiClient.get(`/api/conversations/${id}`);
  return data;
}

export async function deleteConversation(id) {
  await apiClient.delete(`/api/conversations/${id}`);
}

export async function sendConversationMessage(id, question) {
  const { data } = await apiClient.post(`/api/conversations/${id}/messages`, { question }, { timeout: 300000 });
  return data;
}

export function getApiErrorMessage(error, fallback) {
  if (error.response?.status === 403) return "Access denied";
  return backendMessage(error, fallback);
}
