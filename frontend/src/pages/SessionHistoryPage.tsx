import { useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { SessionDetailView } from '../features/history/SessionDetailView';
import { SessionFilterSection } from '../features/history/SessionFilterSection';
import { SessionHistoryList } from '../features/history/SessionHistoryList';
import { useSessionHistory } from '../hooks/useSessionHistory';

export function SessionHistoryPage() {
  const navigate = useNavigate();
  const { sessionId } = useParams();
  const [filters, setFilters] = useState({ dateFrom: '', dateTo: '', exerciseName: '' });
  const historyQuery = useSessionHistory({
    page: 0,
    size: 20,
    dateFrom: filters.dateFrom || undefined,
    dateTo: filters.dateTo || undefined,
    exerciseName: filters.exerciseName || undefined,
  });

  const content = useMemo(() => {
    if (sessionId) {
      return <SessionDetailView sessionId={sessionId} />;
    }
    if (historyQuery.isLoading) {
      return <p className="card">Loading workout history…</p>;
    }
    return <SessionHistoryList sessions={historyQuery.data?.items ?? []} onSelectSession={(selected) => navigate(`/history/${selected}`)} />;
  }, [historyQuery.data?.items, historyQuery.isLoading, navigate, sessionId]);

  return (
    <section className="stack-lg">
      <div className="between">
        <div>
          <p className="eyebrow">Workout History</p>
          <h1>Your sessions over time</h1>
        </div>
        <button className="button ghost" type="button" onClick={() => navigate('/')}>
          Back
        </button>
      </div>
      {!sessionId ? (
        <SessionFilterSection
          dateFrom={filters.dateFrom}
          dateTo={filters.dateTo}
          exerciseName={filters.exerciseName}
          onChange={setFilters}
          onClear={() => setFilters({ dateFrom: '', dateTo: '', exerciseName: '' })}
        />
      ) : null}
      {content}
    </section>
  );
}

