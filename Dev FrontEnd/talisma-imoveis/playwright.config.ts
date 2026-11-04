import { defineConfig } from "@playwright/test";

const porta = Number(process.env.E2E_PORT ?? 3011);
const baseURL = process.env.E2E_BASE_URL ?? "http://127.0.0.1:" + porta;

export default defineConfig({
  testDir: "./e2e",
  timeout: 45000,
  use: { baseURL, trace: "retain-on-failure" },
  webServer: process.env.E2E_BASE_URL ? undefined : {
    command: "npm run start -- --hostname 127.0.0.1 --port " + porta,
    url: baseURL,
    env: { API_URL: process.env.API_URL ?? "http://127.0.0.1:8080" },
    reuseExistingServer: !process.env.CI,
  },
});
