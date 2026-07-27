import { test, expect } from "@playwright/test";

test("greeting appears before one-time quick actions", async ({ page }) => {
  await page.goto("/");

  await page.getByRole("button", { name: "Open AI Chatbot" }).click();

  const panel = page.getByLabel("AI Chatbot");
  await expect(panel.getByRole("heading", { name: "AI Chatbot" })).toBeVisible();
  await expect(panel.getByText(/AI Tutor/i)).toHaveCount(0);

  const greeting = panel.locator(".assistant-message").first();
  const quickActions = panel.getByLabel("Quick questions");

  await expect(greeting).toBeVisible();
  await expect(quickActions).toBeVisible();

  await quickActions.getByRole("button").first().click();
  await expect(quickActions).toHaveCount(0);

  await page.getByRole("button", { name: "Close AI Chatbot" }).click();
  await page.getByRole("button", { name: "Open AI Chatbot" }).click();
  await expect(panel.getByLabel("Quick questions")).toHaveCount(0);
});

test("only one chatbot root is mounted", async ({ page }) => {
  await page.goto("/student/dashboard");
  await expect(page.locator("[data-ai-chatbot-root]")).toHaveCount(1);
});
