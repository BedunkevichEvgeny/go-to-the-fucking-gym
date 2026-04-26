import { useState } from 'react';
import { useExerciseLibrary } from '../../hooks/useExerciseLibrary';
import type { ExerciseDto } from '../../types/api';

interface ExerciseLibrarySearchProps {
  onAddExercise: (exercise: ExerciseDto | { name: string }) => void;
}

export function ExerciseLibrarySearch({ onAddExercise }: ExerciseLibrarySearchProps) {
  const [query, setQuery] = useState('');
  const { data = [], isLoading } = useExerciseLibrary(query);

  return (
    <section className="card">
      <h2>Exercise library</h2>
      <label>
        <span>Search exercise or type custom name</span>
        <input value={query} onChange={(event) => setQuery(event.target.value)} />
      </label>
      <div className="stack-sm">
        {isLoading ? <p>Loading exercises…</p> : null}
        {data.map((exercise) => (
          <button
            key={exercise.id}
            className="button ghost align-left"
            type="button"
            onClick={() => onAddExercise(exercise)}
          >
            {exercise.name} <small>({exercise.category})</small>
          </button>
        ))}
        {query.trim() ? (
          <button
            className="button secondary"
            type="button"
            onClick={() => onAddExercise({ name: query.trim() })}
          >
            Add custom exercise “{query.trim()}”
          </button>
        ) : null}
      </div>
    </section>
  );
}

