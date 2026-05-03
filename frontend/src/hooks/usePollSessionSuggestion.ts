import { useEffect, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../services/api';
import type { LoggedSessionDetail } from '../types/api';

export function usePollSessionSuggestion(sessionId: string | null) {
  const [timedOut, setTimedOut] = useState(false);

  useEffect(() => {
    if (!sessionId) return;
    setTimedOut(false);
    const timer = setTimeout(() => setTimedOut(true), 15000);
    return () => clearTimeout(timer);
  }, [sessionId]);

  const query = useQuery<LoggedSessionDetail>({
    enabled: Boolean(sessionId),
    queryKey: ['session-detail', sessionId],
    queryFn: async () => {
      const response = await api.get<LoggedSessionDetail>(`/logged-sessions/${sessionId}`);
      return response.data;
    },
    refetchInterval: (q) => {
      const data = q.state.data;
      if (timedOut || data?.aiSuggestion) return false;
      return 3000;
    },
    retry: false,
    refetchIntervalInBackground: false,
  });

  const suggestion = query.data?.aiSuggestion ?? null;
  const isPolling = Boolean(sessionId) && !timedOut && !suggestion;

  return { suggestion, timedOut, isPolling };
}

