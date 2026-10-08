import { useNavigate } from "react-router-dom";
import PersistentChat from "./PersistentChat";
import { useAuth } from "../context/AuthContext";

export default function ChatPage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  return <PersistentChat user={user} logout={logout} navigate={navigate} />;
}
