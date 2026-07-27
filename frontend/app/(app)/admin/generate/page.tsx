"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import {
  fetchAdminCourses,
  fetchAdminModules,
  fetchAiUsage,
  fetchJobStatus,
  generateCourse,
} from "@/lib/queries/admin";
import type { AiUsageResponse, JobStatus } from "@/lib/schemas/admin";
import { JobsPanel } from "@/components/jobs-panel";

type GenerateMode = "new-course" | "new-module" | "existing-module";

const MODE_LABELS: Record<GenerateMode, string> = {
  "new-course": "New course",
  "new-module": "New module in existing course",
  "existing-module": "Add to existing module",
};

function formatCost(costUsd: number | null): string {
  if (costUsd === null) return "—";
  return `$${costUsd.toFixed(4)}`;
}

function formatMs(ms: number | null): string {
  if (ms === null) return "—";
  if (ms < 1000) return `${ms}ms`;
  return `${(ms / 1000).toFixed(1)}s`;
}


export default function GeneratePage() {
  const queryClient = useQueryClient();
  const [technology, setTechnology] = useState("");
  const [mode, setMode] = useState<GenerateMode>("new-course");
  const [selectedCourseId, setSelectedCourseId] = useState("");
  const [selectedModuleId, setSelectedModuleId] = useState("");
  const [newModuleName, setNewModuleName] = useState("");
  const [jobId, setJobId] = useState<string | null>(null);
  const [isGenerating, setIsGenerating] = useState(false);
  const [generateError, setGenerateError] = useState<string | null>(null);

  const { data: usage } = useQuery<AiUsageResponse>({
    queryKey: ["ai-usage"],
    queryFn: fetchAiUsage,
    refetchInterval: 10_000,
  });

  const { data: adminCourses = [] } = useQuery({
    queryKey: ["admin-courses"],
    queryFn: fetchAdminCourses,
    enabled: mode !== "new-course",
  });

  const { data: adminModules = [] } = useQuery({
    queryKey: ["admin-modules", selectedCourseId],
    queryFn: () => fetchAdminModules(selectedCourseId),
    enabled: mode === "existing-module" && selectedCourseId !== "",
  });

  const { data: jobStatus } = useQuery<JobStatus>({
    queryKey: ["job", jobId],
    queryFn: () => fetchJobStatus(jobId!),
    enabled: jobId !== null,
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      return status === "SUCCEEDED" || status === "FAILED" ? false : 3_000;
    },
  });

  function isGenerateDisabled(): boolean {
    if (isGenerating || !technology.trim()) return true;
    if (mode === "new-module" && !selectedCourseId) return true;
    if (mode === "existing-module" && (!selectedCourseId || !selectedModuleId)) return true;
    return false;
  }

  async function handleGenerate() {
    setGenerateError(null);
    setIsGenerating(true);
    try {
      const payload: Parameters<typeof generateCourse>[0] = { technology: technology.trim() };
      if (mode === "new-module") {
        payload.courseId = selectedCourseId;
        payload.moduleName = newModuleName.trim() || technology.trim();
      } else if (mode === "existing-module") {
        payload.courseId = selectedCourseId;
        payload.moduleId = selectedModuleId;
      }
      const id = await generateCourse(payload);
      setJobId(id);
      void queryClient.invalidateQueries({ queryKey: ["ai-usage"] });
      void queryClient.invalidateQueries({ queryKey: ["admin-courses"] });
    } catch {
      setGenerateError("Failed to start generation job.");
    } finally {
      setIsGenerating(false);
    }
  }

  const jobDone = jobStatus?.status === "SUCCEEDED" || jobStatus?.status === "FAILED";

  const inputClass =
    "rounded-lg border border-white/10 bg-bg-base px-3 py-2 text-sm text-text-primary focus:outline-none focus:ring-2 focus:ring-accent-blue";

  return (
    <>
      <section className="mb-10 rounded-xl bg-bg-card p-6">
        <h2 className="mb-4 text-base font-semibold text-text-primary">Generate Content with AI</h2>

        <div className="mb-4 flex gap-2">
          {(Object.keys(MODE_LABELS) as GenerateMode[]).map((m) => (
            <button
              key={m}
              onClick={() => { setMode(m); setSelectedCourseId(""); setSelectedModuleId(""); }}
              className={`rounded-lg px-3 py-1.5 text-xs font-semibold transition-colors ${
                mode === m
                  ? "bg-accent-blue text-white"
                  : "bg-bg-base text-text-muted hover:text-text-primary"
              }`}
            >
              {MODE_LABELS[m]}
            </button>
          ))}
        </div>

        <div className="flex flex-col gap-3">
          <input
            type="text"
            placeholder="Topic / technology (e.g. Spring Boot, Java Streams)"
            value={technology}
            onChange={(e) => setTechnology(e.target.value)}
            className={inputClass + " w-full"}
          />

          {(mode === "new-module" || mode === "existing-module") && (
            <select
              value={selectedCourseId}
              onChange={(e) => { setSelectedCourseId(e.target.value); setSelectedModuleId(""); }}
              className={inputClass + " w-full"}
            >
              <option value="">— Select course —</option>
              {adminCourses.map((course) => (
                <option key={course.id} value={course.id}>
                  {course.title} ({course.technology})
                </option>
              ))}
            </select>
          )}

          {mode === "new-module" && selectedCourseId && (
            <input
              type="text"
              placeholder={`Module name (default: "${technology || "topic"}")`}
              value={newModuleName}
              onChange={(e) => setNewModuleName(e.target.value)}
              className={inputClass + " w-full"}
            />
          )}

          {mode === "existing-module" && selectedCourseId && (
            <select
              value={selectedModuleId}
              onChange={(e) => setSelectedModuleId(e.target.value)}
              className={inputClass + " w-full"}
            >
              <option value="">— Select module —</option>
              {adminModules.map((mod) => (
                <option key={mod.id} value={mod.id}>
                  {mod.orderIndex}. {mod.title}
                </option>
              ))}
            </select>
          )}

          <button
            onClick={handleGenerate}
            disabled={isGenerateDisabled()}
            className="self-start rounded-lg bg-accent-java px-4 py-2 text-sm font-semibold text-white hover:opacity-90 disabled:opacity-50"
          >
            {isGenerating ? "Starting…" : "Generate"}
          </button>
        </div>

        {generateError && <p className="mt-2 text-sm text-error">{generateError}</p>}

        {jobStatus && (
          <div className="mt-4 rounded-lg bg-bg-base p-4">
            <div className="flex items-center justify-between">
              <span className="text-sm font-medium text-text-primary">Job: {jobStatus.technology}</span>
              <span
                className={`rounded-full px-2 py-0.5 text-xs font-semibold ${
                  jobStatus.status === "SUCCEEDED"
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
            {jobStatus.currentItem && (
              <p className="mt-1 text-xs text-text-muted">Generating: {jobStatus.currentItem}</p>
            )}
            {jobStatus.totalItems > 0 && (
              <p className="mt-1 text-xs text-text-muted">
                {jobStatus.completedItems} / {jobStatus.totalItems} items
              </p>
            )}
            {jobDone && jobStatus.courseId && (
              <a href={`/courses/${jobStatus.courseId}`} className="mt-3 inline-block text-sm text-accent-blue hover:underline">
                View generated course →
              </a>
            )}
          </div>
        )}
      </section>

      <JobsPanel />

      <section>
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-base font-semibold text-text-primary">AI Usage</h2>
          <span className="text-xs text-text-muted">Auto-refreshes every 10s</span>
        </div>

        {usage && (
          <div className="mb-6 grid grid-cols-3 gap-4">
            <div className="rounded-xl bg-bg-card p-4">
              <p className="text-xs text-text-muted">Total Requests</p>
              <p className="mt-1 text-2xl font-bold text-text-primary">{usage.totalRequests}</p>
            </div>
            <div className="rounded-xl bg-bg-card p-4">
              <p className="text-xs text-text-muted">Total Tokens</p>
              <p className="mt-1 text-2xl font-bold text-text-primary">
                {(usage.totalPromptTokens + usage.totalCompletionTokens).toLocaleString()}
              </p>
            </div>
            <div className="rounded-xl bg-bg-card p-4">
              <p className="text-xs text-text-muted">Total Cost</p>
              <p className="mt-1 text-2xl font-bold text-text-primary">{formatCost(usage.totalCostUsd)}</p>
            </div>
          </div>
        )}

        {!usage || usage.byAgent.length === 0 ? (
          <p className="text-sm text-text-muted">No AI calls logged yet.</p>
        ) : (
          <div className="overflow-x-auto rounded-xl bg-bg-card">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-white/10 text-xs text-text-muted">
                  <th className="px-4 py-3 text-left font-medium">Agent</th>
                  <th className="px-4 py-3 text-right font-medium">Requests</th>
                  <th className="px-4 py-3 text-right font-medium">Tokens in</th>
                  <th className="px-4 py-3 text-right font-medium">Tokens out</th>
                  <th className="px-4 py-3 text-right font-medium">Cost</th>
                  <th className="px-4 py-3 text-right font-medium">Avg latency</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5">
                {usage.byAgent.map((row) => (
                  <tr key={row.agent} className="hover:bg-white/5">
                    <td className="px-4 py-3 font-medium text-text-primary">{row.agent}</td>
                    <td className="px-4 py-3 text-right text-text-muted">{row.requestCount}</td>
                    <td className="px-4 py-3 text-right text-text-muted">{row.promptTokens.toLocaleString()}</td>
                    <td className="px-4 py-3 text-right text-text-muted">{row.completionTokens.toLocaleString()}</td>
                    <td className="px-4 py-3 text-right text-text-muted">{formatCost(row.costUsd)}</td>
                    <td className="px-4 py-3 text-right text-text-muted">{formatMs(row.avgLatencyMs)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </>
  );
}
