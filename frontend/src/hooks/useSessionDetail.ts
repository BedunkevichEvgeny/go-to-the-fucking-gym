import { useQuery } from '@tanstack/react-query';
import { api } from '../services/api';
import type { LoggedSessionDetail } from '../types/api';

export function useSessionDetail(sessionId: string | undefined) {
  return useQuery({
    enabled: Boolean(sessionId),
    queryKey: ['session-detail', sessionId],
    queryFn: async () => {
      const response = await api.get<LoggedSessionDetail>(`/logged-sessions/${sessionId}`);
      return response.data;
    },
  });
}

