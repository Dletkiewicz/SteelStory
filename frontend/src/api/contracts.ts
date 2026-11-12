// Handwritten HTTP contracts matching docs/api/openapi.json.
export interface AccountResponse {
  id: string;
  email: string;
  status: 'ACTIVE' | 'DISABLED';
  createdAt: string;
  updatedAt: string;
}

export interface CredentialsRequest { email: string; password: string }
export interface CsrfResponse { headerName: string; token: string }
export type CharacterClass = 'WARRIOR' | 'ARCHER' | 'MAGE';
export interface Attributes {
  strength: number;
  dexterity: number;
  intelligence: number;
  constitution: number;
  luck: number;
}
export interface CreateCharacterRequest {
  name: string;
  characterClass: CharacterClass;
  startingAllocation: Attributes;
}
export interface CharacterResponse {
  id: string;
  name: string;
  characterClass: CharacterClass;
  level: number;
  experience: number;
  unspentTrainingPoints: number;
  startingAttributes: Attributes;
  trainedAttributes: Attributes;
  createdAt: string;
  updatedAt: string;
}
export interface CharacterListResponse { characters: CharacterResponse[] }
export interface BackendStatus { application: 'steelstory'; status: 'UP' }
