"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { fetchMe } from "@/lib/queries/catalog";

function getGreeting(): string {
  const hour = new Date().getHours();
  if (hour < 12) return "Good morning";
  if (hour < 17) return "Good afternoon";
  return "Good evening";
}

function emailToName(email: string): string {
  return email.split("@")[0] ?? email;
}

// ── Icons ─────────────────────────────────────────────────────────────────────

function StarIcon() {
  return (
    <svg className="h-5 w-5" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
      <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" />
    </svg>
  );
}

function BoltIcon() {
  return (
    <svg className="h-5 w-5" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
      <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z" />
    </svg>
  );
}

function DiamondIcon() {
  return (
    <svg className="h-5 w-5" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
      <path d="M12 2L2 9l10 13 10-13L12 2zm0 3.5L19.5 9 12 19.5 4.5 9 12 5.5z" />
    </svg>
  );
}

function FlameIcon() {
  return (
    <svg className="h-5 w-5" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
      <path d="M12 23c-4.97 0-9-4.03-9-9 0-3.29 1.76-6.17 4.4-7.78C7.93 8.45 8.5 11 10 12c0-4 2.5-7 5-9-1 5 2 8 2 11 0 2.76-2.24 5-5 5z" />
    </svg>
  );
}

function CardsIcon() {
  return (
    <svg className="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <rect x="2" y="5" width="20" height="14" rx="2" />
      <path d="M16 2v4M8 2v4" />
    </svg>
  );
}

function MicIcon() {
  return (
    <svg className="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z" />
      <path d="M19 10v2a7 7 0 0 1-14 0v-2M12 19v4M8 23h8" />
    </svg>
  );
}

// ── Stat card ─────────────────────────────────────────────────────────────────

interface StatCardProps {
  icon: React.ReactNode;
  value: string | number;
  label: string;
  color: string;
  bg: string;
}

function StatCard({ icon, value, label, color, bg }: StatCardProps) {
  return (
    <div className={`flex flex-col items-center gap-1.5 rounded-xl ${bg} px-3 py-4`}>
      <span className={color}>{icon}</span>
      <span className={`text-2xl font-bold ${color}`}>{value}</span>
      <span className="text-center text-xs text-text-muted">{label}</span>
    </div>
  );
}

// ── Quick action ──────────────────────────────────────────────────────────────

interface QuickActionProps {
  href: string;
  icon: React.ReactNode;
  label: string;
  description: string;
  accent?: boolean;
}

function QuickAction({ href, icon, label, description, accent }: QuickActionProps) {
  return (
    <Link
      href={href}
      className={`group flex items-center gap-3 rounded-xl border px-4 py-4 transition-all ${
        accent
          ? "border-accent-java/30 bg-accent-java/10 hover:border-accent-java/60 hover:bg-accent-java/15"
          : "border-white/10 bg-bg-card hover:border-white/20"
      }`}
    >
      <span className={`shrink-0 ${accent ? "text-accent-java" : "text-text-muted group-hover:text-text-primary"}`}>
        {icon}
      </span>
      <div className="min-w-0">
        <p className={`font-semibold ${accent ? "text-accent-java" : "text-text-primary"}`}>
          {label}
        </p>
        <p className="text-sm text-text-muted">{description}</p>
      </div>
      <span className="ml-auto shrink-0 text-text-muted opacity-0 transition-opacity group-hover:opacity-100">
        →
      </span>
    </Link>
  );
}

// ── XP bar ────────────────────────────────────────────────────────────────────

function xpForLevel(level: number): number {
  return (level - 1) * (level - 1) * 100;
}

function XpSection({ xpPoints, level }: { xpPoints: number; level: number }) {
  const currentLevelXp = xpForLevel(level);
  const nextLevelXp = xpForLevel(level + 1);
  const toNext = Math.max(0, nextLevelXp - xpPoints);
  const progress =
    nextLevelXp > currentLevelXp
      ? Math.min(100, Math.round(((xpPoints - currentLevelXp) / (nextLevelXp - currentLevelXp)) * 100))
      : 100;

  return (
    <div className="flex flex-col gap-2">
      <div className="flex items-center justify-between text-xs">
        <span className="font-semibold text-accent-java">Level {level}</span>
        <span className="text-text-muted">
          {toNext > 0 ? `${toNext.toLocaleString()} XP to level ${level + 1}` : "Max level"}
        </span>
      </div>
      <div
        className="relative h-2.5 w-full overflow-hidden rounded-full bg-white/10"
        role="progressbar"
        aria-valuenow={progress}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-label={`XP progress to level ${level + 1}: ${progress}%`}
      >
        <div
          className="h-full rounded-full transition-all duration-700"
          style={{
            width: `${progress}%`,
            background: "linear-gradient(90deg, #EA580C, #3B82F6)",
          }}
        />
      </div>
      <div className="flex items-center justify-between text-xs text-text-muted">
        <span>{xpPoints.toLocaleString()} XP</span>
        <span>{nextLevelXp.toLocaleString()} XP</span>
      </div>
    </div>
  );
}

