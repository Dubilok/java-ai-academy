import { api } from "@/lib/api";
import {
  CourseDetailSchema,
  MeSchema,
  PagedCoursesSchema,
  type CourseDetail,
  type CourseListItem,
  type Me,
} from "@/lib/schemas/catalog";

export async function fetchCourses(cursor?: string): Promise<{ items: CourseListItem[]; nextCursor: string | null }> {
  const params = cursor ? { cursor, limit: 20 } : { limit: 20 };
  const response = await api.get("/courses", { params });
  return PagedCoursesSchema.parse(response.data);
}

export async function fetchCourse(id: string): Promise<CourseDetail> {
  const response = await api.get(`/courses/${id}`);
  return CourseDetailSchema.parse(response.data);
}

export async function fetchMe(): Promise<Me> {
  const response = await api.get("/me");
  return MeSchema.parse(response.data);
}

export async function fetchProgress(): Promise<unknown[]> {
  const response = await api.get("/me/progress");
  return response.data as unknown[];
}
