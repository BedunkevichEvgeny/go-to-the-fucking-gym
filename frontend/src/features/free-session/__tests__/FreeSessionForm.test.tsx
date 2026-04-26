import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { FreeSessionForm } from '../FreeSessionForm';
import type { ExerciseDto, LoggedSessionCreateRequest } from '../../../types/api';
import { useExerciseLibrary } from '../../../hooks/useExerciseLibrary';

vi.mock('../../../hooks/useExerciseLibrary', () => ({
  useExerciseLibrary: vi.fn(),
}));

const exercises: ExerciseDto[] = [
  {
    id: '11111111-1111-1111-1111-111111111111',
    name: 'Bench Press',
    category: 'Chest',
    type: 'STRENGTH',
    description: 'Barbell bench press',
  },
  {
    id: '22222222-2222-2222-2222-222222222222',
    name: 'Running',
    category: 'Cardio',
    type: 'CARDIO',
    description: 'Steady state run',
  },
];

describe('FreeSessionForm', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useExerciseLibrary).mockReturnValue({
      data: exercises,
      isLoading: false,
      isError: false,
      error: null,
    } as unknown as ReturnType<typeof useExerciseLibrary>);
  });

  it('renders free session controls and exercise search section', () => {
    render(<FreeSessionForm onSubmit={vi.fn()} isSaving={false} />);

    expect(screen.getByText('Exercise library')).toBeTruthy();
    expect(screen.getByText('Search exercise or type custom name')).toBeTruthy();
    expect(screen.getByText('How did it feel?')).toBeTruthy();
    expect(screen.getByRole('slider')).toBeTruthy();
    expect(screen.getByLabelText('Comment')).toBeTruthy();
  });

  it('adds library and custom exercises and supports set/lap interactions', () => {
    render(<FreeSessionForm onSubmit={vi.fn()} isSaving={false} />);

    fireEvent.click(screen.getByRole('button', { name: /Bench Press/i }));
    fireEvent.click(screen.getByRole('button', { name: /Running/i }));
    fireEvent.change(screen.getByLabelText('Search exercise or type custom name'), { target: { value: 'Tire Flip' } });
    fireEvent.click(screen.getByRole('button', { name: /Add custom exercise/i }));

    expect(screen.getAllByText('Remove exercise').length).toBe(3);

    const addSetButtons = screen.getAllByRole('button', { name: 'Add set' });
    fireEvent.click(addSetButtons[0]);
    fireEvent.click(screen.getByRole('button', { name: 'Add lap' }));

    expect(screen.queryAllByRole('button', { name: 'Remove set' }).length).toBeGreaterThan(0);
    expect(screen.queryAllByRole('button', { name: 'Remove lap' }).length).toBeGreaterThan(0);
  });

  it('keeps save disabled until at least one exercise is added', () => {
    render(<FreeSessionForm onSubmit={vi.fn()} isSaving={false} />);

    expect(screen.getByRole('button', { name: 'Save free session' })).toBeDisabled();

    fireEvent.click(screen.getByRole('button', { name: /Bench Press/i }));

    expect(screen.getByRole('button', { name: 'Save free session' })).toBeEnabled();
  });

  it('submits expected FREE payload without programSessionId', async () => {
    const onSubmit = vi.fn<(payload: LoggedSessionCreateRequest) => Promise<void>>().mockResolvedValue(undefined);
    render(<FreeSessionForm onSubmit={onSubmit} isSaving={false} />);

    fireEvent.click(screen.getByRole('button', { name: /Bench Press/i }));
    fireEvent.change(screen.getByRole('slider'), { target: { value: '9' } });
    fireEvent.change(screen.getByLabelText('Comment'), { target: { value: 'Great day' } });

    fireEvent.click(screen.getByRole('button', { name: 'Save free session' }));

    await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1));
    const payload = onSubmit.mock.calls[0][0];

    expect(payload.sessionType).toBe('FREE');
    expect(payload.programSessionId).toBeUndefined();
    expect(payload.exerciseEntries.length).toBe(1);
    expect(payload.feelings.rating).toBe(9);
    expect(payload.feelings.comment).toBe('Great day');
  });
});

