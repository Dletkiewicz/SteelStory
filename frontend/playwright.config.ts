import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './tests/browser',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: 'list',
  use: {
    baseURL: 'http://127.0.0.1:5173',
    // Set PLAYWRIGHT_CHANNEL=msedge to use an existing local Edge installation.
    ...(process.env.PLAYWRIGHT_CHANNEL ? { channel: process.env.PLAYWRIGHT_CHANNEL } : {}),
    viewport: { width: 1440, height: 1000 },
    screenshot: 'only-on-failure',
    trace: 'off', // Authentication traces could contain synthetic passwords and CSRF values.
  },
  projects: [
    { name: 'ui', testMatch: '**/ui.spec.ts' },
    { name: 'integration', testMatch: '**/integration.spec.ts' },
  ],
  webServer: {
    command: 'npm run dev',
    url: 'http://127.0.0.1:5173',
    reuseExistingServer: !process.env.CI,
  },
});
