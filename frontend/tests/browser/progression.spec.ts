import { expect, test } from '@playwright/test';
import { authenticatedPage, character } from './fixtures';

const upgraded = { ...character, level: 3, experience: 500, unspentTrainingPoints: 6,
  progression: { currentLevelExperience: 0, nextLevelExperience: 900, remainingExperience: 900, maximumLevel: false } };
const award = { previousLevel: 1, currentLevel: 3, levelsGained: 2, acceptedExperience: 500,
  trainingPointsAwarded: 6, totalExperience: 500, unspentTrainingPoints: 6 };

test('XP grant refreshes persisted data, announces actual level gains and blocks double clicks', async ({ page }) => {
  await authenticatedPage(page);
  let current = character;
  let posts = 0;
  let finish: () => void = () => {};
  const responseGate = new Promise<void>((resolve) => { finish = resolve; });
  await page.route('**/api/v1/characters/test-character', (route) => route.fulfill({ json: current }));
  await page.route('**/api/v1/dev/characters/test-character/grant-xp', async (route) => {
    posts++;
    expect(route.request().postDataJSON()).toEqual({ amount: 500 });
    expect(route.request().headers()['x-csrf-token']).toBe('test-only');
    await responseGate;
    current = upgraded;
    await route.fulfill({ json: award });
  });
  await page.goto('/#/characters/test-character');
  await expect(page.getByRole('heading', { name: 'Panel developerski — XP' })).toBeVisible();
  await page.getByRole('button', { name: 'Przyznaj 500 XP', exact: true }).dblclick();
  await expect(page.getByRole('button', { name: 'Przyznaj 100 XP', exact: true })).toBeDisabled();
  expect(posts).toBe(1);
  finish();
  await expect(page.getByText('Awans! Poziom 3.', { exact: false })).toContainText('+6 pkt nauki');
  await expect(page.getByText('Do następnego poziomu:', { exact: false })).toContainText('900 XP');
  await expect(page.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '0');
  await page.reload();
  await expect(page.getByText('Dostępne punkty nauki:', { exact: false })).toContainText('6');
  await expect(page.getByText('Awans!', { exact: false })).toHaveCount(0);
});

test('below-threshold XP shows progress without a false level-up celebration', async ({ page }) => {
  await authenticatedPage(page);
  let current = character;
  await page.route('**/api/v1/characters/test-character', (route) => route.fulfill({ json: current }));
  await page.route('**/api/v1/dev/characters/test-character/grant-xp', async (route) => {
    current = { ...character, experience: 40,
      progression: { currentLevelExperience: 40, nextLevelExperience: 100, remainingExperience: 60, maximumLevel: false } };
    await route.fulfill({ json: { ...award, currentLevel: 1, levelsGained: 0, acceptedExperience: 40,
      trainingPointsAwarded: 0, totalExperience: 40, unspentTrainingPoints: 0 } });
  });
  await page.goto('/#/characters/test-character');
  await page.getByLabel('Własna nagroda XP', { exact: false }).fill('40');
  await page.getByRole('button', { name: 'Przyznaj XP', exact: true }).click();
  await expect(page.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '40');
  await expect(page.getByText('40 / 100 XP', { exact: true })).toBeVisible();
  await expect(page.getByText('Do następnego poziomu:', { exact: false })).toContainText('60 XP');
  await expect(page.getByText('Awans!', { exact: false })).toHaveCount(0);
});

test('level 200 has complete progress, no level 201 target and a valid same-gender portrait', async ({ page }, testInfo) => {
  await authenticatedPage(page);
  await page.route('**/api/v1/characters/test-character', (route) => route.fulfill({ json: {
    ...character, level: 200, experience: 264670000, unspentTrainingPoints: 597, appearanceGender: 'FEMALE',
    progression: { currentLevelExperience: 0, nextLevelExperience: 0, remainingExperience: 0, maximumLevel: true },
  } }));
  await page.goto('/#/characters/test-character');
  await expect(page.getByText('Poziom maksymalny', { exact: true })).toBeVisible();
  await expect(page.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '100');
  await expect(page.getByText('Dostępne punkty nauki:', { exact: false })).toContainText('597');
  await expect(page.getByText('Do następnego poziomu:', { exact: false })).toHaveCount(0);
  await expect(page.getByText('Droga do poziomu 201')).toHaveCount(0);
  const portrait = page.locator('.character-portrait');
  await expect(portrait).toHaveAttribute('src', '/portraits/warrior/female/tier-1.webp');
  await portrait.evaluate(async (image: HTMLImageElement) => { await image.decode(); });
  await page.setViewportSize({ width: 360, height: 800 });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.screenshot({ path: testInfo.outputPath('maximum-level-mobile.png'), fullPage: true });
});

test('disabled backend tool explains configuration and disables grants', async ({ page }) => {
  await authenticatedPage(page);
  await page.route('**/api/v1/characters/test-character', (route) => route.fulfill({ json: character }));
  await page.route('**/api/v1/dev/characters/test-character/grant-xp', (route) => route.fulfill({ status: 404, json: {} }));
  await page.goto('/#/characters/test-character');
  await page.getByRole('button', { name: 'Przyznaj 100 XP', exact: true }).click();
  await expect(page.getByText('Narzędzie XP jest wyłączone', { exact: false })).toContainText('steelstory.dev.xp.enabled=true');
  await expect(page.getByRole('button', { name: 'Przyznaj 500 XP', exact: true })).toBeDisabled();
});

test('uncertain reward is not retried and requires a read-only refresh', async ({ page }) => {
  await authenticatedPage(page);
  let current = character;
  let posts = 0;
  await page.route('**/api/v1/characters/test-character', (route) => route.fulfill({ json: current }));
  await page.route('**/api/v1/dev/characters/test-character/grant-xp', async (route) => {
    posts++; current = upgraded;
    await route.abort('failed');
  });
  await page.goto('/#/characters/test-character');
  await page.getByRole('button', { name: 'Przyznaj 500 XP', exact: true }).click();
  await expect(page.getByText('Wynik przyznania XP jest nieznany.', { exact: false })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Przyznaj 500 XP', exact: true })).toBeDisabled();
  await page.getByRole('button', { name: 'Odśwież dane postaci' }).click();
  await expect(page.getByText('Dostępne punkty nauki:', { exact: false })).toContainText('6');
  expect(posts).toBe(1);
  await expect(page.getByText('Awans!', { exact: false })).toHaveCount(0);
});

test('invalid input stays local and CSRF recovery does not repeat an award', async ({ page }) => {
  await authenticatedPage(page);
  let posts = 0;
  await page.route('**/api/v1/characters/test-character', (route) => route.fulfill({ json: character }));
  await page.route('**/api/v1/dev/characters/test-character/grant-xp', async (route) => {
    posts++; await route.fulfill({ status: 403, json: { code: 'ACCESS_DENIED' } });
  });
  await page.goto('/#/characters/test-character');
  for (const value of ['0', '-1', '1.5', '1000000001']) {
    await page.getByLabel('Własna nagroda XP', { exact: false }).fill(value);
    await expect(page.getByRole('button', { name: 'Przyznaj XP', exact: true })).toBeDisabled();
  }
  await page.getByRole('button', { name: 'Przyznaj 100 XP', exact: true }).click();
  await page.getByRole('button', { name: 'Odśwież sesję' }).click();
  await expect(page.getByText('Sesja odświeżona.', { exact: false })).toBeVisible();
  expect(posts).toBe(1);
});
