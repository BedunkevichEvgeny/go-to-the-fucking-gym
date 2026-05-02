import { Link, useNavigate } from 'react-router-dom';
import { ExerciseTable } from '../../components/ExerciseTable';
import { SessionTypeBadge } from '../../components/SessionTypeBadge';
import { useSessionDetail } from '../../hooks/useSessionDetail';
import type { ExerciseEntryView } from '../../types/api';
import { ExerciseProgressionLink } from './ExerciseProgressionLink';

/** Props for loading and rendering a single session details screen. */
interface SessionDetailViewProps {
  /** Session identifier used by the detail query hook. */
  sessionId: string;
}

export function SessionDetailView({ sessionId }: SessionDetailViewProps) {
  const navigate = useNavigate();
  const { data, isLoading } = useSessionDetail(sessionId);

  if (isLoading) {
    return <p className="card">Loading session details…</p>;
  }

  if (!data) {
    return <p className="card">Session not found.</p>;
  }

  return (
    <section className="stack-md">
      <button className="button ghost" type="button" onClick={() => navigate(-1)}>
        Back
      </button>
      <section className="card stack-sm">
        <div className="between">
          <h2>{data.name || 'Workout session'}</h2>
          <SessionTypeBadge sessionType={data.sessionType} />
        </div>
        <p>{data.sessionDate}</p>
        <p>Duration: {data.totalDurationSeconds ?? 0}s</p>
        <p>Feeling: {data.feelings?.rating}/10</p>
        {data.feelings?.comment ? <p>{data.feelings.comment}</p> : null}
      </section>
      {data.exerciseEntries.map((entry: ExerciseEntryView) => (
        <section key={entry.exerciseName} className="card stack-sm">
          <div className="between">
            <h3>{entry.exerciseName}</h3>
            <ExerciseProgressionLink exerciseName={entry.exerciseName} />
          </div>
          <ExerciseTable entry={entry} />
        </section>
      ))}
      <Link className="button secondary" to="/history">
        Back to history
      </Link>
    </section>
  );
}


