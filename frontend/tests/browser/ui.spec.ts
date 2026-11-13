import { expect, test } from '@playwright/test';
import { authenticatedPage, character } from './fixtures';

test('development XP panel is absent without frontend opt-in', async ({ page }) => {
  await authenticatedPage(page);
  await page.route('**/api/v1/characters/test-character', (route) => route.fulfill({ json: character }));
  await page.goto('/#/characters/test-character');
  await expect(page.getByRole('heading', { name: 'Doświadczenie i rozwój' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Panel developerski — XP' })).toHaveCount(0);
});

test('deletion requires an exact name, supports Escape and refreshes slots after success', async ({ page }) => {
  await authenticatedPage(page, [character]);
  let deleted = false;
  let deletes = 0;
  await page.route('**/api/v1/characters/test-character', async (route) => {
    if (route.request().method() === 'DELETE') {
      deletes++; deleted = true;
      expect(route.request().headers()['x-csrf-token']).toBe('test-only');
      await route.fulfill({ status: 204 });
    } else await route.fulfill({ json: character });
  });
  await page.route('**/api/v1/characters', (route) => route.fulfill({ json: { characters: deleted ? [] : [character] } }));
  await page.goto('/#/characters/test-character');
  const trigger = page.getByRole('button', { name: 'Usuń postać', exact: true });
  await trigger.click();
  await expect(page.getByRole('button', { name: 'Anuluj' })).toBeFocused();
  await page.keyboard.press('Escape');
  await expect(page.getByRole('dialog')).toHaveCount(0);
  await expect(trigger).toBeFocused();
  expect(deletes).toBe(0);
  await trigger.click();
  const confirm = page.getByRole('button', { name: 'Usuń bezpowrotnie' });
  for (const wrong of ['', 'test hero', 'Test Hero ', ' Test Hero']) {
    await page.getByLabel('Wpisz dokładnie imię postaci:', { exact: false }).fill(wrong);
    await expect(confirm).toBeDisabled();
  }
  await page.getByLabel('Wpisz dokładnie imię postaci:', { exact: false }).fill('Test Hero');
  await confirm.click();
  await expect(page.getByText('Postać Test Hero została usunięta.', { exact: false })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Stwórz postać' })).toHaveCount(3);
  expect(deletes).toBe(1);
});

test('deletion 403 keeps confirmation and refreshes CSRF without repeating DELETE', async ({ page }) => {
  await authenticatedPage(page, [character]);
  let deletes = 0;
  await page.route('**/api/v1/characters/test-character', (route) => {
    if (route.request().method() === 'DELETE') {
      deletes++;
      return route.fulfill({ status: 403, json: { code: 'ACCESS_DENIED' } });
    }
    return route.fulfill({ json: character });
  });
  await page.goto('/#/characters/test-character');
  await page.getByRole('button', { name: 'Usuń postać', exact: true }).click();
  await page.getByLabel('Wpisz dokładnie imię postaci:', { exact: false }).fill(character.name);
  await page.getByRole('button', { name: 'Usuń bezpowrotnie' }).click();
  await page.getByRole('button', { name: 'Odśwież sesję' }).click();
  await expect(page.getByText('Sesja odświeżona.', { exact: false })).toBeVisible();
  expect(deletes).toBe(1);
  await expect(page.getByLabel('Wpisz dokładnie imię postaci:', { exact: false })).toHaveValue(character.name);
});

test('a lost deletion response offers a read-only check and reconciles a missing character', async ({ page }) => {
  await authenticatedPage(page, [character]);
  let deletes = 0;
  await page.route('**/api/v1/characters/test-character', (route) => {
    if (route.request().method() === 'DELETE') { deletes++; return route.abort('failed'); }
    return deletes ? route.fulfill({ status: 404, json: { code: 'CHARACTER_NOT_FOUND' } })
      : route.fulfill({ json: character });
  });
  await page.route('**/api/v1/characters', (route) => route.fulfill({ json: { characters: [] } }));
  await page.goto('/#/characters/test-character');
  await page.getByRole('button', { name: 'Usuń postać', exact: true }).click();
  await page.getByLabel('Wpisz dokładnie imię postaci:', { exact: false }).fill(character.name);
  await page.getByRole('button', { name: 'Usuń bezpowrotnie' }).click();
  await expect(page.getByRole('button', { name: 'Usuń bezpowrotnie' })).toBeDisabled();
  await page.getByRole('button', { name: 'Sprawdź stan postaci' }).click();
  await expect(page.getByText('Postać nie jest już dostępna. Lista postaci została odświeżona.')).toBeVisible();
  await expect(page.getByRole('link', { name: 'Stwórz postać' })).toHaveCount(3);
  expect(deletes).toBe(1);
});

for (const status of [401, 404]) {
  test(`deletion ${status} leaves no actionable stale profile`, async ({ page }) => {
    await authenticatedPage(page, [character]);
    await page.route('**/api/v1/characters/test-character', (route) => route.request().method() === 'DELETE'
      ? route.fulfill({ status, json: { code: status === 401 ? 'UNAUTHENTICATED' : 'CHARACTER_NOT_FOUND' } })
      : route.fulfill({ json: character }));
    await page.goto('/#/characters/test-character');
    await page.getByRole('button', { name: 'Usuń postać', exact: true }).click();
    await page.getByLabel('Wpisz dokładnie imię postaci:', { exact: false }).fill(character.name);
    await page.getByRole('button', { name: 'Usuń bezpowrotnie' }).click();
    if (status === 401) {
      await expect(page.getByRole('button', { name: 'Zaloguj się' })).toBeVisible();
      await expect(page.getByRole('dialog')).toHaveCount(0);
    } else {
      await expect(page.getByRole('button', { name: 'Usuń bezpowrotnie' })).toHaveCount(0);
      await page.getByRole('button', { name: 'Wróć do postaci' }).click();
      await expect(page.getByRole('heading', { name: 'Twoje postacie' })).toBeVisible();
    }
  });
}

test('all six creation portraits load and switching appearance preserves class and allocation', async ({ page }, testInfo) => {
  await authenticatedPage(page);
  await page.goto('/#/characters/new');
  await page.getByRole('radio', { name: /Strzelec/ }).check();
  await page.getByRole('button', { name: 'Dodaj: Zręczność', exact: true }).click();
  for (const [label, gender] of [['Kobieta', 'female'], ['Mężczyzna', 'male']] as const) {
    await page.getByRole('radio', { name: label, exact: true }).check();
    for (const [index, characterClass] of ['warrior', 'archer', 'mage'].entries()) {
      const portrait = page.locator('.class-choice .character-portrait').nth(index);
      await expect(portrait).toHaveAttribute('src', `/portraits/${characterClass}/${gender}/tier-1.webp`);
      await portrait.evaluate(async (image: HTMLImageElement) => { await image.decode(); });
      expect(await portrait.evaluate((image: HTMLImageElement) => [image.naturalWidth, image.naturalHeight]))
        .toEqual([600, 800]);
    }
    await expect(page.getByRole('radio', { name: /Strzelec/ })).toBeChecked();
    await expect(page.locator('#value-dexterity')).toHaveText('6');
    await page.screenshot({ path: testInfo.outputPath(`portraits-${gender}-desktop.png`), fullPage: true });
  }
  await page.setViewportSize({ width: 360, height: 800 });
  await page.getByRole('radio', { name: 'Kobieta', exact: true }).check();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.screenshot({ path: testInfo.outputPath('portraits-female-mobile.png'), fullPage: true });
});

test('class portraits load locally and mobile confirmation is visible without overflow', async ({ page }, testInfo) => {
  await authenticatedPage(page, [character, { ...character, id: 'archer', characterClass: 'ARCHER' },
    { ...character, id: 'mage', characterClass: 'MAGE', appearanceGender: 'FEMALE' }]);
  await page.route('**/api/v1/characters/test-character', (route) => route.fulfill({ json: character }));
  await page.goto('/');
  await expect(page.locator('.character-portrait')).toHaveCount(3);
  await page.locator('.character-portrait').evaluateAll(async (images) => {
    await Promise.all(images.map((image) => (image as HTMLImageElement).decode()));
  });
  for (const card of await page.getByRole('article').all()) {
    const image = await card.getByRole('img').boundingBox();
    const heading = await card.getByRole('heading').boundingBox();
    expect(image).not.toBeNull();
    expect(heading).not.toBeNull();
    expect(image!.y + image!.height).toBeLessThanOrEqual(heading!.y);
  }
  await page.setViewportSize({ width: 360, height: 800 });
  await page.goto('/#/characters/test-character');
  const portrait = page.getByRole('img', { name: 'Portret klasy: Wojownik' });
  await expect(portrait).toBeVisible();
  await expect.poll(() => portrait.evaluate((element) => (element as HTMLImageElement).naturalWidth)).toBeGreaterThan(0);
  await page.getByRole('button', { name: 'Usuń postać', exact: true }).click();
  await expect(page.getByRole('dialog')).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.screenshot({ path: testInfo.outputPath('deletion-mobile.png') });
});

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
  await page.getByRole('radio', { name: 'Kobieta', exact: true }).check();
  await expect(page.locator('#value-strength')).toHaveText('11');
  await expect(page.getByRole('button', { name: 'Dodaj: Siła', exact: true })).toBeDisabled();
  let payload: unknown;
  await page.route('**/api/v1/characters', async (route) => {
    if (route.request().method() === 'POST') {
      payload = route.request().postDataJSON() as unknown;
      await route.fulfill({ status: 201, json: { ...character, name: 'Dark Knight', characterClass: 'MAGE', appearanceGender: 'FEMALE' } });
    } else await route.fulfill({ json: { characters: [{ ...character, name: 'Dark Knight', characterClass: 'MAGE', appearanceGender: 'FEMALE' }] } });
  });
  await page.getByRole('button', { name: 'Stwórz bohatera' }).click();
  await expect(page.getByText('Postać Dark Knight została zapisana.', { exact: false })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Dark Knight' })).toBeVisible();
  expect(payload).toEqual({ name: 'Dark Knight', characterClass: 'MAGE', appearanceGender: 'FEMALE',
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
