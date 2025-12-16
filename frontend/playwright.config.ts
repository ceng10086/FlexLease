/// <reference types="node" />
import { defineConfig, devices } from '@playwright/test';

const baseURL = process.env.E2E_BASE_URL ?? 'http://localhost:8080';

const isHeaded = process.argv.includes('--headed');
const slowMoMs = Number(process.env.E2E_SLOW_MO_MS ?? (isHeaded ? 200 : 0));

// Playwright 会把测试跑在 worker 子进程里；worker 里不一定能通过 argv 可靠判断 headed。
// 这里显式透传一个环境变量给测试用例使用。
process.env.E2E_HEADED = isHeaded ? '1' : '0';

export default defineConfig({
  testDir: './e2e',
  timeout: 8 * 60 * 1000,
  expect: { timeout: 15_000 },
  retries: process.env.CI ? 1 : 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  // 演示场景：headed 下避免并发跑多个浏览器实例（更易讲解，也更少干扰）。
  workers: isHeaded ? 1 : undefined,
  use: {
    baseURL,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    launchOptions: {
      slowMo: slowMoMs > 0 ? slowMoMs : undefined
    }
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] }
    }
  ]
});
