"use client";

import { useQuery } from "@tanstack/react-query";
import dynamic from "next/dynamic";
import { useCallback, useState } from "react";
import { fetchTask, pollSubmission, submitCode } from "@/lib/queries/tasks";

const MonacoEditor = dynamic(
  () => import("@/components/monaco-editor").then((mod) => mod.MonacoEditor),
  { ssr: false, loading: () => <div className="h-full bg-bg-base" /> }
);

type SubmissionStatus = "PENDING" | "PASSED" | "FAILED";

interface SubmissionResult {
  status: SubmissionStatus;
  logs: string | null;
  durationMs: number | null;
}

const DIFFICULTY_LABEL: Record<string, string> = {
  EASY: "Easy",
  MEDIUM: "Medium",
  HARD: "Hard",
};

export default function TaskWorkspacePage({ params }: { params: { taskId: string } }) {
  const { data: task, isLoading, isError } = useQuery({
    queryKey: ["task", params.taskId],
    queryFn: () => fetchTask(params.taskId),
  });

  const [code, setCode] = useState<string>("");
  const [isEditorReady, setIsEditorReady] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [result, setResult] = useState<SubmissionResult | null>(null);
  const [terminalOpen, setTerminalOpen] = useState(false);

  const handleCodeChange = useCallback((value: string) => {
    setCode(value);
  }, []);

  const handleSubmit = useCallback(async () => {
    if (!code.trim()) return;
    setIsSubmitting(true);
    setResult(null);
    setTerminalOpen(true);

    try {
      const { submissionId } = await submitCode(params.taskId, code);

      let attempts = 0;
      const maxAttempts = 30;
      while (attempts < maxAttempts) {
        await new Promise((resolve) => setTimeout(resolve, 1000));
        const submission = await pollSubmission(submissionId);
        if (submission.status !== "PENDING") {
          setResult({
            status: submission.status,
            logs: submission.logs,
            durationMs: submission.durationMs,
          });
          break;
        }
        attempts++;
      }

      if (attempts >= maxAttempts) {
        setResult({ status: "FAILED", logs: "Submission timed out.", durationMs: null });
      }
    } catch {
      setResult({ status: "FAILED", logs: "Submission failed. Please try again.", durationMs: null });
    } finally {
      setIsSubmitting(false);
    }
  }, [code, params.taskId]);

  if (isLoading) {
    return <div className="p-8 text-text-muted">Loading task…</div>;
  }

  if (isError || !task) {
    return <div className="p-8 text-error">Task not found.</div>;
  }

  if (!isEditorReady && task.templateCode && code === "") {
    setCode(task.templateCode);
    setIsEditorReady(true);
  }

  const terminalHeightClass = terminalOpen ? "h-48" : "h-10";

  return (
    <div className="flex h-[calc(100vh-52px)] flex-col">
      <div className="flex flex-1 overflow-hidden">
        <section
          className="w-2/5 overflow-y-auto border-r border-white/10 p-6"
          aria-label="Task description"
        >
          <div className="mb-4 flex items-center gap-2">
            <span className="text-xs font-semibold uppercase tracking-wide text-text-muted">
              {DIFFICULTY_LABEL[task.difficulty] ?? task.difficulty}
            </span>
            <span className="text-text-muted">·</span>
            <span className="text-xs text-text-muted">+{task.xpReward} XP</span>
          </div>
          <h1 className="text-xl font-bold text-text-primary">{task.title}</h1>
          <p className="mt-3 whitespace-pre-wrap text-sm leading-relaxed text-text-muted">
            {task.description}
          </p>
        </section>

        <section className="flex w-3/5 flex-col" aria-label="Code editor">
          <div className="flex items-center justify-between border-b border-white/10 px-4 py-2">
            <span className="text-xs font-medium text-text-muted">Solution.java</span>
            <button
              onClick={handleSubmit}
              disabled={isSubmitting || !code.trim()}
              className="rounded-lg bg-accent-java px-4 py-1.5 text-sm font-semibold text-white hover:opacity-90 disabled:opacity-50"
            >
              {isSubmitting ? "Running…" : "Submit"}
            </button>
          </div>
          <div className="flex-1 overflow-hidden">
            <MonacoEditor value={code} onChange={handleCodeChange} />
          </div>
        </section>
      </div>

      <div className={`flex flex-col border-t border-white/10 bg-bg-base transition-all ${terminalHeightClass}`}>
        <button
          onClick={() => setTerminalOpen((open) => !open)}
          className="flex items-center gap-2 px-4 py-2 text-xs font-medium text-text-muted hover:text-text-primary"
          aria-expanded={terminalOpen}
          aria-controls="terminal-output"
        >
          <span>{terminalOpen ? "▾" : "▸"}</span>
          <span>Terminal</span>
          {result && (
            <span
              className={`ml-2 rounded-full px-2 py-0.5 font-semibold ${
                result.status === "PASSED"
                  ? "bg-success/20 text-success"
                  : "bg-error/20 text-error"
              }`}
            >
              {result.status}
            </span>
          )}
        </button>

        {terminalOpen && (
          <div
            id="terminal-output"
            role="log"
            aria-live="polite"
            aria-label="Test output"
            className="flex-1 overflow-y-auto px-4 pb-4 font-mono text-xs"
          >
            {isSubmitting && (
              <p className="text-text-muted">Running tests…</p>
            )}
            {result && (
              <pre className={result.status === "PASSED" ? "text-success" : "text-error"}>
                {result.logs ?? `${result.status} in ${result.durationMs ?? "?"}ms`}
              </pre>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
