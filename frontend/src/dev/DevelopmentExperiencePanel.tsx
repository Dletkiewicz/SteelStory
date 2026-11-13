import { useRef, useState } from 'react';
import { api } from '../api/client';
import type { CharacterResponse } from '../api/contracts';
import { ApiError } from '../api/errors';
import { ErrorNotice } from '../ui/Feedback';
import { grantDevelopmentExperience } from './experienceApi';

export default function DevelopmentExperiencePanel({ character, refreshed }: {
  character: CharacterResponse; refreshed: (character: CharacterResponse) => void;
}) {
  const pending = useRef(false);
  const [busy, setBusy] = useState(false);
  const [amount, setAmount] = useState('500');
  const [error, setError] = useState<unknown>(null);
  const [notice, setNotice] = useState('');
  const [reconcile, setReconcile] = useState(false);
  const [unavailable, setUnavailable] = useState(false);
  const numericAmount = Number(amount);
  const validAmount = /^\d+$/.test(amount) && Number.isSafeInteger(numericAmount)
    && numericAmount > 0 && numericAmount <= 1_000_000_000;

  async function reload() {
    if (pending.current) return;
    pending.current = true; setBusy(true); setError(null);
    try {
      refreshed(await api.getCharacter(character.id));
      setReconcile(false);
      setNotice('Odczytano zapisany stan postaci. Każde kolejne przyznanie XP jest nową nagrodą.');
    } catch (failure) { setError(failure); }
    finally { pending.current = false; setBusy(false); }
  }

  async function grant(value: number) {
    if (pending.current || reconcile || unavailable) return;
    pending.current = true; setBusy(true); setError(null); setNotice('');
    let awarded = false;
    try {
      const result = await grantDevelopmentExperience(character.id, value);
      awarded = true;
      const saved = await api.getCharacter(character.id);
      refreshed(saved);
      setNotice(result.levelsGained > 0
        ? `Awans! Poziom ${result.currentLevel}. Zdobyte poziomy: ${result.levelsGained}. +${result.trainingPointsAwarded} pkt nauki.`
        : result.acceptedExperience > 0 ? `Zapisano +${result.acceptedExperience} XP.` : 'Poziom maksymalny — nie przyznano dodatkowych XP ani punktów.');
    } catch (failure) {
      if (!awarded && failure instanceof ApiError && failure.status === 404) {
        setUnavailable(true);
      } else {
        setError(failure);
        if (awarded || !(failure instanceof ApiError) || failure.status === 0 || failure.status >= 500) {
          setReconcile(true);
          setNotice(awarded ? 'XP zapisano, lecz nie udało się odświeżyć postaci. Odczytaj stan przed kolejną nagrodą.'
            : 'Wynik przyznania XP jest nieznany. Odczytaj stan; nie ponawiaj tej samej nagrody.');
        }
      }
    } finally { pending.current = false; setBusy(false); }
  }

  return <section className="panel dev-xp-panel" aria-labelledby="dev-xp-title">
    <h2 className="panel-heading" id="dev-xp-title">Panel developerski — XP</h2>
    <div className="panel-body">
      <p className="field-hint">Lokalny test rozwoju. Każde zatwierdzenie przyznaje nową nagrodę.</p>
      {unavailable && <div className="notice" role="status">Narzędzie XP jest wyłączone lub postać jest niedostępna.
        Backend wymaga profilu local i steelstory.dev.xp.enabled=true. Po zmianie konfiguracji uruchom backend ponownie i odśwież stronę.</div>}
      {error !== null && <ErrorNotice error={error instanceof ApiError && error.status === 400
        ? new ApiError(400, 'HTTP_ERROR') : error} />}
      {notice && <div className={`notice ${reconcile ? '' : 'success'}`} role="status">{notice}</div>}
      <fieldset disabled={busy || reconcile || unavailable}>
        <div className="dev-xp-actions">
          <button type="button" className="button secondary" onClick={() => void grant(100)}>Przyznaj 100 XP</button>
          <button type="button" className="button secondary" onClick={() => void grant(500)}>Przyznaj 500 XP</button>
        </div>
        <label htmlFor="dev-xp-amount">Własna nagroda XP (1–1 000 000 000)</label>
        <input id="dev-xp-amount" inputMode="numeric" value={amount} onChange={(event) => setAmount(event.target.value)} />
        <button type="button" className="button" disabled={!validAmount}
          onClick={() => void grant(numericAmount)}>Przyznaj XP</button>
      </fieldset>
      <button type="button" className="button secondary" disabled={busy} onClick={() => void reload()}>
        {busy ? 'Zapisywanie / odczyt…' : 'Odśwież dane postaci'}</button>
    </div>
  </section>;
}
