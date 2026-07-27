"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { useMemo } from "react";
import { fetchCourse, fetchProgress } from "@/lib/queries/catalog";
import { CourseProgressSchema } from "@/lib/schemas/catalog";
import type { LectureStub, TaskStub } from "@/lib/schemas/catalog";

// ── Constants ─────────────────────────────────────────────────────────────────

const DIFFICULTY_LABEL: Record<string, string> = {
  EASY: "Easy",
  MEDIUM: "Medium",
  HARD: "Hard",
};

const DIFFICULTY_CLASS: Record<string, string> = {
  EASY: "text-success bg-success/10",
  MEDIUM: "text-accent-java bg-accent-java/10",
  HARD: "text-error bg-error/10",
};

// ── Icons ─────────────────────────────────────────────────────────────────────

function ChevronRightIcon() {
  return (
    <svg className="h-3.5 w-3.5" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
      <path fillRule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clipRule="evenodd" />
    </svg>
  );
}

function BookIcon() {
  return (
    <svg className="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z" />
      <path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z" />
    </svg>
  );
}

function TasksIcon() {
  return (
    <svg className="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <polyline points="9 11 12 14 22 4" />
      <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11" />
    </svg>
  );
}

function StarIcon() {
  return (
    <svg className="h-4 w-4" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
      <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" />
    </svg>
  );
}

// ── Task row ──────────────────────────────────────────────────────────────────

