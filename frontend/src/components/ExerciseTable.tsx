import type { ExerciseEntryView } from '../types/api';

/** Props for rendering a read-only exercise performance table. */
interface ExerciseTableProps {
  /** Exercise entry containing either sets or cardio laps. */
  entry: ExerciseEntryView;
}

export function ExerciseTable({ entry }: ExerciseTableProps) {
  if (entry.exerciseType === 'CARDIO') {
    return (
      <table className="table">
        <thead>
          <tr>
            <th>Lap</th>
            <th>Duration</th>
            <th>Distance</th>
          </tr>
        </thead>
        <tbody>
          {entry.cardioLaps.map((lap) => (
            <tr key={lap.lapOrder}>
              <td>{lap.lapOrder}</td>
              <td>{lap.durationSeconds}s</td>
              <td>{lap.distanceValue ? `${lap.distanceValue} ${lap.distanceUnit ?? ''}` : '—'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    );
  }

  return (
    <table className="table">
      <thead>
        <tr>
          <th>Set</th>
          <th>Reps</th>
          <th>Weight</th>
          <th>Bodyweight</th>
        </tr>
      </thead>
      <tbody>
        {entry.sets.map((set) => (
          <tr key={set.setOrder}>
            <td>{set.setOrder}</td>
            <td>{set.reps}</td>
            <td>{set.weightValue ? `${set.weightValue} ${set.weightUnit ?? ''}` : '—'}</td>
            <td>{set.isBodyWeight ? 'Yes' : 'No'}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

