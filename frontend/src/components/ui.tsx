import { Loader2, Eye, EyeOff, ChevronDown, Calendar, FileText, X, CheckCircle, AlertCircle, Info, AlertTriangle, ShieldCheck } from "./icons";
import { useState } from "react";

// ─── Button ────────────────────────────────────────────────────────────────

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: "primary" | "secondary" | "disabled";
  loading?: boolean;
  loadingText?: string;
  icon?: React.ReactNode;
  size?: "md" | "sm";
}

export function Button({
  variant = "primary",
  loading = false,
  loadingText,
  icon,
  size = "md",
  children,
  disabled,
  className = "",
  ...props
}: ButtonProps) {
  const isDisabled = disabled || loading || variant === "disabled";

  const base = `inline-flex items-center justify-center gap-2 font-semibold rounded-lg transition-colors text-sm ${
    size === "md" ? "h-11 px-5" : "h-9 px-4"
  }`;

  const styles: Record<string, React.CSSProperties> = {
    primary: {
      backgroundColor: isDisabled ? "#94A3B8" : "#1E3A8A",
      color: "#FFFFFF",
      cursor: isDisabled ? "not-allowed" : "pointer",
    },
    secondary: {
      backgroundColor: "#FFFFFF",
      color: isDisabled ? "#94A3B8" : "#1E3A8A",
      border: `1px solid ${isDisabled ? "#CBD5E1" : "#1E3A8A"}`,
      cursor: isDisabled ? "not-allowed" : "pointer",
    },
    disabled: {
      backgroundColor: "#F1F5F9",
      color: "#94A3B8",
      cursor: "not-allowed",
    },
  };

  return (
    <button
      className={`${base} ${className}`}
      style={styles[variant]}
      disabled={isDisabled}
      {...props}
    >
      {loading ? <Loader2 size={15} className="animate-spin" /> : icon}
      {loading ? (loadingText ?? "Loading...") : children}
    </button>
  );
}

// ─── Input ─────────────────────────────────────────────────────────────────

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  optional?: boolean;
}

export function Input({ label, error, optional, className = "", ...props }: InputProps) {
  return (
    <div className="flex flex-col gap-1.5">
      {label && (
        <label className="text-sm font-medium" style={{ color: "#0F172A" }}>
          {label}
          {optional && (
            <span className="ml-1 font-normal" style={{ color: "#64748B" }}>
              (optional)
            </span>
          )}
        </label>
      )}
      <input
        className={`h-11 px-3 rounded-lg text-sm w-full outline-none transition-all ${className}`}
        style={{
          border: error ? "1px solid #DC2626" : "1px solid #E2E8F0",
          backgroundColor: "#FFFFFF",
          color: "#0F172A",
          boxShadow: error ? "0 0 0 3px rgba(220,38,38,0.08)" : undefined,
        }}
        onFocus={(e) => {
          if (!error) {
            e.target.style.border = "1px solid #1E3A8A";
            e.target.style.boxShadow = "0 0 0 3px rgba(30,58,138,0.08)";
          }
        }}
        onBlur={(e) => {
          if (!error) {
            e.target.style.border = "1px solid #E2E8F0";
            e.target.style.boxShadow = "none";
          }
        }}
        {...props}
      />
      {error && (
        <p className="text-xs" style={{ color: "#DC2626" }}>
          {error}
        </p>
      )}
    </div>
  );
}

// ─── Password Input ─────────────────────────────────────────────────────────

interface PasswordInputProps {
  label?: string;
  error?: string;
  value?: string;
  onChange?: (e: React.ChangeEvent<HTMLInputElement>) => void;
  placeholder?: string;
}

export function PasswordInput({ label, error, value, onChange, placeholder }: PasswordInputProps) {
  const [show, setShow] = useState(false);

  return (
    <div className="flex flex-col gap-1.5">
      {label && (
        <label className="text-sm font-medium" style={{ color: "#0F172A" }}>
          {label}
        </label>
      )}
      <div className="relative">
        <input
          type={show ? "text" : "password"}
          className="h-11 px-3 pr-10 rounded-lg text-sm w-full outline-none transition-all"
          style={{
            border: error ? "1px solid #DC2626" : "1px solid #E2E8F0",
            backgroundColor: "#FFFFFF",
            color: "#0F172A",
          }}
          placeholder={placeholder}
          value={value}
          onChange={onChange}
          onFocus={(e) => {
            if (!error) e.target.style.border = "1px solid #1E3A8A";
          }}
          onBlur={(e) => {
            if (!error) e.target.style.border = "1px solid #E2E8F0";
          }}
        />
        <button
          type="button"
          className="absolute right-3 top-1/2 -translate-y-1/2 transition-colors"
          style={{ color: "#64748B" }}
          onClick={() => setShow(!show)}
        >
          {show ? <EyeOff size={16} /> : <Eye size={16} />}
        </button>
      </div>
      {error && <p className="text-xs" style={{ color: "#DC2626" }}>{error}</p>}
    </div>
  );
}

