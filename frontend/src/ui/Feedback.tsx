import { useState } from 'react';
import { api } from '../api/client';
import { ApiError, errorMessage } from '../api/errors';

export function Loading({ children = 'Odczytywanie kroniki…' }: { children?: string }) {
  return <div className="loading" role="status"><span className="loading-seal" aria-hidden="true">✦</span>{children}</div>;
}

export function ErrorNotice({ error, retry }: { error: unknown; retry?: () => void }) {
  const [refresh, setRefresh] = useState<'idle' | 'pending' | 'done' | 'failed'>('idle');
  async function refreshSession() {
    setRefresh('pending');
    try {
      await api.currentAccount();
      await api.refreshCsrf();
      setRefresh('done');
    } catch { setRefresh('failed'); }
  }
  return <div className="notice error" role="alert">
    <p>{errorMessage(error)}</p>
    {error instanceof ApiError && error.status === 403 && <>
      <button type="button" className="button secondary" disabled={refresh === 'pending'}
        onClick={() => void refreshSession()}>Odśwież sesję</button>
      {refresh === 'done' && <p>Sesja odświeżona. Możesz ponownie zatwierdzić formularz.</p>}
      {refresh === 'failed' && <p>Nie udało się odświeżyć sesji. Sprawdź połączenie.</p>}
    </>}
    {retry && <button type="button" className="button secondary" onClick={retry}>Spróbuj ponownie</button>}
  </div>;
}
