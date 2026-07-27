"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { useMemo } from "react";
import { fetchFlashcards } from "@/lib/queries/interview";
import type { InterviewQuestion } from "@/lib/schemas/interview";

interface TopicSummary {
  technology: string;
  totalCards: number;
  categories: string[];
}

function buildTopics(cards: InterviewQuestion[]): TopicSummary[] {
  const map = new Map<string, { total: number; cats: Set<string> }>();
  for (const card of cards) {
    const entry = map.get(card.technology) ?? { total: 0, cats: new Set<string>() };
    entry.total += 1;
    entry.cats.add(card.category);
    map.set(card.technology, entry);
  }
  return Array.from(map.entries())
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([technology, { total, cats }]) => ({
      technology,
      totalCards: total,
      categories: Array.from(cats).sort(),
    }));
}

function TopicTile({ topic }: { topic: TopicSummary }) {
  const { technology, totalCards, categories } = topic;
  const visibleCats = categories.slice(0, 4);
  const extraCount = categories.length - visibleCats.length;

  return (
    <Link
      href={`/interview/topics/${encodeURIComponent(technology)}`}
      className="group flex flex-col gap-4 rounded-2xl border border-white/10 bg-bg-card p-5 transition-all hover:border-accent-java/40 hover:bg-bg-card"
    >
      {/* Header */}
      <div className="flex items-start justify-between">
        <div className="flex items-center gap-3">
          <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-accent-java/15 text-base font-bold text-accent-java transition-colors group-hover:bg-accent-java/25">
            {technology.charAt(0).toUpperCase()}
          </span>
          <div>
            <p className="text-sm font-semibold text-text-primary">{technology}</p>
            <p className="text-xs text-text-muted">
              {totalCards} card{totalCards !== 1 ? "s" : ""}
            </p>
          </div>
        </div>
        <span className="mt-1 text-text-muted transition-colors group-hover:text-text-primary" aria-hidden="true">
          →
        </span>
      </div>

      {/* Category pills */}
      {categories.length > 0 && (
        <div className="flex flex-wrap gap-1.5">
          {visibleCats.map((cat) => (
            <span
              key={cat}
              className="rounded-md bg-white/5 px-2 py-0.5 text-xs text-text-muted"
            >
              {cat}
            </span>
          ))}
          {extraCount > 0 && (
            <span className="rounded-md bg-white/5 px-2 py-0.5 text-xs text-text-muted">
              +{extraCount} more
            </span>
          )}
        </div>
      )}
    </Link>
  );
}

function SkeletonTile() {
  return (
    <div className="flex flex-col gap-4 rounded-2xl border border-white/10 bg-bg-card p-5">
      <div className="flex items-start gap-3">
        <div className="h-10 w-10 animate-pulse rounded-xl bg-white/10" />
        <div className="flex flex-col gap-1.5 pt-1">
          <div className="h-4 w-20 animate-pulse rounded bg-white/10" />
          <div className="h-3 w-12 animate-pulse rounded bg-white/10" />
        </div>
      </div>
      <div className="flex gap-1.5">
        <div className="h-5 w-16 animate-pulse rounded-md bg-white/10" />
        <div className="h-5 w-20 animate-pulse rounded-md bg-white/10" />
        <div className="h-5 w-14 animate-pulse rounded-md bg-white/10" />
      </div>
    </div>
  );
}

export default function InterviewFlashcardsPage() {
  const { data: cards = [], isLoading, isError } = useQuery({
    queryKey: ["flashcards"],
    queryFn: () => fetchFlashcards(),
  });

  const topics = useMemo(() => buildTopics(cards), [cards]);

  return (
    <div className="mx-auto max-w-5xl px-6 py-10">
      {/* Header */}
      <div className="mb-8 flex items-start justify-between gap-4">
        <div>
          <p className="text-sm text-text-muted">
            Choose a topic to start practising, or jump into a mock interview.
          </p>
          {!isLoading && topics.length > 0 && (
            <p className="mt-1 text-xs text-text-muted">
              {cards.length} card{cards.length !== 1 ? "s" : ""} across {topics.length} topic
              {topics.length !== 1 ? "s" : ""}
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

      {/* Loading */}
      {isLoading && (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {[1, 2, 3, 4, 5, 6].map((n) => (
            <SkeletonTile key={n} />
          ))}
        </div>
      )}

      {/* Error */}
      {isError && (
        <div className="rounded-2xl border border-error/20 bg-error/5 px-6 py-8 text-center">
          <p className="text-sm font-medium text-error">Failed to load flashcards.</p>
          <p className="mt-1 text-xs text-text-muted">Check your connection and try refreshing.</p>
        </div>
      )}

      {/* Empty */}
      {!isLoading && !isError && topics.length === 0 && (
        <div className="rounded-2xl border border-white/10 bg-bg-card px-8 py-16 text-center">
          <p className="text-base font-semibold text-text-primary">No flashcards yet</p>
          <p className="mt-1 text-sm text-text-muted">Ask an admin to generate some cards.</p>
        </div>
      )}

      {/* Topic grid — 3 columns on large screens */}
      {!isLoading && !isError && topics.length > 0 && (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {topics.map((topic) => (
            <TopicTile key={topic.technology} topic={topic} />
          ))}
        </div>
      )}
    </div>
  );
}
