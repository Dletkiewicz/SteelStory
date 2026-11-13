import { useEffect, useState } from 'react';
import { api } from '../api/client';
import type { CharacterResponse } from '../api/contracts';

type CharacterState =
  | { state: 'loading' }
  | { state: 'ready'; characters: CharacterResponse[] }
  | { state: 'error'; error: unknown };

export function useCharacters(revision: number, creating: boolean) {
  const [result, setResult] = useState<CharacterState>({ state: 'loading' });
  const [attempt, setAttempt] = useState(0);
  useEffect(() => {
    let active = true;
    setResult({ state: 'loading' });
    api.listCharacters().then((response) => {
      if (active) setResult({ state: 'ready', characters: response.characters });
    }).catch((error: unknown) => { if (active) setResult({ state: 'error', error }); });
    return () => { active = false; };
  }, [attempt, revision, creating]);
  return { result, reload: () => setAttempt((value) => value + 1) };
}
