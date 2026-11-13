import assert from 'node:assert/strict';
import { afterEach, test, mock } from 'node:test';
import { ApiClient } from '../../src/api/client.ts';
import { ApiError, errorMessage } from '../../src/api/errors.ts';

afterEach(() => mock.restoreAll());

test('DELETE obtains CSRF, uses cookies, encodes the ID and accepts an empty 204', async () => {
  const calls: { path: string; options: RequestInit | undefined }[] = [];
  mock.method(globalThis, 'fetch', async (path: string, options?: RequestInit) => {
    calls.push({ path, options });
    return path.endsWith('/csrf') ? json({ headerName: 'X-CSRF-TOKEN', token: 'delete-test' })
      : new Response(null, { status: 204 });
  });
  assert.equal(await new ApiClient().deleteCharacter('character/id'), undefined);
  assert.equal(calls.length, 2);
  assert.equal(calls[1]?.path, '/api/v1/characters/character%2Fid');
  assert.equal(calls[1]?.options?.method, 'DELETE');
  assert.equal(calls[1]?.options?.credentials, 'same-origin');
  assert.equal(calls[1]?.options?.body, undefined);
  assert.equal(new Headers(calls[1]?.options?.headers).get('X-CSRF-TOKEN'), 'delete-test');
});

test('DELETE never replays after network failure and maps an already missing character to 404', async () => {
  let deletes = 0;
  mock.method(globalThis, 'fetch', async (path: string) => {
    if (path.endsWith('/csrf')) return json({ headerName: 'X-CSRF-TOKEN', token: 'delete-test' });
    deletes++;
    if (deletes === 1) throw new TypeError('lost response');
    return json({ code: 'CHARACTER_NOT_FOUND' }, 404);
  });
  const client = new ApiClient();
  await assert.rejects(client.deleteCharacter('id'), (error: unknown) => error instanceof ApiError && error.status === 0);
  assert.equal(deletes, 1);
  await assert.rejects(client.deleteCharacter('id'), (error: unknown) => error instanceof ApiError && error.status === 404);
  assert.equal(deletes, 2);
});
function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
}

test('login fetches CSRF, sends cookies and the returned header, rotates CSRF, and logout accepts 204', async () => {
  const responses = [json({ headerName: 'X-CSRF-TOKEN', token: 'before' }), json({ id: 'account' }),
    json({ headerName: 'X-CSRF-TOKEN', token: 'after' }), new Response(null, { status: 204 }),
    json({ headerName: 'X-CSRF-TOKEN', token: 'anonymous' }), json({ id: 'registered' }, 201)];
  const calls: { path: string; options: RequestInit | undefined }[] = [];
  mock.method(globalThis, 'fetch', async (path: string, options?: RequestInit) => {
    calls.push({ path, options });
    const response = responses.shift();
    assert.ok(response); return response;
  });
  const client = new ApiClient();
  await client.login({ email: 'a@example.test', password: 'test' });
  assert.equal(await client.logout(), undefined);
  await client.register({ email: 'b@example.test', password: 'long synthetic password' });
  assert.deepEqual(calls.map((call) => call.path), ['/api/v1/auth/csrf', '/api/v1/auth/login',
    '/api/v1/auth/csrf', '/api/v1/auth/logout', '/api/v1/auth/csrf', '/api/v1/auth/register']);
  assert.equal(new Headers(calls[1]?.options?.headers).get('X-CSRF-TOKEN'), 'before');
  assert.equal(new Headers(calls[3]?.options?.headers).get('X-CSRF-TOKEN'), 'after');
  assert.ok(calls.every((call) => call.options?.credentials === 'same-origin'));
});

test('403 clears cached CSRF and never automatically replays a mutation', async () => {
  let posts = 0;
  let tokens = 0;
  mock.method(globalThis, 'fetch', async (path: string) => {
    if (path.endsWith('/csrf')) return json({ headerName: 'X-Contract-Header', token: String(++tokens) });
    posts++; return json({ code: 'ACCESS_DENIED', detail: 'never display raw detail' }, 403);
  });
  const client = new ApiClient();
  await assert.rejects(client.logout(), (error: unknown) => error instanceof ApiError && error.status === 403);
  assert.equal(posts, 1);
  await assert.rejects(client.logout());
  assert.equal(tokens, 2);
  assert.equal(posts, 2);
});

test('401 expires a protected session but invalid login credentials do not emit expiry', async () => {
  let expired = 0;
  mock.method(globalThis, 'fetch', async (path: string) => path.endsWith('/csrf')
    ? json({ headerName: 'X-CSRF-TOKEN', token: 'synthetic' }) : json({ code: 'INVALID_CREDENTIALS' }, 401));
  const client = new ApiClient();
  client.setSessionExpiredListener(() => expired++);
  await assert.rejects(client.login({ email: 'a@example.test', password: 'incorrect' }));
  assert.equal(expired, 0);
  await assert.rejects(client.listCharacters());
  assert.equal(expired, 1);
});

test('network failure during a POST does not retry and reports an uncertain outcome', async () => {
  let posts = 0;
  mock.method(globalThis, 'fetch', async (path: string) => {
    if (path.endsWith('/csrf')) return json({ headerName: 'X-CSRF-TOKEN', token: 'synthetic' });
    posts++; throw new TypeError('failed fetch');
  });
  await assert.rejects(new ApiClient().createCharacter({ name: 'Test Hero', characterClass: 'MAGE', appearanceGender: 'FEMALE',
    startingAllocation: { strength: 0, dexterity: 0, intelligence: 10, constitution: 0, luck: 0 } }),
  (error: unknown) => error instanceof ApiError && error.status === 0);
  assert.equal(posts, 1);
});

test('problem codes map to safe Polish messages, and HTML proxy failures stay sanitized', async () => {
  for (const code of ['CHARACTER_NAME_TAKEN', 'CHARACTER_LIMIT_REACHED', 'INVALID_CHARACTER_INPUT', 'INVALID_CREDENTIALS']) {
    assert.ok(!errorMessage(new ApiError(409, code)).includes(code));
  }
  mock.method(globalThis, 'fetch', async () => new Response('<html>private diagnostic</html>', { status: 502 }));
  await assert.rejects(new ApiClient().listCharacters(), (error: unknown) => {
    assert.ok(error instanceof ApiError);
    assert.equal(error.status, 502);
    assert.ok(!errorMessage(error).includes('private'));
    return true;
  });
});
