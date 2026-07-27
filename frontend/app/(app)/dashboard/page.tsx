"use client";

import { useQuery } from "@tanstack/react-query";
import { useMemo } from "react";
import { CourseCard } from "@/components/course-card";
import { fetchCourses, fetchProgress } from "@/lib/queries/catalog";
import { CourseProgressSchema, type CourseProgress } from "@/lib/schemas/catalog";

export default function CoursesPage() {
  const { data: coursesData, isLoading, isError } = useQuery({
    queryKey: ["courses"],
    queryFn: () => fetchCourses(),
  });

  const { data: progressData } = useQuery({
    queryKey: ["progress"],
    queryFn: fetchProgress,
  });

  const progressMap = useMemo(
    () =>
      new Map<string, CourseProgress>(
        (Array.isArray(progressData) ? progressData : [])
          .map((item) => CourseProgressSchema.safeParse(item))
          .filter((result) => result.success)
          .map((result) => [result.data.courseId, result.data] as [string, CourseProgress]),
      ),
    [progressData],
  );

  const courses = coursesData?.items ?? [];

  return (
    <div className="mx-auto max-w-5xl px-6 py-10">
      <div className="mb-8">
        <h1 className="text-3xl font-bold tracking-tight text-text-primary">Courses</h1>
        <p className="mt-1.5 text-sm text-text-muted">
          Pick a course and start learning.
        </p>
        {!isLoading && courses.length > 0 && (
          <p className="mt-1 text-xs text-text-muted">
            {courses.length} course{courses.length !== 1 ? "s" : ""} available
          </p>
        )}
      </div>

      {isLoading && (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          {[1, 2, 3, 4].map((n) => (
            <div key={n} className="h-40 animate-pulse rounded-2xl border border-white/10 bg-bg-card" />
          ))}
        </div>
      )}

      {isError && (
        <div className="rounded-2xl border border-error/20 bg-error/5 px-6 py-8 text-center">
          <p className="text-sm font-medium text-error">Failed to load courses.</p>
        </div>
      )}

      {!isLoading && !isError && courses.length === 0 && (
        <div className="rounded-2xl border border-white/10 bg-bg-card px-8 py-16 text-center">
          <p className="font-semibold text-text-primary">No courses yet</p>
          <p className="mt-1 text-sm text-text-muted">
            An admin can generate a course from the admin panel.
          </p>
        </div>
      )}

      {!isLoading && !isError && courses.length > 0 && (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          {courses.map((course) => (
            <CourseCard
              key={course.id}
              course={course}
              progress={progressMap.get(course.id)}
            />
          ))}
        </div>
      )}
    </div>
  );
}
