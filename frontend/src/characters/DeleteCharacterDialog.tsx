import { useEffect, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { api } from '../api/client';
import { ApiError } from '../api/errors';
import type { CharacterResponse } from '../api/contracts';
import { ErrorNotice } from '../ui/Feedback';

export function DeleteCharacterDialog({ character, close, deleted, unavailable }: {
  character: CharacterResponse; close: () => void; deleted: () => void; unavailable: () => void;
}) {
  const dialog = useRef<HTMLDialogElement>(null);
  const cancelButton = useRef<HTMLButtonElement>(null);
  const pending = useRef(false);
  const [name, setName] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const missing = error instanceof ApiError && error.status === 404;
  const uncertain = error instanceof ApiError && (error.status === 0 || error.status >= 500);

  useEffect(() => {
    const element = dialog.current;
    const previouslyFocused = document.activeElement;
    element?.showModal();
    cancelButton.current?.focus();
    return () => {
      element?.close();
      if (previouslyFocused instanceof HTMLElement && previouslyFocused.isConnected) previouslyFocused.focus();
    };
  }, []);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (pending.current || name !== character.name || missing || uncertain) return;
    pending.current = true;
    setBusy(true); setError(null);
    try { await api.deleteCharacter(character.id); deleted(); }
    catch (failure) { setError(failure); }
    finally { pending.current = false; setBusy(false); }
  }

  async function checkCharacter() {
    if (pending.current) return;
    pending.current = true; setBusy(true);
    try { await api.getCharacter(character.id); setError(null); }
    catch (failure) {
      if (failure instanceof ApiError && failure.status === 404) unavailable();
      else setError(failure);
    } finally { pending.current = false; setBusy(false); }
  }

  return <dialog ref={dialog} className="deletion-dialog" aria-labelledby="deletion-title"
    aria-describedby="deletion-warning" onCancel={(event) => {
      event.preventDefault(); if (!pending.current) close();
    }}>
    <form onSubmit={(event) => void submit(event)}>
      <h2 className="panel-heading" id="deletion-title">Usuń postać</h2>
      <div className="panel-body">
        <p id="deletion-warning">Usunięcie postaci <strong>{character.name}</strong> jest nieodwracalne.
          Stracisz dostęp do tej postaci. Jej imię pozostanie zarezerwowane, ale zwolnisz miejsce na nowego bohatera.</p>
        {error !== null && <ErrorNotice error={error} />}
        {missing ? <p className="notice">Ta postać nie jest już dostępna. Wróć do listy, aby sprawdzić swoje postacie.</p> : <>
          <label htmlFor="delete-character-name">Wpisz dokładnie imię postaci: {character.name}</label>
          <input id="delete-character-name" autoComplete="off" spellCheck={false} value={name} disabled={busy}
            onChange={(event) => setName(event.target.value)} aria-describedby="delete-name-hint" />
          <p id="delete-name-hint" className="field-hint">Wielkość liter i spacje muszą się zgadzać.</p>
        </>}
        {uncertain && <div className="notice" role="status">
          Wynik usuwania jest nieznany. Sprawdź stan postaci przed kolejną próbą.
          <button type="button" className="button secondary" disabled={busy}
            onClick={() => void checkCharacter()}>Sprawdź stan postaci</button>
        </div>}
        <div className="dialog-actions">
          <button ref={cancelButton} type="button" className="button secondary" disabled={busy} onClick={close}>Anuluj</button>
          {missing ? <button type="button" className="button" onClick={unavailable}>Wróć do postaci</button>
            : <button type="submit" className="button danger" disabled={busy || name !== character.name || uncertain}>
              {busy ? 'Proszę czekać…' : 'Usuń bezpowrotnie'}</button>}
        </div>
      </div>
    </form>
  </dialog>;
}
