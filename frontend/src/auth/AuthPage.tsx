import { useState } from 'react';
import type { FormEvent } from 'react';
import { api } from '../api/client';
import type { CredentialsRequest } from '../api/contracts';
import { ApiError } from '../api/errors';
import { navigate } from '../navigation';
import { ErrorNotice } from '../ui/Feedback';
import { validPassword } from './validation';

export function AuthPage({ registration, message, login }: {
  registration: boolean; message: string; login: (credentials: CredentialsRequest) => Promise<void>;
}) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmation, setConfirmation] = useState('');
  const [visible, setVisible] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [validation, setValidation] = useState('');
  const [registered, setRegistered] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    setValidation(''); setError(null);
    if (!validPassword(password, registration)) {
      setValidation(`Hasło musi zawierać od ${registration ? 15 : 1} do 128 znaków Unicode.`); return;
    }
    if (registration && password !== confirmation) {
      setValidation('Podane hasła nie są identyczne.'); return;
    }
    setBusy(true);
    try {
      if (registration) {
        await api.register({ email, password });
        setRegistered(true); setPassword(''); setConfirmation('');
      } else {
        await login({ email, password });
        // Preserve a bookmarked owned-character profile across authentication.
        if (!window.location.hash.startsWith('#/characters')) navigate('/characters');
      }
    } catch (failure) { setError(failure); }
    finally { setBusy(false); }
  }
  return <div className="auth-page">
    <div className="page-heading"><p className="eyebrow">Księga wędrowców</p>
      <h1>{registration ? 'Początek opowieści' : 'Wróć do swojej opowieści'}</h1>
      <p>{registration ? 'Załóż konto i stwórz swoich bohaterów.' : 'Twoi bohaterowie czekają na kolejny rozdział.'}</p>
    </div>
    <div className="auth-panel panel">
      <div className="panel-heading">{registration ? 'Rejestracja' : 'Logowanie'}</div>
      {registered ? <div className="panel-body">
        <div className="notice success" role="status">Konto utworzone. Zaloguj się, aby wybrać swoją pierwszą postać.</div>
        <a className="button" href="#/login">Przejdź do logowania</a>
      </div> : <form className="panel-body form-stack" onSubmit={(event) => void submit(event)}>
        {message && <p className="notice" role="status">{message}</p>}
        {error !== null && <ErrorNotice error={error} />}
        {error instanceof ApiError && error.status === 0 && <p className="field-hint">
          {registration ? 'Konto mogło zostać utworzone. Spróbuj się zalogować przed ponowną rejestracją.'
            : 'Możesz odświeżyć stronę, aby sprawdzić stan zalogowania.'}</p>}
        {validation && <p className="notice error" role="alert">{validation}</p>}
        <label htmlFor="email">Adres e-mail</label>
        <input id="email" name="email" type="email" autoComplete="username" required maxLength={320}
          value={email} onChange={(event) => setEmail(event.target.value)} disabled={busy} />
        <label htmlFor="password">Hasło</label>
        <div className="password-field">
          <input id="password" name="password" type={visible ? 'text' : 'password'} required
            autoComplete={registration ? 'new-password' : 'current-password'}
            aria-describedby="password-hint" value={password} disabled={busy}
            onChange={(event) => setPassword(event.target.value)} />
          <button type="button" aria-label={visible ? 'Ukryj hasło' : 'Pokaż hasło'} aria-pressed={visible}
            onClick={() => setVisible(!visible)}>{visible ? 'Ukryj' : 'Pokaż'}</button>
        </div>
        <p className="field-hint" id="password-hint">{registration ? '15–128 znaków. Spacje są częścią hasła.' : 'Wpisz hasło do swojego konta.'}</p>
        {registration && <>
          <label htmlFor="confirmation">Powtórz hasło</label>
          <input id="confirmation" name="confirmation" type={visible ? 'text' : 'password'}
            autoComplete="new-password" required value={confirmation} disabled={busy}
            onChange={(event) => setConfirmation(event.target.value)} />
        </>}
        <button className="button" disabled={busy} type="submit">
          {busy ? 'Proszę czekać…' : registration ? 'Załóż konto' : 'Zaloguj się'}<span aria-hidden="true"> →</span>
        </button>
        <p className="auth-switch">{registration ? 'Masz już konto?' : 'Pierwsza wizyta?'}{' '}
          <a href={registration ? '#/login' : '#/register'}>{registration ? 'Zaloguj się' : 'Załóż konto'}</a>
        </p>
      </form>}
    </div>
    <p className="auth-motto">Trzy miejsca w kronice. Niezliczone opowieści.</p>
  </div>;
}
