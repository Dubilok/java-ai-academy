"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { useState } from "react";
import { fetchSession, submitAnswer, finishSession } from "@/lib/queries/interview";
import type { QuestionInSession, EvaluationReport, EvaluationDimension } from "@/lib/schemas/interview";

const DIFFICULTY_CLASS: Record<string, string> = {
  BEGINNER: "text-success",
  INTERMEDIATE: "text-accent-blue",
  ADVANCED: "text-accent-java",
  EXPERT: "text-error",
};

function DifficultyBadge({ difficulty }: { difficulty: string }) {
  return (
    <span className={`text-xs font-semibold uppercase ${DIFFICULTY_CLASS[difficulty] ?? "text-text-muted"}`}>
      {difficulty}
    </span>
  );
}

function ScoreRing({ score }: { score: number }) {
  const color = score >= 70 ? "#10B981" : score >= 40 ? "#3B82F6" : "#F43F5E";
  return (
    <div className="flex flex-col items-center gap-1">
      <svg viewBox="0 0 64 64" className="h-20 w-20" role="img" aria-label={`Overall score: ${score}%`}>
        <circle cx="32" cy="32" r="28" fill="none" stroke="#1E293B" strokeWidth="7" />
        <circle
          cx="32" cy="32" r="28"
          fill="none"
          stroke={color}
          strokeWidth="7"
          strokeDasharray={`${(score / 100) * 176} 176`}
          strokeLinecap="round"
          transform="rotate(-90 32 32)"
        />
      </svg>
      <span className="text-2xl font-bold text-text-primary">{score}%</span>
      <span className="text-xs text-text-muted">Overall score</span>
    </div>
  );
}

function DimensionRow({ dimension }: { dimension: EvaluationDimension }) {
  const barColor =
    dimension.score >= 70 ? "bg-success" : dimension.score >= 40 ? "bg-accent-blue" : "bg-error";
  return (
    <div className="flex flex-col gap-1">
      <div className="flex items-center justify-between text-sm">
        <span className="font-medium text-text-primary">{dimension.name}</span>
        <span className="font-bold text-text-muted">{dimension.score}%</span>
      </div>
      <div className="h-1.5 w-full overflow-hidden rounded-full bg-white/10">
        <div className={`h-full rounded-full transition-all ${barColor}`} style={{ width: `${dimension.score}%` }} />
      </div>
      <p className="text-xs text-text-muted">{dimension.feedback}</p>
    </div>
  );
}

function EvaluationReportPanel({ report, technology }: { report: EvaluationReport; technology: string }) {
  return (
    <div className="flex flex-col gap-8">
      <div className="flex flex-col items-center gap-2 rounded-2xl border border-white/10 bg-bg-card p-8">
        <ScoreRing score={report.overallScore} />
        <p className="mt-2 text-sm text-text-muted">{technology} interview complete</p>
      </div>

      <div className="rounded-2xl border border-white/10 bg-bg-card p-6">
        <h2 className="mb-5 text-sm font-semibold uppercase tracking-wide text-text-muted">
          Dimension breakdown
        </h2>
        <div className="flex flex-col gap-5">
          {report.dimensions.map((dimension) => (
            <DimensionRow key={dimension.name} dimension={dimension} />
          ))}
        </div>
      </div>

      <Link
        href="/interview/sessions"
        className="rounded-lg border border-white/10 px-5 py-3 text-center text-sm text-text-muted hover:border-accent-blue/40 hover:text-text-primary"
      >
        ← Back to sessions
      </Link>
    </div>
  );
}

