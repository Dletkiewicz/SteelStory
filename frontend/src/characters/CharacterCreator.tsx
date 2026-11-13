import { useState } from 'react';
import type { FormEvent } from 'react';
import { api } from '../api/client';
import type { AppearanceGender, CharacterClass, CharacterResponse } from '../api/contracts';
import { ApiError } from '../api/errors';
import { CharacterPortrait } from '../ui/CharacterPortrait';
import { ErrorNotice } from '../ui/Feedback';
import {
  adjustAllocation, appearanceGenders, attributes, characterClasses, classes, creationRequest, genderLabels,
  emptyAllocation, remainingPoints, validCharacterName,
} from './creation';

export function CharacterCreator({ created }: { created: (character: CharacterResponse) => void }) {
  const [name, setName] = useState('');
  const [characterClass, setCharacterClass] = useState<CharacterClass>('WARRIOR');
  const [appearanceGender, setAppearanceGender] = useState<AppearanceGender>('MALE');
  const [allocation, setAllocation] = useState(emptyAllocation);
  const [touched, setTouched] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const remaining = remainingPoints(allocation);
  const validName = validCharacterName(name);
  const uncertain = error instanceof ApiError && error.status === 0;
  const limitReached = error instanceof ApiError && error.code === 'CHARACTER_LIMIT_REACHED';

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setTouched(true);
    if (busy || !validName || remaining !== 0 || uncertain || limitReached) return;
    setBusy(true); setError(null);
    try { created(await api.createCharacter(creationRequest(name, characterClass, appearanceGender, allocation))); }
    catch (failure) { setError(failure); }
    finally { setBusy(false); }
  }

  return <>
    <a className="back-link" href="#/characters">← Wróć do postaci</a>
    <div className="page-heading"><p className="eyebrow">Nowy rozdział</p><h1>Stwórz bohatera</h1>
      <p>Imię, droga i pierwsze talenty. Reszta historii należy do Ciebie.</p></div>
    <form onSubmit={(event) => void submit(event)} className="creator-form">
      {error !== null && <ErrorNotice error={error} />}
      {uncertain && <div className="notice" role="status">
        Wynik tworzenia jest nieznany. Postać mogła zostać zapisana. Sprawdź listę przed kolejną próbą.
        <a className="button secondary" href="#/characters">Sprawdź moje postacie</a>
      </div>}
      {limitReached && <a className="button secondary" href="#/characters">Wróć do wyboru postaci</a>}
      <fieldset disabled={busy || uncertain || limitReached} className="panel name-panel">
        <legend className="panel-heading"><span className="step-number">I</span> Nadaj imię</legend>
        <div className="panel-body"><label htmlFor="character-name">Imię postaci</label>
          <input id="character-name" name="name" autoComplete="off" required minLength={3} maxLength={20}
            value={name} onChange={(event) => setName(event.target.value)} onBlur={() => setTouched(true)}
            aria-invalid={touched && !validName} aria-describedby="name-hint name-error" placeholder="Imię Twojego bohatera" />
          <p id="name-hint" className="field-hint">3–20 znaków: litery A–Z, cyfry i pojedyncze spacje wewnątrz nazwy. Bez polskich znaków.</p>
          <p id="name-error" className="field-error">{touched && !validName ? 'Wpisz poprawne imię bez skrajnych lub podwójnych spacji.' : ''}</p>
        </div>
      </fieldset>
      <fieldset disabled={busy || uncertain || limitReached} className="panel appearance-panel">
        <legend className="panel-heading">Wygląd postaci</legend>
        <div className="panel-body">
          <div className="appearance-choices">{appearanceGenders.map((gender) => <label key={gender}>
            <input type="radio" name="appearanceGender" value={gender} checked={appearanceGender === gender}
              onChange={() => setAppearanceGender(gender)} />{genderLabels[gender]}
          </label>)}</div>
          <p className="field-hint">Wybór dotyczy wyglądu portretu i nie wpływa na atrybuty. Zapiszesz go razem z postacią.</p>
        </div>
      </fieldset>
      <fieldset disabled={busy || uncertain || limitReached} className="class-fieldset">
        <legend className="section-ribbon"><span><span className="step-number">II</span> Wybierz swoją drogę</span></legend>
        <div className="class-choices">{characterClasses.map((choice) => <label key={choice}
          className={`class-choice ${characterClass === choice ? 'selected' : ''}`}>
          <input type="radio" name="characterClass" value={choice} checked={characterClass === choice}
            onChange={() => setCharacterClass(choice)} />
          <CharacterPortrait characterClass={choice} gender={appearanceGender} level={1} />
          <span className="class-name">{classes[choice].label}</span>
          <span>{classes[choice].description}</span><small>Główny atrybut: {classes[choice].attribute}</small>
        </label>)}</div>
        <p className="field-hint">Klasa jest stała. Nie zmienia początkowych wartości atrybutów. Bronie pojawią się w przyszłych etapach gry.</p>
      </fieldset>
      <fieldset disabled={busy || uncertain || limitReached} className="panel allocation-panel">
        <legend className="panel-heading"><span className="step-number">III</span> Rozdziel talenty</legend>
        <div className="allocation-layout panel-body">
          <div className="allocation-rows">{attributes.map(({ key, label, short }) => <div className="allocation-row" key={key}>
            <span className="stat-mark" aria-hidden="true">{short}</span><label htmlFor={`value-${key}`}>{label}</label>
            <button type="button" className="stepper" aria-label={`Odejmij: ${label}`} disabled={allocation[key] === 0}
              onClick={() => setAllocation((value) => adjustAllocation(value, key, -1))}>−</button>
            <output id={`value-${key}`}>{5 + allocation[key]}</output>
            <button type="button" className="stepper" aria-label={`Dodaj: ${label}`} disabled={remaining === 0}
              onClick={() => setAllocation((value) => adjustAllocation(value, key, 1))}>+</button>
            <span className="allocation-bonus">5 + {allocation[key]}</span>
          </div>)}</div>
          <div className="points-summary"><span>Pozostało punktów</span><strong aria-live="polite" aria-atomic="true">{remaining}<small> / 10</small></strong>
            <p>Każdy atrybut zaczyna od 5. Rozdziel wszystkie 10 punktów według własnego pomysłu.</p>
            <button type="button" className="button secondary" disabled={remaining === 10}
              onClick={() => setAllocation(emptyAllocation())}>Wyzeruj przydział</button>
          </div>
        </div>
      </fieldset>
      <div className="form-actions"><p>{remaining > 0 ? `Rozdziel jeszcze ${remaining} pkt, aby zapisać bohatera.` : 'Wszystkie punkty rozdzielone. Twoja historia może się rozpocząć.'}</p>
        <button type="submit" className="button" disabled={busy || !validName || remaining !== 0 || uncertain || limitReached}>
          {busy ? 'Zapisywanie…' : 'Stwórz bohatera'} <span aria-hidden="true">→</span></button></div>
    </form>
  </>;
}
