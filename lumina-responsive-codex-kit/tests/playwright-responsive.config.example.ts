import { defineConfig } from "@playwright/test";

export default defineConfig({
  testDir: ".",
  timeout: 45_000,
  use: {
    baseURL: process.env.BASE_URL ?? "http://localhost:8080",
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  reporter: [["list"], ["html", { open: "never" }]],
});
