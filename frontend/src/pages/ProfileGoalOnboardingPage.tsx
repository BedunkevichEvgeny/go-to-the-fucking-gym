import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  useCreateInitialProposal,
  useCurrentOnboardingAttempt,
  useTrackingAccessGate,
} from '../hooks/useProfileGoalOnboarding';
import type { GoalTargetBucket, OnboardingPrimaryGoal, OnboardingSubmissionRequest } from '../types/onboarding';

const GOAL_OPTIONS: OnboardingPrimaryGoal[] = ['LOSE_WEIGHT', 'BUILD_HEALTHY_BODY', 'STRENGTH', 'BUILD_MUSCLES'];
const TARGET_OPTIONS: GoalTargetBucket[] = ['LOSS_5', 'LOSS_10', 'LOSS_15', 'LOSS_20_PLUS'];

export function ProfileGoalOnboardingPage() {
  const navigate = useNavigate();
  const createInitialProposal = useCreateInitialProposal();
  const { data: currentAttempt } = useCurrentOnboardingAttempt();
  const { data: gate } = useTrackingAccessGate();

  const [form, setForm] = useState<OnboardingSubmissionRequest>({
    age: 30,
    currentWeight: 75,
    weightUnit: 'KG',
    primaryGoal: 'STRENGTH',
    goalTargetBucket: null,
  });

  const proposal = currentAttempt?.latestProposal ?? createInitialProposal.data;

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

      {proposal && (
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
                  <li key={`${session.sequenceNumber}-${exercise.exerciseName}`}>{exercise.exerciseName}</li>
                ))}
              </ul>
            </article>
          ))}
        </section>
      )}
    </section>
  );
}

