"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { createFlashcard, deleteFlashcard, fetchAllFlashcards } from "@/lib/queries/admin";
import type { InterviewQuestion } from "@/lib/schemas/interview";

const DIFFICULTIES = ["BEGINNER", "INTERMEDIATE", "ADVANCED", "EXPERT"] as const;

const DIFFICULTY_BADGE: Record<string, string> = {
  BEGINNER: "text-success",
  INTERMEDIATE: "text-accent-blue",
  ADVANCED: "text-accent-java",
  EXPERT: "text-error",
};

const BLANK_FORM = {
  technology: "",
  category: "",
  question: "",
  shortAnswer: "",
  detailedExplanation: "",
  difficulty: "INTERMEDIATE" as string,
};

const inputClass =
  "w-full rounded-lg border border-white/10 bg-bg-base px-3 py-2 text-sm text-text-primary placeholder-text-muted focus:outline-none focus:ring-2 focus:ring-accent-blue";

function groupByTechnology(cards: InterviewQuestion[]): [string, InterviewQuestion[]][] {
  const map = new Map<string, InterviewQuestion[]>();
  for (const card of cards) {
    const group = map.get(card.technology) ?? [];
    group.push(card);
    map.set(card.technology, group);
  }
  return Array.from(map.entries()).sort(([a], [b]) => a.localeCompare(b));
}

function FlashcardRow({ card, onDelete }: { card: InterviewQuestion; onDelete: () => void }) {
  const [confirming, setConfirming] = useState(false);

  return (
    <div className="flex items-start justify-between gap-4 px-5 py-4">
      <div className="min-w-0 flex-1">
        <div className="mb-1 flex flex-wrap items-center gap-2">
          <span className="text-xs font-medium text-text-muted">{card.technology}</span>
          <span className="text-xs text-white/20">·</span>
          <span className="text-xs text-text-muted">{card.category}</span>
          <span className="text-xs text-white/20">·</span>
          <span className={`text-xs font-semibold ${DIFFICULTY_BADGE[card.difficulty] ?? "text-text-muted"}`}>
            {card.difficulty}
          </span>
        </div>
        <p className="text-sm text-text-primary">{card.question}</p>
        <p className="mt-1 text-xs text-text-muted">{card.shortAnswer}</p>
      </div>
      <div className="shrink-0">
        {confirming ? (
          <div className="flex items-center gap-2">
            <button onClick={onDelete} className="text-xs font-semibold text-error hover:underline">
              Confirm
            </button>
            <button
              onClick={() => setConfirming(false)}
              className="text-xs text-text-muted hover:text-text-primary"
            >
              Cancel
            </button>
          </div>
        ) : (
          <button
            onClick={() => setConfirming(true)}
            className="text-xs text-text-muted hover:text-error"
          >
            Delete
          </button>
        )}
      </div>
    </div>
  );
}