// ── Skeleton ──────────────────────────────────────────────────────────────────

function AccountSkeleton() {
  return (
    <div className="flex flex-col gap-6">
      <div className="rounded-2xl border border-white/10 bg-bg-card p-6">
        <div className="flex items-center gap-4">
          <div className="h-16 w-16 animate-pulse rounded-2xl bg-white/10" />
          <div className="flex flex-col gap-2">
            <div className="h-5 w-40 animate-pulse rounded bg-white/10" />
            <div className="h-4 w-56 animate-pulse rounded bg-white/10" />
          </div>
        </div>
        <div className="mt-6 grid grid-cols-2 gap-3 sm:grid-cols-4">
          {[1, 2, 3, 4].map((n) => (
            <div key={n} className="h-20 animate-pulse rounded-xl bg-white/10" />
          ))}
        </div>
        <div className="mt-5 flex flex-col gap-2">
          <div className="h-2.5 w-full animate-pulse rounded-full bg-white/10" />
        </div>
      </div>
    </div>
  );
}

// ── Page ──────────────────────────────────────────────────────────────────────

export default function AccountPage() {
  const { data: me, isLoading } = useQuery({
    queryKey: ["me"],
    queryFn: fetchMe,
  });

  const name = me ? emailToName(me.email) : "";

  return (
    <div className="mx-auto max-w-2xl px-6 py-10">
      {isLoading && <AccountSkeleton />}

      {me && (
        <div className="flex flex-col gap-6">
          {/* Profile card */}
          <section className="overflow-hidden rounded-2xl border border-white/10 bg-bg-card">
            {/* Greeting */}
            <div className="flex items-center gap-4 px-6 pt-6">
              <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-2xl bg-accent-java/15 text-2xl font-bold text-accent-java">
                {name.charAt(0).toUpperCase()}
              </div>
              <div>
                <h2 className="text-xl font-bold text-text-primary">
                  {getGreeting()}, {name}!
                </h2>
                <p className="text-sm text-text-muted">{me.email}</p>
              </div>
            </div>

            {/* Stats */}
            <div className="mt-5 grid grid-cols-2 gap-3 px-6 sm:grid-cols-4">
              <StatCard
                icon={<StarIcon />}
                value={me.level}
                label="Level"
                color="text-accent-java"
                bg="bg-accent-java/10"
              />
              <StatCard
                icon={<BoltIcon />}
                value={me.xpPoints.toLocaleString()}
                label="Total XP"
                color="text-accent-blue"
                bg="bg-accent-blue/10"
              />
              <StatCard
                icon={<DiamondIcon />}
                value={me.crystals}
                label="Crystals"
                color="text-accent-blue"
                bg="bg-white/5"
              />
              <StatCard
                icon={<FlameIcon />}
                value={me.streak}
                label="Day streak"
                color={me.streak > 0 ? "text-error" : "text-text-muted"}
                bg={me.streak > 0 ? "bg-error/10" : "bg-white/5"}
              />
            </div>

            {/* XP bar */}
            <div className="px-6 pb-6 pt-4">
              <XpSection xpPoints={me.xpPoints} level={me.level} />
            </div>
          </section>

          {/* Quick actions */}
          <section>
            <h2 className="mb-3 text-xs font-semibold uppercase tracking-widest text-text-muted">
              Quick actions
            </h2>
            <div className="flex flex-col gap-3">
              <QuickAction
                href="/interview"
                icon={<CardsIcon />}
                label="Flashcards"
                description="Practise by topic"
                accent
              />
              <QuickAction
                href="/interview/sessions"
                icon={<MicIcon />}
                label="Mock Interview"
                description="AI-powered Q&A session"
              />
            </div>
          </section>
        </div>
      )}
    </div>
  );
}
