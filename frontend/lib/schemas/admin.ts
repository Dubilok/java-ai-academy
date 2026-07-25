import { z } from "zod";

export const AiUsageRowSchema = z.object({
  id: z.string().uuid(),
  agent: z.string(),
  model: z.string(),
  promptTokens: z.number().int().nonnegative(),
  completionTokens: z.number().int().nonnegative(),
  costUsd: z.number().nonnegative().nullable(),
  latencyMs: z.number().int().nonnegative().nullable(),
  outcome: z.string(),
  createdAt: z.string(),
});

export const AiUsageResponseSchema = z.object({
  items: z.array(AiUsageRowSchema),
  nextCursor: z.string().nullable(),
});

export const GenerateCourseResponseSchema = z.object({
  jobId: z.string(),
});

export const JobStatusSchema = z.object({
  jobId: z.string(),
  status: z.enum(["PENDING", "IN_PROGRESS", "DONE", "FAILED"]),
  technology: z.string(),
  attempts: z.number().int().nonnegative(),
  log: z.array(z.string()),
  courseId: z.string().uuid().nullable(),
});

export type AiUsageRow = z.infer<typeof AiUsageRowSchema>;
export type JobStatus = z.infer<typeof JobStatusSchema>;
