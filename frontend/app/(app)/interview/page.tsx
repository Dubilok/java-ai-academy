"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { useState } from "react";
import { Flashcard } from "@/components/flashcard";
import { fetchFlashcards } from "@/lib/queries/interview";
import type { InterviewQuestion } from "@/lib/schemas/interview";

const FILTER_OPTIONS = [
  { value: "", label: "All levels" },
  { value: "BEGINNER", label: "Beginner" },
  { value: "INTERMEDIATE", label: "Intermediate" },
  { value: "ADVANCED", label: "Advanced" },
  { value: "EXPERT", label: "Expert" },
] as const;

const DIFFICULTY_ACTIVE: Record<string, string> = {
  "": "bg-white/15 text-text-primary",
  BEGINNER: "bg-success text-bg-base",
  INTERMEDIATE: "bg-accent-blue text-white",
  ADVANCED: "bg-accent-java text-white",
  EXPERT: "bg-error text-white",
};

const DIFFICULTY_INACTIVE: Record<string, string> = {
  "": "text-text-muted hover:text-text-primary hover:bg-white/5",
  BEGINNER: "text-success hover:bg-success/10",
  INTERMEDIATE: "text-accent-blue hover:bg-accent-blue/10",
  ADVANCED: "text-accent-java hover:bg-accent-java/10",
  EXPERT: "text-error hover:bg-error/10",
};

function groupByTechnology(cards: InterviewQuestion[]): [string, InterviewQuestion[]][] {
  const map = new Map<string, InterviewQuestion[]>();
  for (const card of cards) {
    const group = map.get(card.technology) ?? [];
    group.push(card);
    map.set(card.technology, group);
  }
  return Array.from(map.entries()).sort(([a], [b]) => a.localeCompare(b));
}

function TopicGroup({
  technology,
  cards,
}: {
  technology: string;
  cards: InterviewQuestion[];
}) {
  const [index, setIndex] = useState(0);
  const current = cards[index];

  return (
    <section className="overflow-hidden rounded-2xl border border-white/10 bg-bg-card">
      {/* Header */}
      <div className="flex items-center justify-between border-b border-white/10 px-5 py-3.5">
        <div className="flex items-center gap-3">
          <span
            className="flex h-7 w-7 shrink-0 items-center justify-center rounded-lg bg-accent-java/15 text-xs font-bold text-accent-java"
            aria-hidden="true"
          >
            {technology.charAt(0).toUpperCase()}
          </span>
          <h2 className="text-sm font-semibold text-text-primary">{technology}</h2>
        </div>
        <span className="text-xs text-text-muted">
          {index + 1} / {cards.length}
        </span>
      </div>

      {/* Card area */}
      <div className="p-5 pb-0">
        {current && <Flashcard key={current.id} question={current} />}
      </div>

      {/* Navigation */}
      <div className="flex items-center justify-between px-5 py-4">
        <button
          onClick={() => setIndex((i) => Math.max(i - 1, 0))}
          disabled={index === 0}
          className="rounded-lg border border-white/10 px-4 py-1.5 text-xs text-text-muted transition-colors hover:border-accent-blue hover:text-accent-blue disabled:pointer-events-none disabled:opacity-30"
          aria-label="Previous card"
        >
          ← Prev
        </button>

        {/* Dot progress */}
        <div className="flex items-center gap-1.5" role="tablist" aria-label="Card progress">
          {cards.map((_, dotIndex) => (
            <button
              key={dotIndex}
              role="tab"
              aria-selected={dotIndex === index}
              aria-label={`Card ${dotIndex + 1}`}
              onClick={() => setIndex(dotIndex)}
              className={`h-1.5 rounded-full transition-all duration-200 ${
                dotIndex === index
                  ? "w-5 bg-accent-java"
                  : "w-1.5 bg-white/20 hover:bg-white/40"
              }`}
            />
          ))}
        </div>

        <button
          onClick={() => setIndex((i) => Math.min(i + 1, cards.length - 1))}
          disabled={index >= cards.length - 1}
          className="rounded-lg border border-white/10 px-4 py-1.5 text-xs text-text-muted transition-colors hover:border-accent-blue hover:text-accent-blue disabled:pointer-events-none disabled:opacity-30"
          aria-label="Next card"
        >
          Next →
        </button>
      </div>
    </section>
  );
}

