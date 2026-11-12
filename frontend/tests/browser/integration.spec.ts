import { expect, test } from '@playwright/test';
import type { Page } from '@playwright/test';

// Requires the actual backend local profile and PostgreSQL. No intercepted API responses.
// Each run leaves synthetic accounts/characters: deletion is intentionally not implemented.
test('real browser: register, log in, create three heroes, persist profiles, reload and log out', async ({ page }, testInfo) => {
  test.setTimeout(90000);
  const identity = Date.now().toString(36);
  const email = `browser-${identity}@example.test`;
  const password = 'Synthetic browser test passphrase';
  const names = [`Knight ${identity}`, `Archer ${identity}`, `Mage ${identity}`];
  const pageErrors: string[] = [];
  page.on('pageerror', (error) => pageErrors.push(error.message));

  async function logIn(target: Page) {
    await target.getByLabel('Adres e-mail').fill(email);
    await target.getByLabel('Hasło', { exact: true }).fill(password);
    await target.getByRole('button', { name: 'Zaloguj się' }).click();
  }
  await page.goto('/');
  await expect(page.getByRole('button', { name: 'Zaloguj się' })).toBeVisible();
  await page.screenshot({ path: testInfo.outputPath('login-desktop.png'), fullPage: true });
  await page.getByRole('link', { name: 'Załóż konto' }).click();
  await page.getByLabel('Adres e-mail').fill(email);
  await page.getByLabel('Hasło', { exact: true }).fill(password);
  await page.getByLabel('Powtórz hasło').fill(password);
  await page.getByRole('button', { name: 'Załóż konto' }).click();
  await expect(page.getByText('Konto utworzone.', { exact: false })).toBeVisible();
  await expect(page.getByRole('article')).toHaveCount(0);
  await page.getByRole('link', { name: 'Przejdź do logowania' }).click();
  await logIn(page);
  await expect(page.getByRole('link', { name: 'Stwórz postać' })).toHaveCount(3);

  for (let index = 0; index < 3; index++) {
    const name = names[index];
    expect(name).toBeDefined();
    await page.getByRole('link', { name: 'Stwórz postać' }).first().click();
    await page.getByLabel('Imię postaci', { exact: true }).fill(name!);
    await page.getByRole('radio', { name: [/Wojownik/, /Strzelec/, /Mag/][index]! }).check();
    await expect(page.getByRole('button', { name: 'Stwórz bohatera' })).toBeDisabled();
    for (let point = 0; point < 6; point++) await page.getByRole('button', { name: 'Dodaj: Siła', exact: true }).click();
    for (let point = 0; point < 4; point++) await page.getByRole('button', { name: 'Dodaj: Kondycja', exact: true }).click();
    if (index === 0) {
      await page.screenshot({ path: testInfo.outputPath('creator-desktop.png'), fullPage: true });
      await page.setViewportSize({ width: 360, height: 800 });
      expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
      await page.screenshot({ path: testInfo.outputPath('creator-mobile.png'), fullPage: true });
      await page.setViewportSize({ width: 1440, height: 1000 });
    }
    await page.getByRole('button', { name: 'Stwórz bohatera' }).click();
    await expect(page.getByRole('heading', { name: name!, exact: true })).toBeVisible();
    await page.getByRole('article', { name: new RegExp(name!) }).getByRole('link', { name: 'Wybierz postać' }).click();
    await expect(page.getByRole('heading', { name: name!, exact: true })).toBeVisible();
    await expect(page.getByRole('row', { name: /Siła/ }).getByRole('cell')).toHaveText(['11', '+0', '11']);
    await expect(page.getByRole('row', { name: /Kondycja/ }).getByRole('cell')).toHaveText(['9', '+0', '9']);
    await page.reload();
    await expect(page.getByRole('heading', { name: name!, exact: true })).toBeVisible();
    if (index === 0) await page.screenshot({ path: testInfo.outputPath('profile-desktop.png'), fullPage: true });
    await page.getByRole('link', { name: 'Wróć do postaci' }).click();
  }
  await expect(page.getByRole('article')).toHaveCount(3);
  await expect(page.getByRole('link', { name: 'Stwórz postać' })).toHaveCount(0);
  await page.screenshot({ path: testInfo.outputPath('selection-desktop.png'), fullPage: true });
  await page.setViewportSize({ width: 360, height: 800 });
  await page.screenshot({ path: testInfo.outputPath('selection-mobile.png'), fullPage: true });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.getByRole('button', { name: 'Wyloguj się' }).click();
  await expect(page.getByRole('button', { name: 'Zaloguj się' })).toBeVisible();
  await logIn(page);
  await expect(page.getByRole('link', { name: 'Wybierz postać' })).toHaveCount(3);
  await page.reload();
  await expect(page.getByRole('link', { name: 'Wybierz postać' })).toHaveCount(3);
  expect(await page.evaluate(() => Object.keys(localStorage))).toEqual([]);
  expect(await page.evaluate(() => document.cookie)).not.toContain('SESSION');
  expect(pageErrors).toEqual([]);
  await page.getByRole('button', { name: 'Wyloguj się' }).click();
  await expect(page.getByRole('button', { name: 'Zaloguj się' })).toBeVisible();
});
