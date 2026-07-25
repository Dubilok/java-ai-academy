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
        setState({ kind: "refused", message: "You've used your hint quota for this hour. Come back later!" });
      } else if (status === 400) {
        setState({ kind: "refused", message: "Submit at least once before asking for a hint." });
      } else {
        setState({ kind: "error", message: "Could not fetch hint. Please try again." });
      }
    }
  }

  if (!hasFailed && state.kind === "idle") return null;

  return (
    <div className="mt-4 rounded-xl border border-accent-blue/30 bg-bg-card p-4">
      <div className="flex items-center justify-between">
        <span className="text-sm font-semibold text-accent-blue">AI Mentor</span>
        {state.kind === "idle" && (
          <button
            onClick={requestHint}
            className="rounded-lg border border-accent-blue px-3 py-1 text-xs font-medium text-accent-blue hover:bg-accent-blue/10"
          >
            Ask for a hint
          </button>
        )}
      </div>

      {state.kind === "loading" && (
        <p className="mt-3 animate-pulse text-sm text-text-muted">
          Thinking of a guiding question…
        </p>
      )}

      {state.kind === "hint" && (
        <>
          <p className="mt-3 text-sm leading-relaxed text-text-primary">{state.text}</p>
          <button
            onClick={requestHint}
            className="mt-3 text-xs text-text-muted hover:text-accent-blue"
          >
            Ask another
          </button>
        </>
      )}

      {state.kind === "refused" && (
        <p className="mt-3 text-sm text-text-muted">{state.message}</p>
      )}

      {state.kind === "error" && (
        <p className="mt-3 text-sm text-error">{state.message}</p>
      )}
    </div>
  );
}
