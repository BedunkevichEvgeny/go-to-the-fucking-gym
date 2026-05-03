import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTrackingAccessGate } from '../hooks/useProfileGoalOnboarding';
import { useLogSession } from '../hooks/useLogSession';
import { useNextProgramSession } from '../hooks/useNextProgramSession';
import { usePollSessionSuggestion } from '../hooks/usePollSessionSuggestion';
import { AiCoachingInsightCard } from '../components/AiCoachingInsightCard';
import { ProgramSessionForm } from '../features/program-session/ProgramSessionForm';

export function ProgramSessionPage() {
  const navigate = useNavigate();
  const gate = useTrackingAccessGate();
  const { data, isLoading, error } = useNextProgramSession();
  const logSession = useLogSession();
  const [savedSessionId, setSavedSessionId] = useState<string | null>(null);
  const { suggestion, timedOut, isPolling } = usePollSessionSuggestion(savedSessionId);

  if (gate.isLoading) {
    return <p className="card">Checking onboarding access…</p>;
  }

  if (gate.data && !gate.data.canAccessProgramTracking) {
    return (
      <section className="card stack-sm">
        <p>Onboarding is required before logging program sessions.</p>
        <button className="button primary" type="button" onClick={() => navigate('/profile-goals')}>
          Go to My Profile &amp; Goals
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

  if (savedSessionId) {
    return (
      <section className="stack-lg">
        <section className="card stack-sm">
          <p className="eyebrow">Session saved!</p>
          <p>Your workout has been recorded.</p>
        </section>
        <AiCoachingInsightCard isPolling={isPolling} suggestion={suggestion} timedOut={timedOut} />
        <button
          className="button primary"
          type="button"
          onClick={() => navigate('/history')}
        >
          Continue to History
        </button>
      </section>
    );
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
          const result = await logSession.mutateAsync(payload);
          setSavedSessionId(result.sessionId);
        }}
      />
    </section>
  );
}

