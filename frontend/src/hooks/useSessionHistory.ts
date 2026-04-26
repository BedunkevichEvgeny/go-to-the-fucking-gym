import { useQuery } from '@tanstack/react-query';
import { api } from '../services/api';
import type { SessionHistoryPage } from '../types/api';

interface SessionHistoryParams {
  page: number;
  size: number;
  dateFrom?: string;
  dateTo?: string;
  exerciseName?: string;
}

export function useSessionHistory(params: SessionHistoryParams) {
  return useQuery({
    queryKey: ['history', params],
    queryFn: async () => {
      const response = await api.get<SessionHistoryPage>('/logged-sessions/history', {
        params,
      });
      return response.data;
    },
  });
}

