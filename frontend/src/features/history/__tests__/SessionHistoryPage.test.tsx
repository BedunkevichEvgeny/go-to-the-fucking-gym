import '@testing-library/jest-dom/vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { SessionHistoryPage } from '../../../pages/SessionHistoryPage';
import { useSessionHistory } from '../../../hooks/useSessionHistory';

vi.mock('../../../hooks/useSessionHistory', () => ({
  useSessionHistory: vi.fn(),
}));

vi.mock('../SessionDetailView', () => ({
  SessionDetailView: ({ sessionId }: { sessionId: string }) => <p>Session detail: {sessionId}</p>,
}));

function renderWithRouter(initialEntry: string = '/history') {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[initialEntry]}>
        <Routes>
          <Route path="/history" element={<SessionHistoryPage />} />
          <Route path="/history/:sessionId" element={<SessionHistoryPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('SessionHistoryPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders heading, filter controls, and reverse-chronological session list', () => {
    vi.mocked(useSessionHistory).mockReturnValue({
      data: {
        items: [
          {
            sessionId: 'session-new',
            sessionDate: '2026-04-27',
            sessionType: 'PROGRAM',
            exerciseCount: 4,
            totalDurationSeconds: 3600,
            name: 'Upper Body',
          },
          {
            sessionId: 'session-old',
            sessionDate: '2026-04-24',
            sessionType: 'FREE',
            exerciseCount: 2,
            totalDurationSeconds: 1500,
            name: 'Quick Cardio',
          },
        ],
        page: 0,
        size: 20,
        totalItems: 2,
      },
      isLoading: false,
      isError: false,
      error: null,
      refetch: vi.fn(),
    } as unknown as ReturnType<typeof useSessionHistory>);

    renderWithRouter();

    expect(screen.getByText('Workout History')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Filters' })).toBeInTheDocument();
    expect(screen.getByLabelText('From')).toBeInTheDocument();
    expect(screen.getByLabelText('To')).toBeInTheDocument();
    expect(screen.getByText('Exercise name')).toBeInTheDocument();

    const sessionButtons = screen.getAllByRole('button').filter((button) => button.className.includes('session-card'));
    expect(sessionButtons).toHaveLength(2);
    expect(sessionButtons[0]).toHaveTextContent('Upper Body');
    expect(sessionButtons[1]).toHaveTextContent('Quick Cardio');
  });

  it('opens detail view when a session is selected', () => {
    vi.mocked(useSessionHistory).mockReturnValue({
      data: {
        items: [
          {
            sessionId: 'session-123',
            sessionDate: '2026-04-27',
            sessionType: 'PROGRAM',
            exerciseCount: 3,
            totalDurationSeconds: 2400,
            name: 'Leg Day',
          },
        ],
        page: 0,
        size: 20,
        totalItems: 1,
      },
      isLoading: false,
      isError: false,
      error: null,
      refetch: vi.fn(),
    } as unknown as ReturnType<typeof useSessionHistory>);

    renderWithRouter();

    fireEvent.click(screen.getByRole('button', { name: /Leg Day/i }));

    expect(screen.getByText('Session detail: session-123')).toBeInTheDocument();
  });

  it('shows empty history state', () => {
    vi.mocked(useSessionHistory).mockReturnValue({
      data: {
        items: [],
        page: 0,
        size: 20,
        totalItems: 0,
      },
      isLoading: false,
      isError: false,
      error: null,
      refetch: vi.fn(),
    } as unknown as ReturnType<typeof useSessionHistory>);

    renderWithRouter();

    expect(screen.getByText('No workouts logged yet.')).toBeInTheDocument();
  });

  it('updates hook filters and clears them back to defaults', () => {
    vi.mocked(useSessionHistory).mockReturnValue({
      data: {
        items: [],
        page: 0,
        size: 20,
        totalItems: 0,
      },
      isLoading: false,
      isError: false,
      error: null,
      refetch: vi.fn(),
    } as unknown as ReturnType<typeof useSessionHistory>);

    renderWithRouter();

    expect(vi.mocked(useSessionHistory)).toHaveBeenLastCalledWith({
      page: 0,
      size: 20,
      dateFrom: undefined,
      dateTo: undefined,
      exerciseName: undefined,
    });

    fireEvent.change(screen.getByLabelText('From'), { target: { value: '2026-04-01' } });
    fireEvent.change(screen.getByLabelText('To'), { target: { value: '2026-04-30' } });
    fireEvent.change(screen.getByRole('textbox'), { target: { value: 'Bench Press' } });

    expect(vi.mocked(useSessionHistory)).toHaveBeenLastCalledWith({
      page: 0,
      size: 20,
      dateFrom: '2026-04-01',
      dateTo: '2026-04-30',
      exerciseName: 'Bench Press',
    });

    fireEvent.click(screen.getByRole('button', { name: 'Clear' }));

    expect(vi.mocked(useSessionHistory)).toHaveBeenLastCalledWith({
      page: 0,
      size: 20,
      dateFrom: undefined,
      dateTo: undefined,
      exerciseName: undefined,
    });
  });
});

