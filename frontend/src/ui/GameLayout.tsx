import { useEffect, useRef, useState } from 'react';
import type { ReactNode } from 'react';
import type { AccountResponse } from '../api/contracts';
import { navigate } from '../navigation';
import { ClassEmblem } from './ClassEmblem';
import { ConnectionStatus } from './ConnectionStatus';
import { ErrorNotice } from './Feedback';

export function GameLayout({ account, route, logout, children }: {
  account: AccountResponse | null; route: string; logout: () => Promise<void>; children: ReactNode;
}) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const main = useRef<HTMLElement>(null);
  useEffect(() => { main.current?.focus(); }, [route, account?.id]);
  async function signOut() {
    setBusy(true); setError(null);
    try { await logout(); navigate('/login'); }
    catch (failure) { setError(failure); }
    finally { setBusy(false); }
  }
  return <div className="world">
    <a className="skip-link" href="#main-content" onClick={(event) => {
      event.preventDefault(); main.current?.focus();
    }}>Przejdź do treści</a>
    <header className="masthead">
      <a className="brand" href={account ? '#/characters' : '#/login'} aria-label="Steelstory — strona główna">
        <span className="brand-seal" aria-hidden="true">S</span>
        <span>STEELSTORY<small>Twoja opowieść zaczyna się tutaj</small></span>
      </a>
      <span className="masthead-note">Kronika bohaterów</span>
    </header>
    <div className={`game-frame ${account ? 'signed-in' : 'guest'}`}>
      <aside className="sidebar">
        <div className="sidebar-title">{account ? 'Twoja kronika' : 'Witaj, wędrowcze'}</div>
        <div className="sidebar-emblem"><ClassEmblem characterClass="WARRIOR" /></div>
        {account ? <>
          <p className="account-label">Konto gracza<span>{account.email}</span></p>
          <nav aria-label="Nawigacja gry">
            <a href="#/characters" aria-current={route === '/characters' ? 'page' : undefined}>◈ <span>Twoje postacie</span></a>
            {route.startsWith('/characters/') && route !== '/characters/new' &&
              <a href={`#${route}`} aria-current="page">◇ <span>Karta postaci</span></a>}
            <button type="button" disabled={busy} onClick={() => void signOut()}>↪ <span>{busy ? 'Wylogowywanie…' : 'Wyloguj się'}</span></button>
          </nav>
        </> : <p className="sidebar-story">Każda legenda ma swój początek.<br />Zapisz pierwszy rozdział własnej.</p>}
        <div className="sidebar-bottom"><span className="ornament" aria-hidden="true">— ✦ —</span><ConnectionStatus /></div>
      </aside>
      <main ref={main} id="main-content" tabIndex={-1} className="parchment">
        {error !== null && <ErrorNotice error={error} />}
        {children}
      </main>
    </div>
    <footer className="site-footer"><span>STEELSTORY</span><span>Stal w dłoni. Opowieść przed Tobą.</span></footer>
  </div>;
}
