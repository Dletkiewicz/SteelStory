import { expect, test } from '@playwright/test';
import type { Page } from '@playwright/test';

test('real browser: opt-in XP grants persist, cross portrait boundaries and stop at level 200', async ({ page }, testInfo) => {
  test.setTimeout(90000);
  const identity = Date.now().toString(36);
  await page.goto('http://127.0.0.1:5174/');
  await page.getByRole('link', { name: 'Załóż konto' }).click();
  await expect(page.getByRole('heading', { name: 'Początek opowieści' })).toBeVisible();
  await page.getByLabel('Adres e-mail').fill(`xp-${identity}@example.test`);
  await page.getByLabel('Hasło', { exact: true }).fill('Synthetic XP test passphrase');
  await page.getByLabel('Powtórz hasło').fill('Synthetic XP test passphrase');
  await page.getByRole('button', { name: 'Załóż konto' }).click();
  await page.getByRole('link', { name: 'Przejdź do logowania' }).click();
  await expect(page.getByRole('heading', { name: 'Wróć do swojej opowieści' })).toBeVisible();
  await page.getByLabel('Adres e-mail').fill(`xp-${identity}@example.test`);
  await page.getByLabel('Hasło', { exact: true }).fill('Synthetic XP test passphrase');
  await page.getByRole('button', { name: 'Zaloguj się' }).click();
  await page.getByRole('link', { name: 'Stwórz postać' }).first().click();
  await page.getByLabel('Imię postaci', { exact: true }).fill(`Novice ${identity}`);
  await page.getByRole('radio', { name: /Mag/ }).check();
  await page.getByRole('radio', { name: 'Kobieta', exact: true }).check();
  for (let point = 0; point < 10; point++) await page.getByRole('button', { name: 'Dodaj: Inteligencja', exact: true }).click();
  await page.getByRole('button', { name: 'Stwórz bohatera' }).click();
  await page.getByRole('link', { name: 'Wybierz postać' }).click();
  await expect(page.getByRole('heading', { name: 'Panel developerski — XP' })).toBeVisible();
  for (const [amount, level, points] of [[500, 3, 6], [246500, 20, 57], [40000, 21, 60],
    [1767000, 40, 117], [160000, 41, 120], [1000000000, 200, 597]] as const) {
    await page.getByLabel('Własna nagroda XP', { exact: false }).fill(String(amount));
    await page.getByRole('button', { name: 'Przyznaj XP', exact: true }).click();
    await expect(page.getByText(`Awans! Poziom ${level}.`, { exact: false })).toBeVisible();
    await expect(page.getByText('Dostępne punkty nauki:', { exact: false })).toContainText(String(points));
    const portrait = page.locator('.profile-portrait .character-portrait');
    await expect(portrait).toHaveAttribute('src', '/portraits/mage/female/tier-1.webp');
    await portrait.evaluate(async (image: HTMLImageElement) => { await image.decode(); });
    if (level === 3) await page.screenshot({ path: testInfo.outputPath('progression-desktop.png'), fullPage: true });
  }
  await page.getByRole('button', { name: 'Przyznaj 100 XP', exact: true }).click();
  await expect(page.getByText('Poziom maksymalny — nie przyznano', { exact: false })).toBeVisible();
  await page.reload();
  await expect(page.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '100');
  await expect(page.getByText('Dostępne punkty nauki:', { exact: false })).toContainText('597');
  await expect(page.getByRole('row', { name: /Inteligencja/ }).getByRole('cell')).toHaveText(['15', '+0', '15']);
  await page.getByRole('link', { name: 'Wróć do postaci' }).click();
  await expect(page.getByRole('article')).toContainText(['Poziom 200', 'Wolne', 'Wolne']);
  await page.getByRole('button', { name: 'Wyloguj się' }).click();
});

