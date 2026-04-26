import { useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../services/api';
import type { LoggedSessionCreateRequest, LoggedSessionDetail } from '../types/api';

export function useLogSession() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (payload: LoggedSessionCreateRequest) => {
      const response = await api.post<LoggedSessionDetail>('/logged-sessions', payload);
      return response.data;
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['program-session', 'next'] });
      void queryClient.invalidateQueries({ queryKey: ['history'] });
    },
  });
}

