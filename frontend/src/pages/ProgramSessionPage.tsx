import { useNavigate } from 'react-router-dom';
import { useLogSession } from '../hooks/useLogSession';
import { useNextProgramSession } from '../hooks/useNextProgramSession';
import { ProgramSessionForm } from '../features/program-session/ProgramSessionForm';

export function ProgramSessionPage() {
  const navigate = useNavigate();
  const { data, isLoading, error } = useNextProgramSession();
  const logSession = useLogSession();

  if (isLoading) {
    return <p className="card">Loading next program session…</p>;
  }

  if (error) {
    return <p className="card">Could not load the next program session.</p>;
  }

  if (!data) {
    return <p className="card">No active program. Start a free session or seed a program for your user.</p>;
  }

  return (
    <section className="stack-lg">
      <div className="between">
        <div>
          <p className="eyebrow">Next Program Session</p>
          <h1>
            Session {data.sequenceNumber} — {data.name}
          </h1>
        </div>
        <button className="button ghost" type="button" onClick={() => navigate('/')}>
          Back
        </button>
      </div>
      <ProgramSessionForm
        session={data}
        isSaving={logSession.isPending}
        onSubmit={async (payload) => {
          await logSession.mutateAsync(payload);
          navigate('/history');
        }}
      />
    </section>
  );
}

