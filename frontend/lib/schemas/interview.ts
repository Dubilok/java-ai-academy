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

export const InterviewQuestionsResponseSchema = z.array(InterviewQuestionSchema);

export type InterviewQuestion = z.infer<typeof InterviewQuestionSchema>;

// --- Session schemas ---

export const QuestionInSessionSchema = z.object({
  questionId: z.string().uuid(),
  questionText: z.string(),
  category: z.string(),
  difficulty: z.enum(["BEGINNER", "INTERMEDIATE", "ADVANCED", "EXPERT"]),
});

export const StartSessionResponseSchema = z.object({
  sessionId: z.string().uuid(),
  technology: z.string(),
  status: z.enum(["ACTIVE", "FINISHED"]),
  mode: z.enum(["TEXT", "VOICE"]).optional(),
  firstQuestion: QuestionInSessionSchema.nullable().optional(),
  firstVoiceQuestion: z.string().nullable().optional(),
});

// --- Voice interview schemas ---

export const TurnAssessmentSchema = z.object({
  topicCovered: z.string(),
  strength: z.enum(["STRONG", "ADEQUATE", "WEAK"]),
  note: z.string(),
});

export const TurnResultSchema = z.object({
  question: z.string(),
  topicMap: z.record(z.string(), z.boolean()),
  turnAssessment: TurnAssessmentSchema.nullable().optional(),
  isFinalTurn: z.boolean(),
});

export type TurnAssessment = z.infer<typeof TurnAssessmentSchema>;
export type TurnResult = z.infer<typeof TurnResultSchema>;

export const SubmitAnswerResponseSchema = z.object({
  sessionId: z.string().uuid(),
  answerId: z.string().uuid(),
  sessionStatus: z.enum(["ACTIVE", "FINISHED"]),
  nextQuestion: QuestionInSessionSchema.nullable(),
});

export const EvaluationDimensionSchema = z.object({
  name: z.string(),
  score: z.number().int().min(0).max(100),
  feedback: z.string(),
});

export const EvaluationReportSchema = z.object({
  sessionId: z.string().uuid(),
  technology: z.string(),
  overallScore: z.number().int().min(0).max(100),
  dimensions: z.array(EvaluationDimensionSchema),
  completedAt: z.string(),
  citations: z.array(z.unknown()).nullable(),
});

export const FinishSessionResponseSchema = z.object({
  sessionId: z.string().uuid(),
  status: z.enum(["ACTIVE", "FINISHED"]),
  overallScore: z.number().int(),
  report: EvaluationReportSchema,
});

export const SessionSummarySchema = z.object({
  sessionId: z.string().uuid(),
  technology: z.string(),
  status: z.enum(["ACTIVE", "FINISHED"]),
  score: z.number().int().nullable(),
  createdAt: z.string(),
});

export const SessionDetailSchema = z.object({
  sessionId: z.string().uuid(),
  technology: z.string(),
  status: z.enum(["ACTIVE", "FINISHED"]),
  score: z.number().int().nullable(),
  report: EvaluationReportSchema.nullable(),
  currentQuestion: QuestionInSessionSchema.nullable(),
  createdAt: z.string(),
});

export type QuestionInSession = z.infer<typeof QuestionInSessionSchema>;
export type StartSessionResponse = z.infer<typeof StartSessionResponseSchema>;
export type SubmitAnswerResponse = z.infer<typeof SubmitAnswerResponseSchema>;
export type EvaluationDimension = z.infer<typeof EvaluationDimensionSchema>;
export type EvaluationReport = z.infer<typeof EvaluationReportSchema>;
export type FinishSessionResponse = z.infer<typeof FinishSessionResponseSchema>;
export type SessionSummary = z.infer<typeof SessionSummarySchema>;
export type SessionDetail = z.infer<typeof SessionDetailSchema>;