// Requires the actual backend local profile and PostgreSQL. No intercepted API responses.
// Each run retains synthetic accounts and character history, including the soft-deleted row.
test('real browser: create heroes, delete one, reserve its name and refill the slot', async ({ page }, testInfo) => {
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
    const appearance = index === 1 ? 'MALE' : 'FEMALE';
    await page.getByRole('radio', { name: appearance === 'MALE' ? 'Mężczyzna' : 'Kobieta', exact: true }).check();
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
    const creationResponse = page.waitForResponse((response) => response.url().endsWith('/api/v1/characters')
      && response.request().method() === 'POST');
    await page.getByRole('button', { name: 'Stwórz bohatera' }).click();
    expect((await (await creationResponse).json()).appearanceGender).toBe(appearance);
    await expect(page.getByRole('heading', { name: name!, exact: true })).toBeVisible();
    await page.getByRole('article', { name: new RegExp(name!) }).getByRole('link', { name: 'Wybierz postać' }).click();
    await expect(page.getByRole('heading', { name: name!, exact: true })).toBeVisible();
    await expect(page.getByRole('row', { name: /Siła/ }).getByRole('cell')).toHaveText(['11', '+0', '11']);
    await expect(page.getByRole('row', { name: /Kondycja/ }).getByRole('cell')).toHaveText(['9', '+0', '9']);
    await page.reload();
    await expect(page.getByRole('heading', { name: name!, exact: true })).toBeVisible();
    const portrait = page.locator('.profile-portrait .character-portrait');
    await expect(portrait).toHaveAttribute('src',
      `/portraits/${['warrior', 'archer', 'mage'][index]}/${appearance.toLowerCase()}/tier-1.webp`);
    await portrait.evaluate(async (image: HTMLImageElement) => { await image.decode(); });
    if (index === 0) await page.screenshot({ path: testInfo.outputPath('profile-desktop.png'), fullPage: true });
    await page.getByRole('link', { name: 'Wróć do postaci' }).click();
  }
  await expect(page.getByRole('article')).toHaveCount(3);
  await expect(page.getByRole('link', { name: 'Stwórz postać' })).toHaveCount(0);
  await page.locator('.character-portrait').evaluateAll(async (images) => {
    await Promise.all(images.map((image) => (image as HTMLImageElement).decode()));
  });
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
  await page.getByRole('link', { name: 'Wybierz postać' }).first().click();
  await expect(page.getByRole('heading', { name: names[0]!, exact: true })).toBeVisible();
  const deletedProfileUrl = page.url();
  await page.getByRole('button', { name: 'Usuń postać', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Usuń bezpowrotnie' })).toBeDisabled();
  await page.getByLabel('Wpisz dokładnie imię postaci:', { exact: false }).fill(names[0]!);
  await page.screenshot({ path: testInfo.outputPath('deletion-mobile.png') });
  await page.getByRole('button', { name: 'Usuń bezpowrotnie' }).click();
  await expect(page.getByText(`Postać ${names[0]} została usunięta.`, { exact: false })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Stwórz postać' })).toHaveCount(1);
  await page.goto(deletedProfileUrl);
  await expect(page.getByRole('alert')).toContainText('Nie znaleziono tej postaci');
  await page.getByRole('link', { name: 'Wróć do postaci' }).click();
  await page.getByRole('link', { name: 'Stwórz postać' }).click();
  await page.getByLabel('Imię postaci', { exact: true }).fill(names[0]!.toUpperCase());
  for (let point = 0; point < 10; point++) await page.getByRole('button', { name: 'Dodaj: Siła', exact: true }).click();
  await page.getByRole('button', { name: 'Stwórz bohatera' }).click();
  await expect(page.getByRole('alert')).toContainText('Ta nazwa jest już zajęta');
  await page.getByLabel('Imię postaci', { exact: true }).fill(`Reborn ${identity}`);
  await page.getByRole('button', { name: 'Stwórz bohatera' }).click();
  await expect(page.getByRole('link', { name: 'Wybierz postać' })).toHaveCount(3);
  await expect(page.getByRole('link', { name: 'Stwórz postać' })).toHaveCount(0);
  await page.reload();
  await expect(page.getByRole('heading', { name: `Reborn ${identity}` })).toBeVisible();
  expect(await page.evaluate(() => Object.keys(localStorage))).toEqual([]);
  expect(await page.evaluate(() => document.cookie)).not.toContain('SESSION');
  expect(pageErrors).toEqual([]);
  await page.getByRole('button', { name: 'Wyloguj się' }).click();
  await expect(page.getByRole('button', { name: 'Zaloguj się' })).toBeVisible();
});
