import { useState } from 'react';
import { navigate } from '../navigation';
import { CharacterProfile } from './CharacterProfile';
import { CharactersPage } from './CharactersPage';

export function CharacterRoutes({ route }: { route: string }) {
  const [notice, setNotice] = useState('');
  const profileId = /^\/characters\/([^/]+)$/.exec(route)?.[1];
  function returnToSelection(message: string) {
    setNotice(message);
    navigate('/characters');
  }
  return profileId && profileId !== 'new'
    ? <CharacterProfile key={profileId} id={profileId} returnToSelection={returnToSelection} />
    : <CharactersPage creating={route === '/characters/new'} initialNotice={notice} />;
}
