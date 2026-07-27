"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import { useAuth } from "@/contexts/auth-context";
import { fetchMe } from "@/lib/queries/catalog";

// ── Icons ─────────────────────────────────────────────────────────────────────

function SearchIcon() {
  return (
    <svg className="h-4 w-4 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
    </svg>
  );
}

function MailIcon() {
  return (
    <svg className="h-5 w-5 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <rect x="2" y="4" width="20" height="16" rx="2" /><polyline points="2 4 12 14 22 4" />
    </svg>
  );
}

function BellIcon() {
  return (
    <svg className="h-5 w-5 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9M13.73 21a2 2 0 0 1-3.46 0" />
    </svg>
  );
}

function MoonIcon() {
  return (
    <svg className="h-5 w-5 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
    </svg>
  );
}

function BookIcon() {
  return (
    <svg className="h-5 w-5 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" /><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
    </svg>
  );
}

function CardsIcon() {
  return (
    <svg className="h-5 w-5 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <rect x="2" y="7" width="20" height="14" rx="2" /><path d="M16 3H8a2 2 0 0 0-2 2v2h12V5a2 2 0 0 0-2-2z" />
    </svg>
  );
}

function MicIcon() {
  return (
    <svg className="h-5 w-5 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z" /><path d="M19 10v2a7 7 0 0 1-14 0v-2M12 19v4M8 23h8" />
    </svg>
  );
}

function AdminIcon() {
  return (
    <svg className="h-5 w-5 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <circle cx="12" cy="12" r="3" /><path d="M19.07 4.93a10 10 0 0 1 0 14.14M4.93 4.93a10 10 0 0 0 0 14.14" />
    </svg>
  );
}

function SignOutIcon() {
  return (
    <svg className="h-5 w-5 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9" />
    </svg>
  );
}

function StarIcon() {
  return (
    <svg className="h-3.5 w-3.5" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
      <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" />
    </svg>
  );
}

function DiamondIcon() {
  return (
    <svg className="h-3.5 w-3.5" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
      <path d="M12 2L2 9l10 13 10-13L12 2zm0 3.5L19.5 9 12 19.5 4.5 9 12 5.5z" />
    </svg>
  );
}

// ── Nav config ────────────────────────────────────────────────────────────────

const LEARNING_NAV = [
  { href: "/dashboard",           label: "Courses",       icon: <BookIcon />,  exact: true  },
  { href: "/interview",           label: "Flashcards",    icon: <CardsIcon />, exact: true  },
  { href: "/interview/sessions",  label: "Mock Interview",icon: <MicIcon />,   exact: false },
];

const ADMIN_NAV = { href: "/admin", label: "Admin Console", icon: <AdminIcon /> };

// ── Page title ────────────────────────────────────────────────────────────────

function getPageTitle(pathname: string): string {
  if (pathname === "/dashboard")                    return "Courses";
  if (pathname === "/account")                      return "Account";
  if (pathname.startsWith("/interview/sessions"))   return "Mock Interviews";
  if (pathname.startsWith("/interview/topics"))     return "Interview Topics";
  if (pathname.startsWith("/interview"))            return "Flashcards";
  if (pathname.startsWith("/courses"))              return "Course";
  if (pathname.startsWith("/tasks"))                return "Task Workspace";
  if (pathname.startsWith("/lectures"))             return "Lecture";
  if (pathname.startsWith("/admin/learning-path"))  return "Learning Path";
  if (pathname.startsWith("/admin/generate"))       return "Generate Course";
  if (pathname.startsWith("/admin/import"))         return "Import Content";
  if (pathname.startsWith("/admin/flashcards"))     return "Manage Flashcards";
  if (pathname.startsWith("/admin"))                return "Admin Console";
  return "Java AI Academy";
}

// ── Sidebar nav link ──────────────────────────────────────────────────────────

function NavLink({
  href, label, icon, pathname, exact = false,
}: {
  href: string; label: string; icon: React.ReactNode; pathname: string; exact?: boolean;
}) {
  const isActive = exact
    ? pathname === href
    : pathname === href || pathname.startsWith(href + "/");

  return (
    <Link
      href={href}
      aria-current={isActive ? "page" : undefined}
      className={`flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors ${
        isActive
          ? "bg-accent-java/15 text-accent-java"
          : "text-text-muted hover:bg-white/5 hover:text-text-primary"
      }`}
    >
      {icon}
      {label}
    </Link>
  );
}

// ── Layout ────────────────────────────────────────────────────────────────────

