"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { Markdown } from "@/components/markdown";
import { fetchLecture } from "@/lib/queries/tasks";

export default function LecturePage({ params }: { params: { lectureId: string } }) {
  const { data: lecture, isLoading, isError } = useQuery({
    queryKey: ["lecture", params.lectureId],
    queryFn: () => fetchLecture(params.lectureId),
  });

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
      {/* Header */}
      <div className="mb-8">
        <p className="mb-2 text-xs font-semibold uppercase tracking-widest text-accent-java">
          Lecture
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
                <div className="flex-1">
                  <p className="text-sm font-medium text-text-primary">{task.title}</p>
                </div>
                <span className="text-xs font-semibold text-accent-java opacity-0 transition-opacity group-hover:opacity-100">
                  Start →
                </span>
              </Link>
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
