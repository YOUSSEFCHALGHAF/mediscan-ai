/// <reference types="node" />
import { defineConfig, devices } from '@playwright/test';

/**
 * Tests de bout en bout (E2E) de MediScan AI, dans un vrai navigateur.
 * Prérequis : MySQL et le backend Spring Boot démarrés (http://localhost:8080),
 * données de démonstration chargées. Angular est démarré automatiquement si besoin.
 *
 * Lancer : npm run e2e        Rapport : npm run e2e:rapport
 */
export default defineConfig({
  testDir: './e2e',
  globalSetup: './e2e/verification-backend.ts',
  timeout: 120_000,                 // le parcours complet comprend une analyse OCR
  expect: { timeout: 15_000 },
  fullyParallel: false,             // les tests partagent la même base : un à la fois
  workers: 1,
  retries: 0,
  reporter: [['list'], ['html', { open: 'never', outputFolder: 'playwright-report' }]],
  use: {
    baseURL: 'http://localhost:4200',
    locale: 'fr-FR',
    timezoneId: 'Europe/Paris',
    trace: 'retain-on-failure',     // enregistrement complet en cas d'échec (npx playwright show-trace)
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  launchOptions: {
      slowMo: Number(process.env['SLOWMO'] ?? 0),
    },
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: {
    command: 'npm start',
    url: 'http://localhost:4200',
    reuseExistingServer: true,      // si "ng serve" tourne déjà, on l'utilise
    timeout: 180_000,
  },
});