import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ProposalReviewCard } from '../features/profile-goals/ProposalReviewCard';
import {
  useAcceptProposal,
  useCreateInitialProposal,
  useCurrentOnboardingAttempt,
  useTrackingAccessGate,
} from '../hooks/useProfileGoalOnboarding';
import { useProfileGoalProposalReview } from '../hooks/useProfileGoalProposalReview';
import type {
  GoalTargetBucket,
  OnboardingPrimaryGoal,
  OnboardingSubmissionRequest,
} from '../types/onboarding';

const GOAL_OPTIONS: OnboardingPrimaryGoal[] = ['LOSE_WEIGHT', 'BUILD_HEALTHY_BODY', 'STRENGTH', 'BUILD_MUSCLES'];
const TARGET_OPTIONS: GoalTargetBucket[] = ['LOSS_5', 'LOSS_10', 'LOSS_15', 'LOSS_20_PLUS'];
const FORM_STORAGE_KEY = 'profile-goals.form';

function readStoredForm(): OnboardingSubmissionRequest | null {
  try {
    const raw = localStorage.getItem(FORM_STORAGE_KEY);
    return raw ? (JSON.parse(raw) as OnboardingSubmissionRequest) : null;
  } catch {
    return null;
  }
}

export function ProfileGoalOnboardingPage() {
  const navigate = useNavigate();
  const createInitialProposal = useCreateInitialProposal();
  const rejectReview = useProfileGoalProposalReview();
  const acceptProposal = useAcceptProposal();
  const { data: currentAttempt } = useCurrentOnboardingAttempt();
  const { data: gate } = useTrackingAccessGate();
  const [acceptError, setAcceptError] = useState<string | null>(null);

  // Server is the source of truth for proposals (NFR-004: state is server-persisted).
  // Do NOT fall back to localStorage for proposals — stale cached IDs cause 404 on accept.
  const proposal = currentAttempt?.latestProposal ?? createInitialProposal.data ?? null;

  const [form, setForm] = useState<OnboardingSubmissionRequest>(
    () =>
      readStoredForm() ?? {
        age: 30,
        currentWeight: 75,
        weightUnit: 'KG',
        primaryGoal: 'STRENGTH',
        goalTargetBucket: null,
      },
  );

  useEffect(() => {
    localStorage.setItem(FORM_STORAGE_KEY, JSON.stringify(form));
  }, [form]);

  // Remove any previously cached proposal from localStorage on mount.
  // Proposals must come from the server; stale cached IDs cause 404 on accept after restarts.
  useEffect(() => {
    localStorage.removeItem('profile-goals.proposal');
  }, []);


  return (
    <section className="stack-lg">
      <div className="between">
        <div>
          <p className="eyebrow">Onboarding</p>
          <h1>My Profile & Goals</h1>
        </div>
        <button className="button ghost" type="button" onClick={() => navigate('/')}>
          Back
        </button>
      </div>

      {!gate?.canAccessProgramTracking && (
        <p className="card">Onboarding is required before program tracking is available.</p>
      )}

      <form
        className="card stack-sm"
        onSubmit={(event) => {
          event.preventDefault();
          createInitialProposal.mutate(form);
        }}
      >
        <label className="stack-xs">
          <span>Age</span>
          <input
            aria-label="Age"
            type="number"
            min={13}
            max={100}
            value={form.age}
            onChange={(event) => setForm((prev) => ({ ...prev, age: Number(event.target.value) }))}
          />
        </label>

        <label className="stack-xs">
          <span>Current Weight</span>
          <input
            aria-label="Current Weight"
            type="number"
            step="0.1"
            min={1}
            value={form.currentWeight}
            onChange={(event) => setForm((prev) => ({ ...prev, currentWeight: Number(event.target.value) }))}
          />
        </label>

        <label className="stack-xs">
          <span>Primary Goal</span>
          <select
            aria-label="Primary Goal"
            value={form.primaryGoal}
            onChange={(event) => {
              const goal = event.target.value as OnboardingPrimaryGoal;
              setForm((prev) => ({
                ...prev,
                primaryGoal: goal,
                goalTargetBucket: goal === 'LOSE_WEIGHT' ? prev.goalTargetBucket ?? 'LOSS_5' : null,
              }));
            }}
          >
            {GOAL_OPTIONS.map((goal) => (
              <option key={goal} value={goal}>
                {goal}
              </option>
            ))}
          </select>
        </label>

        {form.primaryGoal === 'LOSE_WEIGHT' && (
          <label className="stack-xs">
            <span>Weight Loss Target</span>
            <select
              value={form.goalTargetBucket ?? 'LOSS_5'}
              onChange={(event) =>
                setForm((prev) => ({ ...prev, goalTargetBucket: event.target.value as GoalTargetBucket }))
              }
            >
              {TARGET_OPTIONS.map((target) => (
                <option key={target} value={target}>
                  {target}
                </option>
              ))}
            </select>
          </label>
        )}

        <button className="button primary" type="submit" disabled={createInitialProposal.isPending}>
          {createInitialProposal.isPending ? 'Generating…' : 'Generate Plan'}
        </button>
      </form>

      {proposal && currentAttempt?.status === 'ACCEPTED' && (
        <section className="card stack-sm">
          <h2>Your Active Program</h2>
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
          <p className="eyebrow">Plan accepted — regenerate below to start a new onboarding.</p>
        </section>
      )}

      {proposal && currentAttempt?.status !== 'ACCEPTED' && (
        <ProposalReviewCard
          proposal={proposal}
          isBusy={rejectReview.isRejecting || acceptProposal.isPending}
          onReject={(requestedChanges) => rejectReview.rejectProposal({ proposalId: proposal.proposalId, requestedChanges })}
          onAccept={async () => {
            setAcceptError(null);
            try {
              await acceptProposal.mutateAsync(proposal.proposalId);
              navigate('/program-session');
            } catch {
              setAcceptError('Failed to accept the plan. Please try again.');
            }
          }}
        />
      )}
      {acceptError && <p className="card" style={{ color: 'var(--color-error, red)' }}>{acceptError}</p>}
    </section>
  );
}
