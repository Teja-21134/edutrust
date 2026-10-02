import Navbar from "../components/Navbar";
import Logo from "../components/Logo";
import {
  Button,
  Input,
  PasswordInput,
  Select,
  DatePicker,
  Dropzone,
  FileRow,
  SourceChip,
  Alert,
  ShieldCheckAvatar,
} from "../components/ui";
import { Send } from "../components/icons";

function SectionLabel({ children }: { children: React.ReactNode }) {
  return (
    <div className="mb-6">
      <h2
        className="text-xs font-semibold uppercase tracking-widest"
        style={{ color: "#94A3B8" }}
      >
        {children}
      </h2>
      <div className="mt-2 h-px" style={{ backgroundColor: "#E2E8F0" }} />
    </div>
  );
}

function Card({ children, className = "" }: { children: React.ReactNode; className?: string }) {
  return (
    <div
      className={`p-6 rounded-xl bg-white ${className}`}
      style={{ border: "1px solid #E2E8F0", boxShadow: "0 1px 4px rgba(0,0,0,0.04)" }}
    >
      {children}
    </div>
  );
}

export default function ComponentSheet() {
  return (
    <div className="min-h-screen" style={{ backgroundColor: "#F8FAFC" }}>
      {/* Header */}
      <div className="bg-white border-b px-8 py-5" style={{ borderColor: "#E2E8F0" }}>
        <div className="max-w-5xl mx-auto">
          <p className="text-xs font-semibold uppercase tracking-widest mb-2" style={{ color: "#64748B" }}>
            EduTrust Design System
          </p>
          <h1 className="text-3xl font-semibold" style={{ color: "#0F172A" }}>
            Component Library
          </h1>
        </div>
      </div>

      <div className="max-w-5xl mx-auto px-8 py-10 space-y-14">

        {/* A — NAVBAR */}
        <section>
          <SectionLabel>Section A — Navbar</SectionLabel>
          <div className="space-y-4">
            <div>
              <p className="text-xs font-medium mb-2" style={{ color: "#64748B" }}>Admin Navbar · Upload active</p>
              <div className="rounded-xl overflow-hidden" style={{ border: "1px solid #E2E8F0" }}>
                <Navbar role="admin" activeTab="upload" />
              </div>
            </div>
            <div>
              <p className="text-xs font-medium mb-2" style={{ color: "#64748B" }}>Admin Navbar · Chat active</p>
              <div className="rounded-xl overflow-hidden" style={{ border: "1px solid #E2E8F0" }}>
                <Navbar role="admin" activeTab="chat" />
              </div>
            </div>
            <div>
              <p className="text-xs font-medium mb-2" style={{ color: "#64748B" }}>Student Navbar</p>
              <div className="rounded-xl overflow-hidden" style={{ border: "1px solid #E2E8F0" }}>
                <Navbar role="student" activeTab="chat" />
              </div>
            </div>
          </div>
        </section>

        {/* B — BUTTONS */}
        <section>
          <SectionLabel>Section B — Buttons</SectionLabel>
          <Card>
            <div className="flex flex-wrap items-center gap-4">
              <Button variant="primary">Primary</Button>
              <Button variant="secondary">Secondary</Button>
              <Button variant="disabled" disabled>Disabled</Button>
              <Button variant="primary" loading loadingText="Signing in...">Loading</Button>
              <Button variant="primary" icon={<Send size={15} />}>Send</Button>
            </div>
          </Card>
        </section>

        {/* C — INPUTS */}
        <section>
          <SectionLabel>Section C — Inputs</SectionLabel>
          <Card>
            <div className="grid grid-cols-2 gap-6">
              <Input label="Default Input" placeholder="Enter value..." />
              <Input label="Error Input" placeholder="Enter email..." error="This field is required" />
              <Select label="Dropdown / Select">
                <option>All Departments</option>
                <option>CSE</option>
                <option>ECE</option>
              </Select>
              <DatePicker label="Date Picker" />
              <PasswordInput label="Password Input" placeholder="Enter password..." />
              <div className="flex flex-col gap-1.5">
                <label className="text-sm font-medium" style={{ color: "#0F172A" }}>
                  Focused Input
                </label>
                <input
                  className="h-11 px-3 rounded-lg text-sm w-full outline-none"
                  style={{
                    border: "1px solid #1E3A8A",
                    backgroundColor: "#FFFFFF",
                    color: "#0F172A",
                    boxShadow: "0 0 0 3px rgba(30,58,138,0.08)",
                  }}
                  placeholder="Focused state..."
                  readOnly
                />
              </div>
            </div>
          </Card>
        </section>

        {/* D — DROPZONE */}
        <section>
          <SectionLabel>Section D — File Dropzone</SectionLabel>
          <Card>
            <div className="grid grid-cols-2 gap-6">
              <Dropzone label="PDF File" />
              <Dropzone label="Error State" error />
            </div>
          </Card>
        </section>

        {/* E — SOURCE CHIP */}
        <section>
          <SectionLabel>Section E — Source Chip</SectionLabel>
          <Card>
            <div className="flex flex-wrap gap-2">
              <SourceChip text="Academic Regulations 2025 · Page 12" />
              <SourceChip text="Leave and Condonation Policy · Page 3" />
              <SourceChip text="Fee Structure 2025-26 · Page 7" />
            </div>
          </Card>
        </section>

        {/* F — CHAT BUBBLES */}
        <section>
          <SectionLabel>Section F — Chat Bubbles</SectionLabel>
          <Card>
            <div className="flex flex-col gap-4 max-w-xl">
              {/* User bubble */}
              <div className="flex justify-end">
                <div
                  className="max-w-sm px-4 py-3 text-sm leading-relaxed"
                  style={{
                    backgroundColor: "#1E3A8A",
                    color: "#FFFFFF",
                    borderRadius: "16px 16px 4px 16px",
                  }}
                >
                  What is the minimum attendance required?
                </div>
              </div>
              {/* Answer bubble */}
              <div className="flex items-start gap-3">
                <ShieldCheckAvatar />
                <div
                  className="max-w-sm px-4 py-3 text-sm leading-relaxed"
                  style={{
                    backgroundColor: "#FFFFFF",
                    color: "#0F172A",
                    border: "1px solid #E2E8F0",
                    borderRadius: "4px 16px 16px 16px",
                    boxShadow: "0 1px 4px rgba(0,0,0,0.04)",
                  }}
                >
                  A student must maintain a minimum of 75% attendance in each semester.
                </div>
              </div>
            </div>
          </Card>
        </section>

        {/* G — ALERTS */}
        <section>
          <SectionLabel>Section G — Alerts</SectionLabel>
          <Card>
            <div className="space-y-3">
              <Alert variant="success" message="Document uploaded successfully. 42 chunks created and indexed." />
              <Alert variant="error" message="Invalid email or password. Please try again." />
              <Alert variant="info" message="Answers are generated only from official institutional documents." />
              <Alert variant="warning" message="This document type requires approval before becoming available to students." />
            </div>
          </Card>
        </section>

        {/* LOGO */}
        <section>
          <SectionLabel>Logo Variants</SectionLabel>
          <div className="grid grid-cols-2 gap-4">
            <Card>
              <div className="flex items-center gap-8">
                <Logo variant="dark" size="md" />
                <Logo variant="dark" size="sm" />
              </div>
            </Card>
            <div
              className="p-6 rounded-xl flex items-center gap-8"
              style={{ backgroundColor: "#1E3A8A" }}
            >
              <Logo variant="white" size="md" />
              <Logo variant="white" size="sm" />
            </div>
          </div>
        </section>

        {/* File row */}
        <section>
          <SectionLabel>Selected File Row</SectionLabel>
          <Card>
            <div className="max-w-sm">
              <FileRow name="Academic_Regulations_2025.pdf" size="2.4 MB" />
            </div>
          </Card>
        </section>

      </div>
    </div>
  );
}
