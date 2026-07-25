"use client";

import { usePathname, useRouter } from "next/navigation";
import { useEffect } from "react";
import { useAuth } from "@/contexts/auth-context";

const NAV_LINKS = [
  { href: "/dashboard", label: "Dashboard" },
  { href: "/interview", label: "Flashcards" },
];

export default function AppLayout({ children }: { children: React.ReactNode }) {
  const { user, isLoading, logout } = useAuth();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (!isLoading && !user) {
      router.replace("/login");
    }
  }, [user, isLoading, router]);

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center" role="status" aria-live="polite">
        <span className="text-text-muted">Loading…</span>
      </div>
    );
  }

  if (!user) return null;

  return (
    <div className="min-h-screen">
      <a
        href="#main-content"
        className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-50 focus:rounded-lg focus:bg-accent-blue focus:px-4 focus:py-2 focus:text-white"
      >
        Skip to main content
      </a>

      <header className="border-b border-white/10 bg-bg-card">
        <div className="flex items-center justify-between px-6 py-3">
          <a href="/dashboard" className="text-lg font-bold text-text-primary">
            Java <span className="text-accent-java">AI</span> Academy
          </a>

          <nav aria-label="Main navigation">
            <ul className="flex items-center gap-6 text-sm">
              {NAV_LINKS.map((link) => {
                const isCurrent = pathname === link.href || pathname.startsWith(link.href + "/");
                return (
                  <li key={link.href}>
                    <a
                      href={link.href}
                      aria-current={isCurrent ? "page" : undefined}
                      className={`transition-colors ${
                        isCurrent
                          ? "font-semibold text-accent-java"
                          : "text-text-muted hover:text-text-primary"
                      }`}
                    >
                      {link.label}
                    </a>
                  </li>
                );
              })}
            </ul>
          </nav>

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
        </div>
      </header>

      <main id="main-content" tabIndex={-1}>
        {children}
      </main>
    </div>
  );
}
