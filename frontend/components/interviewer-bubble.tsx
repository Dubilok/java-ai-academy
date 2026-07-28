"use client";

import { useEffect, useRef } from "react";

interface InterviewerBubbleProps {
  /** Accumulated question text (grows as SSE tokens arrive). */
  streamingText: string;
  /** Whether the interviewer is currently streaming the question. */
  isStreaming: boolean;
  /** Called when a sentence boundary is detected (triggers TTS). */
  onSentenceBoundary?: (sentence: string) => void;
}

const SENTENCE_END_RE = /[.!?]\s/;

/**
 * Chat bubble for the AI interviewer.
 * Displays streamed question tokens with a typing cursor while streaming.
 * Fires onSentenceBoundary when a complete sentence arrives so the
 * parent can feed it to the voice synthesizer incrementally.
 */
export function InterviewerBubble({
  streamingText,
  isStreaming,
  onSentenceBoundary,
}: InterviewerBubbleProps) {
  const lastSentenceBoundaryRef = useRef(0);

  useEffect(() => {
    if (!onSentenceBoundary) return;

    const textSoFar = streamingText.slice(lastSentenceBoundaryRef.current);
    const match = SENTENCE_END_RE.exec(textSoFar);
    if (match != null) {
      const boundaryIndex =
        lastSentenceBoundaryRef.current + match.index + match[0].length;
      const sentence = streamingText.slice(
        lastSentenceBoundaryRef.current,
        boundaryIndex,
      );
      lastSentenceBoundaryRef.current = boundaryIndex;
      onSentenceBoundary(sentence.trim());
    }
  }, [streamingText, onSentenceBoundary]);

  // Reset boundary tracker when text is cleared (new turn)
  useEffect(() => {
    if (!streamingText) {
      lastSentenceBoundaryRef.current = 0;
    }
  }, [streamingText]);

  return (
    <div
      className="flex items-start gap-3"
      aria-live="polite"
      aria-label="Interviewer question"
    >
      {/* Avatar */}
      <div
        className="flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-full bg-accent-blue/20 text-sm font-bold text-accent-blue"
        aria-hidden="true"
      >
        AI
      </div>

      <div className="max-w-prose rounded-2xl rounded-tl-none bg-bg-card px-4 py-3">
        {streamingText ? (
          <p className="text-sm leading-relaxed text-text-primary">
            {streamingText}
            {isStreaming && (
              <span
                className="ml-0.5 inline-block h-4 w-0.5 animate-pulse bg-accent-blue align-middle"
                aria-hidden="true"
              />
            )}
          </p>
        ) : (
          <p className="text-sm text-text-muted italic">
            {isStreaming ? "Thinking…" : "Waiting for question…"}
          </p>
        )}
      </div>
    </div>
  );
}
