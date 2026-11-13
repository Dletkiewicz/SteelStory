import { api } from '../api/client';

export interface ExperienceAwardResponse {
  previousLevel: number;
  currentLevel: number;
  levelsGained: number;
  acceptedExperience: number;
  trainingPointsAwarded: number;
  totalExperience: number;
  unspentTrainingPoints: number;
}

export function grantDevelopmentExperience(id: string, amount: number): Promise<ExperienceAwardResponse> {
  return api.post(`/dev/characters/${encodeURIComponent(id)}/grant-xp`, { amount });
}