export default function AppLayout({ children }: { children: React.ReactNode }) {
  const { user, isLoading, logout } = useAuth();
  const router = useRouter();
  const pathname = usePathname();
  const { data: me } = useQuery({ queryKey: ["me"], queryFn: fetchMe, enabled: !!user });

  useEffect(() => {
    if (!isLoading && !user) router.replace("/login");
  }, [user, isLoading, router]);

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center" role="status" aria-live="polite">
        <span className="text-text-muted">Loading…</span>
      </div>
    );
  }

  if (!user) return null;

  const pageTitle = getPageTitle(pathname);
  const initial = user.email.charAt(0).toUpperCase();
  const username = user.email.split("@")[0] ?? user.email;

  return (
    <div className="flex min-h-screen flex-col bg-bg-base">
      <a
        href="#main-content"
        className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-50 focus:rounded-lg focus:bg-accent-blue focus:px-4 focus:py-2 focus:text-white"
      >
        Skip to main content
      </a>

      {/* ── Top bar (full width) ───────────────────────────────────────────── */}
      <header className="fixed inset-x-0 top-0 z-40 flex h-14 items-center border-b border-white/10 bg-bg-card px-4">

        {/* Logo — fixed width matching sidebar */}
        <Link
          href="/dashboard"
          className="flex w-52 shrink-0 items-center gap-2.5"
          aria-label="Java AI Academy home"
        >
          <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-accent-java text-xs font-extrabold text-white">
            AI
          </div>
          <span className="text-sm font-bold text-text-primary">
            Java <span className="text-accent-java">AI</span> Academy
          </span>
        </Link>

        {/* Page title — aligned with main content left edge (pl-52 sidebar + px-6 content = 232px; header px-4 + w-52 logo = 224px → pl-2 closes the gap) */}
        <span className="shrink-0 pl-2 text-xl font-bold text-text-primary">
          {pageTitle}
        </span>

        {/* Search pill — centered in remaining space */}
        <div className="flex flex-1 items-center justify-center px-8">
          <div className="relative w-full max-w-sm">
            <span className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-text-muted">
              <SearchIcon />
            </span>
            <input
              type="search"
              placeholder="Search"
              disabled
              aria-label="Search (coming soon)"
              className="w-full cursor-not-allowed rounded-full border border-white/10 bg-white/[0.08] py-2 pl-9 pr-4 text-sm text-text-muted placeholder:text-text-muted/60 focus:outline-none"
            />
          </div>
        </div>

        {/* Account cluster — right side */}
        <div className="flex shrink-0 items-center gap-1.5">

          {/* Avatar + username + level / crystals + XP */}
          <Link
            href="/account"
            className="flex items-center gap-2 rounded-lg px-2 py-1 transition-colors hover:bg-white/5"
            aria-label="Account"
          >
            <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-accent-java/20 text-sm font-bold text-accent-java">
              {initial}
            </div>
            <div className="hidden flex-col sm:flex">
              <span className="text-xs font-bold leading-tight text-text-primary">
                {username}
                {me && (
                  <span className="ml-1.5 font-normal text-text-muted">
                    Lv.{me.level}
                  </span>
                )}
              </span>
              {me && (
                <span className="flex items-center gap-2 text-[11px]">
                  <span className="flex items-center gap-0.5 text-amber-400">
                    <StarIcon />
                    {me.crystals}
                  </span>
                  <span className="flex items-center gap-0.5 text-accent-blue">
                    <DiamondIcon />
                    {me.xpPoints.toLocaleString()}
                  </span>
                </span>
              )}
            </div>
          </Link>

          {/* Free plan badge */}
          <span className="hidden rounded border border-white/15 px-2.5 py-1 text-[11px] font-medium text-text-muted lg:inline-flex">
            Free plan
          </span>

          {/* Utility icon buttons */}
          <button
            className="rounded-md p-1.5 text-text-muted transition-colors hover:bg-white/5 hover:text-text-primary"
            aria-label="Messages"
          >
            <MailIcon />
          </button>
          <button
            className="rounded-md p-1.5 text-text-muted transition-colors hover:bg-white/5 hover:text-text-primary"
            aria-label="Notifications"
          >
            <BellIcon />
          </button>
          <button
            className="rounded-md p-1.5 text-text-muted transition-colors hover:bg-white/5 hover:text-text-primary"
            aria-label="Toggle theme"
          >
            <MoonIcon />
          </button>
        </div>
      </header>

      {/* ── Body: sidebar + content ────────────────────────────────────────── */}
      <div className="flex flex-1 pt-14">

        {/* Left sidebar */}
        <aside className="fixed bottom-0 left-0 top-14 z-30 flex w-52 flex-col border-r border-white/10 bg-bg-card">
          <nav className="flex-1 overflow-y-auto px-3 py-4" aria-label="Main navigation">
            <p className="mb-1.5 px-3 text-[10px] font-semibold uppercase tracking-widest text-text-muted/50">
              Learning
            </p>
            <ul className="flex flex-col gap-0.5">
              {LEARNING_NAV.map((item) => (
                <li key={item.href}>
                  <NavLink
                    href={item.href}
                    label={item.label}
                    icon={item.icon}
                    pathname={pathname}
                    exact={item.exact}
                  />
                </li>
              ))}
            </ul>

            {user.role === "ROLE_ADMIN" && (
              <>
                <p className="mb-1.5 mt-6 px-3 text-[10px] font-semibold uppercase tracking-widest text-text-muted/50">
                  Admin
                </p>
                <ul>
                  <li>
                    <NavLink
                      href={ADMIN_NAV.href}
                      label={ADMIN_NAV.label}
                      icon={ADMIN_NAV.icon}
                      pathname={pathname}
                    />
                  </li>
                </ul>
              </>
            )}
          </nav>

          <div className="shrink-0 border-t border-white/10 px-3 py-3">
            <button
              onClick={() => { logout(); router.push("/login"); }}
              className="flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-text-muted transition-colors hover:bg-white/5 hover:text-text-primary"
            >
              <SignOutIcon />
              Sign out
            </button>
          </div>
        </aside>

        {/* Main content */}
        <main id="main-content" tabIndex={-1} className="flex-1 pl-52">
          {children}
        </main>
      </div>
    </div>
  );
}