function TaskRow({ task, number }: { task: TaskStub; number: number }) {
  const diffClass = DIFFICULTY_CLASS[task.difficulty] ?? "text-text-muted bg-white/5";
  const diffLabel = DIFFICULTY_LABEL[task.difficulty] ?? task.difficulty;
  return (
    <Link
      href={`/tasks/${task.id}`}
      className="group flex items-center gap-3 px-5 py-3 transition-colors hover:bg-white/5"
    >
      <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-white/8 font-mono text-[10px] text-text-muted">
        {number}
      </span>
      <span className="flex-1 text-sm text-text-primary">{task.title}</span>
      <div className="flex shrink-0 items-center gap-2">
        <span className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${diffClass}`}>
          {diffLabel}
        </span>
        <span className="rounded-full bg-accent-java/10 px-2 py-0.5 text-[10px] font-semibold text-accent-java">
          +{task.xpReward} XP
        </span>
      </div>
      <ChevronRightIcon />
    </Link>
  );
}

// ── Lecture card ──────────────────────────────────────────────────────────────

function LectureCard({ lecture, number }: { lecture: LectureStub; number: number }) {
  const totalXp = lecture.tasks.reduce((sum, task) => sum + task.xpReward, 0);

  return (
    <div className="overflow-hidden rounded-xl border border-white/10 bg-bg-card">
      <Link
        href={`/lectures/${lecture.id}`}
        className="flex items-center gap-3 px-5 py-4 transition-colors hover:bg-white/5"
      >
        <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-lg bg-accent-blue/10 font-mono text-xs font-bold text-accent-blue">
          {number}
        </span>
        <span className="flex-1 font-medium text-text-primary">{lecture.title}</span>
        <div className="flex shrink-0 items-center gap-3 text-xs text-text-muted">
          {lecture.tasks.length > 0 && (
            <>
              <span>{lecture.tasks.length} task{lecture.tasks.length !== 1 ? "s" : ""}</span>
              {totalXp > 0 && <span>+{totalXp} XP</span>}
            </>
          )}
          <ChevronRightIcon />
        </div>
      </Link>

      {lecture.tasks.length > 0 && (
        <div className="divide-y divide-white/5 border-t border-white/5">
          {lecture.tasks.map((task, taskIdx) => (
            <TaskRow key={task.id} task={task} number={taskIdx + 1} />
          ))}
        </div>
      )}

      {lecture.tasks.length === 0 && (
        <p className="px-5 pb-4 text-xs text-text-muted">No tasks in this lecture yet.</p>
      )}
    </div>
  );
}

// ── Skeleton ──────────────────────────────────────────────────────────────────

function CourseSkeleton() {
  return (
    <div className="flex flex-col gap-6">
      <div className="h-40 animate-pulse rounded-2xl bg-bg-card" />
      {[1, 2].map((modIdx) => (
        <div key={modIdx} className="flex flex-col gap-3">
          <div className="h-4 w-40 animate-pulse rounded bg-bg-card" />
          {[1, 2, 3].map((lectIdx) => (
            <div key={lectIdx} className="h-20 animate-pulse rounded-xl bg-bg-card" />
          ))}
        </div>
      ))}
    </div>
  );
}

// ── Page ──────────────────────────────────────────────────────────────────────

export default function CourseDetailPage({ params }: { params: { courseId: string } }) {
  const { data: course, isLoading, isError } = useQuery({
    queryKey: ["course", params.courseId],
    queryFn: () => fetchCourse(params.courseId),
  });

  const { data: progressData } = useQuery({
    queryKey: ["progress"],
    queryFn: fetchProgress,
  });

  const progress = useMemo(() => {
    if (!progressData || !Array.isArray(progressData)) return null;
    const result = progressData
      .map((item) => CourseProgressSchema.safeParse(item))
      .filter((result) => result.success)
      .find((result) => result.data.courseId === params.courseId);
    return result?.data ?? null;
  }, [progressData, params.courseId]);

  const stats = useMemo(() => {
    if (!course) return null;
    let totalLectures = 0;
    let totalTasks = 0;
    let totalXp = 0;
    for (const mod of course.modules) {
      totalLectures += mod.lectures.length;
      for (const lecture of mod.lectures) {
        totalTasks += lecture.tasks.length;
        for (const task of lecture.tasks) {
          totalXp += task.xpReward;
        }
      }
    }
    return { totalLectures, totalTasks, totalXp };
  }, [course]);

  if (isLoading) {
    return (
      <div className="mx-auto max-w-4xl px-6 py-10">
        <div className="mb-8 h-4 w-32 animate-pulse rounded bg-bg-card" />
        <CourseSkeleton />
      </div>
    );
  }

  if (isError || !course) {
    return (
      <div className="mx-auto max-w-4xl px-6 py-16 text-center">
        <p className="text-lg font-semibold text-error">Course not found.</p>
        <Link href="/dashboard" className="mt-4 inline-block text-sm text-accent-blue hover:underline">
          ← Back to Courses
        </Link>
      </div>
    );
  }

  const completionPercent = progress?.completionPercent ?? 0;
  const passedTasks = progress?.passedTasks ?? 0;

  return (
    <div className="mx-auto max-w-4xl px-6 py-10">
      {/* Breadcrumb */}
      <nav className="mb-6 flex items-center gap-2 text-xs text-text-muted" aria-label="Breadcrumb">
        <Link href="/dashboard" className="hover:text-text-primary transition-colors">
          Courses
        </Link>
        <ChevronRightIcon />
        <span className="text-text-primary">{course.title}</span>
      </nav>

      {/* Hero card */}
      <div className="mb-8 overflow-hidden rounded-2xl border border-white/10 bg-bg-card">
        <div className="h-1 bg-gradient-to-r from-accent-java via-accent-blue to-accent-java/30" />
        <div className="px-8 py-6">
          <span className="text-xs font-semibold uppercase tracking-widest text-accent-java">
            {course.technology}
          </span>
          <h1 className="mt-2 text-3xl font-bold tracking-tight text-text-primary">
            {course.title}
          </h1>
          <p className="mt-2 text-sm leading-relaxed text-text-muted">{course.description}</p>

          {/* Stats row */}
          {stats && (
            <div className="mt-5 flex flex-wrap gap-x-6 gap-y-2 text-xs text-text-muted">
              <span className="flex items-center gap-1.5">
                <span className="text-text-muted/60">⊞</span>
                {course.modules.length} module{course.modules.length !== 1 ? "s" : ""}
              </span>
              <span className="flex items-center gap-1.5">
                <BookIcon />
                {stats.totalLectures} lecture{stats.totalLectures !== 1 ? "s" : ""}
              </span>
              <span className="flex items-center gap-1.5">
                <TasksIcon />
                {stats.totalTasks} task{stats.totalTasks !== 1 ? "s" : ""}
              </span>
              {stats.totalXp > 0 && (
                <span className="flex items-center gap-1.5 text-accent-java">
                  <StarIcon />
                  {stats.totalXp.toLocaleString()} XP available
                </span>
              )}
            </div>
          )}

          {/* Progress bar */}
          {progress && stats && stats.totalTasks > 0 && (
            <div className="mt-5 flex flex-col gap-1.5">
              <div className="flex justify-between text-xs">
                <span className="text-text-muted">
                  {passedTasks} / {stats.totalTasks} tasks completed
                </span>
                <span className={completionPercent === 100 ? "font-semibold text-success" : "text-text-muted"}>
                  {completionPercent === 100 ? "✓ Complete" : `${completionPercent}%`}
                </span>
              </div>
              <div className="h-1.5 w-full overflow-hidden rounded-full bg-white/10">
                <div
                  className={`h-full rounded-full transition-all duration-700 ${completionPercent === 100 ? "bg-success" : "bg-accent-java"}`}
                  style={{ width: `${completionPercent}%` }}
                />
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Module + lecture tree */}
      <div className="flex flex-col gap-8">
        {course.modules.map((mod, modIdx) => (
          <section key={mod.id} aria-labelledby={`module-${mod.id}`}>
            {/* Module header */}
            <div className="mb-3 flex items-center gap-3">
              <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-accent-java/15 text-xs font-bold text-accent-java">
                {modIdx + 1}
              </span>
              <h2
                id={`module-${mod.id}`}
                className="font-semibold text-text-primary"
              >
                {mod.title}
              </h2>
              <span className="text-xs text-text-muted">
                {mod.lectures.length} lecture{mod.lectures.length !== 1 ? "s" : ""}
              </span>
            </div>

            {/* Lectures */}
            <div className="ml-10 flex flex-col gap-2">
              {mod.lectures.length > 0 ? (
                mod.lectures.map((lecture, lectIdx) => (
                  <LectureCard key={lecture.id} lecture={lecture} number={lectIdx + 1} />
                ))
              ) : (
                <p className="text-sm text-text-muted">No lectures in this module yet.</p>
              )}
            </div>
          </section>
        ))}

        {course.modules.length === 0 && (
          <div className="rounded-2xl border border-white/10 bg-bg-card px-8 py-12 text-center">
            <p className="font-semibold text-text-primary">No content yet</p>
            <p className="mt-1 text-sm text-text-muted">Modules will appear here once generated.</p>
          </div>
        )}
      </div>
    </div>
  );
}
