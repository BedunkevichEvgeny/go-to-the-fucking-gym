import { useNavigate } from 'react-router-dom';
import { useTrackingAccessGate } from '../hooks/useProfileGoalOnboarding';
import { useLogSession } from '../hooks/useLogSession';
import { useNextProgramSession } from '../hooks/useNextProgramSession';
import { ProgramSessionForm } from '../features/program-session/ProgramSessionForm';

export function ProgramSessionPage() {
  const navigate = useNavigate();
  const gate = useTrackingAccessGate();
  const { data, isLoading, error } = useNextProgramSession();
  const logSession = useLogSession();

  if (gate.isLoading) {
    return <p className="card">Checking onboarding access…</p>;
  }

  if (gate.data && !gate.data.canAccessProgramTracking) {
    return (
      <section className="card stack-sm">
        <p>Onboarding is required before logging program sessions.</p>
        <button className="button primary" type="button" onClick={() => navigate('/profile-goals')}>
          Go to My Profile & Goals
        </button>
      </section>
    );
  }

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

