import type {
  AccountResponse, BackendStatus, CharacterListResponse, CharacterResponse,
  CreateCharacterRequest, CredentialsRequest, CsrfResponse,
} from './contracts.ts';
import { ApiError } from './errors.ts';

export class ApiClient {
  private csrf: CsrfResponse | null = null;
  private onExpired: () => void = () => {};

  setSessionExpiredListener(listener: () => void): () => void {
    this.onExpired = listener;
    return () => { this.onExpired = () => {}; };
  }

  private async request<T>(path: string, method = 'GET', body?: unknown, protectedRequest = true): Promise<T> {
    const headers: Record<string, string> = { Accept: 'application/json, application/problem+json' };
    if (method === 'POST' || method === 'DELETE') {
      if (!this.csrf) await this.refreshCsrf();
      if (!this.csrf) throw new ApiError(0, 'INVALID_RESPONSE');
      headers[this.csrf.headerName] = this.csrf.token;
    }
    if (body !== undefined) headers['Content-Type'] = 'application/json';

    let response: Response;
    try {
      response = await fetch(`/api/v1${path}`, {
        method, headers, credentials: 'same-origin', cache: 'no-store',
        signal: AbortSignal.timeout(15000),
        ...(body === undefined ? {} : { body: JSON.stringify(body) }),
      });
    } catch {
      throw new ApiError(0, 'NETWORK_ERROR');
    }

    if (!response.ok) {
      let code = 'HTTP_ERROR';
      try {
        const problem: unknown = await response.json();
        if (typeof problem === 'object' && problem !== null && 'code' in problem &&
            typeof problem.code === 'string') code = problem.code;
      } catch { /* A proxy failure may return HTML; never expose it to the player. */ }
      if (response.status === 401 || response.status === 403) this.csrf = null;
      if (response.status === 401 && protectedRequest) this.onExpired();
      // Never replay mutations: timeout/network failures may occur after a successful commit.
      throw new ApiError(response.status, code);
    }
    if (response.status === 204) return undefined as T;
    try {
      return await response.json() as T;
    } catch {
      throw new ApiError(0, 'INVALID_RESPONSE');
    }
  }

  async refreshCsrf(): Promise<void> {
    this.csrf = null;
    const token = await this.request<CsrfResponse>('/auth/csrf', 'GET', undefined, false);
    if (typeof token.headerName !== 'string' || !token.headerName ||
        typeof token.token !== 'string' || !token.token) throw new ApiError(0, 'INVALID_RESPONSE');
    this.csrf = token;
  }

  currentAccount(notifyExpired = true): Promise<AccountResponse> {
    return this.request('/auth/me', 'GET', undefined, notifyExpired);
  }
  register(credentials: CredentialsRequest): Promise<AccountResponse> {
    return this.request('/auth/register', 'POST', credentials, false);
  }
  async login(credentials: CredentialsRequest): Promise<AccountResponse> {
    const account = await this.request<AccountResponse>('/auth/login', 'POST', credentials, false);
    this.csrf = null;
    await this.refreshCsrf();
    return account;
  }
  async logout(): Promise<void> {
    await this.request<void>('/auth/logout', 'POST');
    this.csrf = null;
  }
  listCharacters(): Promise<CharacterListResponse> { return this.request('/characters'); }
  // Shared authenticated transport also used by separately bundled development tools.
  post<T>(path: string, body: unknown): Promise<T> { return this.request(path, 'POST', body); }
  getCharacter(id: string): Promise<CharacterResponse> {
    return this.request(`/characters/${encodeURIComponent(id)}`);
  }
  createCharacter(request: CreateCharacterRequest): Promise<CharacterResponse> {
    return this.request('/characters', 'POST', request);
  }
  deleteCharacter(id: string): Promise<void> {
    return this.request(`/characters/${encodeURIComponent(id)}`, 'DELETE');
  }
  async status(): Promise<BackendStatus> {
    const result = await this.request<BackendStatus>('/status', 'GET', undefined, false);
    if (result.application !== 'steelstory' || result.status !== 'UP') throw new ApiError(0, 'INVALID_RESPONSE');
    return result;
  }
}

export const api = new ApiClient();
