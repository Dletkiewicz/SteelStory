import type { CharacterClass } from '../api/contracts';

// Original vector heraldry, shared by class choices, character slots and profiles.
export function ClassEmblem({ characterClass }: { characterClass?: CharacterClass }) {
  return (
    <svg viewBox="0 0 160 180" fill="none" className={`class-emblem ${characterClass?.toLowerCase() ?? 'empty'}`}
      aria-hidden="true">
      <path className="emblem-shield" d="M80 8 143 29v61c0 39-29 64-63 82-34-18-63-57-63-82V29Z" />
      <path className="emblem-inset" d="M80 17 134 36v54c0 32-24 55-54 73-30-18-54-41-54-73V36Z" />
      <path className="emblem-ornament" d="m35 47 7-7 7 7-7 7Zm76 0 7-7 7 7-7 7ZM73 145l7-7 7 7-7 7Z" />
      {characterClass === 'WARRIOR' && <g stroke="currentColor" strokeWidth="3" strokeLinejoin="round">
        <path fill="#ddd5bd" d="m80 32 11 20-5 58H74l-5-58Z" />
        <path d="M80 42v64M55 107l25 7 25-7-2 12-23-3-23 3Z" fill="#b29252" />
        <path d="M75 117h10v22H75Z" fill="#785337" /><path d="m75 124 10 5m-10 2 10 5" />
        <circle cx="80" cy="143" r="6" fill="#b29252" />
      </g>}
      {characterClass === 'ARCHER' && <g stroke="currentColor" strokeWidth="3" strokeLinecap="round">
        <path d="M57 37c57 23 57 77 0 103" strokeWidth="6" /><path d="m57 37 17 52-17 51" />
        <path d="M39 89h82m-10-8 10 8-10 8M42 82l9 7-9 7" />
        <path d="M88 76v27" stroke="#b29252" strokeWidth="7" />
      </g>}
      {characterClass === 'MAGE' && <g stroke="currentColor" strokeWidth="3" strokeLinejoin="round">
        <path d="M41 90q19-6 39 6 20-12 39-6v41q-20-4-39 6-20-10-39-6Z" fill="#d9c49a" />
        <path d="M80 96v41m-31-35 22 6m-22 4 22 6m18-10 22-6m-22 16 22-6" />
        <path d="m80 29 5 20 20 5-20 5-5 20-5-20-20-5 20-5Z" fill="#b29252" />
        <path d="m47 61 2 6 6 2-6 2-2 6-2-6-6-2 6-2Z" />
      </g>}
      {!characterClass && <path d="M80 68v44M58 90h44" stroke="currentColor" strokeWidth="3" />}
    </svg>
  );
}
