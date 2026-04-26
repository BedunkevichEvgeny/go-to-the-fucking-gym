import { fireEvent, render, screen } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ExerciseLibrarySearch } from '../ExerciseLibrarySearch';
import { useExerciseLibrary } from '../../../hooks/useExerciseLibrary';

vi.mock('../../../hooks/useExerciseLibrary', () => ({
  useExerciseLibrary: vi.fn(),
}));

describe('ExerciseLibrarySearch', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useExerciseLibrary).mockReturnValue({
      data: [
        {
          id: '11111111-1111-1111-1111-111111111111',
          name: 'Bench Press',
          category: 'Chest',
          type: 'STRENGTH',
          description: 'Barbell bench press',
        },
      ],
      isLoading: false,
      isError: false,
      error: null,
    } as unknown as ReturnType<typeof useExerciseLibrary>);
  });

  it('triggers useExerciseLibrary with typed search query', () => {
    render(<ExerciseLibrarySearch onAddExercise={vi.fn()} />);

    fireEvent.change(screen.getByLabelText('Search exercise or type custom name'), {
      target: { value: 'bench' },
    });

    expect(useExerciseLibrary).toHaveBeenLastCalledWith('bench');
  });

  it('renders clickable search results with exercise name and category', () => {
    const onAddExercise = vi.fn();
    render(<ExerciseLibrarySearch onAddExercise={onAddExercise} />);

    const resultButton = screen.getByRole('button', { name: /Bench Press/i });
    expect(resultButton).toHaveTextContent('Bench Press');
    expect(resultButton).toHaveTextContent('(Chest)');

    fireEvent.click(resultButton);
    expect(onAddExercise).toHaveBeenCalledWith(expect.objectContaining({ name: 'Bench Press', category: 'Chest' }));
  });

  it('allows adding custom exercise from typed input', () => {
    const onAddExercise = vi.fn();
    render(<ExerciseLibrarySearch onAddExercise={onAddExercise} />);

    fireEvent.change(screen.getByLabelText('Search exercise or type custom name'), {
      target: { value: 'Tire Flip' },
    });
    fireEvent.click(screen.getByRole('button', { name: /Add custom exercise/i }));

    expect(onAddExercise).toHaveBeenCalledWith({ name: 'Tire Flip' });
  });
});

