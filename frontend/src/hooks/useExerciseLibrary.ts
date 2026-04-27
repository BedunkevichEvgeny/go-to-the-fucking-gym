import { useQuery } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { api } from '../services/api';
import type { ExerciseDto } from '../types/api';

export function useExerciseLibrary(query: string) {
  const [debouncedQuery, setDebouncedQuery] = useState(query);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setDebouncedQuery(query);
    }, 300);

    return () => window.clearTimeout(timer);
  }, [query]);

  return useQuery({
    queryKey: ['exercise-library', debouncedQuery],
    queryFn: async () => {
      const response = await api.get<ExerciseDto[]>('/exercises', {
        params: debouncedQuery ? { query: debouncedQuery } : {},
      });
      return response.data;
    },
  });
}

