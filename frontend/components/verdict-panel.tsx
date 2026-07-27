"use client";

import confetti from "canvas-confetti";
import { useEffect } from "react";

type SubmissionStatus = "PENDING" | "PASSED" | "FAILED";

interface VerdictPanelProps {
  status: SubmissionStatus;
  logs: string | null;
  durationMs: number | null;
  isOpen: boolean;
  isLoading: boolean;
  onToggle: () => void;
}

export function VerdictPanel({
  status,
  logs,
  durationMs,
  isOpen,
  isLoading,
  onToggle,
}: VerdictPanelProps) {
  useEffect(() => {
    if (status === "PASSED") {
      confetti({
        particleCount: 120,
        spread: 80,
        origin: { y: 0.7 },
        colors: ["#EA580C", "#10B981", "#3B82F6", "#F1F5F9"],
      });
    }
  }, [status]);

  const isPassed = !isLoading && status === "PASSED";
  const isFailed = !isLoading && status === "FAILED";

  const barBg = isPassed
    ? "bg-success/10 border-success/30"
    : isFailed
      ? "bg-error/10 border-error/30"
      : "bg-bg-card border-white/10";

  const panelHeightClass = isOpen ? "h-52" : "h-10";

  return (
    <div className={`flex flex-col border-t transition-all duration-200 ${panelHeightClass} ${barBg}`}>
      {/* Status bar / toggle */}
      <button
        onClick={onToggle}
        className="flex h-10 shrink-0 items-center gap-2.5 px-4 text-xs font-medium transition-colors hover:bg-white/5"
        aria-expanded={isOpen}
        aria-controls="terminal-output"
      >
        {/* Indicator dot */}
        {isLoading && (
          <span className="h-2 w-2 animate-pulse rounded-full bg-accent-java" />
        )}
        {isPassed && (
          <svg className="h-3.5 w-3.5 text-success" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
            <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
          </svg>
        )}
        {isFailed && (
          <svg className="h-3.5 w-3.5 text-error" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
            <path fillRule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clipRule="evenodd" />
          </svg>
        )}
        {!isLoading && status === "PENDING" && (
          <span className="h-2 w-2 rounded-full bg-white/30" />
        )}

        {/* Label */}
        <span
          className={
            isPassed
              ? "text-success"
              : isFailed
                ? "text-error"
                : "text-text-muted"
          }
          aria-live="assertive"
        >
          {isLoading
            ? "Running tests…"
            : isPassed
              ? "All tests passed"
              : isFailed
                ? "Tests failed"
                : "Terminal"}
        </span>

        {/* Duration */}
        {durationMs !== null && !isLoading && (
          <span className="text-text-muted">{durationMs} ms</span>
        )}

        {/* Toggle chevron */}
        <svg
          className={`ml-auto h-3.5 w-3.5 text-text-muted transition-transform ${isOpen ? "rotate-180" : ""}`}
          viewBox="0 0 20 20"
          fill="currentColor"
          aria-hidden="true"
        >
          <path fillRule="evenodd" d="M14.707 12.707a1 1 0 01-1.414 0L10 9.414l-3.293 3.293a1 1 0 01-1.414-1.414l4-4a1 1 0 011.414 0l4 4a1 1 0 010 1.414z" clipRule="evenodd" />
        </svg>
      </button>

      {/* Terminal content */}
      {isOpen && (
        <div
          id="terminal-output"
          role="log"
          aria-live="polite"
          aria-label="Test output"
          className="flex-1 overflow-y-auto bg-[#0d1117] px-4 pb-4 pt-2 font-mono text-xs"
        >
          {isLoading && (
            <p className="text-text-muted">
              <span className="text-accent-java">$</span> Running tests in sandbox…
            </p>
          )}
          {isPassed && (
            <pre className="whitespace-pre-wrap text-success">
              {logs ?? "✓ All tests passed!"}
            </pre>
          )}
          {isFailed && (
            <pre className="whitespace-pre-wrap text-error">
              {logs ?? "✗ Tests failed."}
            </pre>
          )}
        </div>
      )}
    </div>
  );
}
