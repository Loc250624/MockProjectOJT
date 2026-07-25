import { expect, test, type Page } from "@playwright/test";

const viewports = [
  { width: 320, height: 568 },
  { width: 375, height: 667 },
  { width: 390, height: 844 },
  { width: 768, height: 1024 },
  { width: 1280, height: 800 },
];

function routesFromEnv(name: string, fallback: string[]): string[] {
  const value = process.env[name];
  return value ? value.split(",").map((route) => route.trim()).filter(Boolean) : fallback;
}

const publicRoutes = routesFromEnv("RESPONSIVE_PUBLIC_ROUTES", ["/", "/courses", "/blog"]);

async function assertNoPageOverflow(page: Page) {
  const result = await page.evaluate(() => {
    const root = document.documentElement;
    const body = document.body;
    const overflow = Math.max(root.scrollWidth, body?.scrollWidth ?? 0) - root.clientWidth;

    const offenders = [...document.querySelectorAll<HTMLElement>("body *")]
      .map((element) => {
        const rect = element.getBoundingClientRect();
        const style = getComputedStyle(element);
        return {
          tag: element.tagName.toLowerCase(),
          id: element.id,
          className: typeof element.className === "string" ? element.className.slice(0, 120) : "",
          left: rect.left,
          right: rect.right,
          width: rect.width,
          position: style.position,
          overflowX: style.overflowX,
        };
      })
      .filter((item) => item.width > 0 && (item.right > root.clientWidth + 2 || item.left < -2))
      .slice(0, 25);

    return { overflow, clientWidth: root.clientWidth, scrollWidth: root.scrollWidth, offenders };
  });

  expect(result, JSON.stringify(result, null, 2)).toMatchObject({ overflow: expect.any(Number) });
  expect(result.overflow, JSON.stringify(result, null, 2)).toBeLessThanOrEqual(2);
}

for (const viewport of viewports) {
  for (const route of publicRoutes) {
    test(`${route} has no page overflow at ${viewport.width}x${viewport.height}`, async ({ page }) => {
      await page.setViewportSize(viewport);
      await page.goto(route, { waitUntil: "networkidle" });
      await expect(page.locator("body")).toBeVisible();
      await assertNoPageOverflow(page);
      await page.screenshot({
        path: `test-results/responsive/${route.replace(/[^a-z0-9]+/gi, "_") || "home"}-${viewport.width}x${viewport.height}.png`,
        fullPage: true,
      });
    });
  }
}
