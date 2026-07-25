"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
} from "react";
import { api, clearAccessToken, setAccessToken } from "@/lib/api";
import { AuthResponseSchema } from "@/lib/schemas/auth";
import { clearTokens, getRefreshToken, setRefreshToken } from "@/lib/tokens";

interface AuthUser {
  email: string;
}

interface AuthContextValue {
  user: AuthUser | null;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const initialized = useRef(false);

  const handleAuthResponse = useCallback((data: unknown) => {
    const parsed = AuthResponseSchema.parse(data);
    setAccessToken(parsed.accessToken);
    setRefreshToken(parsed.refreshToken);
    const payload = JSON.parse(atob(parsed.accessToken.split(".")[1] ?? ""));
    setUser({ email: String(payload.sub) });
  }, []);

  useEffect(() => {
    if (initialized.current) return;
    initialized.current = true;

    const refreshToken = getRefreshToken();
    if (!refreshToken) {
      setIsLoading(false);
      return;
    }

    api
      .post("/auth/refresh", { refreshToken })
      .then((res) => {
        handleAuthResponse(res.data);
      })
      .catch(() => {
        clearTokens();
        clearAccessToken();
      })
      .finally(() => {
        setIsLoading(false);
      });
  }, [handleAuthResponse]);

  const login = useCallback(
    async (email: string, password: string) => {
      const res = await api.post("/auth/login", { email, password });
      handleAuthResponse(res.data);
    },
    [handleAuthResponse]
  );

  const register = useCallback(
    async (email: string, password: string) => {
      const res = await api.post("/auth/register", { email, password });
      handleAuthResponse(res.data);
    },
    [handleAuthResponse]
  );

  const logout = useCallback(() => {
    clearTokens();
    clearAccessToken();
    setUser(null);
  }, []);

  return (
    <AuthContext.Provider value={{ user, isLoading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside AuthProvider");
  return ctx;
}
