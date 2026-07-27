import { api } from "@/lib/api";
import {
  AdminCourseSchema,
  AdminModuleSchema,
  AiUsageResponseSchema,
  GenerateCourseResponseSchema,
  JobStatusSchema,
  type AdminCourse,
  type AdminModule,
  type AiUsageResponse,
  type JobStatus,
} from "@/lib/schemas/admin";
import { InterviewQuestionsResponseSchema, type InterviewQuestion } from "@/lib/schemas/interview";
import { z } from "zod";

export async function fetchAiUsage(): Promise<AiUsageResponse> {
  const response = await api.get("/admin/ai/usage");
  return AiUsageResponseSchema.parse(response.data);
}

export async function fetchAdminCourses(): Promise<AdminCourse[]> {
  const response = await api.get("/admin/courses");
  return z.array(AdminCourseSchema).parse(response.data);
}

export async function fetchAdminModules(courseId: string): Promise<AdminModule[]> {
  const response = await api.get(`/admin/courses/${courseId}/modules`);
  return z.array(AdminModuleSchema).parse(response.data);
}

export async function generateCourse(payload: {
  technology: string;
  courseId?: string;
  moduleId?: string;
  moduleName?: string;
}): Promise<string> {
  const response = await api.post("/admin/ai/generate-course", payload);
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

export async function fetchAllFlashcards(): Promise<InterviewQuestion[]> {
  const response = await api.get("/interview/questions");
  return InterviewQuestionsResponseSchema.parse(response.data);
}

export async function createFlashcard(payload: {
  technology: string;
  category: string;
  question: string;
  shortAnswer: string;
  detailedExplanation?: string;
  difficulty: string;
}): Promise<InterviewQuestion> {
  const response = await api.post("/admin/interview/questions", payload);
  return response.data as InterviewQuestion;
}

export async function deleteFlashcard(id: string): Promise<void> {
  await api.delete(`/admin/interview/questions/${id}`);
}
