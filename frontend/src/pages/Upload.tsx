import Navbar from "../components/Navbar";
import {
  Button,
  Input,
  Select,
  DatePicker,
  Dropzone,
  FileRow,
  Alert,
  ProgressBar,
} from "../components/ui";
import { Upload as UploadIcon, RotateCcw, MessageSquare } from "../components/icons";

type UploadState = "empty" | "fileSelected" | "uploading" | "success" | "error";

interface UploadProps {
  state?: UploadState;
  onNavigate?: (page: string) => void;
}

const sampleData = {
  title: "Academic Regulations 2025",
  department: "CSE",
  docType: "Regulations",
  year: "2025-26",
  version: "v2",
  date: "2025-08-15",
  authority: "Principal",
};

export default function Upload({ state = "empty", onNavigate }: UploadProps) {
  const isDisabled = state === "uploading" || state === "success";
  const fieldOpacity = isDisabled ? 0.5 : 1;

  return (
    <div className="min-h-screen flex flex-col" style={{ backgroundColor: "#F8FAFC" }}>
      <Navbar
        role="admin"
        activeTab="upload"
        onTabChange={(tab) => onNavigate?.(tab === "chat" ? "chat-empty" : "upload-empty")}
      />

      <div className="flex-1 px-8 py-12">
        <div className="max-w-3xl mx-auto">
          {/* Page heading */}
          <div className="mb-8">
            <h1 className="text-3xl font-semibold" style={{ color: "#0F172A" }}>
              Upload Document
            </h1>
            <p className="text-base mt-2" style={{ color: "#64748B" }}>
              Add an official institutional document so students can ask questions about it.
            </p>
          </div>

          {/* Form card */}
          <div
            className="bg-white rounded-xl p-8"
            style={{
              border: "1px solid #E2E8F0",
              boxShadow: "0 1px 6px rgba(15,23,42,0.05)",
              opacity: state === "uploading" ? 0.85 : 1,
            }}
          >
            {/* Success alert */}
            {state === "success" && (
              <div className="mb-6">
                <Alert
                  variant="success"
                  message="Document uploaded successfully. 42 chunks created and indexed."
                />
              </div>
            )}

            {/* Error alert */}
            {state === "error" && (
              <div className="mb-6">
                <Alert variant="error" message="Only PDF files are allowed. Please upload a valid PDF document." />
              </div>
            )}

            {/* Form grid */}
            <div
              className="grid grid-cols-2 gap-6"
              style={{ pointerEvents: isDisabled ? "none" : "auto", opacity: fieldOpacity }}
            >
              {/* Document title — full width */}
              <div className="col-span-2">
                <Input
                  label="Document Title"
                  placeholder="e.g., Academic Regulations 2025"
                  defaultValue={state !== "empty" && state !== "error" ? sampleData.title : ""}
                  readOnly={isDisabled}
                />
              </div>

              {/* Department */}
              <Select
                label="Department"
                defaultValue={state !== "empty" && state !== "error" ? sampleData.department : ""}
                onChange={() => {}}
              >
                <option value="">All Departments</option>
                <option value="CSE">CSE</option>
                <option value="ECE">ECE</option>
                <option value="EEE">EEE</option>
                <option value="Mechanical">Mechanical</option>
                <option value="Civil">Civil</option>
              </Select>

              {/* Document type */}
              <Select
                label="Document Type"
                defaultValue={state !== "empty" && state !== "error" ? sampleData.docType : ""}
                onChange={() => {}}
              >
                <option value="">Select type...</option>
                <option value="Regulations">Regulations</option>
                <option value="Circular">Circular</option>
                <option value="Academic Calendar">Academic Calendar</option>
                <option value="Fee Structure">Fee Structure</option>
                <option value="Policy">Policy</option>
                <option value="Other">Other</option>
              </Select>

              {/* Academic year */}
              <Select
                label="Academic Year"
                defaultValue={state !== "empty" && state !== "error" ? sampleData.year : ""}
                onChange={() => {}}
              >
                <option value="">Select year...</option>
                <option value="2023-24">2023-24</option>
                <option value="2024-25">2024-25</option>
                <option value="2025-26">2025-26</option>
                <option value="2026-27">2026-27</option>
              </Select>

              {/* Version */}
              <Input
                label="Version"
                placeholder="e.g., v2"
                defaultValue={state !== "empty" && state !== "error" ? sampleData.version : ""}
                readOnly={isDisabled}
              />

              {/* Document date */}
              <DatePicker
                label="Document Date"
                defaultValue={state !== "empty" && state !== "error" ? sampleData.date : ""}
              />

              {/* Issuing authority */}
              <Select
                label="Issuing Authority"
                optional
                defaultValue={state !== "empty" && state !== "error" ? sampleData.authority : ""}
                onChange={() => {}}
              >
                <option value="">Select authority...</option>
                <option value="Principal">Principal</option>
                <option value="Dean">Dean</option>
                <option value="Department Head">Department Head</option>
                <option value="Exam Cell">Exam Cell</option>
              </Select>

              {/* PDF file — full width */}
              <div className="col-span-2">
                {state === "fileSelected" || state === "uploading" || state === "success" ? (
                  <div className="flex flex-col gap-1.5">
                    <label className="text-sm font-medium" style={{ color: "#0F172A" }}>
                      PDF File
                    </label>
                    <FileRow
                      name="Academic_Regulations_2025.pdf"
                      size="2.4 MB"
                    />
                  </div>
                ) : (
                  <Dropzone label="PDF File" error={state === "error"} />
                )}
              </div>
            </div>

            {/* Progress bar — uploading state */}
            {state === "uploading" && (
              <div className="mt-6 flex flex-col gap-2">
                <div className="flex items-center justify-between">
                  <p className="text-sm" style={{ color: "#64748B" }}>
                    Processing document... extracting text and creating chunks
                  </p>
                  <span className="text-sm font-medium" style={{ color: "#1E3A8A" }}>
                    65%
                  </span>
                </div>
                <ProgressBar value={65} />
              </div>
            )}

            {/* Action buttons */}
            <div className="mt-8 flex items-center justify-end gap-3">
              {state === "success" ? (
                <>
                  <Button variant="secondary" icon={<UploadIcon size={15} />}>
                    Upload another
                  </Button>
                  <Button
                    variant="primary"
                    icon={<MessageSquare size={15} />}
                    onClick={() => onNavigate?.("chat-empty")}
                  >
                    Go to Chat
                  </Button>
                </>
              ) : (
                <>
                  <Button variant="secondary" icon={<RotateCcw size={15} />}>
                    Reset
                  </Button>
                  <Button
                    variant={state === "empty" || state === "error" ? "disabled" : "primary"}
                    disabled={state === "empty" || state === "error"}
                    loading={state === "uploading"}
                    loadingText="Uploading..."
                    icon={<UploadIcon size={15} />}
                  >
                    Upload
                  </Button>
                </>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
