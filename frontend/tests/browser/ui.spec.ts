import { expect, test } from '@playwright/test';
import { authenticatedPage, character } from './fixtures';

test('first anonymous visit has no expired-session message and registration validates confirmation', async ({ page }) => {
  await authenticatedPage(page);
  await page.route('**/api/v1/auth/me', (route) => route.fulfill({ status: 401, json: { code: 'UNAUTHENTICATED' } }));
  await page.goto('/');
  await expect(page.getByRole('button', { name: 'Zaloguj się' })).toBeVisible();
  await expect(page.getByText('Sesja wygasła. Zaloguj się ponownie.', { exact: true })).toHaveCount(0);
  await page.getByRole('link', { name: 'Załóż konto' }).click();
  await page.getByLabel('Adres e-mail').fill('synthetic@example.test');
  await page.getByLabel('Hasło', { exact: true }).fill('Synthetic test passphrase');
  await page.getByLabel('Powtórz hasło').fill('Different test passphrase');
  await page.getByRole('button', { name: 'Załóż konto' }).click();
  await expect(page.getByRole('alert')).toContainText('Podane hasła nie są identyczne');
  await page.getByRole('button', { name: 'Pokaż hasło' }).click();
  await expect(page.getByLabel('Hasło', { exact: true })).toHaveAttribute('type', 'text');
});

test('creation conflict keeps the draft, and explicit CSRF recovery does not submit it', async ({ page }) => {
  await authenticatedPage(page);
  await page.goto('/#/characters/new');
  await page.getByLabel('Imię postaci', { exact: true }).fill('Taken Hero');
  for (let i = 0; i < 10; i++) await page.getByRole('button', { name: 'Dodaj: Siła', exact: true }).click();
  let posts = 0;
  await page.route('**/api/v1/characters', (route) => {
    posts++;
    return route.fulfill({ status: posts === 1 ? 409 : 403,
      json: { code: posts === 1 ? 'CHARACTER_NAME_TAKEN' : 'ACCESS_DENIED' } });
  });
  await page.getByRole('button', { name: 'Stwórz bohatera' }).click();
  await expect(page.getByRole('alert')).toContainText('Ta nazwa jest już zajęta');
  await expect(page.locator('#value-strength')).toHaveText('15');
  await page.getByLabel('Imię postaci', { exact: true }).fill('Another Hero');
  await page.getByRole('button', { name: 'Stwórz bohatera' }).click();
  await page.getByRole('button', { name: 'Odśwież sesję' }).click();
  await expect(page.getByText('Sesja odświeżona.', { exact: false })).toBeVisible();
  expect(posts).toBe(2);
  await expect(page.getByLabel('Imię postaci', { exact: true })).toHaveValue('Another Hero');
});

test('selection renders exactly three slots and never offers a fourth', async ({ page }) => {
  await authenticatedPage(page);
  await page.goto('/');
  await expect(page.getByRole('article')).toHaveCount(3);
  await expect(page.getByRole('link', { name: 'Stwórz postać' })).toHaveCount(3);
  await page.route('**/api/v1/characters', (route) => route.fulfill({ json: {
    characters: [character, { ...character, id: 'two', name: 'Second Hero' }, { ...character, id: 'three', name: 'Third Hero' }],
  } }));
  await page.getByRole('button', { name: 'Odśwież listę' }).click();
  await expect(page.getByRole('link', { name: 'Wybierz postać' })).toHaveCount(3);
  await expect(page.getByRole('link', { name: 'Stwórz postać' })).toHaveCount(0);
  await page.goto('/#/characters/new');
  await expect(page.getByRole('heading', { name: 'Wszystkie miejsca są zajęte' })).toBeVisible();
});

