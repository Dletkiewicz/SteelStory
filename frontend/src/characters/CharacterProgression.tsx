import type { CharacterResponse } from '../api/contracts';

export function CharacterProgression({ character }: { character: CharacterResponse }) {
  const { currentLevelExperience, nextLevelExperience, remainingExperience, maximumLevel } = character.progression;
  const percent = maximumLevel ? 100 : Math.floor(currentLevelExperience / nextLevelExperience * 100);
  const format = (value: number) => value.toLocaleString('pl-PL');
  return <section className="panel progression-panel" aria-labelledby="progression-title">
    <h2 className="panel-heading" id="progression-title">Doświadczenie i rozwój</h2>
    <div className="panel-body">
      <div className="xp-summary"><strong>{maximumLevel ? 'Poziom maksymalny' : `Droga do poziomu ${character.level + 1}`}</strong>
        <span>{maximumLevel ? '100%' : `${format(currentLevelExperience)} / ${format(nextLevelExperience)} XP`}</span></div>
      <div className="xp-track" role="progressbar" aria-label="Postęp poziomu" aria-valuemin={0}
        aria-valuemax={100} aria-valuenow={percent}
        aria-valuetext={maximumLevel ? 'Poziom maksymalny' : `${currentLevelExperience} z ${nextLevelExperience} XP`}>
        <span style={{ width: `${percent}%` }} /></div>
      {!maximumLevel && <p>Do następnego poziomu: <strong>{format(remainingExperience)} XP</strong></p>}
      <p>Łączne doświadczenie: <strong>{format(character.experience)} XP</strong></p>
      <p>Dostępne punkty nauki: <strong>{character.unspentTrainingPoints}</strong></p>
      <p className="field-hint">Punkty zachowujesz na później. Nauka u nauczyciela nie jest jeszcze dostępna.</p>
    </div>
  </section>;
}
