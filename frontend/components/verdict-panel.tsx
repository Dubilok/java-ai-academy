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

  const panelHeightClass = isOpen ? "h-48" : "h-10";

  return (
    <div
      className={`flex flex-col border-t border-white/10 bg-bg-base transition-all ${panelHeightClass}`}
    >
      <button
        onClick={onToggle}
        className="flex items-center gap-2 px-4 py-2 text-xs font-medium text-text-muted hover:text-text-primary"
        aria-expanded={isOpen}
        aria-controls="terminal-output"
      >
        <span>{isOpen ? "▾" : "▸"}</span>
        <span>Terminal</span>
        {!isLoading && status !== "PENDING" && (
          <span
            className={`ml-2 rounded-full px-2 py-0.5 font-semibold ${
              status === "PASSED" ? "bg-success/20 text-success" : "bg-error/20 text-error"
            }`}
            aria-live="assertive"
          >
            {status}
          </span>
        )}
        {isLoading && (
          <span className="ml-2 animate-pulse text-text-muted">running…</span>
        )}
        {durationMs !== null && !isLoading && (
          <span className="ml-auto text-text-muted">{durationMs}ms</span>
        )}
      </button>

      {isOpen && (
        <div
          id="terminal-output"
          role="log"
          aria-live="polite"
          aria-label="Test output"
          className="flex-1 overflow-y-auto px-4 pb-4 font-mono text-xs"
        >
          {isLoading && <p className="text-text-muted">Running tests in sandbox…</p>}
          {!isLoading && status !== "PENDING" && (
            <pre
              className={`whitespace-pre-wrap ${
                status === "PASSED" ? "text-success" : "text-error"
              }`}
            >
              {logs ?? (status === "PASSED" ? "All tests passed!" : "Tests failed.")}
            </pre>
          )}
        </div>
      )}
    </div>
  );
}
