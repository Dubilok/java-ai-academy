"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

const TABS = [
  { label: "Learning Path", href: "/admin/learning-path" },
  { label: "Generate Course", href: "/admin/generate" },
  { label: "Flashcards", href: "/admin/flashcards" },
] as const;

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();

  return (
    <div className="mx-auto max-w-5xl px-8 py-10">
      <h1 className="mb-6 text-2xl font-bold text-text-primary">Admin Console</h1>

      <nav className="mb-8 flex gap-1 border-b border-white/10" aria-label="Admin sections">
        {TABS.map((tab) => {
          const isActive = pathname === tab.href || pathname.startsWith(tab.href + "/");
          return (
            <Link
              key={tab.href}
              href={tab.href}
              className={`-mb-px rounded-t-lg px-4 py-2 text-sm font-medium transition-colors ${
                isActive
                  ? "border border-b-bg-base border-white/10 bg-bg-card text-text-primary"
                  : "text-text-muted hover:text-text-primary"
              }`}
              aria-current={isActive ? "page" : undefined}
            >
              {tab.label}
            </Link>
          );
        })}
      </nav>

      {children}
    </div>
  );
}
