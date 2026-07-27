"use client";

import { useQuery } from "@tanstack/react-query";
import { fetchAiUsage, fetchAllJobs } from "@/lib/queries/admin";
import type { CourseUsageSummary, JobStatus } from "@/lib/schemas/admin";

function formatCost(costUsd: number | null): string {
  if (costUsd === null) return "—";
  return `$${costUsd.toFixed(4)}`;
}

function JobCard({ job, courseUsage }: { job: JobStatus; courseUsage?: CourseUsageSummary }) {
  const isRunning = job.status === "RUNNING";
  const isDone = job.status === "SUCCEEDED";
  const isFailed = job.status === "FAILED";
  const percent = job.totalItems > 0 ? Math.round((job.completedItems / job.totalItems) * 100) : 0;

  return (
    <div className="rounded-lg border border-white/10 bg-bg-base p-4">
      <div className="flex items-center justify-between gap-3">
        <div className="min-w-0 flex-1">
          <p className="truncate text-sm font-medium text-text-primary">{job.technology}</p>
          {job.currentItem && isRunning && (
            <p className="mt-0.5 truncate text-xs text-text-muted">{job.currentItem}</p>
          )}
        </div>
        <span
          className={`shrink-0 rounded-full px-2 py-0.5 text-xs font-semibold ${
            isRunning
              ? "bg-accent-blue/20 text-accent-blue"
              : isDone
                ? "bg-success/20 text-success"
                : "bg-error/20 text-error"
          }`}
        >
          {job.status}
        </span>
      </div>

      {job.totalItems > 0 && (
        <div className="mt-3">
          <div className="mb-1 flex justify-between text-xs text-text-muted">
            <span>
              {job.completedItems} / {job.totalItems} items
            </span>
            <span>{percent}%</span>
          </div>
          <div className="h-1.5 w-full overflow-hidden rounded-full bg-white/10">
            <div
              className={`h-full rounded-full transition-all duration-500 ${
                isDone ? "bg-success" : isFailed ? "bg-error" : "bg-accent-java"
              }`}
              style={{ width: `${percent}%` }}
            />
          </div>
        </div>
      )}

      {courseUsage && (
        <div className="mt-2 flex gap-4 text-xs text-text-muted">
          <span>
            Tokens in:{" "}
            <span className="text-text-primary">{courseUsage.promptTokens.toLocaleString()}</span>
          </span>
          <span>
            Tokens out:{" "}
            <span className="text-text-primary">
              {courseUsage.completionTokens.toLocaleString()}
            </span>
          </span>
          <span>
            Cost:{" "}
            <span className="text-text-primary">{formatCost(courseUsage.costUsd)}</span>
          </span>
        </div>
      )}

      {isFailed && job.errorMessage && (
        <p className="mt-2 text-xs text-error">{job.errorMessage}</p>
      )}

      {isDone && job.courseId && (
        <a
          href={`/courses/${job.courseId}`}
          className="mt-2 inline-block text-xs text-accent-blue hover:underline"
        >
          View course →
        </a>
      )}
    </div>
  );
}

export function JobsPanel() {
  const { data: jobs = [] } = useQuery<JobStatus[]>({
    queryKey: ["all-jobs"],
    queryFn: fetchAllJobs,
    refetchInterval: (query) => {
      const hasRunning = query.state.data?.some((job) => job.status === "RUNNING");
      return hasRunning ? 3_000 : 10_000;
    },
  });

  const { data: usage } = useQuery({
    queryKey: ["ai-usage"],
    queryFn: fetchAiUsage,
    refetchInterval: 10_000,
  });

  if (jobs.length === 0) return null;

  const byCourse = usage?.byCourse ?? [];

  return (
    <section className="mb-10 rounded-xl bg-bg-card p-6">
      <h2 className="mb-4 text-base font-semibold text-text-primary">Generation Jobs</h2>
      <div className="flex flex-col gap-3">
        {jobs.map((job) => {
          const courseUsage = job.courseId
            ? byCourse.find((row) => row.courseId === job.courseId)
            : undefined;
          return <JobCard key={job.jobId} job={job} courseUsage={courseUsage} />;
        })}
      </div>
    </section>
  );
}
