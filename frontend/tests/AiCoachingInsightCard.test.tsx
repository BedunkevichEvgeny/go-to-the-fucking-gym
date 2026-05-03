import { render, screen } from '@testing-library/react';
import { AiCoachingInsightCard } from '../src/components/AiCoachingInsightCard';

describe('AiCoachingInsightCard', () => {
  it('renders loading state when isPolling=true and no suggestion', () => {
    render(<AiCoachingInsightCard isPolling={true} suggestion={null} timedOut={false} />);
    expect(screen.getByText(/Generating your coaching insight/i)).toBeInTheDocument();
  });

  it('renders suggestion text when suggestion is provided', () => {
    render(<AiCoachingInsightCard isPolling={false} suggestion="Great job today!" timedOut={false} />);
    expect(screen.getByText('Great job today!')).toBeInTheDocument();
    expect(screen.getByText('AI Coaching Insight')).toBeInTheDocument();
  });

  it('renders fallback message when timedOut=true and no suggestion', () => {
    render(<AiCoachingInsightCard isPolling={false} suggestion={null} timedOut={true} />);
    expect(screen.getByText(/Coaching insight unavailable right now/i)).toBeInTheDocument();
  });
});

