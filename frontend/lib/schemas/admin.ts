import { z } from "zod";

export const AgentUsageSummarySchema = z.object({
  agent: z.string(),
  requestCount: z.number().int().nonnegative(),
  promptTokens: z.number().int().nonnegative(),
  completionTokens: z.number().int().nonnegative(),
  costUsd: z.number().nullable(),
  avgLatencyMs: z.number().int().nonnegative().nullable(),
});

export const CourseUsageSummarySchema = z.object({
  courseId: z.string().uuid(),
  title: z.string(),
  requestCount: z.number().int().nonnegative(),
  promptTokens: z.number().int().nonnegative(),
  completionTokens: z.number().int().nonnegative(),
  costUsd: z.number().nullable(),
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
  byCourse: z.array(CourseUsageSummarySchema),
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

// ── Curriculum proposal (from POST /admin/ai/propose-curriculum) ──────────────

export const ModuleProposalSchema = z.object({
  moduleName: z.string(),
  lectureTopics: z.array(z.string()),
});

export const CurriculumProposalSchema = z.object({
  technology: z.string(),
  courseName: z.string(),
  description: z.string(),
  modules: z.array(ModuleProposalSchema),
});

// ── Additional module / lecture proposals ─────────────────────────────────────

export const ProposeModulesResponseSchema = z.object({
  modules: z.array(ModuleProposalSchema),
});

export const ProposeLecturesResponseSchema = z.object({
  lectureTopics: z.array(z.string()),
});

// ── Job status (from GET /admin/ai/jobs/{id}) ─────────────────────────────────

export const JobStatusSchema = z.object({
  jobId: z.string(),
  status: z.enum(["RUNNING", "SUCCEEDED", "FAILED"]),
  technology: z.string(),
  totalItems: z.number().int().nonnegative(),
  completedItems: z.number().int().nonnegative(),
  currentItem: z.string().nullable(),
  courseId: z.string().uuid().nullable(),
  errorMessage: z.string().nullable(),
});

export type AgentUsageSummary = z.infer<typeof AgentUsageSummarySchema>;
export type CourseUsageSummary = z.infer<typeof CourseUsageSummarySchema>;
export type AiUsageResponse = z.infer<typeof AiUsageResponseSchema>;
export type AdminCourse = z.infer<typeof AdminCourseSchema>;
export type AdminModule = z.infer<typeof AdminModuleSchema>;
export type ModuleProposal = z.infer<typeof ModuleProposalSchema>;
export type CurriculumProposal = z.infer<typeof CurriculumProposalSchema>;
export type JobStatus = z.infer<typeof JobStatusSchema>;
export type ProposeModulesResponse = z.infer<typeof ProposeModulesResponseSchema>;
export type ProposeLecturesResponse = z.infer<typeof ProposeLecturesResponseSchema>;
