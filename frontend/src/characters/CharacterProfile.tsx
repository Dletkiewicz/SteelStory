import { lazy, Suspense, useEffect, useState } from 'react';
import { api } from '../api/client';
import type { CharacterResponse } from '../api/contracts';
import { CharacterPortrait } from '../ui/CharacterPortrait';
import { ErrorNotice, Loading } from '../ui/Feedback';
import { attributes, classes } from './creation';
import { DeleteCharacterDialog } from './DeleteCharacterDialog';
import { CharacterProgression } from './CharacterProgression';

// Vite removes the dynamic import and the entire development module from production builds.
const DevelopmentExperiencePanel = import.meta.env.DEV && import.meta.env.VITE_DEV_XP_ENABLED === 'true'
  ? lazy(() => import('../dev/DevelopmentExperiencePanel')) : null;
const localHost = ['localhost', '127.0.0.1', '[::1]'].includes(window.location.hostname);

type ProfileState = { state: 'loading' } | { state: 'error'; error: unknown }
  | { state: 'ready'; character: CharacterResponse };

export function CharacterProfile({ id, returnToSelection }: { id: string; returnToSelection: (message: string) => void }) {
  const [result, setResult] = useState<ProfileState>({ state: 'loading' });
  const [attempt, setAttempt] = useState(0);
  const [deleting, setDeleting] = useState(false);
  useEffect(() => {
    let active = true;
    setResult({ state: 'loading' });
    api.getCharacter(id).then((character) => { if (active) setResult({ state: 'ready', character }); })
      .catch((error: unknown) => { if (active) setResult({ state: 'error', error }); });
    return () => { active = false; };
  }, [id, attempt]);

  const character = result.state === 'ready' ? result.character : null;
  return <>
    <a className="back-link" href="#/characters">← Wróć do postaci</a>
    {result.state === 'loading' && <Loading />}
    {result.state === 'error' && <ErrorNotice error={result.error} retry={() => setAttempt((value) => value + 1)} />}
    {character && <>
      <div className="page-heading"><p className="eyebrow">Karta postaci</p><h1>{character.name}</h1>
        <p>{classes[character.characterClass].label} · Poziom {character.level}</p></div>
      <div className="profile-overview panel"><div className="profile-portrait">
        <CharacterPortrait characterClass={character.characterClass} gender={character.appearanceGender}
          level={character.level} /></div>
        <div className="profile-details"><h2>{classes[character.characterClass].label}</h2><p>{classes[character.characterClass].description}</p>
          <dl className="character-facts"><div><dt>Poziom</dt><dd>{character.level}</dd></div>
            <div><dt>Doświadczenie</dt><dd>{character.experience.toLocaleString('pl-PL')} <small>XP</small></dd></div>
            <div><dt>Wolne punkty treningu</dt><dd>{character.unspentTrainingPoints}</dd></div></dl>
        </div>
      </div>
      <CharacterProgression character={character} />
      <section className="panel attribute-sheet" aria-labelledby="attributes-title">
        <h2 className="panel-heading" id="attributes-title">Atrybuty bohatera</h2>
        <table><caption className="sr-only">Początkowe, wytrenowane i całkowite trwałe atrybuty postaci</caption>
          <thead><tr><th scope="col">Atrybut</th><th scope="col">Początkowe</th><th scope="col">Trening</th><th scope="col">Suma trwała</th></tr></thead>
          <tbody>{attributes.map(({ key, short, label }) => <tr key={key}>
            <th scope="row"><span className="stat-mark" aria-hidden="true">{short}</span>{label}</th>
            <td>{character.startingAttributes[key]}</td><td>+{character.trainedAttributes[key]}</td>
            <td className="total-stat">{character.startingAttributes[key] + character.trainedAttributes[key]}</td>
          </tr>)}</tbody>
        </table>
        <p className="field-hint">Suma trwała = atrybut początkowy + wytrenowany bonus.</p>
      </section>
      <p className="footnote"><span aria-hidden="true">✦</span> Każdy awans daje 3 punkty nauki. Nauczyciele i ekwipunek pojawią się w kolejnych etapach gry.</p>
      {DevelopmentExperiencePanel && localHost && <Suspense fallback={<Loading />}>
        <DevelopmentExperiencePanel character={character} refreshed={(saved) => setResult({ state: 'ready', character: saved })} />
      </Suspense>}
      <div className="profile-deletion"><button className="button secondary" type="button"
        onClick={() => setDeleting(true)}>Usuń postać</button></div>
      {deleting && <DeleteCharacterDialog character={character} close={() => setDeleting(false)}
        deleted={() => returnToSelection(`Postać ${character.name} została usunięta. Możesz stworzyć nowego bohatera.`)}
        unavailable={() => returnToSelection('Postać nie jest już dostępna. Lista postaci została odświeżona.')} />}
    </>}
  </>;
}
