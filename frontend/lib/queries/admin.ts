import { api } from "@/lib/api";
import {
  AiUsageResponseSchema,
  GenerateCourseResponseSchema,
  JobStatusSchema,
  type AiUsageRow,
  type JobStatus,
} from "@/lib/schemas/admin";

export async function fetchAiUsage(): Promise<AiUsageRow[]> {
  const response = await api.get("/admin/ai/usage");
  const parsed = AiUsageResponseSchema.safeParse(response.data);
  return parsed.success ? parsed.data.items : (response.data as AiUsageRow[]);
}

export async function generateCourse(technology: string): Promise<string> {
  const response = await api.post("/admin/ai/generate-course", { technology });
  const parsed = GenerateCourseResponseSchema.parse(response.data);
  return parsed.jobId;
}

export async function fetchJobStatus(jobId: string): Promise<JobStatus> {
  const response = await api.get(`/admin/ai/jobs/${jobId}`);
  return JobStatusSchema.parse(response.data);
}

export async function publishCourse(courseId: string): Promise<void> {
  await api.post(`/admin/courses/${courseId}/publish`);
}
