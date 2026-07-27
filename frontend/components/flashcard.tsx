"use client";

import { useState } from "react";
import type { InterviewQuestion } from "@/lib/schemas/interview";

const DIFFICULTY_COLOR: Record<string, { text: string; dot: string }> = {
  BEGINNER: { text: "text-success", dot: "bg-success" },
  INTERMEDIATE: { text: "text-accent-blue", dot: "bg-accent-blue" },
  ADVANCED: { text: "text-accent-java", dot: "bg-accent-java" },
  EXPERT: { text: "text-error", dot: "bg-error" },
};

function FlipIcon({ className }: { className?: string }) {
  return (
    <svg
      className={className}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8" />
      <path d="M3 3v5h5" />
    </svg>
  );
}

interface FlashcardProps {
  question: InterviewQuestion;
  heightClass?: string;
}

export function Flashcard({ question, heightClass = "h-64" }: FlashcardProps) {
  const [isFlipped, setIsFlipped] = useState(false);
  const colors = DIFFICULTY_COLOR[question.difficulty] ?? {
    text: "text-text-muted",
    dot: "bg-white/30",
  };
  const difficultyLabel =
    question.difficulty.charAt(0) + question.difficulty.slice(1).toLowerCase();

  function handleFlip() {
    setIsFlipped((prev) => !prev);
  }

  function handleKeyDown(event: React.KeyboardEvent) {
    if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      handleFlip();
    }
  }

  return (
    <div
      className={`${heightClass} w-full cursor-pointer select-none`}
      style={{ perspective: "1200px" }}
      onClick={handleFlip}
      onKeyDown={handleKeyDown}
      role="button"
      tabIndex={0}
      aria-label={
        isFlipped
          ? `Answer: ${question.shortAnswer}`
          : `Question: ${question.question}. Press Enter to reveal answer.`
      }
      aria-pressed={isFlipped}
    >
      <div
        className="relative h-full w-full"
        style={{
          transformStyle: "preserve-3d",
          transform: isFlipped ? "rotateY(180deg)" : "rotateY(0deg)",
          transition: "transform 0.45s cubic-bezier(0.4, 0, 0.2, 1)",
        }}
      >
        {/* Front */}
        <div
          className="absolute inset-0 flex flex-col rounded-xl border border-white/10 bg-bg-base p-5"
          style={{ backfaceVisibility: "hidden" }}
        >
          <div className="flex items-start justify-between gap-2">
            <span className="rounded-md bg-white/5 px-2 py-0.5 text-xs font-medium text-text-muted">
              {question.category}
            </span>
            <span className={`flex shrink-0 items-center gap-1.5 text-xs font-semibold ${colors.text}`}>
              <span className={`inline-block h-1.5 w-1.5 rounded-full ${colors.dot}`} />
              {difficultyLabel}
            </span>
          </div>

          <p className="mt-4 flex-1 text-sm leading-relaxed text-text-primary">
            {question.question}
          </p>

          <div className="mt-3 flex items-center gap-1.5 text-xs text-text-muted">
            <FlipIcon className="h-3.5 w-3.5" />
            Tap to reveal answer
          </div>
        </div>

        {/* Back */}
        <div
          className="absolute inset-0 flex flex-col rounded-xl border border-accent-blue/20 bg-bg-base p-5"
          style={{ backfaceVisibility: "hidden", transform: "rotateY(180deg)" }}
        >
          <div className="flex items-start justify-between gap-2">
            <span className="rounded-md bg-accent-blue/10 px-2 py-0.5 text-xs font-semibold text-accent-blue">
              Answer
            </span>
            <span className="shrink-0 text-xs text-text-muted">{question.technology}</span>
          </div>

          <p className="mt-4 flex-1 text-sm leading-relaxed text-text-primary">
            {question.shortAnswer}
          </p>

          {question.detailedExplanation && (
            <p
              className="mt-2 text-xs leading-relaxed text-text-muted"
              style={{
                display: "-webkit-box",
                WebkitLineClamp: 2,
                WebkitBoxOrient: "vertical",
                overflow: "hidden",
              }}
            >
              {question.detailedExplanation}
            </p>
          )}

          <div className="mt-3 flex items-center gap-1.5 text-xs text-text-muted">
            <FlipIcon className="h-3.5 w-3.5" />
            Tap to see question
          </div>
        </div>
      </div>
    </div>
  );
}
