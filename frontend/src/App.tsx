import { AuthPage } from './auth/AuthPage';
import { useSession } from './auth/useSession';
import { CharacterRoutes } from './characters/CharacterRoutes';
import { useRoute } from './navigation';
import { GameLayout } from './ui/GameLayout';
import { Loading } from './ui/Feedback';

export default function App() {
  const route = useRoute();
  const { session, login, logout, retry } = useSession();
  const account = session.state === 'authenticated' ? session.account : null;
  return <GameLayout key={account?.id ?? 'guest'} account={account} route={route} logout={logout}>
    {session.state === 'loading' && <Loading>Otwieranie kroniki…</Loading>}
    {session.state === 'error' && <div className="notice error" role="alert"><p>{session.message}</p>
      <button className="button" onClick={retry}>Spróbuj ponownie</button></div>}
    {session.state === 'anonymous' && <AuthPage key={route === '/register' ? 'register' : 'login'}
      registration={route === '/register'} message={session.message} login={login} />}
    {account && <CharacterRoutes key={account.id} route={route} />}
  </GameLayout>;
}
