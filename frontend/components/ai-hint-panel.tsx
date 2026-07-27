"use client";

import { useState } from "react";
import { fetchAiHint } from "@/lib/queries/tasks";

interface AiHintPanelProps {
  taskId: string;
  hasFailed: boolean;
}

type HintState =
  | { kind: "idle" }
  | { kind: "loading" }
  | { kind: "hint"; text: string }
  | { kind: "refused"; message: string }
  | { kind: "error"; message: string };

function SparkleIcon() {
  return (
    <svg className="h-4 w-4" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
      <path d="M12 2l2.4 7.4H22l-6.2 4.5 2.4 7.4L12 17l-6.2 4.3 2.4-7.4L2 9.4h7.6z" />
    </svg>
  );
}

export function AiHintPanel({ taskId, hasFailed }: AiHintPanelProps) {
  const [state, setState] = useState<HintState>({ kind: "idle" });

  async function requestHint() {
    setState({ kind: "loading" });
    try {
      const { hint } = await fetchAiHint(taskId);
      setState({ kind: "hint", text: hint });
    } catch (err: unknown) {
      const status =
        err instanceof Object && "response" in err
          ? (err as { response?: { status?: number } }).response?.status
          : undefined;

      if (status === 429) {
        setState({ kind: "refused", message: "Hint quota reached for this hour. Check back later!" });
      } else if (status === 400) {
        setState({ kind: "refused", message: "Submit at least once before asking for a hint." });
      } else {
        setState({ kind: "error", message: "Could not fetch hint. Please try again." });
      }
    }
  }

  if (!hasFailed && state.kind === "idle") return null;

  return (
    <div className="mt-6 overflow-hidden rounded-xl border border-accent-blue/20 bg-accent-blue/5">
      {/* Header */}
      <div className="flex items-center gap-2 border-b border-accent-blue/10 px-4 py-3">
        <span className="text-accent-blue">
          <SparkleIcon />
        </span>
        <span className="text-xs font-semibold uppercase tracking-widest text-accent-blue">
          AI Mentor
        </span>
        {state.kind === "idle" && (
          <button
            onClick={requestHint}
            className="ml-auto rounded-lg border border-accent-blue/40 px-3 py-1 text-xs font-medium text-accent-blue transition-colors hover:bg-accent-blue/10"
          >
            Ask for a hint
          </button>
        )}
      </div>

      {/* Body */}
      <div className="px-4 py-3">
        {state.kind === "loading" && (
          <p className="animate-pulse text-sm text-text-muted">
            Thinking of a guiding question…
          </p>
        )}

        {state.kind === "hint" && (
          <>
            <p className="text-sm leading-relaxed text-text-primary">{state.text}</p>
            <button
              onClick={requestHint}
              className="mt-3 text-xs text-text-muted underline-offset-2 hover:text-accent-blue hover:underline"
            >
              Ask another question
            </button>
          </>
        )}

        {state.kind === "refused" && (
          <p className="text-sm text-text-muted">{state.message}</p>
        )}

        {state.kind === "error" && (
          <p className="text-sm text-error">{state.message}</p>
        )}
      </div>
    </div>
  );
}
