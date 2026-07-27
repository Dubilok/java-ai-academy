"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { fetchSessions, startSession } from "@/lib/queries/interview";
import type { SessionSummary } from "@/lib/schemas/interview";

const TECHNOLOGIES = [
  "Java",
  "Spring Boot",
  "PostgreSQL",
  "Docker",
  "Kubernetes",
  "Redis",
  "Kafka",
  "AWS",
  "System Design",
];

function StatusBadge({ status }: { status: SessionSummary["status"] }) {
  if (status === "ACTIVE") {
    return (
      <span className="rounded-full bg-accent-blue/10 px-2 py-0.5 text-xs font-medium text-accent-blue">
        Active
      </span>
    );
  }
  return (
    <span className="rounded-full bg-success/10 px-2 py-0.5 text-xs font-medium text-success">
      Finished
    </span>
  );
}

function SessionRow({ session }: { session: SessionSummary }) {
  const date = new Date(session.createdAt).toLocaleDateString("en-GB", {
    day: "numeric",
    month: "short",
    year: "numeric",
  });

  return (
    <Link
      href={`/interview/sessions/${session.sessionId}`}
      className="flex items-center justify-between rounded-xl border border-white/5 bg-bg-card px-5 py-4 transition-colors hover:border-accent-blue/30 hover:bg-white/5"
    >
      <div className="flex items-center gap-4">
        <div>
          <p className="text-sm font-medium text-text-primary">{session.technology}</p>
          <p className="mt-0.5 text-xs text-text-muted">{date}</p>
        </div>
      </div>
      <div className="flex items-center gap-4">
        {session.status === "FINISHED" && session.score !== null && (
          <span className="text-sm font-bold text-text-primary">{session.score}%</span>
        )}
        <StatusBadge status={session.status} />
        <span className="text-text-muted">→</span>
      </div>
    </Link>
  );
}

export default function InterviewSessionsPage() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const [technology, setTechnology] = useState("Java");
  const [customTech, setCustomTech] = useState("");
  const [useCustom, setUseCustom] = useState(false);

  const { data: sessions = [], isLoading } = useQuery({
    queryKey: ["interview-sessions"],
    queryFn: fetchSessions,
  });

  const { mutate: start, isPending, error } = useMutation({
    mutationFn: () => startSession(useCustom && customTech.trim() ? customTech.trim() : technology),
    onSuccess: (data) => {
      void queryClient.invalidateQueries({ queryKey: ["interview-sessions"] });
      router.push(`/interview/sessions/${data.sessionId}`);
    },
  });

  return (
    <div className="mx-auto max-w-3xl px-8 py-10">
      <nav className="mb-6 flex items-center gap-2 text-xs text-text-muted" aria-label="Breadcrumb">
        <Link href="/interview" className="hover:text-text-primary">Flashcards</Link>
        <span>/</span>
        <span className="text-text-primary">Mock Interviews</span>
      </nav>

      <h1 className="mb-8 text-2xl font-bold text-text-primary">Mock Interviews</h1>

      {/* Start new session card */}
      <div className="mb-10 rounded-2xl border border-white/10 bg-bg-card p-6">
        <h2 className="mb-4 text-sm font-semibold uppercase tracking-wide text-text-muted">
          Start a new interview
        </h2>

        <div className="flex flex-wrap items-end gap-3">
          {!useCustom ? (
            <div className="flex flex-col gap-1">
              <label htmlFor="tech-select" className="text-xs text-text-muted">Technology</label>
              <select
                id="tech-select"
                value={technology}
                onChange={(e) => setTechnology(e.target.value)}
                className="rounded-lg border border-white/10 bg-bg-base px-3 py-2 text-sm text-text-primary focus:outline-none focus:ring-2 focus:ring-accent-blue"
              >
                {TECHNOLOGIES.map((t) => (
                  <option key={t} value={t}>{t}</option>
                ))}
              </select>
            </div>
          ) : (
            <div className="flex flex-col gap-1">
              <label htmlFor="custom-tech" className="text-xs text-text-muted">Custom topic</label>
              <input
                id="custom-tech"
                type="text"
                value={customTech}
                onChange={(e) => setCustomTech(e.target.value)}
                placeholder="e.g. Microservices"
                className="rounded-lg border border-white/10 bg-bg-base px-3 py-2 text-sm text-text-primary placeholder-text-muted focus:outline-none focus:ring-2 focus:ring-accent-blue"
              />
            </div>
          )}

          <button
            type="button"
            onClick={() => setUseCustom((v) => !v)}
            className="text-xs text-accent-blue hover:underline"
          >
            {useCustom ? "← Pick from list" : "Custom topic"}
          </button>

          <button
            onClick={() => start()}
            disabled={isPending || (useCustom && !customTech.trim())}
            className="rounded-lg bg-accent-java px-5 py-2 text-sm font-semibold text-white transition-opacity hover:opacity-90 disabled:opacity-40"
          >
            {isPending ? "Starting…" : "Start interview"}
          </button>
        </div>

        {error && (
          <p className="mt-3 text-xs text-error">Failed to start session. Please try again.</p>
        )}
      </div>

      {/* Past sessions */}
      <h2 className="mb-4 text-sm font-semibold uppercase tracking-wide text-text-muted">
        Past sessions
      </h2>

      {isLoading && <p className="text-sm text-text-muted">Loading sessions…</p>}

      {!isLoading && sessions.length === 0 && (
        <p className="text-sm text-text-muted">No sessions yet — start your first interview above.</p>
      )}

      <div className="flex flex-col gap-3">
        {sessions.map((session) => (
          <SessionRow key={session.sessionId} session={session} />
        ))}
      </div>
    </div>
  );
}
