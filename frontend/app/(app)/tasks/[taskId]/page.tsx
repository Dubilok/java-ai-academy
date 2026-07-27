"use client";

import { useQuery } from "@tanstack/react-query";
import dynamic from "next/dynamic";
import Link from "next/link";
import { useCallback, useState } from "react";
import { AiHintPanel } from "@/components/ai-hint-panel";
import { Markdown } from "@/components/markdown";
import { VerdictPanel } from "@/components/verdict-panel";
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

  return (
    <div className="flex h-[calc(100vh-52px)] flex-col">
      <div className="flex flex-1 overflow-hidden">
        <section
          className="w-2/5 overflow-y-auto border-r border-white/10 p-6"
          aria-label="Task description"
        >
          <div className="mb-3">
            <Link
              href={`/courses/${task.courseId}`}
              className="inline-flex items-center gap-1 text-xs text-text-muted hover:text-accent-blue transition-colors"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                className="h-3.5 w-3.5"
                viewBox="0 0 20 20"
                fill="currentColor"
                aria-hidden="true"
              >
                <path
                  fillRule="evenodd"
                  d="M9.707 16.707a1 1 0 01-1.414 0l-6-6a1 1 0 010-1.414l6-6a1 1 0 011.414 1.414L5.414 9H17a1 1 0 110 2H5.414l4.293 4.293a1 1 0 010 1.414z"
                  clipRule="evenodd"
                />
              </svg>
              Back to course
            </Link>
          </div>
          <div className="mb-4 flex items-center gap-2">
            <span className="text-xs font-semibold uppercase tracking-wide text-text-muted">
              {DIFFICULTY_LABEL[task.difficulty] ?? task.difficulty}
            </span>
            <span className="text-text-muted">·</span>
            <span className="text-xs text-text-muted">+{task.xpReward} XP</span>
          </div>
          <h1 className="text-xl font-bold text-text-primary">{task.title}</h1>
          <div className="mt-3">
            <Markdown content={task.description} className="prose-p:text-sm prose-li:text-sm" />
          </div>

          <AiHintPanel taskId={params.taskId} hasFailed={result?.status === "FAILED"} />
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

      <VerdictPanel
        status={result?.status ?? "PENDING"}
        logs={result?.logs ?? null}
        durationMs={result?.durationMs ?? null}
        isOpen={terminalOpen}
        isLoading={isSubmitting}
        onToggle={() => setTerminalOpen((open) => !open)}
      />
    </div>
  );
}
