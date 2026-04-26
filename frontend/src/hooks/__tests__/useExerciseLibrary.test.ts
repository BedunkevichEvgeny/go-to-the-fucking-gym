import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import { createElement } from 'react';
import type { ReactNode } from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { api } from '../../services/api';
import { useExerciseLibrary } from '../useExerciseLibrary';

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

describe('useExerciseLibrary', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('queries exercise endpoint with query param and returns exercise data', async () => {
    vi.mocked(api.get).mockResolvedValue({
      data: [{ id: '1', name: 'Bench Press', category: 'Chest', type: 'STRENGTH' }],
    });

    const { result } = renderHook(() => useExerciseLibrary('bench'), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.data).toHaveLength(1);
    });

    expect(api.get).toHaveBeenCalledWith('/exercises', { params: { query: 'bench' } });
    expect(result.current.data?.[0].name).toBe('Bench Press');
  });

  it('debounces search updates before requesting API', async () => {
    vi.mocked(api.get).mockResolvedValue({ data: [] });

    const { rerender } = renderHook(({ query }) => useExerciseLibrary(query), {
      initialProps: { query: '' },
      wrapper: createWrapper(),
    });

    rerender({ query: 'b' });
    rerender({ query: 'be' });
    rerender({ query: 'ben' });

    await new Promise((resolve) => setTimeout(resolve, 150));
    expect(api.get).not.toHaveBeenLastCalledWith('/exercises', { params: { query: 'ben' } });

    await waitFor(
      () => {
        expect(api.get).toHaveBeenLastCalledWith('/exercises', { params: { query: 'ben' } });
      },
      { timeout: 2500 },
    );
  });
});
