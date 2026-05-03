# Data Model: Profile Goal Onboarding

## Overview
This model adds onboarding profile/goal capture and AI proposal revision continuity while reusing feature 001 program entities for accepted plan activation.

## New Entities

### ProfileGoalOnboardingAttempt
- Fields:
  - `id` (UUID, required)
  - `userId` (UUID, required, indexed)
  - `age` (int, required, min 13, max 100)
  - `currentWeight` (decimal, required, >0)
  - `weightUnit` (enum: `KG`, `LBS`, required)
  - `primaryGoal` (enum: `LOSE_WEIGHT`, `BUILD_HEALTHY_BODY`, `STRENGTH`, `BUILD_MUSCLES`, required)
  - `goalTargetBucket` (enum: `LOSS_5`, `LOSS_10`, `LOSS_15`, `LOSS_20_PLUS`, optional; required only for `LOSE_WEIGHT`)
  - `status` (enum: `IN_PROGRESS`, `ACCEPTED`, `ABANDONED`, required)
  - `createdAt` (timestamp, required)
  - `updatedAt` (timestamp, required)
- Relationships:
  - One `ProfileGoalOnboardingAttempt` has many `PlanProposal`.
- Validation rules:
  - At most one `IN_PROGRESS` attempt per user.
  - Brand-new users must complete an `ACCEPTED` attempt before program-session tracking.

### PlanProposal
- Fields:
  - `id` (UUID, required)
  - `attemptId` (UUID, required, indexed)
  - `userId` (UUID, required, indexed)
  - `version` (int, required, min 1)
  - `status` (enum: `PROPOSED`, `REJECTED`, `ACCEPTED`, required)
  - `proposalPayload` (JSON, required)  
  - `provider` (string, required, fixed value `AZURE_OPENAI` for this feature)
  - `modelDeployment` (string, required)
  - `createdAt` (timestamp, required)
- Relationships:
  - One `PlanProposal` belongs to one `ProfileGoalOnboardingAttempt`.
  - One `PlanProposal` has zero or one `ProposalFeedback` (for rejected versions).
- Validation rules:
  - `(attemptId, version)` unique.
  - Only one proposal per attempt can reach `ACCEPTED`.

### ProposalFeedback
- Fields:
  - `id` (UUID, required)
  - `attemptId` (UUID, required, indexed)
  - `proposalId` (UUID, required, unique)
  - `userId` (UUID, required, indexed)
  - `requestedChanges` (string, required, 1..2000)
  - `createdAt` (timestamp, required)
- Relationships:
  - One `ProposalFeedback` belongs to one rejected `PlanProposal`.
- Validation rules:
  - Feedback can be created only when referenced proposal status is `REJECTED`.

### AcceptedProgramActivation
- Fields:
  - `id` (UUID, required)
  - `attemptId` (UUID, required, unique)
  - `proposalId` (UUID, required, unique)
  - `userId` (UUID, required, indexed)
  - `activatedProgramId` (UUID, required, FK to `WorkoutProgram`)
  - `replacedProgramId` (UUID, optional, FK to previous `WorkoutProgram`)
  - `activatedAt` (timestamp, required)
- Relationships:
  - One accepted proposal creates one activation record.
- Validation rules:
  - Activation transaction must preserve existing `LoggedSession` and progression history.

## Existing Entities Reused from Feature 001
- `WorkoutProgram`
- `ProgramSession`
- `ProgramExerciseTarget`
- `LoggedSession` and related history/progression entities remain unchanged.

## Relationship Summary
- `User 1 -> many ProfileGoalOnboardingAttempt`
- `ProfileGoalOnboardingAttempt 1 -> many PlanProposal`
- `PlanProposal 1 -> 0..1 ProposalFeedback`
- `PlanProposal 1 -> 0..1 AcceptedProgramActivation`
- `AcceptedProgramActivation 1 -> 1 WorkoutProgram`

## State Transitions

### ProfileGoalOnboardingAttempt
- `IN_PROGRESS -> ACCEPTED` when a proposal is accepted and activation succeeds.
- `IN_PROGRESS -> ABANDONED` when user exits and a new attempt supersedes it.

### PlanProposal
- `PROPOSED -> REJECTED` when user submits feedback.
- `PROPOSED -> ACCEPTED` when user confirms acceptance.
- Terminal states: `REJECTED`, `ACCEPTED`.

### Program Activation
- On accept, current active `WorkoutProgram` (if any) transitions out of active status, and a new active program is created from proposal payload.

## Migration Notes
- Add new Flyway migration for onboarding attempt/proposal/feedback/activation tables.
- Add indexes for `(user_id, status)` on attempts and `(attempt_id, version)` on proposals.
- Keep existing 001 tables and columns backward-compatible.
- Do not modify historical session data during activation replacement.

