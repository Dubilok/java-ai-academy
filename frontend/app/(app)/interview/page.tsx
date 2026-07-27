"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { useState } from "react";
import { Flashcard } from "@/components/flashcard";
import { fetchFlashcards } from "@/lib/queries/interview";
import type { InterviewQuestion } from "@/lib/schemas/interview";

const DIFFICULTIES = ["BEGINNER", "INTERMEDIATE", "ADVANCED", "EXPERT"] as const;

function groupByTechnology(cards: InterviewQuestion[]): [string, InterviewQuestion[]][] {
  const map = new Map<string, InterviewQuestion[]>();
  for (const card of cards) {
    const group = map.get(card.technology) ?? [];
    group.push(card);
    map.set(card.technology, group);
  }
  return Array.from(map.entries()).sort(([a], [b]) => a.localeCompare(b));
}

function TopicGroup({ technology, cards }: { technology: string; cards: InterviewQuestion[] }) {
  const [index, setIndex] = useState(0);
  const current = cards[index];

  return (
    <section className="rounded-2xl border border-white/10 bg-bg-card p-6">
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-base font-semibold text-text-primary">{technology}</h2>
        <span className="text-xs text-text-muted">
          {index + 1} / {cards.length}
        </span>
      </div>

      {current && <Flashcard key={current.id} question={current} />}

      <div className="mt-4 flex justify-between">
        <button
          onClick={() => setIndex((i) => Math.max(i - 1, 0))}
          disabled={index === 0}
          className="rounded-lg border border-white/10 px-4 py-2 text-sm text-text-muted hover:border-accent-blue hover:text-accent-blue disabled:opacity-30"
          aria-label="Previous card"
        >
          ← Prev
        </button>
        <button
          onClick={() => setIndex((i) => Math.min(i + 1, cards.length - 1))}
          disabled={index >= cards.length - 1}
          className="rounded-lg border border-white/10 px-4 py-2 text-sm text-text-muted hover:border-accent-blue hover:text-accent-blue disabled:opacity-30"
          aria-label="Next card"
        >
          Next →
        </button>
      </div>
    </section>
  );
}

export default function InterviewFlashcardsPage() {
  const [difficulty, setDifficulty] = useState("");

  const { data: cards = [], isLoading, isError } = useQuery({
    queryKey: ["flashcards", difficulty],
    queryFn: () => fetchFlashcards({ difficulty: difficulty || undefined }),
  });

  const groups = groupByTechnology(cards);

  return (
    <div className="mx-auto max-w-3xl px-8 py-10">
      <div className="mb-6 flex items-start justify-between">
        <div>
          <h1 className="text-2xl font-bold text-text-primary">Flashcards</h1>
          <p className="mt-1 text-sm text-text-muted">Click a card to reveal the answer.</p>
        </div>
        <Link
          href="/interview/sessions"
          className="rounded-lg border border-white/10 px-4 py-2 text-sm text-text-muted hover:border-accent-blue hover:text-text-primary"
        >
          Mock interview →
        </Link>
      </div>

      {/* Difficulty filter */}
      <div className="mb-8 flex flex-wrap gap-2">
        <button
          onClick={() => setDifficulty("")}
          className={`rounded-full px-3 py-1 text-xs font-semibold transition-colors ${
            difficulty === ""
              ? "bg-white/10 text-text-primary"
              : "text-text-muted hover:text-text-primary"
          }`}
        >
          All
        </button>
        {DIFFICULTIES.map((d) => (
          <button
            key={d}
            onClick={() => setDifficulty(difficulty === d ? "" : d)}
            className={`rounded-full px-3 py-1 text-xs font-semibold transition-colors ${
              difficulty === d
                ? "bg-white/10 text-text-primary"
                : "text-text-muted hover:text-text-primary"
            }`}
          >
            {d.charAt(0) + d.slice(1).toLowerCase()}
          </button>
        ))}
      </div>

      {isLoading && <p className="text-text-muted">Loading flashcards…</p>}
      {isError && <p className="text-error">Failed to load flashcards.</p>}

      {!isLoading && !isError && cards.length === 0 && (
        <p className="text-text-muted">No flashcards found.</p>
      )}

      <div className="flex flex-col gap-6">
        {groups.map(([technology, groupCards]) => (
          <TopicGroup key={technology} technology={technology} cards={groupCards} />
        ))}
      </div>
    </div>
  );
}
