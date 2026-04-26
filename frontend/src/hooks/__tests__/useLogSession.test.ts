import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { useLogSession } from '../useLogSession';
import type { LoggedSessionCreateRequest, LoggedSessionDetail } from '../../types/api';
import { api } from '../../services/api';

vi.mock('../../services/api', () => ({
  api: {
    post: vi.fn(),
  },
}));

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
}

const requestFixture: LoggedSessionCreateRequest = {
  sessionType: 'PROGRAM',
  programSessionId: '11111111-1111-1111-1111-111111111111',
  sessionDate: '2026-04-27',
  feelings: {
    rating: 8,
  },
  exerciseEntries: [
    {
      exerciseName: 'Bench Press',
      exerciseType: 'STRENGTH',
      sets: [{ reps: 8, isBodyWeight: false, weightValue: 70, weightUnit: 'KG' }],
      cardioLaps: [],
    },
  ],
};

const responseFixture: LoggedSessionDetail = {
  sessionId: '22222222-2222-2222-2222-222222222222',
  sessionType: 'PROGRAM',
  programSessionId: '11111111-1111-1111-1111-111111111111',
  sessionDate: '2026-04-27',
  feelings: { rating: 8 },
  exerciseEntries: [
    {
      exerciseName: 'Bench Press',
      exerciseType: 'STRENGTH',
      sets: [{ setOrder: 1, reps: 8, isBodyWeight: false, weightValue: 70, weightUnit: 'KG' }],
      cardioLaps: [],
    },
  ],
};

describe('useLogSession', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('posts payload and returns saved session details', async () => {
    vi.mocked(api.post).mockResolvedValue({ data: responseFixture });

    const { result } = renderHook(() => useLogSession(), {
      wrapper: createWrapper(),
    });

    let payloadResult: LoggedSessionDetail | undefined;
    await act(async () => {
      payloadResult = await result.current.mutateAsync(requestFixture);
    });

    expect(api.post).toHaveBeenCalledWith('/logged-sessions', requestFixture);
    expect(payloadResult?.sessionId).toBe(responseFixture.sessionId);
  });

  it('exposes loading state while mutation is in-flight', async () => {
    let resolvePromise: ((value: { data: LoggedSessionDetail }) => void) | null = null;
    const delayed = new Promise<{ data: LoggedSessionDetail }>((resolve) => {
      resolvePromise = resolve;
    });
    vi.mocked(api.post).mockReturnValue(delayed);

    const { result } = renderHook(() => useLogSession(), {
      wrapper: createWrapper(),
    });

    act(() => {
      result.current.mutate(requestFixture);
    });

    await waitFor(() => expect(result.current.isLoading).toBe(true));

    await act(async () => {
      resolvePromise?.({ data: responseFixture });
      await delayed;
    });

    await waitFor(() => expect(result.current.isLoading).toBe(false));
  });

  it('sets error state on 400/403 style API failures', async () => {
    vi.mocked(api.post).mockRejectedValue({
      response: { status: 403, data: { message: 'Forbidden' } },
    });

    const { result } = renderHook(() => useLogSession(), {
      wrapper: createWrapper(),
    });

    await act(async () => {
      await expect(result.current.mutateAsync(requestFixture)).rejects.toBeTruthy();
    });

    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(result.current.error).toBeTruthy();
  });
});

