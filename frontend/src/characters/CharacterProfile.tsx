import { useEffect, useState } from 'react';
import { api } from '../api/client';
import type { CharacterResponse } from '../api/contracts';
import { ClassEmblem } from '../ui/ClassEmblem';
import { ErrorNotice, Loading } from '../ui/Feedback';
import { attributes, classes } from './creation';

type ProfileState = { state: 'loading' } | { state: 'error'; error: unknown }
  | { state: 'ready'; character: CharacterResponse };

export function CharacterProfile({ id }: { id: string }) {
  const [result, setResult] = useState<ProfileState>({ state: 'loading' });
  const [attempt, setAttempt] = useState(0);
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
      <div className="profile-overview panel"><div className="profile-portrait"><ClassEmblem characterClass={character.characterClass} /></div>
        <div className="profile-details"><h2>{classes[character.characterClass].label}</h2><p>{classes[character.characterClass].description}</p>
          <dl className="character-facts"><div><dt>Poziom</dt><dd>{character.level}</dd></div>
            <div><dt>Doświadczenie</dt><dd>{character.experience.toLocaleString('pl-PL')} <small>XP</small></dd></div>
            <div><dt>Wolne punkty treningu</dt><dd>{character.unspentTrainingPoints}</dd></div></dl>
        </div>
      </div>
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
      <p className="footnote"><span aria-hidden="true">✦</span> To początek Twojej opowieści. Rozwój postaci i ekwipunek pojawią się w kolejnych etapach gry.</p>
    </>}
  </>;
}
