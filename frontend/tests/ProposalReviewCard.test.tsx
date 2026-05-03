import { render, screen } from '@testing-library/react';
import { vi } from 'vitest';
import { ProposalReviewCard } from '../src/features/profile-goals/ProposalReviewCard';
import type { PlanProposalResponse } from '../src/types/onboarding';

// ===== T072: ProposalReviewCard renders exercise details correctly =====

const baseProposal: PlanProposalResponse = {
  attemptId: 'attempt-1',
  proposalId: 'proposal-1',
  version: 1,
  status: 'PROPOSED',
  generatedBy: { provider: 'AZURE_OPENAI', deployment: 'gpt-35-turbo' },
  sessions: [],
};

describe('ProposalReviewCard', () => {
  const onReject = vi.fn();
  const onAccept = vi.fn();

  // ── Strength exercise: sets×reps and weight ──────────────────────────────

  it('renders sets×reps for a strength exercise', () => {
    const proposal: PlanProposalResponse = {
      ...baseProposal,
      sessions: [
        {
          sequenceNumber: 1,
          name: 'Strength Day',
          exercises: [
            {
              exerciseName: 'Bench Press',
              exerciseType: 'STRENGTH',
              targetSets: 4,
              targetReps: 8,
              targetWeight: 80,
              targetWeightUnit: 'KG',
              targetDurationSeconds: null,
              targetDistance: null,
              targetDistanceUnit: null,
            },
          ],
        },
      ],
    };

    render(<ProposalReviewCard proposal={proposal} isBusy={false} onReject={onReject} onAccept={onAccept} />);

    expect(screen.getByText(/4×8 reps/)).toBeInTheDocument();
    expect(screen.getByText(/80/)).toBeInTheDocument();
    expect(screen.getByText(/KG/)).toBeInTheDocument();
  });

  // ── Cardio exercise: duration in seconds ────────────────────────────────

  it('renders duration for a cardio exercise', () => {
    const proposal: PlanProposalResponse = {
      ...baseProposal,
      sessions: [
        {
          sequenceNumber: 1,
          name: 'Cardio Day',
          exercises: [
            {
              exerciseName: 'Treadmill Run',
              exerciseType: 'CARDIO',
              targetSets: null,
              targetReps: null,
              targetWeight: null,
              targetWeightUnit: null,
              targetDurationSeconds: 1800,
              targetDistance: null,
              targetDistanceUnit: null,
            },
          ],
        },
      ],
    };

    render(<ProposalReviewCard proposal={proposal} isBusy={false} onReject={onReject} onAccept={onAccept} />);

    expect(screen.getByText(/1800s/)).toBeInTheDocument();
  });

  // ── Distance exercise: distance value and unit ───────────────────────────

  it('renders distance and distance unit for a distance exercise', () => {
    const proposal: PlanProposalResponse = {
      ...baseProposal,
      sessions: [
        {
          sequenceNumber: 1,
          name: 'Run Day',
          exercises: [
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
          ],
        },
      ],
    };

    render(<ProposalReviewCard proposal={proposal} isBusy={false} onReject={onReject} onAccept={onAccept} />);

    expect(screen.getByText(/\/ 5 KM/)).toBeInTheDocument();
  });

  // ── Absent optional fields: sets/reps/weight NOT rendered when null ──────

  it('does NOT render sets×reps when targetSets and targetReps are null', () => {
    const proposal: PlanProposalResponse = {
      ...baseProposal,
      sessions: [
        {
          sequenceNumber: 1,
          name: 'Cardio Only',
          exercises: [
            {
              exerciseName: 'Jump Rope',
              exerciseType: 'CARDIO',
              targetSets: null,
              targetReps: null,
              targetWeight: null,
              targetWeightUnit: null,
              targetDurationSeconds: 300,
              targetDistance: null,
              targetDistanceUnit: null,
            },
          ],
        },
      ],
    };

    render(<ProposalReviewCard proposal={proposal} isBusy={false} onReject={onReject} onAccept={onAccept} />);

    expect(screen.queryByText(/×.*reps/)).not.toBeInTheDocument();
  });

  it('does NOT render weight when targetWeight is null', () => {
    const proposal: PlanProposalResponse = {
      ...baseProposal,
      sessions: [
        {
          sequenceNumber: 1,
          name: 'Bodyweight Day',
          exercises: [
            {
              exerciseName: 'Pull-Up',
              exerciseType: 'BODYWEIGHT',
              targetSets: 3,
              targetReps: 10,
              targetWeight: null,
              targetWeightUnit: null,
              targetDurationSeconds: null,
              targetDistance: null,
              targetDistanceUnit: null,
            },
          ],
        },
      ],
    };

    render(<ProposalReviewCard proposal={proposal} isBusy={false} onReject={onReject} onAccept={onAccept} />);

    expect(screen.queryByText(/@/)).not.toBeInTheDocument();
  });
});


