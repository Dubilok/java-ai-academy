import { api } from "@/lib/api";
import {
  InterviewQuestionsResponseSchema,
  StartSessionResponseSchema,
  SubmitAnswerResponseSchema,
  FinishSessionResponseSchema,
  SessionDetailSchema,
  TurnResultSchema,
  type InterviewQuestion,
  type StartSessionResponse,
  type SubmitAnswerResponse,
  type FinishSessionResponse,
  type SessionDetail,
  type SessionSummary,
  type TurnResult,
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

export async function startVoiceSession(
  technology: string,
  maxTurns = 5,
): Promise<StartSessionResponse> {
  const response = await api.post("/interview/sessions", {
    technology,
    mode: "VOICE",
    maxTurns,
  });
  return StartSessionResponseSchema.parse(response.data);
}

/**
 * Submits a voice answer and returns the full TurnResult parsed from
 * the SSE `done` event. Streams token events to onToken callback.
 */
export async function submitVoiceAnswer(
  sessionId: string,
  transcript: string,
  turnIndex: number,
  durationSeconds: number,
  onToken: (token: string) => void,
): Promise<TurnResult> {
  const { getAccessToken } = await import("@/lib/api");
  const token = getAccessToken();

  const baseUrl = process.env["NEXT_PUBLIC_API_URL"] ?? "http://localhost:8080";
  const response = await fetch(
    `${baseUrl}/api/v1/interview/sessions/${sessionId}/voice-answers`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify({ transcript, turnIndex, durationSeconds }),
    },
  );

  if (!response.ok || !response.body) {
    throw new Error(`Voice answer submission failed: ${response.status}`);
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });

    const lines = buffer.split("\n");
    buffer = lines.pop() ?? "";

    let eventName = "";
    let eventData = "";

    for (const line of lines) {
      if (line.startsWith("event:")) {
        eventName = line.slice(6).trim();
      } else if (line.startsWith("data:")) {
        eventData = line.slice(5).trim();
      } else if (line === "") {
        if (eventName === "token" && eventData) {
          try {
            const payload = JSON.parse(eventData) as { value?: string };
            if (payload.value) onToken(payload.value);
          } catch {
            // ignore parse errors on individual token events
          }
        } else if (eventName === "done" && eventData) {
          try {
            return TurnResultSchema.parse(JSON.parse(eventData));
          } catch {
            throw new Error("Failed to parse done event: " + eventData);
          }
        }
        eventName = "";
        eventData = "";
      }
    }
  }

  throw new Error("SSE stream ended without a done event");
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
