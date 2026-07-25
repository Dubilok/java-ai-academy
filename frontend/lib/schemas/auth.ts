import { z } from "zod";

export const AuthResponseSchema = z.object({
  accessToken: z.string().min(1),
  refreshToken: z.string().min(1),
  tokenType: z.string().default("Bearer"),
});

export const ProblemDetailSchema = z.object({
  status: z.number(),
  title: z.string(),
  detail: z.string().optional(),
});

export type AuthResponse = z.infer<typeof AuthResponseSchema>;
export type ProblemDetail = z.infer<typeof ProblemDetailSchema>;
