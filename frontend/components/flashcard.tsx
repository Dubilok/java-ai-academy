"use client";

import { useState } from "react";
import type { InterviewQuestion } from "@/lib/schemas/interview";

const DIFFICULTY_CLASS: Record<string, string> = {
  BEGINNER: "text-success",
  INTERMEDIATE: "text-accent-java",
  ADVANCED: "text-accent-blue",
  EXPERT: "text-error",
};

interface FlashcardProps {
  question: InterviewQuestion;
}

export function Flashcard({ question }: FlashcardProps) {
  const [isFlipped, setIsFlipped] = useState(false);

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
      className="h-56 cursor-pointer"
      style={{ perspective: "1000px" }}
      onClick={handleFlip}
      onKeyDown={handleKeyDown}
      role="button"
      tabIndex={0}
      aria-label={isFlipped ? `Answer: ${question.shortAnswer}` : `Question: ${question.question}. Press Enter to reveal answer.`}
      aria-pressed={isFlipped}
    >
      <div
        className="relative h-full w-full"
        style={{
          transformStyle: "preserve-3d",
          transform: isFlipped ? "rotateY(180deg)" : "rotateY(0deg)",
          transition: "transform 0.4s ease-out",
        }}
      >
        <div
          className="absolute inset-0 flex flex-col rounded-xl bg-bg-card p-5"
          style={{ backfaceVisibility: "hidden" }}
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-text-muted">{question.category}</span>
            <span className={`text-xs font-semibold ${DIFFICULTY_CLASS[question.difficulty] ?? "text-text-muted"}`}>
              {question.difficulty}
            </span>
          </div>
          <p className="mt-3 flex-1 text-sm leading-relaxed text-text-primary">{question.question}</p>
          <p className="mt-2 text-xs text-text-muted">Click to reveal answer</p>
        </div>

        <div
          className="absolute inset-0 flex flex-col rounded-xl bg-bg-card p-5"
          style={{ backfaceVisibility: "hidden", transform: "rotateY(180deg)" }}
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-accent-blue">Answer</span>
            <span className="text-xs text-text-muted">{question.technology}</span>
          </div>
          <p className="mt-3 flex-1 text-sm leading-relaxed text-text-primary">{question.shortAnswer}</p>
          <p className="mt-2 text-xs text-text-muted">Click to see question</p>
        </div>
      </div>
    </div>
  );
}
