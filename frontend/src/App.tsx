import { useEffect, useState } from 'react';
import { fetchStatus } from './api';

type Connection = { state: 'checking' } | { state: 'connected' } | { state: 'failed'; message: string };

export default function App() {
  const [connection, setConnection] = useState<Connection>({ state: 'checking' });
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    const timeout = window.setTimeout(() => controller.abort('timeout'), 10000);
    setConnection({ state: 'checking' });
    fetchStatus(controller.signal)
      .then(() => setConnection({ state: 'connected' }))
      .catch((error: unknown) => {
        if (controller.signal.aborted && controller.signal.reason !== 'timeout') return;
        setConnection({ state: 'failed', message: error instanceof Error ? error.message : 'Connection failed' });
      });
    return () => { window.clearTimeout(timeout); controller.abort(); };
  }, [attempt]);

  return (
    <main className="min-h-screen bg-slate-950 px-6 py-20 text-slate-100">
      <section className="mx-auto max-w-xl space-y-6">
        <p className="text-sm uppercase tracking-widest text-slate-400">Foundation</p>
        <h1 className="text-4xl font-semibold">Steelstory</h1>
        <p className="text-slate-300">The beginning of an asynchronous multiplayer RPG.</p>
        <div role="status" aria-live="polite" className="rounded-lg border border-slate-700 p-5">
          {connection.state === 'checking' && 'Checking backend connection…'}
          {connection.state === 'connected' && 'Backend connected.'}
          {connection.state === 'failed' && `Backend unavailable. ${connection.message}`}
        </div>
        <button type="button" disabled={connection.state === 'checking'}
          onClick={() => setAttempt((value) => value + 1)}
          className="rounded bg-slate-200 px-4 py-2 text-slate-950 disabled:opacity-50 focus-visible:outline-2 focus-visible:outline-offset-4">
          Check connection
        </button>
      </section>
    </main>
  );
}
