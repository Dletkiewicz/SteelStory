import { useSyncExternalStore } from 'react';

function subscribe(listener: () => void) {
  window.addEventListener('hashchange', listener);
  return () => window.removeEventListener('hashchange', listener);
}
export function useRoute(): string {
  return useSyncExternalStore(subscribe, () => window.location.hash.slice(1) || '/characters');
}
export function navigate(route: string): void { window.location.hash = route; }
