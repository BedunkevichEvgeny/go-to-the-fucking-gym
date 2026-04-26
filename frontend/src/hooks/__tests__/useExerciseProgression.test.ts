import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import { createElement } from 'react';
import type { ReactNode } from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { api } from '../../services/api';
import { useExerciseProgression } from '../useExerciseProgression';

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

describe('useExerciseProgression', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('queries progression endpoint and returns progression response data', async () => {
    vi.mocked(api.get).mockResolvedValue({
      data: {
        exerciseName: 'Bench Press',
        points: [{ sessionId: 'session-1', sessionDate: '2026-04-27', metricType: 'WEIGHT', metricValue: 100 }],
      },
    });

    const { result } = renderHook(() => useExerciseProgression('Bench Press'), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.data?.exerciseName).toBe('Bench Press');
    });

    expect(api.get).toHaveBeenCalledWith('/progression/Bench%20Press');
    expect(result.current.isLoading).toBe(false);
    expect(result.current.error).toBeNull();
  });

  it('does not query endpoint when exercise name is undefined', () => {
    renderHook(() => useExerciseProgression(undefined), {
      wrapper: createWrapper(),
    });

    expect(api.get).not.toHaveBeenCalled();
  });
});

