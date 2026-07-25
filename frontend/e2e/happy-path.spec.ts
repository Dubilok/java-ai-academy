import { expect, test } from "@playwright/test";

/**
 * Happy-path E2E: login → open task → fail → get AI hint → pass
 *
 * Prerequisites (run against a local stack with seed data):
 *   - Backend running at localhost:8080
 *   - Seed user: student@example.com / Student123!
 *   - Seed task with a known UUID reachable from the dashboard
 *
 * These tests are integration tests against a live backend.
 * They are skipped in CI unless PLAYWRIGHT_LIVE=true is set.
 */

const SKIP = !process.env.PLAYWRIGHT_LIVE;

test.describe("Student happy path", () => {
  test.skip(SKIP, "Skipped unless PLAYWRIGHT_LIVE=true");

  test("login with valid credentials redirects to dashboard", async ({ page }) => {
    await page.goto("/login");

    await page.getByLabel("Email").fill("student@example.com");
    await page.getByLabel("Password").fill("Student123!");
    await page.getByRole("button", { name: "Sign in" }).click();

    await expect(page).toHaveURL(/\/dashboard/);
    await expect(page.getByText("Dashboard")).toBeVisible();
  });

  test("login with wrong password shows error", async ({ page }) => {
    await page.goto("/login");

    await page.getByLabel("Email").fill("student@example.com");
    await page.getByLabel("Password").fill("WrongPassword");
    await page.getByRole("button", { name: "Sign in" }).click();

    await expect(page.getByRole("alert")).toBeVisible();
    await expect(page.getByRole("alert")).toContainText("Invalid email or password");
  });

  test("unauthenticated user is redirected to login", async ({ page }) => {
    await page.goto("/dashboard");
    await expect(page).toHaveURL(/\/login/);
  });

  test("task workspace shows description, editor, and submit button", async ({ page }) => {
    await page.goto("/login");
    await page.getByLabel("Email").fill("student@example.com");
    await page.getByLabel("Password").fill("Student123!");
    await page.getByRole("button", { name: "Sign in" }).click();
    await page.waitForURL(/\/dashboard/);

    const courseLink = page.getByRole("link").filter({ hasText: /Java/i }).first();
    await courseLink.click();

    const taskLink = page.getByRole("link").filter({ hasText: /Task|task/i }).first();
    await taskLink.click();

    await expect(page.getByLabel("Task description")).toBeVisible();
    await expect(page.getByLabel("Code editor")).toBeVisible();
    await expect(page.getByRole("button", { name: "Submit" })).toBeVisible();
  });

  test("submitting broken code shows FAILED verdict and reveals AI hint button", async ({ page }) => {
    await page.goto("/login");
    await page.getByLabel("Email").fill("student@example.com");
    await page.getByLabel("Password").fill("Student123!");
    await page.getByRole("button", { name: "Sign in" }).click();
    await page.waitForURL(/\/dashboard/);

    const courseLink = page.getByRole("link").filter({ hasText: /Java/i }).first();
    await courseLink.click();
    const taskLink = page.getByRole("link").filter({ hasText: /Task|task/i }).first();
    await taskLink.click();

    await page.waitForSelector("[aria-label='Code editor']");

    await page.getByRole("button", { name: "Submit" }).click();

    await expect(page.getByText("FAILED")).toBeVisible({ timeout: 45_000 });

    await expect(page.getByRole("button", { name: "Ask for a hint" })).toBeVisible();
  });
});

test.describe("Public pages", () => {
  test("home page renders the landing content", async ({ page }) => {
    await page.goto("/");
    await expect(page.getByRole("heading", { name: /Java.*Academy/i })).toBeVisible();
    await expect(page.getByText("Start learning")).toBeVisible();
  });

  test("login page has accessible form elements", async ({ page }) => {
    await page.goto("/login");
    await expect(page.getByLabel("Email")).toBeVisible();
    await expect(page.getByLabel("Password")).toBeVisible();
    await expect(page.getByRole("button", { name: "Sign in" })).toBeVisible();
  });
});
