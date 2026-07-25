import { api } from "@/lib/api";
import {
  InterviewQuestionsResponseSchema,
  type InterviewQuestion,
} from "@/lib/schemas/interview";

export async function fetchFlashcards(params?: {
  technology?: string;
  category?: string;
  difficulty?: string;
}): Promise<InterviewQuestion[]> {
  const response = await api.get("/interview/questions", { params });
  const parsed = InterviewQuestionsResponseSchema.parse(response.data);
  return parsed.items;
}
