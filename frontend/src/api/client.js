import axios from "axios";

export const apiClient = axios.create({
  baseURL: "http://localhost:8080",
  headers: { "Content-Type": "application/json" },
});

const wait = (milliseconds) => new Promise((resolve) => setTimeout(resolve, milliseconds));

export async function loginMock(email, password) {
  await wait(250);
  if (password === "wrong") return { ok: false, message: "Invalid email or password" };
  const isAdmin = email.toLowerCase().includes("admin");
  return { ok: true, user: { email, role: isAdmin ? "ADMIN" : "STUDENT", name: isAdmin ? "Admin User" : "Student User" } };
}

export async function uploadDocumentMock(payload, onProgress) {
  for (const progress of [20, 40, 65, 85, 100]) {
    await wait(180);
    onProgress?.(progress);
  }
  return { ok: true, chunks: 42, fileName: payload.file.name };
}

export async function askQuestionMock(question) {
  await wait(2000);
  const normalizedQuestion = question.toLowerCase();
  if (normalizedQuestion.includes("error")) return { ok: false, type: "error" };
  if (normalizedQuestion.includes("unknown")) return { ok: true, type: "no-answer" };
  return {
    ok: true,
    type: "answer",
    answer: "A student must maintain a minimum of 75% attendance in each semester to be eligible to appear for the semester-end examinations.",
    sources: ["Academic Regulations 2025 · Page 12", "Leave and Condonation Policy · Page 3"],
  };
}
