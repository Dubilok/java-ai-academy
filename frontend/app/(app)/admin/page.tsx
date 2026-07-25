"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { fetchAiUsage, fetchJobStatus, generateCourse } from "@/lib/queries/admin";
import type { JobStatus } from "@/lib/schemas/admin";

function formatCost(costUsd: number | null): string {
  if (costUsd === null) return "—";
  return `$${costUsd.toFixed(4)}`;
}

function formatMs(ms: number | null): string {
  if (ms === null) return "—";
  if (ms < 1000) return `${ms}ms`;
  return `${(ms / 1000).toFixed(1)}s`;
}

export default function AdminPage() {
  const queryClient = useQueryClient();
  const [technology, setTechnology] = useState("");
  const [jobId, setJobId] = useState<string | null>(null);
  const [isGenerating, setIsGenerating] = useState(false);
  const [generateError, setGenerateError] = useState<string | null>(null);

  const { data: usageRows = [] } = useQuery({
    queryKey: ["ai-usage"],
    queryFn: fetchAiUsage,
    refetchInterval: 10_000,
  });

  const { data: jobStatus } = useQuery<JobStatus>({
    queryKey: ["job", jobId],
    queryFn: () => fetchJobStatus(jobId!),
    enabled: jobId !== null,
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      return status === "DONE" || status === "FAILED" ? false : 3_000;
    },
  });

  async function handleGenerate() {
    if (!technology.trim()) return;
    setGenerateError(null);
    setIsGenerating(true);
    try {
      const id = await generateCourse(technology.trim());
      setJobId(id);
      void queryClient.invalidateQueries({ queryKey: ["ai-usage"] });
    } catch {
      setGenerateError("Failed to start generation job.");
    } finally {
      setIsGenerating(false);
    }
  }

  const jobDone = jobStatus?.status === "DONE" || jobStatus?.status === "FAILED";

  return (
    <div className="mx-auto max-w-5xl p-8">
      <h1 className="mb-8 text-2xl font-bold text-text-primary">Admin Console</h1>

      <section className="mb-10 rounded-xl bg-bg-card p-6">
        <h2 className="mb-4 text-lg font-semibold text-text-primary">Generate Course with AI</h2>
        <div className="flex gap-3">
          <input
            type="text"
            placeholder="Technology (e.g. Spring Boot)"
            value={technology}
            onChange={(e) => setTechnology(e.target.value)}
            className="flex-1 rounded-lg border border-white/10 bg-bg-base px-3 py-2 text-sm text-text-primary placeholder-text-muted focus:outline-none focus:ring-2 focus:ring-accent-blue"
          />
          <button
            onClick={handleGenerate}
            disabled={isGenerating || !technology.trim()}
            className="rounded-lg bg-accent-java px-4 py-2 text-sm font-semibold text-white hover:opacity-90 disabled:opacity-50"
          >
            {isGenerating ? "Starting…" : "Generate"}
          </button>
        </div>
        {generateError && <p className="mt-2 text-sm text-error">{generateError}</p>}

        {jobStatus && (
          <div className="mt-4 rounded-lg bg-bg-base p-4">
            <div className="flex items-center justify-between">
              <span className="text-sm font-medium text-text-primary">
                Job: {jobStatus.technology}
              </span>
              <span
                className={`rounded-full px-2 py-0.5 text-xs font-semibold ${
                  jobStatus.status === "DONE"
                    ? "bg-success/20 text-success"
                    : jobStatus.status === "FAILED"
                    ? "bg-error/20 text-error"
                    : "bg-accent-blue/20 text-accent-blue"
                }`}
                aria-live="polite"
              >
                {jobStatus.status}
              </span>
            </div>
            <p className="mt-1 text-xs text-text-muted">
              Attempts: {jobStatus.attempts} / 3
            </p>
            {jobStatus.log.length > 0 && (
              <ul className="mt-2 space-y-1">
                {jobStatus.log.map((entry, index) => (
                  <li key={index} className="font-mono text-xs text-text-muted">
                    {entry}
                  </li>
                ))}
              </ul>
            )}
            {jobDone && jobStatus.courseId && (
              <a
                href={`/courses/${jobStatus.courseId}`}
                className="mt-3 inline-block text-sm text-accent-blue hover:underline"
              >
                View generated course →
              </a>
            )}
          </div>
        )}
      </section>

      <section>
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-text-primary">AI Usage Log</h2>
          <span className="text-xs text-text-muted">Auto-refreshes every 10s</span>
        </div>
        {usageRows.length === 0 ? (
          <p className="text-text-muted">No AI calls logged yet.</p>
        ) : (
          <div className="overflow-x-auto rounded-xl bg-bg-card">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-white/10 text-xs text-text-muted">
                  <th className="px-4 py-3 text-left font-medium">Agent</th>
                  <th className="px-4 py-3 text-left font-medium">Model</th>
                  <th className="px-4 py-3 text-right font-medium">Tokens in</th>
                  <th className="px-4 py-3 text-right font-medium">Tokens out</th>
                  <th className="px-4 py-3 text-right font-medium">Cost</th>
                  <th className="px-4 py-3 text-right font-medium">Latency</th>
                  <th className="px-4 py-3 text-left font-medium">Outcome</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5">
                {usageRows.map((row) => (
                  <tr key={row.id} className="hover:bg-white/5">
                    <td className="px-4 py-3 text-text-primary">{row.agent}</td>
                    <td className="px-4 py-3 text-text-muted">{row.model}</td>
                    <td className="px-4 py-3 text-right text-text-muted">
                      {row.promptTokens.toLocaleString()}
                    </td>
                    <td className="px-4 py-3 text-right text-text-muted">
                      {row.completionTokens.toLocaleString()}
                    </td>
                    <td className="px-4 py-3 text-right text-text-muted">
                      {formatCost(row.costUsd)}
                    </td>
                    <td className="px-4 py-3 text-right text-text-muted">
                      {formatMs(row.latencyMs)}
                    </td>
                    <td className="px-4 py-3">
                      <span
                        className={`rounded-full px-2 py-0.5 text-xs font-semibold ${
                          row.outcome === "SUCCESS"
                            ? "bg-success/20 text-success"
                            : "bg-error/20 text-error"
                        }`}
                      >
                        {row.outcome}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}
