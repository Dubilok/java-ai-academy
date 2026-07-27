"use client";

import { useQuery } from "@tanstack/react-query";
import { CourseCard } from "@/components/course-card";
import { XpBar } from "@/components/xp-bar";
import { fetchCourses, fetchMe, fetchProgress } from "@/lib/queries/catalog";
import { CourseProgressSchema, type CourseProgress } from "@/lib/schemas/catalog";

export default function DashboardPage() {
  const { data: me } = useQuery({
    queryKey: ["me"],
    queryFn: fetchMe,
  });

  const { data: coursesData } = useQuery({
    queryKey: ["courses"],
    queryFn: () => fetchCourses(),
  });

  const { data: progressData } = useQuery({
    queryKey: ["progress"],
    queryFn: fetchProgress,
  });

  const progressMap = new Map<string, CourseProgress>(
    (Array.isArray(progressData) ? progressData : [])
      .map((item) => CourseProgressSchema.safeParse(item))
      .filter((result) => result.success)
      .map((result) => {
        const parsed = result.data;
        return [parsed.courseId, parsed] as [string, CourseProgress];
      })
  );

  return (
    <div className="mx-auto max-w-5xl px-8 py-10">
      {me && (
        <section className="mb-8 rounded-xl bg-bg-card p-6">
          <div className="flex items-center justify-between gap-4">
            <div>
              <h1 className="text-xl font-bold text-text-primary">Welcome back!</h1>
              <p className="text-sm text-text-muted">{me.email}</p>
            </div>
            <div className="flex items-center gap-4 text-sm">
              <div className="flex flex-col items-center">
                <span className="text-xl font-bold text-accent-java">{me.crystals}</span>
                <span className="text-xs text-text-muted">crystals</span>
              </div>
              <div className="flex flex-col items-center">
                <span className="text-xl font-bold text-accent-blue">{me.streak}</span>
                <span className="text-xs text-text-muted">day streak</span>
              </div>
            </div>
          </div>
          <div className="mt-4">
            <XpBar xpPoints={me.xpPoints} level={me.level} />
          </div>
        </section>
      )}

      <section>
        <h2 className="mb-4 text-lg font-semibold text-text-primary">Courses</h2>
        {!coursesData ? (
          <p className="text-text-muted">Loading courses…</p>
        ) : coursesData.items.length === 0 ? (
          <p className="text-text-muted">No courses available yet.</p>
        ) : (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            {coursesData.items.map((course) => (
              <CourseCard
                key={course.id}
                course={course}
                progress={progressMap.get(course.id)}
              />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
