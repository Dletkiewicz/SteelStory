import assert from 'node:assert/strict';
import { readdir, readFile } from 'node:fs/promises';

// The local XP module must be eliminated even if VITE_DEV_XP_ENABLED=true during build.
const directory = new URL('../dist/assets/', import.meta.url);
for (const file of await readdir(directory)) {
  if (!file.endsWith('.js')) continue;
  const source = await readFile(new URL(file, directory), 'utf8');
  for (const developmentMarker of ['/dev/characters/', '/grant-xp', 'Panel developerski', 'steelstory.dev.xp.enabled']) {
    assert.ok(!source.includes(developmentMarker), `Development XP tool leaked into ${file}`);
  }
}
console.log('Production bundle excludes the development XP panel and endpoint.');
