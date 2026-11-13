import type { CharacterResponse } from '../api/contracts';
import { ClassEmblem } from '../ui/ClassEmblem';
import { CharacterPortrait } from '../ui/CharacterPortrait';
import { attributes, classes } from './creation';

export function CharacterSelection({ characters, notice, refresh }: {
  characters: CharacterResponse[]; notice: string; refresh: () => void;
}) {
  return <>
    <div className="page-heading heading-with-action">
      <div><p className="eyebrow">Kronika bohaterów</p><h1>Twoje postacie</h1>
        <p>Wybierz bohatera, którego opowieść chcesz kontynuować.</p></div>
      <button type="button" className="button secondary" onClick={refresh}>Odśwież listę</button>
    </div>
    {notice && <div className="notice success" role="status">{notice}</div>}
    <div className="section-ribbon"><span>Wybór bohatera</span><span>{characters.length} / 3 miejsc</span></div>
    {characters.length === 0 && <p className="intro-note">Twoja kronika jest jeszcze pusta. Stwórz pierwszego bohatera.</p>}
    <div className="character-slots">
      {Array.from({ length: 3 }, (_, index) => {
        const character = characters[index];
        return <article className={`character-slot ${character ? 'occupied' : 'vacant'}`} key={index}
          aria-label={`Miejsce ${index + 1}${character ? `: ${character.name}` : ': puste'}`}>
          <div className="slot-topline"><span>Miejsce {['I', 'II', 'III'][index]}</span>
            <span>{character ? `Poziom ${character.level}` : 'Wolne'}</span></div>
          <div className="slot-portrait">{character
            ? <CharacterPortrait characterClass={character.characterClass} gender={character.appearanceGender}
                level={character.level} />
            : <ClassEmblem />}</div>
          {character ? <>
            <h2>{character.name}</h2><p className="class-label">{classes[character.characterClass].label}</p>
            <dl className="mini-stats">{attributes.map(({ key, short, label }) => <div key={key}>
              <dt><abbr title={label}>{short}</abbr></dt><dd>{character.startingAttributes[key] + character.trainedAttributes[key]}</dd>
            </div>)}</dl>
            <a href={`#/characters/${character.id}`} className="button">Wybierz postać <span aria-hidden="true">→</span></a>
          </> : <>
            <h2>Niezapisana historia</h2><p className="class-label">Miejsce na nowego bohatera</p>
            <p className="empty-description">Wybierz swoją drogę.<br />Nadaj imię nowej legendzie.</p>
            <a className="button secondary" href="#/characters/new">Stwórz postać <span aria-hidden="true">+</span></a>
          </>}
        </article>;
      })}
    </div>
    <p className="footnote"><span aria-hidden="true">✦</span> {characters.length >= 3
      ? 'Wszystkie miejsca w kronice są zajęte. Wybierz jedną ze swoich postaci.'
      : 'Na jednym koncie możesz mieć trzy postacie. Ich klasy mogą się powtarzać.'}</p>
  </>;
}
