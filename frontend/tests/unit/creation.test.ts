import assert from 'node:assert/strict';
import { test } from 'node:test';
import {
  adjustAllocation, creationRequest, emptyAllocation, remainingPoints, validCharacterName,
} from '../../src/characters/creation.ts';
import { validPassword } from '../../src/auth/validation.ts';

test('character names use the exact ASCII, length and internal-space rules', () => {
  for (const name of ['Ab3', 'Dark Knight', 'a'.repeat(20), 'A 1 B']) assert.ok(validCharacterName(name), name);
  for (const name of ['', 'Ab', 'a'.repeat(21), 'Łucznik', ' Aaa', 'Aaa ', 'Dark  Knight', 'A-B', 'A\nB']) {
    assert.equal(validCharacterName(name), false, name);
  }
});

test('allocation allows all ten points in one attribute, bounds controls and resets', () => {
  let allocation = emptyAllocation();
  allocation = adjustAllocation(allocation, 'strength', -1);
  assert.equal(allocation.strength, 0);
  for (let index = 0; index < 10; index++) allocation = adjustAllocation(allocation, 'strength', 1);
  assert.equal(remainingPoints(allocation), 0);
  assert.equal(adjustAllocation(allocation, 'luck', 1).luck, 0);
  allocation = adjustAllocation(allocation, 'strength', -1);
  allocation = adjustAllocation(allocation, 'luck', 1);
  assert.equal(allocation.strength, 9);
  assert.equal(allocation.luck, 1);
  allocation = emptyAllocation();
  assert.equal(remainingPoints(allocation), 10);
});

test('creation sends bonuses only and refuses incomplete, negative or fractional allocations', () => {
  const bonuses = { strength: 6, dexterity: 0, intelligence: 0, constitution: 4, luck: 0 };
  assert.deepEqual(creationRequest('Dark Knight', 'WARRIOR', bonuses), {
    name: 'Dark Knight', characterClass: 'WARRIOR', startingAllocation: bonuses,
  });
  for (const allocation of [emptyAllocation(), { ...bonuses, strength: 5 }, { ...bonuses, strength: 7 },
    { ...bonuses, strength: 11, constitution: -1 }, { ...bonuses, strength: 5.5, constitution: 4.5 }]) {
    assert.throws(() => creationRequest('Dark Knight', 'WARRIOR', allocation));
  }
});

test('password validation counts Unicode code points, preserves spaces and distinguishes login', () => {
  assert.equal(validPassword('x', false), true);
  assert.equal(validPassword('x'.repeat(14), true), false);
  assert.equal(validPassword(' '.repeat(15), true), true);
  assert.equal(validPassword('𝄞'.repeat(128), true), true);
  assert.equal(validPassword('𝄞'.repeat(129), true), false);
  assert.equal(validPassword('x'.repeat(15) + '\uD800', true), false);
  assert.equal(validPassword('', false), false);
});
