import { useState } from 'react';
import type { PlanProposalResponse } from '../../types/onboarding';

type ProposalReviewCardProps = {
  proposal: PlanProposalResponse;
  isBusy: boolean;
  onReject: (requestedChanges: string) => void;
  onAccept: () => void;
};

export function ProposalReviewCard({ proposal, isBusy, onReject, onAccept }: ProposalReviewCardProps) {
  const [requestedChanges, setRequestedChanges] = useState('');

  return (
    <section className="card stack-sm">
      <h2>Latest Proposal</h2>
      <p>
        Version {proposal.version} • {proposal.generatedBy.provider} ({proposal.generatedBy.deployment})
      </p>

      {proposal.sessions.map((session) => (
        <article key={`${session.sequenceNumber}-${session.name}`} className="stack-xs">
          <strong>
            Session {session.sequenceNumber}: {session.name}
          </strong>
          <ul>
            {session.exercises.map((exercise) => (
              <li key={`${session.sequenceNumber}-${exercise.exerciseName}`}>
                <strong>{exercise.exerciseName}</strong>
                {exercise.targetSets != null && exercise.targetReps != null && (
                  <span> — {exercise.targetSets}×{exercise.targetReps} reps</span>
                )}
                {exercise.targetWeight != null && (
                  <span> @ {exercise.targetWeight} {exercise.targetWeightUnit ?? ''}</span>
                )}
                {exercise.targetDurationSeconds != null && (
                  <span> — {exercise.targetDurationSeconds}s</span>
                )}
                {exercise.targetDistance != null && (
                  <span> / {exercise.targetDistance} {exercise.targetDistanceUnit ?? ''}</span>
                )}
              </li>
            ))}
          </ul>
        </article>
      ))}

      <label className="stack-xs">
        <span>What should change?</span>
        <textarea
          aria-label="What should change?"
          placeholder="Describe what you want to adjust"
          value={requestedChanges}
          onChange={(event) => setRequestedChanges(event.target.value)}
          rows={3}
        />
      </label>

      <div className="between">
        <button className="button secondary" type="button" disabled={isBusy} onClick={onAccept}>
          Accept Plan
        </button>
        <button
          className="button ghost"
          type="button"
          disabled={isBusy}
          onClick={() => onReject(requestedChanges)}
        >
          Reject & Revise
        </button>
      </div>
    </section>
  );
}

