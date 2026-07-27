import { api } from "@/lib/api";
import {
  InterviewQuestionsResponseSchema,
  StartSessionResponseSchema,
  SubmitAnswerResponseSchema,
  FinishSessionResponseSchema,
  SessionDetailSchema,
  type InterviewQuestion,
  type StartSessionResponse,
  type SubmitAnswerResponse,
  type FinishSessionResponse,
  type SessionDetail,
  type SessionSummary,
  SessionSummarySchema,
} from "@/lib/schemas/interview";

export async function fetchFlashcards(params?: {
  technology?: string;
  category?: string;
  difficulty?: string;
}): Promise<InterviewQuestion[]> {
  const response = await api.get("/interview/questions", { params });
  return InterviewQuestionsResponseSchema.parse(response.data);
}

export async function fetchSessions(): Promise<SessionSummary[]> {
  const response = await api.get("/interview/sessions");
  return SessionSummarySchema.array().parse(response.data);
}

export async function fetchSession(sessionId: string): Promise<SessionDetail> {
  const response = await api.get(`/interview/sessions/${sessionId}`);
  return SessionDetailSchema.parse(response.data);
}

export async function startSession(technology: string): Promise<StartSessionResponse> {
  const response = await api.post("/interview/sessions", { technology });
  return StartSessionResponseSchema.parse(response.data);
}

export async function submitAnswer(
  sessionId: string,
  answerText: string
): Promise<SubmitAnswerResponse> {
  const response = await api.post(`/interview/sessions/${sessionId}/answers`, { answerText });
  return SubmitAnswerResponseSchema.parse(response.data);
}

export async function finishSession(sessionId: string): Promise<FinishSessionResponse> {
  const response = await api.post(`/interview/sessions/${sessionId}/finish`);
  return FinishSessionResponseSchema.parse(response.data);
}
