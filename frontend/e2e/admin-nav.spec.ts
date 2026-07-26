import { expect, test, type Page } from "@playwright/test";

/**
 * Admin nav link visibility tests.
 *
 * All backend calls are intercepted via page.route() — no live server required.
 * These tests run unconditionally in CI.
 */

const API = "http://localhost:8080/api/v1";

function makeJwt(payload: Record<string, unknown>): string {
  const encode = (obj: unknown) =>
    Buffer.from(JSON.stringify(obj)).toString("base64url");
  const header = encode({ alg: "HS256", typ: "JWT" });
  const body = encode(payload);
  return `${header}.${body}.fakesig`;
}

const ADMIN_TOKEN = makeJwt({
  sub: "admin@test.com",
  role: "ROLE_ADMIN",
  exp: 9_999_999_999,
});

const STUDENT_TOKEN = makeJwt({
  sub: "student@test.com",
  role: "ROLE_STUDENT",
  exp: 9_999_999_999,
});

function authBody(accessToken: string) {
  return JSON.stringify({
    accessToken,
    refreshToken: "fake-refresh-token",
    tokenType: "Bearer",
  });
}

async function mockDashboardApis(page: Page) {
  await page.route(`${API}/me`, (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        id: "00000000-0000-0000-0000-000000000001",
        email: "test@test.com",
        role: "ROLE_ADMIN",
        xpPoints: 0,
        crystals: 0,
        level: 1,
        streak: 0,
        createdAt: "2026-01-01T00:00:00Z",
      }),
    })
  );
  await page.route(`${API}/me/progress*`, (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify([]),
    })
  );
  await page.route(`${API}/courses*`, (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ items: [], nextCursor: null }),
    })
  );
}

async function mockAdminApis(page: Page) {
  await page.route(`${API}/admin/ai/usage*`, (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        from: null,
        to: null,
        totalRequests: 0,
        totalPromptTokens: 0,
        totalCompletionTokens: 0,
        totalCostUsd: null,
        byAgent: [],
        byUser: [],
        byCourse: [],
      }),
    })
  );
  await page.route(`${API}/admin/courses*`, (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify([]),
    })
  );
  await page.route(`${API}/admin/ai/evaluations*`, (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify([]),
    })
  );
}

async function loginAs(page: Page, accessToken: string) {
  await page.route(`${API}/auth/login`, (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: authBody(accessToken),
    })
  );

  await page.goto("/login");
  await page.getByLabel("Email").fill("user@test.com");
  await page.getByLabel("Password").fill("Password1!");
  await page.getByRole("button", { name: "Sign in" }).click();
  await page.waitForURL(/\/dashboard/);
}

test.describe("Admin nav link — role-based visibility", () => {
  test.beforeEach(async ({ page }) => {
    // Block session-restore so stored tokens from a prior run don't interfere.
    await page.route(`${API}/auth/refresh`, (route) =>
      route.fulfill({ status: 401, body: "{}" })
    );
  });

  test("admin user sees the Admin link in the nav", async ({ page }) => {
    await mockDashboardApis(page);
    await loginAs(page, ADMIN_TOKEN);

    await expect(page.getByRole("link", { name: "Admin" })).toBeVisible();
  });

  test("student user does not see the Admin link in the nav", async ({ page }) => {
    await mockDashboardApis(page);
    await loginAs(page, STUDENT_TOKEN);

    await expect(page.getByRole("link", { name: "Admin" })).not.toBeVisible();
  });

  test("Admin nav link points to /admin", async ({ page }) => {
    await mockDashboardApis(page);
    await loginAs(page, ADMIN_TOKEN);

    const adminLink = page.getByRole("link", { name: "Admin" });
    await expect(adminLink).toHaveAttribute("href", "/admin");
  });

  test("Admin nav link has aria-current=page when on /admin", async ({ page }) => {
    // Clicking the <a href="/admin"> causes a full page reload. The auth context
    // re-initialises and calls /auth/refresh. Override the beforeEach 401 so the
    // session-restore succeeds and the user stays logged in on the /admin page.
    await page.route(`${API}/auth/refresh`, (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: authBody(ADMIN_TOKEN),
      })
    );
    await mockDashboardApis(page);
    await mockAdminApis(page);
    await loginAs(page, ADMIN_TOKEN);

    await page.getByRole("link", { name: "Admin" }).click();
    await page.waitForURL(/\/admin/);

    await expect(page.getByRole("link", { name: "Admin" })).toHaveAttribute(
      "aria-current",
      "page"
    );
  });

  test("clicking Admin link navigates to /admin", async ({ page }) => {
    // Same full-page-reload concern: override refresh so the user stays logged in.
    await page.route(`${API}/auth/refresh`, (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: authBody(ADMIN_TOKEN),
      })
    );
    await mockDashboardApis(page);
    await mockAdminApis(page);
    await loginAs(page, ADMIN_TOKEN);

    await page.getByRole("link", { name: "Admin" }).click();
    await expect(page).toHaveURL(/\/admin/);
  });
});
