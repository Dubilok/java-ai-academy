import { z } from "zod";

export const AgentUsageSummarySchema = z.object({
  agent: z.string(),
  requestCount: z.number().int().nonnegative(),
  promptTokens: z.number().int().nonnegative(),
  completionTokens: z.number().int().nonnegative(),
  costUsd: z.number().nullable(),
  avgLatencyMs: z.number().int().nonnegative().nullable(),
});

export const AiUsageResponseSchema = z.object({
  from: z.string().nullable(),
  to: z.string().nullable(),
  totalRequests: z.number().int().nonnegative(),
  totalPromptTokens: z.number().int().nonnegative(),
  totalCompletionTokens: z.number().int().nonnegative(),
  totalCostUsd: z.number().nullable(),
  byAgent: z.array(AgentUsageSummarySchema),
  byUser: z.array(z.unknown()),
  byCourse: z.array(z.unknown()),
});

export const GenerateCourseResponseSchema = z.object({
  jobId: z.string(),
});

export const AdminCourseSchema = z.object({
  id: z.string().uuid(),
  title: z.string(),
  technology: z.string(),
});

export const AdminModuleSchema = z.object({
  id: z.string().uuid(),
  title: z.string(),
  orderIndex: z.number().int(),
});

export type AdminCourse = z.infer<typeof AdminCourseSchema>;
export type AdminModule = z.infer<typeof AdminModuleSchema>;

export const JobStatusSchema = z.object({
  jobId: z.string(),
  status: z.enum(["PENDING", "IN_PROGRESS", "DONE", "FAILED"]),
  technology: z.string(),
  attempts: z.number().int().nonnegative(),
  log: z.array(z.string()),
  courseId: z.string().uuid().nullable(),
});

export type AgentUsageSummary = z.infer<typeof AgentUsageSummarySchema>;
export type AiUsageResponse = z.infer<typeof AiUsageResponseSchema>;
export type JobStatus = z.infer<typeof JobStatusSchema>;