export default function FlashcardsAdminPage() {
  const queryClient = useQueryClient();
  const [form, setForm] = useState(BLANK_FORM);
  const [formError, setFormError] = useState<string | null>(null);

  const { data: flashcards = [], isLoading } = useQuery({
    queryKey: ["admin-flashcards"],
    queryFn: fetchAllFlashcards,
  });

  const { mutate: create, isPending: isCreating } = useMutation({
    mutationFn: createFlashcard,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["admin-flashcards"] });
      setForm(BLANK_FORM);
      setFormError(null);
    },
    onError: () => setFormError("Failed to create flashcard. Check all required fields."),
  });

  const { mutate: remove } = useMutation({
    mutationFn: deleteFlashcard,
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ["admin-flashcards"] }),
  });

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!form.technology.trim() || !form.category.trim() || !form.question.trim() || !form.shortAnswer.trim()) {
      setFormError("Technology, category, question, and short answer are required.");
      return;
    }
    setFormError(null);
    create({
      technology: form.technology.trim(),
      category: form.category.trim(),
      question: form.question.trim(),
      shortAnswer: form.shortAnswer.trim(),
      detailedExplanation: form.detailedExplanation.trim() || undefined,
      difficulty: form.difficulty,
    });
  }

  return (
    <>
      {/* Add form */}
      <section className="mb-8 rounded-xl bg-bg-card p-6">
        <h2 className="mb-4 text-base font-semibold text-text-primary">Add flashcard</h2>
        <form onSubmit={handleSubmit} className="flex flex-col gap-3">
          <div className="grid grid-cols-2 gap-3">
            <div className="flex flex-col gap-1">
              <label className="text-xs text-text-muted">Technology *</label>
              <input
                value={form.technology}
                onChange={(e) => setForm((p) => ({ ...p, technology: e.target.value }))}
                placeholder="Java, Spring, PostgreSQL…"
                className={inputClass}
              />
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-xs text-text-muted">Category *</label>
              <input
                value={form.category}
                onChange={(e) => setForm((p) => ({ ...p, category: e.target.value }))}
                placeholder="Core, Collections, Concurrency…"
                className={inputClass}
              />
            </div>
          </div>

          <div className="flex flex-col gap-1">
            <label className="text-xs text-text-muted">Question *</label>
            <textarea
              rows={2}
              value={form.question}
              onChange={(e) => setForm((p) => ({ ...p, question: e.target.value }))}
              placeholder="What is the difference between…"
              className={inputClass}
            />
          </div>

          <div className="flex flex-col gap-1">
            <label className="text-xs text-text-muted">Short answer * (shown on card flip)</label>
            <textarea
              rows={2}
              value={form.shortAnswer}
              onChange={(e) => setForm((p) => ({ ...p, shortAnswer: e.target.value }))}
              placeholder="Concise one-sentence answer…"
              className={inputClass}
            />
          </div>

          <div className="flex flex-col gap-1">
            <label className="text-xs text-text-muted">Detailed explanation (optional)</label>
            <textarea
              rows={3}
              value={form.detailedExplanation}
              onChange={(e) => setForm((p) => ({ ...p, detailedExplanation: e.target.value }))}
              placeholder="Deeper explanation used as context for AI evaluation…"
              className={inputClass}
            />
          </div>

          <div className="flex items-end gap-4">
            <div className="flex flex-col gap-1">
              <label className="text-xs text-text-muted">Difficulty</label>
              <select
                value={form.difficulty}
                onChange={(e) => setForm((p) => ({ ...p, difficulty: e.target.value }))}
                className="rounded-lg border border-white/10 bg-bg-base px-3 py-2 text-sm text-text-primary focus:outline-none focus:ring-2 focus:ring-accent-blue"
              >
                {DIFFICULTIES.map((d) => (
                  <option key={d} value={d}>
                    {d.charAt(0) + d.slice(1).toLowerCase()}
                  </option>
                ))}
              </select>
            </div>
            <button
              type="submit"
              disabled={isCreating}
              className="rounded-lg bg-accent-java px-5 py-2 text-sm font-semibold text-white hover:opacity-90 disabled:opacity-50"
            >
              {isCreating ? "Saving…" : "Add flashcard"}
            </button>
          </div>

          {formError && <p className="text-xs text-error">{formError}</p>}
        </form>
      </section>

      {/* List grouped by technology */}
      <section>
        {isLoading && <p className="text-sm text-text-muted">Loading…</p>}

        {!isLoading && flashcards.length === 0 && (
          <p className="text-sm text-text-muted">No flashcards yet — add one above.</p>
        )}

        <div className="flex flex-col gap-6">
          {groupByTechnology(flashcards).map(([technology, cards]) => (
            <div key={technology} className="overflow-hidden rounded-xl bg-bg-card">
              <div className="flex items-center justify-between border-b border-white/10 px-5 py-3">
                <span className="text-sm font-semibold text-text-primary">{technology}</span>
                <span className="text-xs text-text-muted">
                  {cards.length} card{cards.length !== 1 ? "s" : ""}
                </span>
              </div>
              <div className="divide-y divide-white/5">
                {cards.map((card) => (
                  <FlashcardRow key={card.id} card={card} onDelete={() => remove(card.id)} />
                ))}
              </div>
            </div>
          ))}
        </div>
      </section>
    </>
  );
}
