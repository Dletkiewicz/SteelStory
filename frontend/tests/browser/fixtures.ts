import type { Page } from '@playwright/test';
import type { AccountResponse, CharacterResponse } from '../../src/api/contracts';

export const account: AccountResponse = {
  id: 'test-account', email: 'synthetic@example.test', status: 'ACTIVE',
  createdAt: '2026-11-12T10:00:00Z', updatedAt: '2026-11-12T10:00:00Z',
};
export const character: CharacterResponse = {
  id: 'test-character', name: 'Test Hero', characterClass: 'WARRIOR', level: 1,
  experience: 0, unspentTrainingPoints: 0,
  startingAttributes: { strength: 11, dexterity: 5, intelligence: 5, constitution: 9, luck: 5 },
  trainedAttributes: { strength: 0, dexterity: 0, intelligence: 0, constitution: 0, luck: 0 },
  createdAt: '2026-11-12T10:00:00Z', updatedAt: '2026-11-12T10:00:00Z',
};

// Failure fixtures are test-only. Production code always calls the actual API.
export async function authenticatedPage(page: Page, characters: CharacterResponse[] = []) {
  await page.route('**/api/v1/status', (route) => route.fulfill({ json: { application: 'steelstory', status: 'UP' } }));
  await page.route('**/api/v1/auth/me', (route) => route.fulfill({ json: account }));
  await page.route('**/api/v1/auth/csrf', (route) => route.fulfill({ json: { headerName: 'X-CSRF-TOKEN', token: 'test-only' } }));
  await page.route('**/api/v1/characters', (route) => route.fulfill({ json: { characters } }));
}
