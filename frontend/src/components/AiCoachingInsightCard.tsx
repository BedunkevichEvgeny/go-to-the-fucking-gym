interface AiCoachingInsightCardProps {
  isPolling: boolean;
  suggestion: string | null;
  timedOut: boolean;
}

export function AiCoachingInsightCard({ isPolling, suggestion, timedOut }: AiCoachingInsightCardProps) {
  if (isPolling && !suggestion) {
    return (
      <section className="card stack-sm" aria-label="AI Coaching Insight">
        <p className="eyebrow">AI Coaching Insight</p>
        <div role="status" aria-live="polite">
          <span className="spinner" aria-hidden="true" />
          <p>Generating your coaching insight…</p>
        </div>
      </section>
    );
  }

  if (suggestion) {
    return (
      <section className="card stack-sm" aria-label="AI Coaching Insight">
        <p className="eyebrow">AI Coaching Insight</p>
        <p>{suggestion}</p>
      </section>
    );
  }

  if (timedOut) {
    return (
      <section className="card stack-sm" aria-label="AI Coaching Insight">
        <p className="eyebrow">AI Coaching Insight</p>
        <p>Coaching insight unavailable right now. Check back in session history.</p>
      </section>
    );
  }

  return null;
}