// ─── Select ─────────────────────────────────────────────────────────────────

interface SelectProps extends React.SelectHTMLAttributes<HTMLSelectElement> {
  label?: string;
  error?: string;
  optional?: boolean;
}

export function Select({ label, error, optional, children, className = "", ...props }: SelectProps) {
  return (
    <div className="flex flex-col gap-1.5">
      {label && (
        <label className="text-sm font-medium" style={{ color: "#0F172A" }}>
          {label}
          {optional && (
            <span className="ml-1 font-normal" style={{ color: "#64748B" }}>
              (optional)
            </span>
          )}
        </label>
      )}
      <div className="relative">
        <select
          className={`h-11 px-3 pr-8 rounded-lg text-sm w-full outline-none appearance-none transition-all ${className}`}
          style={{
            border: error ? "1px solid #DC2626" : "1px solid #E2E8F0",
            backgroundColor: "#FFFFFF",
            color: "#0F172A",
          }}
          {...props}
        >
          {children}
        </select>
        <ChevronDown
          size={15}
          className="absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none"
          style={{ color: "#64748B" }}
        />
      </div>
      {error && <p className="text-xs" style={{ color: "#DC2626" }}>{error}</p>}
    </div>
  );
}

// ─── Date Picker ─────────────────────────────────────────────────────────────

interface DatePickerProps {
  label?: string;
  value?: string;
  defaultValue?: string;
  onChange?: (e: React.ChangeEvent<HTMLInputElement>) => void;
}

export function DatePicker({ label, value, defaultValue, onChange }: DatePickerProps) {
  return (
    <div className="flex flex-col gap-1.5">
      {label && (
        <label className="text-sm font-medium" style={{ color: "#0F172A" }}>
          {label}
        </label>
      )}
      <div className="relative">
        <input
          type="date"
          className="h-11 px-3 pr-10 rounded-lg text-sm w-full outline-none"
          style={{
            border: "1px solid #E2E8F0",
            backgroundColor: "#FFFFFF",
            color: "#0F172A",
          }}
          value={value}
          defaultValue={defaultValue}
          onChange={onChange}
        />
        <Calendar
          size={15}
          className="absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none"
          style={{ color: "#64748B" }}
        />
      </div>
    </div>
  );
}

// ─── Dropzone ─────────────────────────────────────────────────────────────────

interface DropzoneProps {
  error?: boolean;
  label?: string;
  onFile?: (file: File) => void;
}

export function Dropzone({ error, label, onFile }: DropzoneProps) {
  const [isDragging, setIsDragging] = useState(false);
  const handleFile = (file?: File) => { if (file) onFile?.(file); };
  return (
    <div className="flex flex-col gap-1.5 w-full">
      {label && (
        <label className="text-sm font-medium" style={{ color: "#0F172A" }}>
          {label}
        </label>
      )}
      <div
        className="w-full rounded-xl flex flex-col items-center justify-center gap-2 py-10 px-6 transition-all"
        style={{
          border: error ? "2px dashed #DC2626" : "2px dashed #CBD5E1",
          backgroundColor: error ? "#FEF2F2" : "#FFFFFF",
        }}
        onDragOver={(event) => { event.preventDefault(); setIsDragging(true); }}
        onDragLeave={() => setIsDragging(false)}
        onDrop={(event) => { event.preventDefault(); setIsDragging(false); handleFile(event.dataTransfer.files[0]); }}
      >
        <input id="pdf-file-input" type="file" accept="application/pdf,.pdf" className="hidden" onChange={(event) => handleFile(event.target.files?.[0])} />
        <svg
          width="40"
          height="40"
          viewBox="0 0 24 24"
          fill="none"
          stroke={error ? "#DC2626" : isDragging ? "#1E3A8A" : "#94A3B8"}
          strokeWidth="1.5"
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          <polyline points="16 16 12 12 8 16" />
          <line x1="12" y1="12" x2="12" y2="21" />
          <path d="M20.39 18.39A5 5 0 0 0 18 9h-1.26A8 8 0 1 0 3 16.3" />
        </svg>
        <div className="text-center">
          <p className="text-sm font-medium" style={{ color: error ? "#DC2626" : "#0F172A" }}>
            Drag and drop your PDF here
          </p>
          <p className="text-sm mt-0.5" style={{ color: "#64748B" }}>
            or{" "}
              <label
                htmlFor="pdf-file-input"
                className="font-medium cursor-pointer"
                style={{ color: error ? "#DC2626" : "#1E3A8A" }}
              >
                Choose file
              </label>
          </p>
        </div>
        <p className="text-xs" style={{ color: "#94A3B8" }}>
          PDF only, max 20 MB
        </p>
      </div>
    </div>
  );
}

