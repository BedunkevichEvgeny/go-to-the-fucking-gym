import { useQuery } from '@tanstack/react-query';
import { api } from '../services/api';
import type { ExerciseDto } from '../types/api';

export function useExerciseLibrary(query: string) {
  return useQuery({
    queryKey: ['exercise-library', query],
    queryFn: async () => {
      const response = await api.get<ExerciseDto[]>('/exercises', {
        params: query ? { query } : {},
      });
      return response.data;
    },
  });
}

