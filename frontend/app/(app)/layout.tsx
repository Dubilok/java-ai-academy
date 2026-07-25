"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { useAuth } from "@/contexts/auth-context";

export default function AppLayout({ children }: { children: React.ReactNode }) {
  const { user, isLoading, logout } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!isLoading && !user) {
      router.replace("/login");
    }
  }, [user, isLoading, router]);

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <span className="text-text-muted">Loading…</span>
      </div>
    );
  }

  if (!user) return null;

  return (
    <div className="min-h-screen">
      <header className="flex items-center justify-between border-b border-white/10 bg-bg-card px-6 py-3">
        <a href="/dashboard" className="text-lg font-bold text-text-primary">
          Java <span className="text-accent-java">AI</span> Academy
        </a>
        <div className="flex items-center gap-4">
          <span className="text-sm text-text-muted">{user.email}</span>
          <button
            onClick={() => {
              logout();
              router.push("/login");
            }}
            className="text-sm text-text-muted hover:text-text-primary"
          >
            Sign out
          </button>
        </div>
      </header>
      <main>{children}</main>
    </div>
  );
}
