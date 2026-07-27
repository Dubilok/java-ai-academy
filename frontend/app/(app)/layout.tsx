"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect } from "react";
import { useAuth } from "@/contexts/auth-context";

const NAV_LINKS = [
  { href: "/dashboard", label: "Courses" },
  { href: "/interview", label: "Flashcards" },
];

const ADMIN_NAV_LINK = { href: "/admin", label: "Admin" };

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

  const initial = user.email.charAt(0).toUpperCase();
  const isAccountActive = pathname === "/account";

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
          <Link href="/dashboard" className="text-lg font-bold text-text-primary">
            Java <span className="text-accent-java">AI</span> Academy
          </Link>

          <nav aria-label="Main navigation">
            <ul className="flex items-center gap-6 text-sm">
              {[...NAV_LINKS, ...(user.role === "ROLE_ADMIN" ? [ADMIN_NAV_LINK] : [])].map((link) => {
                const isCurrent =
                  pathname === link.href ||
                  (link.href !== "/dashboard" && pathname.startsWith(link.href + "/"));
                return (
                  <li key={link.href}>
                    <Link
                      href={link.href}
                      aria-current={isCurrent ? "page" : undefined}
                      className={`transition-colors ${
                        isCurrent
                          ? "font-semibold text-accent-java"
                          : "text-text-muted hover:text-text-primary"
                      }`}
                    >
                      {link.label}
                    </Link>
                  </li>
                );
              })}
            </ul>
          </nav>

          <div className="flex items-center gap-3">
            <Link
              href="/account"
              aria-label="Account"
              aria-current={isAccountActive ? "page" : undefined}
              className={`flex h-8 w-8 items-center justify-center rounded-full text-sm font-bold transition-colors ${
                isAccountActive
                  ? "bg-accent-java text-white"
                  : "bg-accent-java/20 text-accent-java hover:bg-accent-java/30"
              }`}
            >
              {initial}
            </Link>
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