function ActiveInterview({
  sessionId,
  initialQuestion,
}: {
  sessionId: string;
  initialQuestion: QuestionInSession;
}) {
  const queryClient = useQueryClient();
  const [answerText, setAnswerText] = useState("");
  const [currentQuestion, setCurrentQuestion] = useState<QuestionInSession>(initialQuestion);
  const [questionNumber, setQuestionNumber] = useState(1);
  const [finished, setFinished] = useState(false);
  const [report, setReport] = useState<EvaluationReport | null>(null);
  const [isFinishing, setIsFinishing] = useState(false);
  const [finishError, setFinishError] = useState(false);

  const { mutate: submit, isPending: isSubmitting, error: submitError } = useMutation({
    mutationFn: () => submitAnswer(sessionId, answerText),
    onSuccess: (data) => {
      setAnswerText("");
      if (data.nextQuestion) {
        setCurrentQuestion(data.nextQuestion);
        setQuestionNumber((n) => n + 1);
      }
    },
  });

  async function handleFinish() {
    setFinishError(false);
    setIsFinishing(true);
    try {
      const data = await finishSession(sessionId);
      void queryClient.invalidateQueries({ queryKey: ["interview-sessions"] });
      void queryClient.invalidateQueries({ queryKey: ["interview-session", sessionId] });
      setReport(data.report);
      setFinished(true);
    } catch {
      setFinishError(true);
    } finally {
      setIsFinishing(false);
    }
  }

  if (finished && report) {
    return <EvaluationReportPanel report={report} technology={currentQuestion.category} />;
  }

  return (
    <div className="flex flex-col gap-6">
      {/* Question card */}
      <div className="rounded-2xl border border-white/10 bg-bg-card p-7">
        <div className="mb-3 flex items-center justify-between">
          <span className="text-xs font-semibold uppercase tracking-wide text-text-muted">
            Question {questionNumber}
          </span>
          <div className="flex items-center gap-3">
            <span className="text-xs text-text-muted">{currentQuestion.category}</span>
            <DifficultyBadge difficulty={currentQuestion.difficulty} />
          </div>
        </div>
        <p className="text-base leading-relaxed text-text-primary">{currentQuestion.questionText}</p>
      </div>

      {/* Answer area */}
      <div className="flex flex-col gap-3">
        <label htmlFor="answer-input" className="text-sm font-medium text-text-muted">
          Your answer
        </label>
        <textarea
          id="answer-input"
          rows={6}
          value={answerText}
          onChange={(e) => setAnswerText(e.target.value)}
          placeholder="Type your answer here…"
          className="w-full rounded-xl border border-white/10 bg-bg-card px-4 py-3 text-sm text-text-primary placeholder-text-muted focus:outline-none focus:ring-2 focus:ring-accent-blue"
        />
        {submitError && (
          <p className="text-xs text-error">Failed to submit. Please try again.</p>
        )}
      </div>

      {/* Actions */}
      <div className="flex items-center justify-between gap-4">
        <button
          type="button"
          onClick={handleFinish}
          disabled={isFinishing || isSubmitting}
          className="rounded-lg border border-white/10 px-4 py-2 text-sm text-text-muted transition-colors hover:border-accent-blue/40 hover:text-text-primary disabled:opacity-40"
        >
          {isFinishing ? "Evaluating…" : "Finish & get score"}
        </button>
        {finishError && <p className="text-xs text-error">Failed to evaluate. Please try again.</p>}

        <button
          type="button"
          onClick={() => submit()}
          disabled={isSubmitting || !answerText.trim() || isFinishing}
          className="rounded-lg bg-accent-java px-6 py-2 text-sm font-semibold text-white transition-opacity hover:opacity-90 disabled:opacity-40"
        >
          {isSubmitting ? "Submitting…" : "Submit answer →"}
        </button>
      </div>
    </div>
  );
}

export default function SessionPage({ params }: { params: { sessionId: string } }) {
  const { data: session, isLoading, isError } = useQuery({
    queryKey: ["interview-session", params.sessionId],
    queryFn: () => fetchSession(params.sessionId),
  });

  if (isLoading) {
    return <div className="p-10 text-text-muted">Loading session…</div>;
  }

  if (isError || !session) {
    return <div className="p-10 text-error">Session not found.</div>;
  }

  return (
    <div className="mx-auto max-w-2xl px-8 py-10">
      <nav className="mb-6 flex items-center gap-2 text-xs text-text-muted" aria-label="Breadcrumb">
        <Link href="/interview" className="hover:text-text-primary">Flashcards</Link>
        <span>/</span>
        <Link href="/interview/sessions" className="hover:text-text-primary">Mock Interviews</Link>
        <span>/</span>
        <span className="text-text-primary">{session.technology}</span>
      </nav>

      <div className="mb-8 flex items-start justify-between">
        <h1 className="text-2xl font-bold text-text-primary">{session.technology} Interview</h1>
        {session.status === "FINISHED" && session.score !== null && (
          <span className="rounded-full bg-success/10 px-3 py-1 text-sm font-bold text-success">
            {session.score}%
          </span>
        )}
      </div>

      {session.status === "FINISHED" && session.report ? (
        <EvaluationReportPanel report={session.report} technology={session.technology} />
      ) : session.status === "FINISHED" ? (
        <div className="rounded-xl bg-bg-card p-6 text-text-muted">
          <p>This session is finished but no evaluation report is available.</p>
          <Link href="/interview/sessions" className="mt-4 inline-block text-accent-blue hover:underline">
            ← Back to sessions
          </Link>
        </div>
      ) : session.currentQuestion ? (
        <ActiveInterview sessionId={params.sessionId} initialQuestion={session.currentQuestion} />
      ) : (
        <div className="rounded-xl bg-bg-card p-6 text-text-muted">
          <p>No question available for this session.</p>
          <Link href="/interview/sessions" className="mt-4 inline-block text-accent-blue hover:underline">
            ← Back to sessions
          </Link>
        </div>
      )}
    </div>
  );
}
