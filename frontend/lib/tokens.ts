const REFRESH_TOKEN_KEY = "jaa_refresh_token";
const LOGGED_IN_COOKIE = "jaa_logged_in";

export function getRefreshToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(REFRESH_TOKEN_KEY);
}

export function setRefreshToken(token: string): void {
  localStorage.setItem(REFRESH_TOKEN_KEY, token);
  document.cookie = `${LOGGED_IN_COOKIE}=1; path=/; max-age=${60 * 60 * 24 * 30}; SameSite=Lax`;
}

export function clearTokens(): void {
  localStorage.removeItem(REFRESH_TOKEN_KEY);
  document.cookie = `${LOGGED_IN_COOKIE}=; path=/; max-age=0; SameSite=Lax`;
}

export function isLoggedInCookieSet(): boolean {
  if (typeof document === "undefined") return false;
  return document.cookie.split(";").some((c) => c.trim().startsWith(`${LOGGED_IN_COOKIE}=`));
}
