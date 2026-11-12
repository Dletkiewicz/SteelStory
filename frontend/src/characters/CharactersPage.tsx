import { useState } from 'react';
import type { CharacterResponse } from '../api/contracts';
import { navigate } from '../navigation';
import { ErrorNotice, Loading } from '../ui/Feedback';
import { CharacterCreator } from './CharacterCreator';
import { CharacterSelection } from './CharacterSelection';
import { useCharacters } from './useCharacters';

export function CharactersPage({ creating }: { creating: boolean }) {
  const [revision, setRevision] = useState(0);
  const [notice, setNotice] = useState('');
  const { result, reload } = useCharacters(revision, creating);

  function created(character: CharacterResponse) {
    setNotice(`Postać ${character.name} została zapisana. Możesz teraz otworzyć jej kartę.`);
    setRevision((value) => value + 1);
    navigate('/characters');
  }
  if (result.state === 'loading') return <Loading />;
  if (result.state === 'error') return <ErrorNotice error={result.error} retry={reload} />;
  if (creating && result.characters.length < 3) return <CharacterCreator created={created} />;
  if (creating) return <><h1>Wszystkie miejsca są zajęte</h1><p>Na koncie są już trzy postacie.</p>
    <a className="button" href="#/characters">Wybierz postać</a></>;
  return <CharacterSelection characters={result.characters} notice={notice} refresh={reload} />;
}
