import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Navbar from "../components/Navbar";
import { Alert, Button, DatePicker, Dropzone, FileRow, Input, ProgressBar, Select } from "../components/ui";
import { MessageSquare, RotateCcw, Upload as UploadIcon } from "../components/icons";
import { deleteDocument, getApiErrorMessage, listDocuments, uploadDocument } from "../api/client";
import { useAuth } from "../context/AuthContext";

type UploadState = "empty" | "fileSelected" | "uploading" | "success" | "error";
type DocumentSummary = { id: string; title: string; department: string; docType: string; academicYear: string; version: string; chunks: number };
const emptyFields = { title: "", department: "", docType: "", year: "", version: "", date: "", authority: "" };

export default function UploadPage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [state, setState] = useState<UploadState>("empty");
  const [file, setFile] = useState<File | null>(null);
  const [progress, setProgress] = useState(0);
  const [chunks, setChunks] = useState(0);
  const [error, setError] = useState("");
  const [fields, setFields] = useState(emptyFields);
  const [documents, setDocuments] = useState<DocumentSummary[]>([]);
  const [accessDenied, setAccessDenied] = useState("");

  useEffect(() => {
    loadDocuments();
    return () => setProgress(0);
  }, []);

  async function loadDocuments() {
    try { setDocuments(await listDocuments()); }
    catch (requestError) { setAccessDenied(getApiErrorMessage(requestError, "Could not load documents")); }
  }

  function updateField(field: string, value: string) { setFields((current) => ({ ...current, [field]: value })); }
  function handleFile(nextFile: File) {
    if (nextFile.type !== "application/pdf" && !nextFile.name.toLowerCase().endsWith(".pdf")) {
      setError("Only PDF files are allowed"); setState("error"); setFile(null); return;
    }
    if (nextFile.size > 20 * 1024 * 1024) { setError("PDF files must be smaller than 20 MB"); setState("error"); setFile(null); return; }
    setFile(nextFile); setError(""); setState("fileSelected");
  }
  function reset() { setFile(null); setState("empty"); setProgress(0); setError(""); setFields(emptyFields); }
  async function submit() {
    if (!file) { setError("Please choose a PDF document"); setState("error"); return; }
    if (!fields.title || !fields.docType || !fields.year || !fields.date) { setError("Complete the required document fields before uploading"); setState("error"); return; }
    setState("uploading"); setProgress(0);
    try {
      const result = await uploadDocument({ ...fields, academicYear: fields.year, docDate: fields.date, file }, setProgress);
      setChunks(result.chunks); setState("success"); await loadDocuments();
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, "Upload failed")); setState("error");
    }
  }
  const disabled = state === "uploading" || state === "success";

  async function removeDocument(document: DocumentSummary) {
    if (!window.confirm(`Delete "${document.title}"?`)) return;
    try { await deleteDocument(document.id); await loadDocuments(); }
    catch (requestError) { setAccessDenied(getApiErrorMessage(requestError, "Could not delete document")); }
  }

  return <div className="min-h-screen flex flex-col" style={{ backgroundColor: "#F8FAFC" }}>
    <Navbar role="admin" activeTab="upload" userName={user?.name} onLogout={() => { logout(); navigate("/login"); }} />
    <div className="flex-1 px-8 py-12"><div className="max-w-3xl mx-auto">
      <div className="mb-8"><h1 className="text-3xl font-semibold" style={{ color: "#0F172A" }}>Upload Document</h1><p className="text-base mt-2" style={{ color: "#64748B" }}>Add an official institutional document so students can ask questions about it.</p></div>
      <div className="bg-white rounded-xl p-8" style={{ border: "1px solid #E2E8F0", boxShadow: "0 1px 6px rgba(15,23,42,0.05)", opacity: state === "uploading" ? 0.85 : 1 }}>
        {state === "success" && <div className="mb-6"><Alert variant="success" message={`Document uploaded successfully. ${chunks} chunks created and indexed.`} /></div>}
        {state === "error" && <div className="mb-6"><Alert variant="error" message={error} /></div>}
        <div className="grid grid-cols-2 gap-6" style={{ pointerEvents: disabled ? "none" : "auto", opacity: disabled ? 0.5 : 1 }}>
          <div className="col-span-2"><Input label="Document Title" placeholder="e.g., Academic Regulations 2025" value={fields.title} onChange={(event) => updateField("title", event.target.value)} /></div>
          <Select label="Department" value={fields.department} onChange={(event) => updateField("department", event.target.value)}><option value="">All Departments</option><option>CSE</option><option>ECE</option><option>EEE</option><option>Mechanical</option><option>Civil</option></Select>
          <Select label="Document Type" value={fields.docType} onChange={(event) => updateField("docType", event.target.value)}><option value="">Select type...</option><option>Regulations</option><option>Circular</option><option>Academic Calendar</option><option>Fee Structure</option><option>Policy</option><option>Other</option></Select>
          <Select label="Academic Year" value={fields.year} onChange={(event) => updateField("year", event.target.value)}><option value="">Select year...</option><option>2023-24</option><option>2024-25</option><option>2025-26</option><option>2026-27</option></Select>
          <Input label="Version" placeholder="e.g., v2" value={fields.version} onChange={(event) => updateField("version", event.target.value)} />
          <DatePicker label="Document Date" value={fields.date} onChange={(event) => updateField("date", event.target.value)} />
          <Select label="Issuing Authority" optional value={fields.authority} onChange={(event) => updateField("authority", event.target.value)}><option value="">Select authority...</option><option>Principal</option><option>Dean</option><option>Department Head</option><option>Exam Cell</option></Select>
          <div className="col-span-2">{file ? <div className="flex flex-col gap-1.5"><label className="text-sm font-medium" style={{ color: "#0F172A" }}>PDF File</label><FileRow name={file.name} size={`${(file.size / 1024 / 1024).toFixed(1)} MB`} onRemove={() => { setFile(null); setState("empty"); }} /></div> : <Dropzone label="PDF File" error={state === "error"} onFile={handleFile} />}</div>
        </div>
        {state === "uploading" && <div className="mt-6 flex flex-col gap-2"><div className="flex items-center justify-between"><p className="text-sm" style={{ color: "#64748B" }}>Processing document... extracting text and creating chunks</p><span className="text-sm font-medium" style={{ color: "#1E3A8A" }}>{progress}%</span></div><ProgressBar value={progress} /></div>}
        <div className="mt-8 flex items-center justify-end gap-3">{state === "success" ? <><Button variant="secondary" icon={<UploadIcon size={15} />} onClick={reset}>Upload another</Button><Button variant="primary" icon={<MessageSquare size={15} />} onClick={() => navigate("/chat")}>Go to Chat</Button></> : <><Button variant="secondary" icon={<RotateCcw size={15} />} onClick={reset}>Reset</Button><Button variant={!file || disabled ? "disabled" : "primary"} disabled={!file || disabled} loading={state === "uploading"} loadingText="Uploading..." icon={<UploadIcon size={15} />} onClick={submit}>Upload</Button></>}</div>
      </div>
      <div className="mt-8">
        {accessDenied && <div className="mb-4"><Alert variant="error" message={accessDenied} /></div>}
        <h2 className="text-xl font-semibold mb-4" style={{ color: "#0F172A" }}>Uploaded Documents</h2>
        <div className="bg-white rounded-xl overflow-hidden" style={{ border: "1px solid #E2E8F0" }}>
          <table className="w-full text-sm">
            <thead style={{ backgroundColor: "#F8FAFC" }}><tr>
              {['Title', 'Department', 'Type', 'Year', 'Version', 'Chunks', ''].map((heading) => <th key={heading} className="text-left px-4 py-3 font-medium" style={{ color: "#64748B" }}>{heading}</th>)}
            </tr></thead>
            <tbody>{documents.map((document) => <tr key={document.id} style={{ borderTop: "1px solid #E2E8F0" }}>
              <td className="px-4 py-3" style={{ color: "#0F172A" }}>{document.title}</td><td className="px-4 py-3" style={{ color: "#64748B" }}>{document.department}</td><td className="px-4 py-3" style={{ color: "#64748B" }}>{document.docType}</td><td className="px-4 py-3" style={{ color: "#64748B" }}>{document.academicYear}</td><td className="px-4 py-3" style={{ color: "#64748B" }}>{document.version}</td><td className="px-4 py-3" style={{ color: "#64748B" }}>{document.chunks}</td><td className="px-4 py-3"><button className="text-sm font-medium" style={{ color: "#DC2626" }} onClick={() => removeDocument(document)}>Delete</button></td>
            </tr>)}</tbody>
          </table>
          {!documents.length && <p className="px-4 py-5 text-sm" style={{ color: "#64748B" }}>No documents uploaded yet.</p>}
        </div>
      </div>
    </div></div>
  </div>;
}
