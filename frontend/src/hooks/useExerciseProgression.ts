import { useQuery } from '@tanstack/react-query';
import { api } from '../services/api';
import type { ProgressionResponse } from '../types/api';

export function useExerciseProgression(exerciseName: string | undefined) {
  return useQuery<ProgressionResponse>({
    enabled: Boolean(exerciseName),
    queryKey: ['progression', exerciseName],
    queryFn: async () => {
      const response = await api.get<ProgressionResponse>(`/progression/${encodeURIComponent(exerciseName ?? '')}`);
      return response.data;
    },
  });
}


