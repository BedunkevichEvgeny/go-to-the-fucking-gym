import { useQuery } from '@tanstack/react-query';
import { AxiosError } from 'axios';
import { api } from '../services/api';
import type { ProgramSessionView } from '../types/api';

export function useNextProgramSession() {
  return useQuery({
    queryKey: ['program-session', 'next'],
    queryFn: async () => {
      try {
        const response = await api.get<ProgramSessionView>('/program-sessions/next');
        return response.data;
      } catch (error) {
        const axiosError = error as AxiosError;
        if (axiosError.response?.status === 204) {
          return null;
        }
        throw error;
      }
    },
  });
}

