import Link from "next/link";
import type { CourseListItem, CourseProgress } from "@/lib/schemas/catalog";

interface CourseCardProps {
  course: CourseListItem;
  progress?: CourseProgress;
}

const DIFFICULTY_COLORS: Record<string, string> = {
  EASY: "text-success",
  MEDIUM: "text-accent-java",
  HARD: "text-error",
};

export function CourseCard({ course, progress }: CourseCardProps) {
  const completionPercent = progress?.completionPercent ?? 0;
  const passedTasks = progress?.passedTasks ?? 0;
  const totalTasks = progress?.totalTasks ?? 0;
  const isComplete = completionPercent === 100;
  const hasStarted = passedTasks > 0;

  return (
    <Link
      href={`/courses/${course.id}`}
      className="group flex flex-col gap-4 rounded-2xl border border-white/10 bg-bg-card p-5 transition-all hover:border-accent-blue/40"
    >
      {/* Header */}
      <div className="flex items-start justify-between gap-2">
        <div className="min-w-0">
          <p className="text-xs font-semibold uppercase tracking-wide text-accent-java">
            {course.technology}
          </p>
          <h2 className="mt-1 truncate font-semibold text-text-primary transition-colors group-hover:text-accent-blue">
            {course.title}
          </h2>
        </div>
        {isComplete && (
          <span className="shrink-0 rounded-full bg-success/20 px-2 py-0.5 text-xs font-medium text-success">
            ✓ Done
          </span>
        )}
        {!isComplete && hasStarted && (
          <span className="shrink-0 rounded-full bg-accent-java/15 px-2 py-0.5 text-xs font-medium text-accent-java">
            In progress
          </span>
        )}
      </div>

      {/* Description */}
      <p className="line-clamp-2 flex-1 text-sm leading-relaxed text-text-muted">
        {course.description}
      </p>

      {/* Progress */}
      {totalTasks > 0 ? (
        <div className="flex flex-col gap-1.5">
          <div className="flex justify-between text-xs text-text-muted">
            <span>{passedTasks} / {totalTasks} tasks</span>
            <span className={isComplete ? "text-success" : ""}>{completionPercent}%</span>
          </div>
          <div className="h-1.5 w-full overflow-hidden rounded-full bg-white/10">
            <div
              className={`h-full rounded-full transition-all duration-500 ${isComplete ? "bg-success" : "bg-accent-java"}`}
              style={{ width: `${completionPercent}%` }}
            />
          </div>
        </div>
      ) : (
        <div className="flex items-center justify-between">
          <span className="text-xs text-text-muted">No tasks yet</span>
          <span className="text-xs font-medium text-accent-blue opacity-0 transition-opacity group-hover:opacity-100">
            View course →
          </span>
        </div>
      )}
    </Link>
  );
}

export { DIFFICULTY_COLORS };
