import { SessionTypeBadge } from '../../components/SessionTypeBadge';
import type { SessionHistoryItem } from '../../types/api';

/** Props for rendering selectable workout history cards. */
interface SessionHistoryListProps {
  /** Sessions displayed in reverse-chronological order. */
  sessions: SessionHistoryItem[];
  /** Called when a session card is selected. */
  onSelectSession: (sessionId: string) => void;
}

export function SessionHistoryList({ sessions, onSelectSession }: SessionHistoryListProps) {
  if (sessions.length === 0) {
    return <p className="card">No workouts logged yet.</p>;
  }

  return (
    <div className="stack-md">
      {sessions.map((session) => (
        <button key={session.sessionId} className="card align-left session-card" type="button" onClick={() => onSelectSession(session.sessionId)}>
          <div className="between">
            <strong>{session.name || 'Workout session'}</strong>
            <SessionTypeBadge sessionType={session.sessionType} />
          </div>
          <p>{session.sessionDate}</p>
          <p>{session.exerciseCount} exercises • {session.totalDurationSeconds ?? 0}s</p>
        </button>
      ))}
    </div>
  );
}

