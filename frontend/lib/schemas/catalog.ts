import { z } from "zod";

export const TaskStubSchema = z.object({
  id: z.string().uuid(),
  title: z.string(),
  difficulty: z.enum(["EASY", "MEDIUM", "HARD"]),
  xpReward: z.number().int().nonnegative(),
});

export const LectureStubSchema = z.object({
  id: z.string().uuid(),
  title: z.string(),
  orderIndex: z.number().int(),
  tasks: z.array(TaskStubSchema),
});

export const ModuleSchema = z.object({
  id: z.string().uuid(),
  title: z.string(),
  orderIndex: z.number().int(),
  lectures: z.array(LectureStubSchema),
});

export const CourseDetailSchema = z.object({
  id: z.string().uuid(),
  title: z.string(),
  description: z.string(),
  technology: z.string(),
  modules: z.array(ModuleSchema),
});

export const CourseListItemSchema = z.object({
  id: z.string().uuid(),
  title: z.string(),
  description: z.string(),
  technology: z.string(),
});

export const PagedCoursesSchema = z.object({
  items: z.array(CourseListItemSchema),
  nextCursor: z.string().nullable(),
});

export const CourseProgressSchema = z.object({
  courseId: z.string().uuid(),
  totalTasks: z.number().int().nonnegative(),
  passedTasks: z.number().int().nonnegative(),
  completionPercent: z.number().int().min(0).max(100),
});

export const MeSchema = z.object({
  id: z.string().uuid(),
  email: z.string().email(),
  role: z.string(),
  xpPoints: z.number().int().nonnegative(),
  crystals: z.number().int().nonnegative(),
  level: z.number().int().positive(),
  streak: z.number().int().nonnegative(),
  createdAt: z.string(),
});

export type TaskStub = z.infer<typeof TaskStubSchema>;
export type LectureStub = z.infer<typeof LectureStubSchema>;
export type CourseModule = z.infer<typeof ModuleSchema>;
export type CourseDetail = z.infer<typeof CourseDetailSchema>;
export type CourseListItem = z.infer<typeof CourseListItemSchema>;
export type CourseProgress = z.infer<typeof CourseProgressSchema>;
export type Me = z.infer<typeof MeSchema>;
