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

  return (
    <a
      href={`/courses/${course.id}`}
      className="group flex flex-col gap-3 rounded-xl bg-bg-card p-5 hover:ring-1 hover:ring-accent-blue"
    >
      <div className="flex items-start justify-between gap-2">
        <div>
          <p className="text-xs font-semibold uppercase tracking-wide text-text-muted">
            {course.technology}
          </p>
          <h2 className="mt-0.5 font-semibold text-text-primary group-hover:text-accent-blue">
            {course.title}
          </h2>
        </div>
        {isComplete && (
          <span className="shrink-0 rounded-full bg-success/20 px-2 py-0.5 text-xs font-medium text-success">
            Complete
          </span>
        )}
      </div>

      <p className="line-clamp-2 text-sm text-text-muted">{course.description}</p>

      {totalTasks > 0 && (
        <div className="flex flex-col gap-1">
          <div className="flex justify-between text-xs text-text-muted">
            <span>
              {passedTasks}/{totalTasks} tasks
            </span>
            <span>{completionPercent}%</span>
          </div>
          <div className="h-1.5 w-full overflow-hidden rounded-full bg-white/10">
            <div
              className={`h-full rounded-full ${isComplete ? "bg-success" : "bg-accent-java"}`}
              style={{ width: `${completionPercent}%` }}
            />
          </div>
        </div>
      )}
    </a>
  );
}

export { DIFFICULTY_COLORS };
