import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { ProposalReviewCard } from '../src/features/profile-goals/ProposalReviewCard';
import type { PlanProposalResponse } from '../src/types/onboarding';

// ===== T072-BUG-B2: ProposalReviewCard exercise detail field rendering =====

const makeProposal = (exercises: object[]): PlanProposalResponse => ({
  attemptId: 'attempt-1',
  proposalId: 'proposal-1',
  version: 1,
  status: 'PROPOSED' as const,
  generatedBy: { provider: 'AZURE_OPENAI', deployment: 'gpt-35-turbo' },
  sessions: [
    {
      sequenceNumber: 1,
      name: 'Day 1',
      exercises: exercises as PlanProposalResponse['sessions'][0]['exercises'],
    },
  ],
});

const noop = vi.fn();

describe('ProposalReviewCard – exercise detail rendering', () => {
  it('renders targetSets, targetReps, targetWeight and targetWeightUnit for strength exercises', () => {
    const proposal = makeProposal([
      {
        exerciseName: 'Barbell Squat',
        exerciseType: 'STRENGTH',
        targetSets: 4,
        targetReps: 8,
        targetWeight: 100,
        targetWeightUnit: 'KG',
        targetDurationSeconds: null,
        targetDistance: null,
        targetDistanceUnit: null,
      },
    ]);

    render(<ProposalReviewCard proposal={proposal} isBusy={false} onReject={noop} onAccept={noop} />);

    expect(screen.getByText(/Barbell Squat/)).toBeInTheDocument();
    expect(screen.getByText(/4×8 reps/)).toBeInTheDocument();
    expect(screen.getByText(/100 KG/)).toBeInTheDocument();
  });

  it('renders targetDurationSeconds as formatted duration string', () => {
    const proposal = makeProposal([
      {
        exerciseName: 'Plank Hold',
        exerciseType: 'STRENGTH',
        targetSets: null,
        targetReps: null,
        targetWeight: null,
        targetWeightUnit: null,
        targetDurationSeconds: 60,
        targetDistance: null,
        targetDistanceUnit: null,
      },
    ]);

    render(<ProposalReviewCard proposal={proposal} isBusy={false} onReject={noop} onAccept={noop} />);

    expect(screen.getByText(/60s/)).toBeInTheDocument();
  });

  it('renders targetDistance and targetDistanceUnit for cardio exercises', () => {
    const proposal = makeProposal([
      {
        exerciseName: 'Outdoor Run',
        exerciseType: 'CARDIO',
        targetSets: null,
        targetReps: null,
        targetWeight: null,
        targetWeightUnit: null,
        targetDurationSeconds: null,
        targetDistance: 5,
        targetDistanceUnit: 'KM',
      },
    ]);

    render(<ProposalReviewCard proposal={proposal} isBusy={false} onReject={noop} onAccept={noop} />);

    expect(screen.getByText(/Outdoor Run/)).toBeInTheDocument();
    // Component renders: " / 5 KM"
    expect(screen.getByText(/5 KM/)).toBeInTheDocument();
  });

  it('renders no empty/broken detail rows when no optional fields are set', () => {
    const proposal = makeProposal([
      {
        exerciseName: 'Mystery Exercise',
        exerciseType: 'STRENGTH',
        targetSets: null,
        targetReps: null,
        targetWeight: null,
        targetWeightUnit: null,
        targetDurationSeconds: null,
        targetDistance: null,
        targetDistanceUnit: null,
      },
    ]);

    render(<ProposalReviewCard proposal={proposal} isBusy={false} onReject={noop} onAccept={noop} />);

    expect(screen.getByText(/Mystery Exercise/)).toBeInTheDocument();
    expect(screen.queryByText(/×/)).not.toBeInTheDocument();
    expect(screen.queryByText(/@/)).not.toBeInTheDocument();
    expect(screen.queryByText(/\d+s/)).not.toBeInTheDocument();
  });
});
