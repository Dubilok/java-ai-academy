"use client";

import { useQuery } from "@tanstack/react-query";
import dynamic from "next/dynamic";
import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
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

const DIFFICULTY_CLASS: Record<string, string> = {
  EASY: "bg-success/15 text-success",
  MEDIUM: "bg-accent-java/15 text-accent-java",
  HARD: "bg-error/15 text-error",
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

  const handleReset = useCallback(() => {
    if (task?.templateCode) {
      setCode(task.templateCode);
    }
  }, [task]);

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

  // Cmd/Ctrl+Enter to submit
  useEffect(() => {
    function onKeyDown(event: KeyboardEvent) {
      if ((event.metaKey || event.ctrlKey) && event.key === "Enter") {
        event.preventDefault();
        void handleSubmit();
      }
    }
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [handleSubmit]);

  if (isLoading) {
    return (
      <div className="flex h-[calc(100vh-52px)] flex-col">
        <div className="flex flex-1 overflow-hidden">
          <div className="w-2/5 border-r border-white/10 p-6">
            <div className="mb-6 h-3 w-24 animate-pulse rounded bg-bg-card" />
            <div className="mb-2 flex gap-2">
              <div className="h-5 w-16 animate-pulse rounded-full bg-bg-card" />
              <div className="h-5 w-16 animate-pulse rounded-full bg-bg-card" />
            </div>
            <div className="mb-4 h-7 w-3/4 animate-pulse rounded bg-bg-card" />
            <div className="space-y-2">
              {[1, 2, 3, 4, 5].map((n) => (
                <div key={n} className="h-3 animate-pulse rounded bg-bg-card" style={{ width: `${70 + (n % 3) * 10}%` }} />
              ))}
            </div>
          </div>
          <div className="w-3/5 bg-bg-base" />
        </div>
      </div>
    );
  }

  if (isError || !task) {
    return (
      <div className="flex h-[calc(100vh-52px)] items-center justify-center">
        <div className="text-center">
          <p className="text-lg font-semibold text-error">Task not found.</p>
          <Link href="/dashboard" className="mt-3 inline-block text-sm text-accent-blue hover:underline">
            ← Back to Courses
          </Link>
        </div>
      </div>
    );
  }

  if (!isEditorReady && task.templateCode && code === "") {
    setCode(task.templateCode);
    setIsEditorReady(true);
  }

  const diffClass = DIFFICULTY_CLASS[task.difficulty] ?? "bg-white/10 text-text-muted";
  const diffLabel = DIFFICULTY_LABEL[task.difficulty] ?? task.difficulty;

  return (
    <div className="flex h-[calc(100vh-52px)] flex-col">
      <div className="flex flex-1 overflow-hidden">

        {/* ── Left panel: task description ──────────────────────────── */}
        <section
          className="flex w-2/5 flex-col overflow-hidden border-r border-white/10"
          aria-label="Task description"
        >
          {/* Task header */}
          <div className="shrink-0 border-b border-white/10 px-6 py-4">
            <Link
              href={`/courses/${task.courseId}`}
              className="inline-flex items-center gap-1 text-xs text-text-muted transition-colors hover:text-accent-blue"
            >
              <svg className="h-3 w-3" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                <path fillRule="evenodd" d="M9.707 16.707a1 1 0 01-1.414 0l-6-6a1 1 0 010-1.414l6-6a1 1 0 011.414 1.414L5.414 9H17a1 1 0 110 2H5.414l4.293 4.293a1 1 0 010 1.414z" clipRule="evenodd" />
              </svg>
              Back to course
            </Link>

            <div className="mt-3 flex items-center gap-2">
              <span className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${diffClass}`}>
                {diffLabel}
              </span>
              <span className="rounded-full bg-accent-java/15 px-2.5 py-0.5 text-xs font-semibold text-accent-java">
                +{task.xpReward} XP
              </span>
            </div>

            <h1 className="mt-2 text-xl font-bold text-text-primary">{task.title}</h1>
          </div>

          {/* Task body */}
          <div className="flex-1 overflow-y-auto px-6 py-5">
            <Markdown content={task.description} className="prose-p:text-sm prose-li:text-sm" />
            <AiHintPanel taskId={params.taskId} hasFailed={result?.status === "FAILED"} />
          </div>
        </section>

        {/* ── Right panel: code editor ───────────────────────────────── */}
        <section className="flex w-3/5 flex-col" aria-label="Code editor">
          {/* Editor toolbar */}
          <div className="flex shrink-0 items-center gap-0 border-b border-white/10 bg-bg-card">
            {/* File tab */}
            <div className="flex items-center gap-1.5 border-b-2 border-accent-java bg-bg-base px-4 py-2.5">
              <svg className="h-3.5 w-3.5 text-accent-java" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
                <path d="M9.37 5.51A7.35 7.35 0 0 0 9.1 7.5c0 4.08 3.32 7.4 7.4 7.4.68 0 1.35-.09 1.99-.27A7.014 7.014 0 0 1 12 19c-3.86 0-7-3.14-7-7 0-2.93 1.81-5.45 4.37-6.49zM12 3a9 9 0 1 0 9 9c0-.46-.04-.92-.1-1.36a5.389 5.389 0 0 1-4.4 2.26 5.403 5.403 0 0 1-3.14-9.8c-.44-.06-.9-.1-1.36-.1z"/>
              </svg>
              <span className="text-xs font-medium text-text-primary">Solution.java</span>
              <span className="ml-1 text-[10px] text-text-muted">Java</span>
            </div>
            <div className="flex-1" />
            {/* Reset */}
            <button
              onClick={handleReset}
              title="Reset to template"
              className="px-3 py-2 text-xs text-text-muted transition-colors hover:text-text-primary"
            >
              Reset
            </button>
            {/* Submit */}
            <button
              onClick={() => void handleSubmit()}
              disabled={isSubmitting || !code.trim()}
              className="mx-3 flex items-center gap-2 rounded-lg bg-accent-java px-4 py-1.5 text-sm font-semibold text-white transition-opacity hover:opacity-90 disabled:opacity-50"
            >
              {isSubmitting ? (
                <>
                  <span className="h-3.5 w-3.5 animate-spin rounded-full border-2 border-white/30 border-t-white" />
                  Running…
                </>
              ) : (
                <>
                  Run
                  <kbd className="hidden rounded bg-white/20 px-1 py-0.5 font-mono text-[10px] sm:inline">
                    ⌘↵
                  </kbd>
                </>
              )}
            </button>
          </div>

          {/* Monaco editor */}
          <div className="flex-1 overflow-hidden">
            <MonacoEditor value={code} onChange={handleCodeChange} />
          </div>
        </section>
      </div>

      {/* ── Bottom: verdict / terminal ─────────────────────────────── */}
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