test('allocator resets, changes class without reallocating and sends the bonus-only payload', async ({ page }) => {
  await authenticatedPage(page);
  await page.goto('/#/characters/new');
  await page.getByLabel('Imię postaci', { exact: true }).fill('Dark Knight');
  await expect(page.getByRole('button', { name: 'Odejmij: Siła', exact: true })).toBeDisabled();
  await page.getByRole('button', { name: 'Dodaj: Siła', exact: true }).click();
  await expect(page.locator('#value-strength')).toHaveText('6');
  await page.getByRole('button', { name: 'Wyzeruj przydział' }).click();
  await expect(page.locator('#value-strength')).toHaveText('5');
  for (let i = 0; i < 6; i++) await page.getByRole('button', { name: 'Dodaj: Siła', exact: true }).click();
  for (let i = 0; i < 4; i++) await page.getByRole('button', { name: 'Dodaj: Kondycja', exact: true }).click();
  await page.getByRole('radio', { name: /Mag/ }).check();
  await expect(page.locator('#value-strength')).toHaveText('11');
  await expect(page.getByRole('button', { name: 'Dodaj: Siła', exact: true })).toBeDisabled();
  let payload: unknown;
  await page.route('**/api/v1/characters', async (route) => {
    if (route.request().method() === 'POST') {
      payload = route.request().postDataJSON() as unknown;
      await route.fulfill({ status: 201, json: { ...character, name: 'Dark Knight', characterClass: 'MAGE' } });
    } else await route.fulfill({ json: { characters: [{ ...character, name: 'Dark Knight', characterClass: 'MAGE' }] } });
  });
  await page.getByRole('button', { name: 'Stwórz bohatera' }).click();
  await expect(page.getByText('Postać Dark Knight została zapisana.', { exact: false })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Dark Knight' })).toBeVisible();
  expect(payload).toEqual({ name: 'Dark Knight', characterClass: 'MAGE',
    startingAllocation: { strength: 6, dexterity: 0, intelligence: 0, constitution: 4, luck: 0 } });
});

test('uncertain creation disables resubmission until the player reconciles the list', async ({ page }) => {
  await authenticatedPage(page);
  await page.goto('/#/characters/new');
  await page.getByLabel('Imię postaci', { exact: true }).fill('Lost Reply');
  for (let i = 0; i < 10; i++) await page.getByRole('button', { name: 'Dodaj: Siła', exact: true }).click();
  let posts = 0;
  await page.route('**/api/v1/characters', async (route) => {
    if (route.request().method() === 'POST') { posts++; await route.abort('failed'); }
    else await route.fulfill({ json: { characters: [{ ...character, name: 'Lost Reply' }] } });
  });
  await page.getByRole('button', { name: 'Stwórz bohatera' }).click();
  await expect(page.getByText('Wynik tworzenia jest nieznany.', { exact: false })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Stwórz bohatera' })).toBeDisabled();
  expect(posts).toBe(1);
  await page.getByRole('link', { name: 'Sprawdź moje postacie' }).click();
  await expect(page.getByRole('heading', { name: 'Lost Reply' })).toBeVisible();
});

test('list failure can be retried and expired sessions remove protected UI', async ({ page }) => {
  await authenticatedPage(page);
  await page.route('**/api/v1/characters', (route) => route.fulfill({ status: 503, json: { code: 'INTERNAL_ERROR' } }));
  await page.goto('/');
  await expect(page.getByRole('alert')).toContainText('Serwer jest chwilowo niedostępny');
  await page.route('**/api/v1/characters', (route) => route.fulfill({ json: { characters: [character] } }));
  await page.getByRole('button', { name: 'Spróbuj ponownie' }).click();
  await expect(page.getByRole('heading', { name: 'Test Hero' })).toBeVisible();
  await page.route('**/api/v1/characters', (route) => route.fulfill({ status: 401, json: { code: 'UNAUTHENTICATED' } }));
  await page.getByRole('button', { name: 'Odśwież listę' }).click();
  await expect(page.getByRole('button', { name: 'Zaloguj się' })).toBeVisible();
  await expect(page.getByText('Sesja wygasła. Zaloguj się ponownie.', { exact: true })).toBeVisible();
  await expect(page.getByRole('article')).toHaveCount(0);
});

test('logout errors preserve authenticated state and 403 recovery never repeats logout', async ({ page }) => {
  await authenticatedPage(page, [character]);
  let posts = 0;
  await page.route('**/api/v1/auth/logout', (route) => {
    posts++; return route.fulfill({ status: 403, json: { code: 'ACCESS_DENIED' } });
  });
  await page.goto('/');
  await page.getByRole('button', { name: 'Wyloguj się' }).click();
  await page.getByRole('button', { name: 'Odśwież sesję' }).click();
  await expect(page.getByText('Sesja odświeżona.', { exact: false })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Test Hero' })).toBeVisible();
  expect(posts).toBe(1);
});

test('mobile creator and character sheet stay within a 360px viewport', async ({ page }) => {
  await page.setViewportSize({ width: 360, height: 800 });
  await authenticatedPage(page, [character]);
  await page.route('**/api/v1/characters/test-character', (route) => route.fulfill({ json: character }));
  for (const route of ['/', '/#/characters/new', '/#/characters/test-character']) {
    await page.goto(route);
    await expect(page.locator('h1')).toBeVisible();
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
  }
  await expect(page.getByRole('row', { name: /Siła/ })).toContainText('11');
});
