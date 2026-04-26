import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import { createElement } from 'react';
import type { ReactNode } from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { api } from '../../services/api';
import { useSessionHistory } from '../useSessionHistory';

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

describe('useSessionHistory', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('builds query params and returns session history page data', async () => {
    vi.mocked(api.get).mockResolvedValue({
      data: {
        items: [
          {
            sessionId: 'session-1',
            sessionDate: '2026-04-27',
            sessionType: 'PROGRAM',
            exerciseCount: 3,
          },
        ],
        page: 0,
        size: 20,
        totalItems: 1,
      },
    });

    const { result } = renderHook(
      () =>
        useSessionHistory({
          page: 0,
          size: 20,
          dateFrom: '2026-04-01',
          dateTo: '2026-04-30',
          exerciseName: 'Bench Press',
        }),
      {
        wrapper: createWrapper(),
      },
    );

    await waitFor(() => {
      expect(result.current.data?.items).toHaveLength(1);
    });

    expect(api.get).toHaveBeenCalledWith('/logged-sessions/history', {
      params: {
        page: 0,
        size: 20,
        dateFrom: '2026-04-01',
        dateTo: '2026-04-30',
        exerciseName: 'Bench Press',
      },
    });
    expect(result.current.isLoading).toBe(false);
    expect(result.current.error).toBeNull();
  });
});

