import { z } from "zod";

export const TaskDetailSchema = z.object({
  id: z.string().uuid(),
  courseId: z.string().uuid(),
  title: z.string(),
  description: z.string(),
  difficulty: z.enum(["EASY", "MEDIUM", "HARD"]),
  templateCode: z.string(),
  xpReward: z.number().int().nonnegative(),
});

export const LectureDetailSchema = z.object({
  id: z.string().uuid(),
  courseId: z.string().uuid(),
  title: z.string(),
  contentMarkdown: z.string(),
  orderIndex: z.number().int(),
  tasks: z.array(
    z.object({
      id: z.string().uuid(),
      title: z.string(),
    })
  ),
});

export type TaskDetail = z.infer<typeof TaskDetailSchema>;
export type LectureDetail = z.infer<typeof LectureDetailSchema>;
