"use client";

import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { Flashcard } from "@/components/flashcard";
import { fetchFlashcards } from "@/lib/queries/interview";

const DIFFICULTIES = ["BEGINNER", "INTERMEDIATE", "ADVANCED", "EXPERT"] as const;

export default function InterviewFlashcardsPage() {
  const [technology, setTechnology] = useState("");
  const [difficulty, setDifficulty] = useState("");
  const [cardIndex, setCardIndex] = useState(0);

  const { data: cards = [], isLoading, isError } = useQuery({
    queryKey: ["flashcards", technology, difficulty],
    queryFn: () =>
      fetchFlashcards({
        technology: technology || undefined,
        difficulty: difficulty || undefined,
      }),
  });

  const currentCard = cards[cardIndex];

  function goNext() {
    setCardIndex((i) => Math.min(i + 1, cards.length - 1));
  }

  function goPrev() {
    setCardIndex((i) => Math.max(i - 1, 0));
  }

  return (
    <div className="mx-auto max-w-2xl p-8">
      <h1 className="mb-2 text-2xl font-bold text-text-primary">Flashcards</h1>
      <p className="mb-6 text-text-muted">Click a card to reveal the answer.</p>

      <div className="mb-6 flex flex-wrap gap-3">
        <input
          type="text"
          placeholder="Technology (e.g. Java)"
          value={technology}
          onChange={(e) => { setTechnology(e.target.value); setCardIndex(0); }}
          className="rounded-lg border border-white/10 bg-bg-card px-3 py-2 text-sm text-text-primary placeholder-text-muted focus:outline-none focus:ring-2 focus:ring-accent-blue"
        />
        <select
          value={difficulty}
          onChange={(e) => { setDifficulty(e.target.value); setCardIndex(0); }}
          className="rounded-lg border border-white/10 bg-bg-card px-3 py-2 text-sm text-text-primary focus:outline-none focus:ring-2 focus:ring-accent-blue"
          aria-label="Filter by difficulty"
        >
          <option value="">All difficulties</option>
          {DIFFICULTIES.map((d) => (
            <option key={d} value={d}>
              {d.charAt(0) + d.slice(1).toLowerCase()}
            </option>
          ))}
        </select>
      </div>

      {isLoading && <p className="text-text-muted">Loading flashcards…</p>}
      {isError && <p className="text-error">Failed to load flashcards.</p>}

      {!isLoading && !isError && cards.length === 0 && (
        <p className="text-text-muted">No flashcards found for the selected filters.</p>
      )}

      {!isLoading && currentCard && (
        <>
          <div className="mb-4 flex items-center justify-between text-sm text-text-muted">
            <span>
              {cardIndex + 1} of {cards.length}
            </span>
            <a href="/interview/sessions" className="text-accent-blue hover:underline">
              Start a mock interview →
            </a>
          </div>

          <Flashcard key={currentCard.id} question={currentCard} />

          <div className="mt-4 flex justify-between">
            <button
              onClick={goPrev}
              disabled={cardIndex === 0}
              className="rounded-lg border border-white/10 px-4 py-2 text-sm text-text-muted hover:border-accent-blue hover:text-accent-blue disabled:opacity-30"
              aria-label="Previous card"
            >
              ← Previous
            </button>
            <button
              onClick={goNext}
              disabled={cardIndex >= cards.length - 1}
              className="rounded-lg border border-white/10 px-4 py-2 text-sm text-text-muted hover:border-accent-blue hover:text-accent-blue disabled:opacity-30"
              aria-label="Next card"
            >
              Next →
            </button>
          </div>
        </>
      )}
    </div>
  );
}
