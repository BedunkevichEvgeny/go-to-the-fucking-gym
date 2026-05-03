import { render, screen } from '@testing-library/react';
import { vi, beforeEach, afterEach, describe, it, expect } from 'vitest';

// ===== T073-BUG-B3: stale localStorage proposal is NOT rendered and is removed on mount =====

const staleProposalFixture = {
  attemptId: 'stale-attempt',
  proposalId: 'stale-proposal-id',
  version: 1,
  status: 'PROPOSED',
  generatedBy: { provider: 'AZURE_OPENAI', deployment: 'gpt-35-turbo' },
  sessions: [
    {
      sequenceNumber: 1,
      name: 'Stale Workout Day',
      exercises: [
        {
          exerciseName: 'Stale Squat Exercise',
          exerciseType: 'STRENGTH',
          targetSets: 3,
          targetReps: 10,
          targetWeight: 100,
          targetWeightUnit: 'KG',
          targetDurationSeconds: null,
          targetDistance: null,
          targetDistanceUnit: null,
        },
      ],
    },
  ],
};

const mockNavigate = vi.fn();

vi.mock('react-router-dom', () => ({
  useNavigate: () => mockNavigate,
}));

vi.mock('../src/hooks/useProfileGoalOnboarding', () => ({
  useCurrentOnboardingAttempt: () => ({ data: null, isLoading: false }),
  useCreateInitialProposal: () => ({ data: undefined, isPending: false, mutate: vi.fn() }),
  useTrackingAccessGate: () => ({ data: { canAccessProgramTracking: true, reasonCode: 'ALLOWED' } }),
  useAcceptProposal: () => ({ isPending: false, mutateAsync: vi.fn() }),
}));

vi.mock('../src/hooks/useProfileGoalProposalReview', () => ({
  useProfileGoalProposalReview: () => ({ rejectProposal: vi.fn(), isRejecting: false }),
}));

import { ProfileGoalOnboardingPage } from '../src/pages/ProfileGoalOnboardingPage';

describe('ProfileGoalOnboardingPage – stale localStorage proposal', () => {
  beforeEach(() => {
    localStorage.setItem('profile-goals.proposal', JSON.stringify(staleProposalFixture));
  });

  afterEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  it('does NOT render the stale proposal exercise name from localStorage', () => {
    render(<ProfileGoalOnboardingPage />);
    expect(screen.queryByText(/Stale Squat Exercise/)).not.toBeInTheDocument();
  });

  it('removes the stale proposal key from localStorage on mount', () => {
    render(<ProfileGoalOnboardingPage />);
    expect(localStorage.getItem('profile-goals.proposal')).toBeNull();
  });
});