function SkeletonGroup() {
  return (
    <div className="overflow-hidden rounded-2xl border border-white/10 bg-bg-card">
      <div className="flex items-center gap-3 border-b border-white/10 px-5 py-3.5">
        <div className="h-7 w-7 animate-pulse rounded-lg bg-white/10" />
        <div className="h-4 w-24 animate-pulse rounded bg-white/10" />
      </div>
      <div className="p-5 pb-0">
        <div className="h-64 animate-pulse rounded-xl bg-white/5" />
      </div>
      <div className="flex items-center justify-between px-5 py-4">
        <div className="h-7 w-16 animate-pulse rounded-lg bg-white/10" />
        <div className="flex gap-1.5">
          {[1, 2, 3].map((n) => (
            <div key={n} className="h-1.5 w-1.5 animate-pulse rounded-full bg-white/10" />
          ))}
        </div>
        <div className="h-7 w-16 animate-pulse rounded-lg bg-white/10" />
      </div>
    </div>
  );
}

export default function InterviewFlashcardsPage() {
  const [difficulty, setDifficulty] = useState("");

  const { data: cards = [], isLoading, isError } = useQuery({
    queryKey: ["flashcards", difficulty],
    queryFn: () => fetchFlashcards({ difficulty: difficulty || undefined }),
  });

  const groups = groupByTechnology(cards);
  const totalTopics = groups.length;

  return (
    <div className="mx-auto max-w-5xl px-6 py-10">
      {/* Page header */}
      <div className="mb-8 flex items-start justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold tracking-tight text-text-primary">Flashcards</h1>
          <p className="mt-1.5 text-sm text-text-muted">
            Tap a card to reveal the answer. Practice until every flip is instant.
          </p>
          {!isLoading && cards.length > 0 && (
            <p className="mt-1 text-xs text-text-muted">
              {cards.length} card{cards.length !== 1 ? "s" : ""} across {totalTopics} topic
              {totalTopics !== 1 ? "s" : ""}
            </p>
          )}
        </div>
        <Link
          href="/interview/sessions"
          className="shrink-0 rounded-lg bg-accent-java px-4 py-2 text-sm font-semibold text-white transition-opacity hover:opacity-90"
        >
          Mock interview →
        </Link>
      </div>

      {/* Difficulty filter */}
      <div className="mb-8 flex flex-wrap gap-2">
        {FILTER_OPTIONS.map(({ value, label }) => {
          const isActive = difficulty === value;
          return (
            <button
              key={value}
              onClick={() => setDifficulty(value)}
              className={`rounded-full px-3.5 py-1.5 text-xs font-semibold transition-all ${
                isActive
                  ? (DIFFICULTY_ACTIVE[value] ?? "bg-white/15 text-text-primary")
                  : (DIFFICULTY_INACTIVE[value] ?? "text-text-muted hover:text-text-primary")
              }`}
            >
              {label}
            </button>
          );
        })}
      </div>

      {/* Loading skeleton */}
      {isLoading && (
        <div className="grid grid-cols-1 gap-5 md:grid-cols-2">
          {[1, 2, 3, 4].map((n) => (
            <SkeletonGroup key={n} />
          ))}
        </div>
      )}

      {/* Error state */}
      {isError && (
        <div className="rounded-2xl border border-error/20 bg-error/5 px-6 py-8 text-center">
          <p className="text-sm font-medium text-error">Failed to load flashcards.</p>
          <p className="mt-1 text-xs text-text-muted">Check your connection and try refreshing.</p>
        </div>
      )}

      {/* Empty state */}
      {!isLoading && !isError && cards.length === 0 && (
        <div className="rounded-2xl border border-white/10 bg-bg-card px-8 py-16 text-center">
          <p className="text-base font-semibold text-text-primary">No flashcards found</p>
          <p className="mt-1 text-sm text-text-muted">
            {difficulty ? "Try a different difficulty level, or " : ""}
            ask an admin to generate some cards.
          </p>
          {difficulty && (
            <button
              onClick={() => setDifficulty("")}
              className="mt-4 rounded-lg border border-white/10 px-4 py-2 text-sm text-text-muted hover:text-text-primary"
            >
              Clear filter
            </button>
          )}
        </div>
      )}

      {/* Topic groups grid */}
      {!isLoading && !isError && groups.length > 0 && (
        <div className="grid grid-cols-1 gap-5 md:grid-cols-2">
          {groups.map(([technology, groupCards]) => (
            <TopicGroup key={technology} technology={technology} cards={groupCards} />
          ))}
        </div>
      )}
    </div>
  );
}
