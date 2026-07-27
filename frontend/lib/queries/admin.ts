import { api } from "@/lib/api";
import {
  AdminCourseSchema,
  AdminModuleSchema,
  AiUsageResponseSchema,
  CurriculumProposalSchema,
  ImportContentResponseSchema,
  JobStatusSchema,
  ProposeLecturesResponseSchema,
  ProposeModulesResponseSchema,
  type AdminCourse,
  type AdminModule,
  type AiUsageResponse,
  type CurriculumProposal,
  type ImportContentResponse,
  type JobStatus,
  type ModuleProposal,
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

export async function proposeCurriculum(technology: string): Promise<CurriculumProposal> {
  const response = await api.post("/admin/ai/propose-curriculum", { technology });
  return CurriculumProposalSchema.parse(response.data);
}

export async function proposeMoreModules(
  technology: string,
  existingModules: string[]
): Promise<ModuleProposal[]> {
  const response = await api.post("/admin/ai/propose-modules", { technology, existingModules });
  return ProposeModulesResponseSchema.parse(response.data).modules;
}

export async function proposeMoreLectures(
  technology: string,
  moduleName: string,
  existingLectures: string[]
): Promise<string[]> {
  const response = await api.post("/admin/ai/propose-lectures", {
    technology,
    moduleName,
    existingLectures,
  });
  return ProposeLecturesResponseSchema.parse(response.data).lectureTopics;
}

export async function generateCourse(payload: {
  technology: string;
  courseId?: string;
  moduleId?: string;
  moduleName?: string;
  curriculum?: {
    courseName: string;
    description: string;
    modules: Array<{
      moduleName: string;
      lectures: Array<{ lectureTitle: string; taskCount: number }>;
    }>;
  };
}): Promise<string> {
  const response = await api.post("/admin/ai/generate-course", payload);
  return z.object({ jobId: z.string() }).parse(response.data).jobId;
}

export async function fetchAllJobs(): Promise<JobStatus[]> {
  const response = await api.get("/admin/ai/jobs");
  return z.array(JobStatusSchema).parse(response.data);
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

export async function generateFlashcards(payload: {
  technology: string;
  category?: string;
  count: number;
  difficulty?: string;
}): Promise<InterviewQuestion[]> {
  const response = await api.post("/admin/interview/questions/generate", payload);
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

export async function importContent(moduleId: string, rawJson: string): Promise<ImportContentResponse> {
  const response = await api.post("/admin/content/import", { moduleId, rawJson });
  return ImportContentResponseSchema.parse(response.data);
}
