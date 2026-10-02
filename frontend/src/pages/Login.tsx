import { useState } from "react";
import Logo from "../components/Logo";
import { Button, Input, PasswordInput, Alert } from "../components/ui";
import { CheckCircle } from "../components/icons";

type LoginState = "default" | "error" | "loading";

interface LoginProps {
  state?: LoginState;
  onLogin?: (email: string, password: string) => void;
}

function GeometricPattern() {
  return (
    <svg
      className="absolute inset-0 w-full h-full"
      viewBox="0 0 720 1024"
      xmlns="http://www.w3.org/2000/svg"
      preserveAspectRatio="xMidYMid slice"
    >
      {/* Subtle shield outlines */}
      {[
        { x: 60, y: 80, scale: 1.2, opacity: 0.06 },
        { x: 580, y: 120, scale: 0.8, opacity: 0.05 },
        { x: 300, y: 200, scale: 2, opacity: 0.04 },
        { x: 100, y: 500, scale: 1.5, opacity: 0.05 },
        { x: 620, y: 480, scale: 1, opacity: 0.06 },
        { x: 400, y: 750, scale: 1.8, opacity: 0.04 },
        { x: 50, y: 850, scale: 0.9, opacity: 0.05 },
        { x: 660, y: 880, scale: 1.3, opacity: 0.05 },
      ].map((s, i) => (
        <path
          key={i}
          d={`M${s.x} ${s.y - 20 * s.scale} L${s.x - 18 * s.scale} ${s.y - 8 * s.scale} L${s.x - 18 * s.scale} ${s.y + 8 * s.scale} C${s.x - 18 * s.scale} ${s.y + 20 * s.scale} ${s.x} ${s.y + 28 * s.scale} ${s.x} ${s.y + 28 * s.scale} C${s.x} ${s.y + 28 * s.scale} ${s.x + 18 * s.scale} ${s.y + 20 * s.scale} ${s.x + 18 * s.scale} ${s.y + 8 * s.scale} L${s.x + 18 * s.scale} ${s.y - 8 * s.scale} Z`}
          fill="none"
          stroke="white"
          strokeWidth="1"
          opacity={s.opacity}
        />
      ))}
      {/* Thin horizontal lines */}
      {[150, 350, 550, 750, 900].map((y, i) => (
        <line
          key={`h${i}`}
          x1="0" y1={y} x2="720" y2={y}
          stroke="white"
          strokeWidth="0.5"
          opacity="0.04"
        />
      ))}
      {/* Thin diagonal lines */}
      {[0, 200, 400, 600].map((x, i) => (
        <line
          key={`d${i}`}
          x1={x} y1="0" x2={x + 400} y2="1024"
          stroke="white"
          strokeWidth="0.5"
          opacity="0.03"
        />
      ))}
      {/* Small dots */}
      {Array.from({ length: 20 }).map((_, i) => (
        <circle
          key={`dot${i}`}
          cx={(i * 73 + 40) % 720}
          cy={(i * 137 + 60) % 1024}
          r="2"
          fill="white"
          opacity="0.05"
        />
      ))}
    </svg>
  );
}

function FeaturePoint({ text }: { text: string }) {
  return (
    <div className="flex items-center gap-3">
      <div
        className="w-5 h-5 rounded-full flex items-center justify-center shrink-0"
        style={{ backgroundColor: "#0D9488" }}
      >
        <CheckCircle size={12} color="white" />
      </div>
      <span className="text-sm" style={{ color: "#BAE6FD" }}>
        {text}
      </span>
    </div>
  );
}

export default function Login({ state = "default", onLogin }: LoginProps) {
  const [email, setEmail] = useState(state === "error" ? "wrong@example.com" : "");
  const [password, setPassword] = useState(state === "error" ? "wrongpass" : "");

  return (
    <div className="flex h-screen overflow-hidden">
      {/* Left Panel */}
      <div
        className="w-1/2 relative flex flex-col items-center justify-between py-16 px-16 overflow-hidden"
        style={{ backgroundColor: "#1E3A8A" }}
      >
        <GeometricPattern />

        {/* Top: Logo */}
        <div className="relative z-10 self-start">
          <Logo variant="white" size="md" />
        </div>

        {/* Center: Hero */}
        <div className="relative z-10 flex flex-col gap-6 max-w-md">
          <h1
            className="text-4xl font-semibold leading-tight"
            style={{ color: "#FFFFFF" }}
          >
            Trusted answers from your institution's documents
          </h1>
          <p className="text-base leading-relaxed" style={{ color: "#93C5FD" }}>
            Every answer comes with the source document and page number.
          </p>
        </div>

        {/* Bottom: Feature points */}
        <div className="relative z-10 self-start flex flex-col gap-3">
          <FeaturePoint text="Answers only from official documents" />
          <FeaturePoint text="Source and page citations" />
          <FeaturePoint text="Built to avoid wrong or made-up answers" />
        </div>
      </div>

      {/* Right Panel */}
      <div
        className="w-1/2 flex flex-col items-center justify-center px-16"
        style={{ backgroundColor: "#F8FAFC" }}
      >
        {/* Login Card */}
        <div
          className="w-full max-w-md p-10 rounded-xl bg-white"
          style={{
            boxShadow: "0 4px 24px rgba(15,23,42,0.08), 0 1px 4px rgba(15,23,42,0.04)",
          }}
        >
          <div className="mb-8">
            <h2 className="text-2xl font-semibold" style={{ color: "#0F172A" }}>
              Welcome back
            </h2>
            <p className="text-sm mt-1" style={{ color: "#64748B" }}>
              Sign in to continue
            </p>
          </div>

          <div className="flex flex-col gap-5">
            <Input
              label="Email"
              type="email"
              placeholder="you@college.edu"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              error={state === "error" ? "Invalid email or password" : undefined}
            />

            <PasswordInput
              label="Password"
              placeholder="Enter your password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              error={state === "error" ? "Invalid email or password" : undefined}
            />

            {state === "error" && (
              <Alert variant="error" message="Invalid email or password. Please check your credentials and try again." />
            )}

            <Button
              variant={state === "loading" ? "primary" : state === "default" || state === "error" ? "primary" : "primary"}
              loading={state === "loading"}
              loadingText="Signing in..."
              className="w-full mt-1"
              onClick={() => onLogin?.(email, password)}
            >
              Login
            </Button>
          </div>
        </div>

        <p className="mt-6 text-sm" style={{ color: "#94A3B8" }}>
          Contact your administrator if you need access
        </p>
      </div>
    </div>
  );
}
