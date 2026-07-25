import { z } from "zod";

export const InterviewQuestionSchema = z.object({
  id: z.string().uuid(),
  technology: z.string(),
  category: z.string(),
  question: z.string(),
  shortAnswer: z.string(),
  detailedExplanation: z.string().optional(),
  difficulty: z.enum(["BEGINNER", "INTERMEDIATE", "ADVANCED", "EXPERT"]),
});

export const InterviewQuestionsResponseSchema = z.object({
  items: z.array(InterviewQuestionSchema),
  nextCursor: z.string().nullable(),
});

export type InterviewQuestion = z.infer<typeof InterviewQuestionSchema>;
