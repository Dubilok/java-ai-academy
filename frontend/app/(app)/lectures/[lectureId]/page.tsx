"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { useMemo } from "react";
import { Markdown } from "@/components/markdown";
import { fetchCourse } from "@/lib/queries/catalog";
import { fetchLecture } from "@/lib/queries/tasks";

export default function LecturePage({ params }: { params: { lectureId: string } }) {
  const { data: lecture, isLoading, isError } = useQuery({
    queryKey: ["lecture", params.lectureId],
    queryFn: () => fetchLecture(params.lectureId),
  });

  const { data: course } = useQuery({
    queryKey: ["course", lecture?.courseId],
    queryFn: () => fetchCourse(lecture!.courseId),
    enabled: !!lecture?.courseId,
  });

  // Flat ordered list of all lectures across all modules
  const allLectures = useMemo(() => {
    if (!course) return [];
    return course.modules
      .slice()
      .sort((a, b) => a.orderIndex - b.orderIndex)
      .flatMap((mod) =>
        mod.lectures.slice().sort((a, b) => a.orderIndex - b.orderIndex)
      );
  }, [course]);

  const currentIndex = allLectures.findIndex((l) => l.id === params.lectureId);
  const prevLecture = currentIndex > 0 ? allLectures[currentIndex - 1] : null;
  const nextLecture = currentIndex >= 0 && currentIndex < allLectures.length - 1
    ? allLectures[currentIndex + 1]
    : null;

  if (isLoading) {
    return (
      <div className="flex h-48 items-center justify-center text-text-muted">
        Loading lecture…
      </div>
    );
  }

  if (isError || !lecture) {
    return <div className="p-8 text-error">Lecture not found.</div>;
  }

  return (
    <div className="mx-auto max-w-5xl px-8 py-10">
      {/* Breadcrumb */}
      {course && (
        <nav className="mb-6 flex items-center gap-2 text-xs text-text-muted" aria-label="Breadcrumb">
          <Link href="/dashboard" className="hover:text-text-primary">Dashboard</Link>
          <span>/</span>
          <Link href={`/courses/${course.id}`} className="hover:text-text-primary">{course.title}</Link>
          <span>/</span>
          <span className="text-text-primary">{lecture.title}</span>
        </nav>
      )}

      {/* Header */}
      <div className="mb-8">
        <p className="mb-2 text-xs font-semibold uppercase tracking-widest text-accent-java">
          Lecture {currentIndex >= 0 ? currentIndex + 1 : ""}
          {allLectures.length > 0 ? ` of ${allLectures.length}` : ""}
        </p>
        <h1 className="text-3xl font-bold leading-tight text-text-primary">{lecture.title}</h1>
      </div>

      {/* Content */}
      <div className="rounded-2xl bg-bg-card p-10">
        <Markdown content={lecture.contentMarkdown} />
      </div>

      {/* Tasks */}
      {lecture.tasks.length > 0 && (
        <section className="mt-10">
          <h2 className="mb-4 flex items-center gap-2 text-sm font-semibold uppercase tracking-widest text-text-muted">
            <span className="h-px flex-1 bg-white/10" />
            Practice tasks
            <span className="h-px flex-1 bg-white/10" />
          </h2>
          <div className="flex flex-col gap-3">
            {lecture.tasks.map((task, index) => (
              <Link
                key={task.id}
                href={`/tasks/${task.id}`}
                className="group flex items-center gap-4 rounded-xl border border-white/5 bg-bg-card px-5 py-4 transition-colors hover:border-accent-java/40 hover:bg-white/5"
              >
                <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-accent-java/10 text-sm font-bold text-accent-java">
                  {index + 1}
                </span>
                <p className="flex-1 text-sm font-medium text-text-primary">{task.title}</p>
                <span className="text-xs font-semibold text-accent-java opacity-0 transition-opacity group-hover:opacity-100">
                  Start →
                </span>
              </Link>
            ))}
          </div>
        </section>
      )}

      {/* Prev / Next navigation */}
      <nav
        className="mt-10 flex items-stretch gap-4"
        aria-label="Lecture navigation"
      >
        {prevLecture ? (
          <Link
            href={`/lectures/${prevLecture.id}`}
            className="flex flex-1 flex-col gap-1 rounded-xl border border-white/10 bg-bg-card px-5 py-4 transition-colors hover:border-accent-blue/40 hover:bg-white/5"
          >
            <span className="text-xs font-semibold uppercase tracking-wide text-text-muted">
              ← Previous
            </span>
            <span className="text-sm font-medium text-text-primary">{prevLecture.title}</span>
          </Link>
        ) : course ? (
          <Link
            href={`/courses/${course.id}`}
            className="flex flex-1 flex-col gap-1 rounded-xl border border-white/10 bg-bg-card px-5 py-4 transition-colors hover:border-accent-blue/40 hover:bg-white/5"
          >
            <span className="text-xs font-semibold uppercase tracking-wide text-text-muted">
              ← Back to course
            </span>
            <span className="text-sm font-medium text-text-primary">{course.title}</span>
          </Link>
        ) : (
          <div className="flex-1" />
        )}

        {nextLecture ? (
          <Link
            href={`/lectures/${nextLecture.id}`}
            className="flex flex-1 flex-col gap-1 rounded-xl border border-white/10 bg-bg-card px-5 py-4 text-right transition-colors hover:border-accent-blue/40 hover:bg-white/5"
          >
            <span className="text-xs font-semibold uppercase tracking-wide text-text-muted">
              Next →
            </span>
            <span className="text-sm font-medium text-text-primary">{nextLecture.title}</span>
          </Link>
        ) : course ? (
          <Link
            href={`/courses/${course.id}`}
            className="flex flex-1 flex-col gap-1 rounded-xl border border-success/30 bg-bg-card px-5 py-4 text-right transition-colors hover:bg-white/5"
          >
            <span className="text-xs font-semibold uppercase tracking-wide text-success">
              Course complete ✓
            </span>
            <span className="text-sm font-medium text-text-primary">Back to course</span>
          </Link>
        ) : (
          <div className="flex-1" />
        )}
      </nav>
    </div>
  );
}
