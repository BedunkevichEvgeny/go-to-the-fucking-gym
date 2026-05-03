import '@testing-library/jest-dom/vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { ProposalReviewCard } from '../ProposalReviewCard';
import type { PlanProposalResponse } from '../../../types/onboarding';

const proposal: PlanProposalResponse = {
  attemptId: '11111111-1111-1111-1111-111111111111',
  proposalId: '22222222-2222-2222-2222-222222222222',
  version: 1,
  status: 'PROPOSED',
  generatedBy: {
    provider: 'AZURE_OPENAI',
    deployment: 'gpt-35-turbo',
  },
  sessions: [
    {
      sequenceNumber: 1,
      name: 'Upper Strength',
      exercises: [{ exerciseName: 'Bench Press', exerciseType: 'STRENGTH' }],
    },
  ],
};

describe('ProposalReviewCard', () => {
  it('collects reject feedback and submits revision request', () => {
    const onReject = vi.fn();
    const onAccept = vi.fn();

    render(<ProposalReviewCard proposal={proposal} isBusy={false} onReject={onReject} onAccept={onAccept} />);

    fireEvent.change(screen.getByLabelText(/What should change\?/i), {
      target: { value: 'Reduce lower body intensity' },
    });
    fireEvent.click(screen.getByRole('button', { name: /Reject & Revise/i }));

    expect(onReject).toHaveBeenCalledWith('Reduce lower body intensity');
  });

  it('disables actions while submit is in progress', () => {
    const onReject = vi.fn();
    const onAccept = vi.fn();

    render(<ProposalReviewCard proposal={proposal} isBusy onReject={onReject} onAccept={onAccept} />);

    expect(screen.getByRole('button', { name: /Accept Plan/i })).toBeDisabled();
    expect(screen.getByRole('button', { name: /Reject & Revise/i })).toBeDisabled();
  });
});

