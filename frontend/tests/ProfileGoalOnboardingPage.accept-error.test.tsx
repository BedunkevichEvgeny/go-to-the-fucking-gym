import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { vi, afterEach, describe, it, expect } from 'vitest';

// ===== T074-BUG-B4: onAccept error renders message and does NOT navigate =====

const validProposal = {
  attemptId: 'attempt-valid',
  proposalId: 'proposal-valid',
  version: 1,
  status: 'PROPOSED',
  generatedBy: { provider: 'AZURE_OPENAI', deployment: 'gpt-35-turbo' },
  sessions: [],
};

const mockNavigate = vi.fn();
const mockMutateAsync = vi.fn().mockRejectedValue(new Error('404'));

vi.mock('react-router-dom', () => ({
  useNavigate: () => mockNavigate,
}));

vi.mock('../src/hooks/useProfileGoalOnboarding', () => ({
  useCurrentOnboardingAttempt: () => ({
    data: { latestProposal: validProposal },
    isLoading: false,
  }),
  useCreateInitialProposal: () => ({ data: undefined, isPending: false, mutate: vi.fn() }),
  useTrackingAccessGate: () => ({ data: { canAccessProgramTracking: true, reasonCode: 'ALLOWED' } }),
  useAcceptProposal: () => ({ isPending: false, mutateAsync: mockMutateAsync }),
}));

vi.mock('../src/hooks/useProfileGoalProposalReview', () => ({
  useProfileGoalProposalReview: () => ({ rejectProposal: vi.fn(), isRejecting: false }),
}));

import { ProfileGoalOnboardingPage } from '../src/pages/ProfileGoalOnboardingPage';

describe('ProfileGoalOnboardingPage – accept error handling', () => {
  afterEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  it('shows error message and does NOT navigate when mutateAsync rejects', async () => {
    render(<ProfileGoalOnboardingPage />);

    const acceptButton = screen.getByRole('button', { name: /accept plan/i });
    fireEvent.click(acceptButton);

    await waitFor(() => {
      expect(screen.getByText('Failed to accept the plan. Please try again.')).toBeInTheDocument();
    });

    expect(mockNavigate).not.toHaveBeenCalled();
  });
});


