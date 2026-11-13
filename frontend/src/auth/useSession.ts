import { useCallback, useEffect, useState } from 'react';
import { api } from '../api/client';
import type { AccountResponse, CredentialsRequest } from '../api/contracts';
import { ApiError, errorMessage } from '../api/errors';

export type Session =
  | { state: 'loading' }
  | { state: 'anonymous'; message: string }
  | { state: 'authenticated'; account: AccountResponse }
  | { state: 'error'; message: string };

export function useSession() {
  const [session, setSession] = useState<Session>({ state: 'loading' });
  const [attempt, setAttempt] = useState(0);

  useEffect(() => api.setSessionExpiredListener(() => {
    setSession({ state: 'anonymous', message: 'Sesja wygasła. Zaloguj się ponownie.' });
  }), []);

  useEffect(() => {
    let active = true;
    api.currentAccount(false).then((account) => {
      if (active) setSession({ state: 'authenticated', account });
    }).catch((error: unknown) => {
      if (!active) return;
      setSession(error instanceof ApiError && error.status === 401
        ? { state: 'anonymous', message: '' }
        : { state: 'error', message: errorMessage(error) });
    });
    return () => { active = false; };
  }, [attempt]);

  const login = useCallback(async (credentials: CredentialsRequest) => {
    try {
      const account = await api.login(credentials);
      setSession({ state: 'authenticated', account });
    } catch (error) {
      // Login may have succeeded before its response or the CSRF refresh was lost.
      if (error instanceof ApiError && error.status === 0) {
        try {
          const account = await api.currentAccount(false);
          await api.refreshCsrf();
          setSession({ state: 'authenticated', account });
          return;
        } catch { /* Keep the original failure; do not replay credentials. */ }
      }
      throw error;
    }
  }, []);

  const logout = useCallback(async () => {
    await api.logout();
    setSession({ state: 'anonymous', message: 'Wylogowano pomyślnie.' });
  }, []);

  return { session, login, logout, retry: () => {
    setSession({ state: 'loading' });
    setAttempt((value) => value + 1);
  } };
}
