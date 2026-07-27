"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { fetchCourse } from "@/lib/queries/catalog";
import type { LectureStub, TaskStub } from "@/lib/schemas/catalog";

const DIFFICULTY_LABEL: Record<string, string> = {
  EASY: "Easy",
  MEDIUM: "Medium",
  HARD: "Hard",
};

const DIFFICULTY_CLASS: Record<string, string> = {
  EASY: "text-success",
  MEDIUM: "text-accent-java",
  HARD: "text-error",
};

function TaskRow({ task }: { task: TaskStub }) {
  return (
    <Link
      href={`/tasks/${task.id}`}
      className="flex items-center justify-between rounded-lg px-4 py-3 hover:bg-white/5"
    >
      <div className="flex items-center gap-3">
        <span className="flex h-7 w-7 items-center justify-center rounded-full bg-white/10 text-xs font-medium text-text-muted">
          ✦
        </span>
        <span className="text-sm text-text-primary">{task.title}</span>
      </div>
      <div className="flex items-center gap-3 text-xs">
        <span className={DIFFICULTY_CLASS[task.difficulty] ?? "text-text-muted"}>
          {DIFFICULTY_LABEL[task.difficulty] ?? task.difficulty}
        </span>
        <span className="text-text-muted">+{task.xpReward} XP</span>
      </div>
    </Link>
  );
}

function LectureSection({ lecture }: { lecture: LectureStub }) {
  return (
    <div className="rounded-xl bg-bg-card p-5">
      <Link
        href={`/lectures/${lecture.id}`}
        className="block text-base font-semibold text-text-primary hover:text-accent-blue"
      >
        {lecture.title}
      </Link>
      {lecture.tasks.length > 0 ? (
        <div className="mt-3 divide-y divide-white/5">
          {lecture.tasks.map((task) => (
            <TaskRow key={task.id} task={task} />
          ))}
        </div>
      ) : (
        <p className="mt-2 text-sm text-text-muted">No tasks yet.</p>
      )}
    </div>
  );
}

export default function CourseDetailPage({ params }: { params: { courseId: string } }) {
  const { data: course, isLoading, isError } = useQuery({
    queryKey: ["course", params.courseId],
    queryFn: () => fetchCourse(params.courseId),
  });

  if (isLoading) {
    return <div className="p-8 text-text-muted">Loading course…</div>;
  }

  if (isError || !course) {
    return <div className="p-8 text-error">Course not found.</div>;
  }

  return (
    <div className="mx-auto max-w-5xl px-8 py-10">
      <nav className="mb-6 flex items-center gap-2 text-xs text-text-muted" aria-label="Breadcrumb">
        <Link href="/dashboard" className="hover:text-text-primary">Dashboard</Link>
        <span>/</span>
        <span className="text-text-primary">{course.title}</span>
      </nav>

      <div className="mb-8">
        <p className="text-xs font-semibold uppercase tracking-wide text-text-muted">
          {course.technology}
        </p>
        <h1 className="mt-1 text-3xl font-bold text-text-primary">{course.title}</h1>
        <p className="mt-2 text-text-muted">{course.description}</p>
      </div>

      <div className="flex flex-col gap-4">
        {course.modules.map((mod) => (
          <section key={mod.id}>
            <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-text-muted">
              Module {mod.orderIndex + 1} — {mod.title}
            </h2>
            <div className="flex flex-col gap-3">
              {mod.lectures.map((lecture) => (
                <LectureSection key={lecture.id} lecture={lecture} />
              ))}
            </div>
          </section>
        ))}
      </div>
    </div>
  );
}
