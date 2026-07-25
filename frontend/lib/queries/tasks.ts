import { api } from "@/lib/api";
import { LectureDetailSchema, TaskDetailSchema, type LectureDetail, type TaskDetail } from "@/lib/schemas/task";

export async function fetchTask(id: string): Promise<TaskDetail> {
  const response = await api.get(`/tasks/${id}`);
  return TaskDetailSchema.parse(response.data);
}

export async function fetchLecture(id: string): Promise<LectureDetail> {
  const response = await api.get(`/lectures/${id}`);
  return LectureDetailSchema.parse(response.data);
}

export async function submitCode(
  taskId: string,
  source: string
): Promise<{ submissionId: string }> {
  const response = await api.post<{ submissionId: string }>(`/tasks/${taskId}/submissions`, { source });
  return response.data;
}

export async function pollSubmission(submissionId: string): Promise<{
  id: string;
  status: "PENDING" | "PASSED" | "FAILED";
  logs: string | null;
  durationMs: number | null;
}> {
  const response = await api.get(`/submissions/${submissionId}`);
  return response.data as {
    id: string;
    status: "PENDING" | "PASSED" | "FAILED";
    logs: string | null;
    durationMs: number | null;
  };
}

export async function fetchAiHint(taskId: string): Promise<{ hint: string }> {
  const response = await api.post<{ hint: string }>(`/tasks/${taskId}/ai-hint`);
  return response.data;
}
