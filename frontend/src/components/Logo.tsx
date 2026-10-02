interface LogoProps {
  variant?: "dark" | "white";
  size?: "sm" | "md";
}

export default function Logo({ variant = "dark", size = "md" }: LogoProps) {
  const iconColor = variant === "white" ? "#FFFFFF" : "#1E3A8A";
  const textColor = variant === "white" ? "#FFFFFF" : "#0F172A";
  const iconSize = size === "sm" ? 28 : 32;

  return (
    <div className="flex items-center gap-2">
      <svg width={iconSize} height={iconSize} viewBox="0 0 32 32" fill="none">
        {/* Shield */}
        <path
          d="M16 2L4 7v9c0 6.6 5.2 12.8 12 14.4C22.8 28.8 28 22.6 28 16V7L16 2z"
          fill={iconColor}
          opacity="1"
        />
        {/* Graduation cap inside shield */}
        <path
          d="M16 10l-6 3 6 3 6-3-6-3z"
          fill="white"
          opacity="0.9"
        />
        <path
          d="M10 14.5v3c0 1.7 2.7 3 6 3s6-1.3 6-3v-3l-6 3-6-3z"
          fill="white"
          opacity="0.7"
        />
        <path d="M22 13v4" stroke="white" strokeWidth="1.5" strokeLinecap="round" opacity="0.8" />
        {/* Check mark */}
        <path
          d="M12 19.5l2.5 2.5 5.5-5.5"
          stroke="white"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
          opacity="0.95"
        />
      </svg>
      <span
        className="font-semibold text-lg tracking-tight"
        style={{ color: textColor, fontFamily: "Inter, sans-serif" }}
      >
        EduTrust
      </span>
    </div>
  );
}
