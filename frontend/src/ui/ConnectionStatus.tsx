import { useEffect, useState } from 'react';
import { api } from '../api/client';

export function ConnectionStatus() {
  const [status, setStatus] = useState<'checking' | 'online' | 'offline'>('checking');
  const [attempt, setAttempt] = useState(0);
  useEffect(() => {
    let active = true;
    api.status().then(() => { if (active) setStatus('online'); })
      .catch(() => { if (active) setStatus('offline'); });
    return () => { active = false; };
  }, [attempt]);
  return <div className="connection">
    <span className={`connection-dot ${status}`} aria-hidden="true" />
    <span role="status">{status === 'online' ? 'Serwer dostępny' : status === 'offline' ? 'Brak połączenia' : 'Łączenie…'}</span>
    <button type="button" disabled={status === 'checking'} onClick={() => {
      setStatus('checking'); setAttempt((value) => value + 1);
    }} aria-label="Sprawdź połączenie z serwerem">↻</button>
  </div>;
}
