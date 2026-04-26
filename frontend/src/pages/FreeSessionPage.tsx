import { useNavigate } from 'react-router-dom';
import { FreeSessionForm } from '../features/free-session/FreeSessionForm';
import { useLogFreeSession } from '../hooks/useLogFreeSession';

export function FreeSessionPage() {
  const navigate = useNavigate();
  const logSession = useLogFreeSession();

  return (
    <section className="stack-lg">
      <div className="between">
        <div>
          <p className="eyebrow">Start Free Session</p>
          <h1>Log an off-program workout</h1>
        </div>
        <button className="button ghost" type="button" onClick={() => navigate('/')}>
          Back
        </button>
      </div>
      <FreeSessionForm
        isSaving={logSession.isPending}
        onSubmit={async (payload) => {
          await logSession.mutateAsync(payload);
          navigate('/history');
        }}
      />
    </section>
  );
}

