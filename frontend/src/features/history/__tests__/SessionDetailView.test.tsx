import '@testing-library/jest-dom/vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { SessionDetailView } from '../SessionDetailView';
import { useSessionDetail } from '../../../hooks/useSessionDetail';

vi.mock('../../../hooks/useSessionDetail', () => ({
  useSessionDetail: vi.fn(),
}));

vi.mock('../ExerciseProgressionLink', () => ({
  ExerciseProgressionLink: ({ exerciseName }: { exerciseName: string }) => <button type="button">View Progression {exerciseName}</button>,
}));

const mockedNavigate = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockedNavigate,
  };
});

describe('SessionDetailView', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders complete session details with strength, cardio, and feelings data', () => {
    vi.mocked(useSessionDetail).mockReturnValue({
      data: {
        sessionId: 'session-1',
        sessionType: 'PROGRAM',
        sessionDate: '2026-04-27',
        name: 'Upper Body Day',
        totalDurationSeconds: 5400,
        feelings: {
          rating: 9,
          comment: 'Felt very strong',
        },
        exerciseEntries: [
          {
            exerciseName: 'Bench Press',
            exerciseType: 'STRENGTH',
            sets: [{ setOrder: 1, reps: 8, isBodyWeight: false, weightValue: 70, weightUnit: 'KG' }],
            cardioLaps: [],
          },
          {
            exerciseName: 'Running',
            exerciseType: 'CARDIO',
            sets: [],
            cardioLaps: [{ lapOrder: 1, durationSeconds: 900, distanceValue: 2.5, distanceUnit: 'KM' }],
          },
        ],
      },
      isLoading: false,
      isError: false,
      error: null,
    } as unknown as ReturnType<typeof useSessionDetail>);

    render(
      <MemoryRouter>
        <SessionDetailView sessionId="session-1" />
      </MemoryRouter>,
    );

    expect(screen.getByText('Upper Body Day')).toBeInTheDocument();
    expect(screen.getByText('2026-04-27')).toBeInTheDocument();
    expect(screen.getByText('Duration: 5400s')).toBeInTheDocument();
    expect(screen.getByText('Feeling: 9/10')).toBeInTheDocument();
    expect(screen.getByText('Felt very strong')).toBeInTheDocument();

    expect(screen.getByText('Bench Press')).toBeInTheDocument();
    expect(screen.getByText('Running')).toBeInTheDocument();
    expect(screen.getAllByRole('table')).toHaveLength(2);

    expect(screen.getByText('8')).toBeInTheDocument();
    expect(screen.getByText('70 KG')).toBeInTheDocument();
    expect(screen.getByText('900s')).toBeInTheDocument();
    expect(screen.getByText('2.5 KM')).toBeInTheDocument();

    expect(screen.getByRole('link', { name: 'Back to history' })).toBeInTheDocument();
  });

  it('uses the back button to return to previous page', () => {
    vi.mocked(useSessionDetail).mockReturnValue({
      data: {
        sessionId: 'session-2',
        sessionType: 'FREE',
        sessionDate: '2026-04-27',
        feelings: { rating: 7 },
        exerciseEntries: [],
      },
      isLoading: false,
      isError: false,
      error: null,
    } as unknown as ReturnType<typeof useSessionDetail>);

    render(
      <MemoryRouter>
        <SessionDetailView sessionId="session-2" />
      </MemoryRouter>,
    );

    fireEvent.click(screen.getByRole('button', { name: 'Back' }));

    expect(mockedNavigate).toHaveBeenCalledWith(-1);
  });
});

