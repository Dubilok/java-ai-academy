"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useMemo, useState } from "react";
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

function CardViewer({ cards }: { cards: InterviewQuestion[] }) {
  const [index, setIndex] = useState(0);
  const [category, setCategory] = useState("");

  const categories = useMemo(() => {
    const set = new Set(cards.map((card) => card.category));
    return Array.from(set).sort();
  }, [cards]);

  const filteredCards = useMemo(
    () => (category ? cards.filter((card) => card.category === category) : cards),
    [cards, category],
  );

  const current = filteredCards[index];

  function selectCategory(cat: string) {
    setCategory(cat);
    setIndex(0);
  }

  const showCategoryTabs = categories.length > 1;

  return (
    <div className="flex flex-col gap-4">
      {/* Category tabs */}
      {showCategoryTabs && (
        <div className="flex gap-1.5 overflow-x-auto pb-1 scrollbar-none">
          <button
            onClick={() => selectCategory("")}
            className={`shrink-0 rounded-lg px-4 py-2 text-sm font-medium transition-colors ${
              category === ""
                ? "bg-white/10 text-text-primary"
                : "text-text-muted hover:bg-white/5 hover:text-text-primary"
            }`}
          >
            Mixed
          </button>
          {categories.map((cat) => (
            <button
              key={cat}
              onClick={() => selectCategory(cat)}
              className={`shrink-0 rounded-lg px-4 py-2 text-sm font-medium transition-colors ${
                category === cat
                  ? "bg-accent-java/15 text-accent-java"
                  : "text-text-muted hover:bg-white/5 hover:text-text-primary"
              }`}
            >
              {cat}
            </button>
          ))}
        </div>
      )}

      {/* Card */}
      <div className="rounded-2xl border border-white/10 bg-bg-card p-6">
        {current ? (
          <Flashcard key={current.id} question={current} heightClass="h-80" />
        ) : (
          <div className="flex h-80 items-center justify-center rounded-xl border border-white/10 bg-bg-base">
            <p className="text-sm text-text-muted">No cards match this filter.</p>
          </div>
        )}

        {/* Navigation */}
        <div className="mt-5 flex items-center justify-between">
          <button
            onClick={() => setIndex((i) => Math.max(i - 1, 0))}
            disabled={index === 0 || filteredCards.length === 0}
            className="rounded-lg border border-white/10 px-5 py-2 text-sm text-text-muted transition-colors hover:border-accent-blue hover:text-accent-blue disabled:pointer-events-none disabled:opacity-30"
            aria-label="Previous card"
          >
            ← Previous
          </button>

          {/* Dots + counter */}
          <div className="flex flex-col items-center gap-2">
            <div className="flex items-center gap-1.5" role="tablist" aria-label="Card progress">
              {filteredCards.map((_, dotIndex) => (
                <button
                  key={dotIndex}
                  role="tab"
                  aria-selected={dotIndex === index}
                  aria-label={`Card ${dotIndex + 1}`}
                  onClick={() => setIndex(dotIndex)}
                  className={`h-1.5 rounded-full transition-all duration-200 ${
                    dotIndex === index
                      ? "w-6 bg-accent-java"
                      : "w-1.5 bg-white/20 hover:bg-white/40"
                  }`}
                />
              ))}
            </div>
            <span className="text-xs text-text-muted">
              {filteredCards.length > 0 ? index + 1 : 0} / {filteredCards.length}
            </span>
          </div>

          <button
            onClick={() => setIndex((i) => Math.min(i + 1, filteredCards.length - 1))}
            disabled={index >= filteredCards.length - 1 || filteredCards.length === 0}
            className="rounded-lg border border-white/10 px-5 py-2 text-sm text-text-muted transition-colors hover:border-accent-blue hover:text-accent-blue disabled:pointer-events-none disabled:opacity-30"
            aria-label="Next card"
          >
            Next →
          </button>
        </div>
      </div>
    </div>
  );
}

export default function TopicPage() {
  const params = useParams();
  const technology = decodeURIComponent(params["technology"] as string);

  const [difficulty, setDifficulty] = useState("");

  const { data: cards = [], isLoading, isError } = useQuery({
    queryKey: ["flashcards", technology, difficulty],
    queryFn: () =>
      fetchFlashcards({ technology, difficulty: difficulty || undefined }),
  });

  return (
    <div className="mx-auto max-w-3xl px-6 py-10">
      {/* Breadcrumb */}
      <Link
        href="/interview"
        className="mb-6 inline-flex items-center gap-1.5 text-sm text-text-muted hover:text-text-primary"
      >
        ← Flashcards
      </Link>

      {/* Header */}
      <div className="mb-8 flex items-start justify-between gap-4">
        <div className="flex items-center gap-4">
          <span className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-accent-java/15 text-lg font-bold text-accent-java">
            {technology.charAt(0).toUpperCase()}
          </span>
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-text-primary">
              {technology}
            </h1>
            {!isLoading && (
              <p className="mt-0.5 text-sm text-text-muted">
                {cards.length} card{cards.length !== 1 ? "s" : ""}
                {difficulty ? ` · ${difficulty.charAt(0) + difficulty.slice(1).toLowerCase()}` : ""}
              </p>
            )}
          </div>
        </div>
        <Link
          href="/interview/sessions"
          className="shrink-0 rounded-lg bg-accent-java px-4 py-2 text-sm font-semibold text-white transition-opacity hover:opacity-90"
        >
          Mock interview →
        </Link>
      </div>

      {/* Difficulty filter */}
      <div className="mb-6 flex flex-wrap gap-2">
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

      {/* Content */}
      {isLoading && (
        <div className="rounded-2xl border border-white/10 bg-bg-card p-6">
          <div className="h-80 animate-pulse rounded-xl bg-white/5" />
          <div className="mt-5 flex items-center justify-between">
            <div className="h-9 w-28 animate-pulse rounded-lg bg-white/10" />
            <div className="flex gap-1.5">
              {[1, 2, 3].map((n) => (
                <div key={n} className="h-1.5 w-1.5 animate-pulse rounded-full bg-white/10" />
              ))}
            </div>
            <div className="h-9 w-28 animate-pulse rounded-lg bg-white/10" />
          </div>
        </div>
      )}

      {isError && (
        <div className="rounded-2xl border border-error/20 bg-error/5 px-6 py-8 text-center">
          <p className="text-sm font-medium text-error">Failed to load flashcards.</p>
        </div>
      )}

      {!isLoading && !isError && <CardViewer cards={cards} />}
    </div>
  );
}
