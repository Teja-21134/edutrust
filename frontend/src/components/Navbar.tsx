import { LogOut, Upload, MessageSquare } from "./icons";
import Logo from "./Logo";
import { useNavigate } from "react-router-dom";

interface NavbarProps {
  role: "admin" | "student";
  activeTab?: "upload" | "chat";
  onTabChange?: (tab: "upload" | "chat") => void;
  userName?: string;
  onLogout?: () => void;
}

export default function Navbar({ role, activeTab = "chat", onTabChange, userName, onLogout }: NavbarProps) {
  const isAdmin = role === "admin";
  const navigate = useNavigate();
  const goTo = (tab: "upload" | "chat") => {
    onTabChange?.(tab);
    navigate(`/${tab}`);
  };

  return (
    <nav
      className="h-16 bg-white border-b flex items-center px-8 gap-6 shrink-0"
      style={{ borderColor: "#E2E8F0" }}
    >
      {/* Logo */}
      <div className="flex-shrink-0">
        <Logo />
      </div>

      {/* Nav links */}
      <div className="flex-1 flex items-center gap-1 ml-6">
        {isAdmin && (
          <button
            onClick={() => goTo("upload")}
            className="flex items-center gap-1.5 px-3 h-16 text-sm font-medium relative transition-colors"
            style={{
              color: activeTab === "upload" ? "#1E3A8A" : "#64748B",
              borderBottom: activeTab === "upload" ? "2px solid #1E3A8A" : "2px solid transparent",
            }}
          >
            <Upload size={15} />
            Upload
          </button>
        )}
        <button
          onClick={() => goTo("chat")}
          className="flex items-center gap-1.5 px-3 h-16 text-sm font-medium relative transition-colors"
          style={{
            color: activeTab === "chat" ? "#1E3A8A" : "#64748B",
            borderBottom: activeTab === "chat" ? "2px solid #1E3A8A" : "2px solid transparent",
          }}
        >
          <MessageSquare size={15} />
          Chat
        </button>
      </div>

      {/* User controls */}
      <div className="flex items-center gap-3">
        {/* Avatar */}
        <div
          className="w-9 h-9 rounded-full flex items-center justify-center text-white text-sm font-semibold shrink-0"
          style={{ backgroundColor: "#1E3A8A" }}
        >
          {isAdmin ? "AU" : "SU"}
        </div>

        <div className="flex flex-col items-start">
          <span className="text-sm font-medium text-gray-900 leading-tight">
            {userName ?? (isAdmin ? "Admin User" : "Student User")}
          </span>
          <span
            className="text-xs font-medium px-1.5 py-0.5 rounded mt-0.5"
            style={{
              backgroundColor: "#EFF6FF",
              color: "#1E3A8A",
            }}
          >
            {isAdmin ? "Admin" : "Student"}
          </span>
        </div>

        <button
          className="flex items-center gap-1.5 text-sm ml-2 transition-colors"
          style={{ color: "#64748B" }}
          onClick={onLogout}
        >
          <LogOut size={15} />
          Logout
        </button>
      </div>
    </nav>
  );
}
