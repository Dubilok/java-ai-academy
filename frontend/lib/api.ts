import axios from "axios";
import { AuthResponseSchema } from "./schemas/auth";
import { clearTokens, getRefreshToken, setRefreshToken } from "./tokens";

const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1";

export const api = axios.create({
  baseURL: BASE_URL,
  headers: { "Content-Type": "application/json" },
});

let accessToken: string | null = null;
let refreshPromise: Promise<string | null> | null = null;

export function setAccessToken(token: string): void {
  accessToken = token;
}

export function getAccessToken(): string | null {
  return accessToken;
}

export function clearAccessToken(): void {
  accessToken = null;
}

api.interceptors.request.use((config) => {
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error: unknown) => {
    if (!axios.isAxiosError(error) || error.response?.status !== 401) {
      return Promise.reject(error);
    }

    const originalRequest = error.config;
    if (!originalRequest || (originalRequest as { _retry?: boolean })._retry) {
      return Promise.reject(error);
    }
    (originalRequest as { _retry?: boolean })._retry = true;

    if (!refreshPromise) {
      refreshPromise = doRefresh().finally(() => {
        refreshPromise = null;
      });
    }

    const newToken = await refreshPromise;
    if (!newToken) {
      clearTokens();
      clearAccessToken();
      if (typeof window !== "undefined") {
        window.location.href = "/login";
      }
      return Promise.reject(error);
    }

    originalRequest.headers = originalRequest.headers ?? {};
    originalRequest.headers.Authorization = `Bearer ${newToken}`;
    return api(originalRequest);
  }
);

async function doRefresh(): Promise<string | null> {
  const refreshToken = getRefreshToken();
  if (!refreshToken) return null;

  try {
    const response = await axios.post(`${BASE_URL}/auth/refresh`, { refreshToken });
    const parsed = AuthResponseSchema.safeParse(response.data);
    if (!parsed.success) return null;

    setAccessToken(parsed.data.accessToken);
    setRefreshToken(parsed.data.refreshToken);
    return parsed.data.accessToken;
  } catch {
    return null;
  }
}