// ─── Selected File Row ───────────────────────────────────────────────────────

interface FileRowProps {
  name: string;
  size: string;
  onRemove?: () => void;
}

export function FileRow({ name, size, onRemove }: FileRowProps) {
  return (
    <div
      className="flex items-center gap-3 p-3 rounded-lg"
      style={{ border: "1px solid #E2E8F0", backgroundColor: "#F8FAFC" }}
    >
      <div
        className="w-9 h-9 rounded-lg flex items-center justify-center shrink-0"
        style={{ backgroundColor: "#EFF6FF" }}
      >
        <FileText size={18} style={{ color: "#1E3A8A" }} />
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-sm font-medium truncate" style={{ color: "#0F172A" }}>
          {name}
        </p>
        <p className="text-xs" style={{ color: "#64748B" }}>
          {size}
        </p>
      </div>
      <button
        onClick={onRemove}
        className="shrink-0 p-1 rounded transition-colors hover:bg-gray-100"
        style={{ color: "#94A3B8" }}
      >
        <X size={16} />
      </button>
    </div>
  );
}

// ─── Source Chip ─────────────────────────────────────────────────────────────

interface SourceChipProps {
  text: string;
}

export function SourceChip({ text }: SourceChipProps) {
  return (
    <span
      className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium"
      style={{ backgroundColor: "#CCFBF1", color: "#0D9488" }}
    >
      <FileText size={11} />
      {text}
    </span>
  );
}

// ─── Alert ───────────────────────────────────────────────────────────────────

interface AlertProps {
  variant: "success" | "error" | "info" | "warning";
  message: string;
}

const alertConfig = {
  success: {
    bg: "#DCFCE7",
    color: "#16A34A",
    Icon: CheckCircle,
  },
  error: {
    bg: "#FEF2F2",
    color: "#DC2626",
    Icon: AlertCircle,
  },
  info: {
    bg: "#EFF6FF",
    color: "#1E3A8A",
    Icon: Info,
  },
  warning: {
    bg: "#FEF3C7",
    color: "#D97706",
    Icon: AlertTriangle,
  },
};

export function Alert({ variant, message }: AlertProps) {
  const cfg = alertConfig[variant];
  const Icon = cfg.Icon;
  return (
    <div
      className="flex items-start gap-2.5 p-3.5 rounded-lg text-sm"
      style={{ backgroundColor: cfg.bg, color: cfg.color }}
    >
      <Icon size={16} className="shrink-0 mt-0.5" />
      <span>{message}</span>
    </div>
  );
}

// ─── Shield Check Avatar ───────────────────────────────────────────────────

export function ShieldCheckAvatar() {
  return (
    <div
      className="w-8 h-8 rounded-full flex items-center justify-center shrink-0"
      style={{ backgroundColor: "#CCFBF1" }}
    >
      <ShieldCheck size={16} style={{ color: "#0D9488" }} />
    </div>
  );
}

// ─── Progress Bar ────────────────────────────────────────────────────────────

interface ProgressBarProps {
  value: number;
}

export function ProgressBar({ value }: ProgressBarProps) {
  return (
    <div
      className="w-full h-2 rounded-full overflow-hidden"
      style={{ backgroundColor: "#E2E8F0" }}
    >
      <div
        className="h-full rounded-full transition-all"
        style={{ width: `${value}%`, backgroundColor: "#1E3A8A" }}
      />
    </div>
  );
}
