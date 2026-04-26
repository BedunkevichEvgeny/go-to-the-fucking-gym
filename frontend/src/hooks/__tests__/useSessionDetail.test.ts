import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import { createElement } from 'react';
import type { ReactNode } from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { api } from '../../services/api';
import { useSessionDetail } from '../useSessionDetail';

vi.mock('../../services/api', () => ({
  api: {
    get: vi.fn(),
  },
}));

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  });

  return ({ children }: { children: ReactNode }) =>
    createElement(QueryClientProvider, { client: queryClient }, children);
}

describe('useSessionDetail', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('queries session detail endpoint and returns logged session details', async () => {
    vi.mocked(api.get).mockResolvedValue({
      data: {
        sessionId: 'session-1',
        sessionType: 'FREE',
        sessionDate: '2026-04-27',
        feelings: { rating: 8 },
        exerciseEntries: [],
      },
    });

    const { result } = renderHook(() => useSessionDetail('session-1'), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.data?.sessionId).toBe('session-1');
    });

    expect(api.get).toHaveBeenCalledWith('/logged-sessions/session-1');
    expect(result.current.isLoading).toBe(false);
    expect(result.current.error).toBeNull();
  });
});

