import { render, screen } from '@testing-library/react';
import { vi, afterEach, describe, it, expect } from 'vitest';

// ===== T081-BUG-010-FE-TEST: accepted state renders read-only "Your Active Program" card =====

const acceptedProposalFixture = {
  attemptId: 'accepted-attempt-id',
  proposalId: 'accepted-proposal-id',
  version: 2,
  status: 'ACCEPTED',
  generatedBy: { provider: 'AZURE_OPENAI', deployment: 'gpt-35-turbo' },
  sessions: [
    {
      sequenceNumber: 1,
      name: 'Push Day',
      exercises: [
        {
          exerciseName: 'Bench Press',
          exerciseType: 'STRENGTH',
          targetSets: 3,
          targetReps: 8,
          targetWeight: 60,
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
  useCurrentOnboardingAttempt: () => ({
    data: {
      attemptId: 'accepted-attempt-id',
      status: 'ACCEPTED',
      latestProposal: acceptedProposalFixture,
    },
    isLoading: false,
  }),
  useCreateInitialProposal: () => ({ data: undefined, isPending: false, mutate: vi.fn() }),
  useTrackingAccessGate: () => ({ data: { canAccessProgramTracking: true, reasonCode: 'ALLOWED' } }),
  useAcceptProposal: () => ({ isPending: false, mutateAsync: vi.fn() }),
}));

vi.mock('../src/hooks/useProfileGoalProposalReview', () => ({
  useProfileGoalProposalReview: () => ({ rejectProposal: vi.fn(), isRejecting: false }),
}));

import { ProfileGoalOnboardingPage } from '../src/pages/ProfileGoalOnboardingPage';

describe('ProfileGoalOnboardingPage – accepted state (read-only)', () => {
  afterEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  it('renders "Your Active Program" heading', () => {
    render(<ProfileGoalOnboardingPage />);
    expect(screen.getByRole('heading', { name: /your active program/i })).toBeInTheDocument();
  });

  it('does NOT render an Accept button', () => {
    render(<ProfileGoalOnboardingPage />);
    expect(screen.queryByRole('button', { name: /accept/i })).not.toBeInTheDocument();
  });

  it('does NOT render a Reject button or feedback form', () => {
    render(<ProfileGoalOnboardingPage />);
    expect(screen.queryByRole('button', { name: /reject/i })).not.toBeInTheDocument();
  });

  it('renders proposal exercise details in read-only view', () => {
    render(<ProfileGoalOnboardingPage />);
    expect(screen.getByText(/Bench Press/i)).toBeInTheDocument();
  });
});

