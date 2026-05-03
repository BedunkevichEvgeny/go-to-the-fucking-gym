# Feature Specification: Profile Goal Onboarding

**Feature Branch**: `002-profile-goal-onboarding`  
**Created**: 2026-05-02  
**Status**: Draft  
**Input**: User description: "Create or update a feature specification for workout-tracker with this requested capability, aligned with and extending feature 001-workout-tracker: On first page user opens profile & goals onboarding, submits age/weight/goal, receives AI-generated structured workout plan, can accept or reject, can iterate with change requests until accepted, and accepted plan is persisted and fully compatible with existing 001 entities/APIs/flows."

**Language Rule**: This specification MUST be written in English.

## Clarifications

### Session 2026-05-02

- Q: Should onboarding be mandatory for brand-new users, and how should reruns behave for existing users? → A: Onboarding is mandatory for brand-new users; existing users may rerun onboarding, and their active plan is replaced only after they accept a new proposal while prior logs/progression remain intact.
- Q: Should accepted plan replacement happen immediately or be schedulable? → A: Accepted replacement happens immediately; deferred/scheduled activation is out of scope.
- Q: Should prior proposal conversations remain user-visible long-term? → A: No long-term user-visible conversation history is required; minimal internal linkage is sufficient.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Create Initial Plan from Profile & Goal (Priority: P1)

A user lands on the first page and selects "My Profile & Goals". They complete onboarding questions for age, current weight, and primary training goal. After submitting, they receive a structured AI-generated workout plan proposal that they can review before committing. For brand-new users, this onboarding flow is required before they can start program-based workout tracking.

**Why this priority**: Without this flow, users cannot generate a program to start the existing workout tracking loop.

**Independent Test**: Can be tested by opening onboarding, submitting valid profile/goal values, and verifying a structured plan proposal is returned and shown for review.

**Acceptance Scenarios**:

1. **Given** an authenticated user on the first page, **When** they click "My Profile & Goals", **Then** the system opens onboarding with questions for age, weight, and goal selection.
2. **Given** a user completing onboarding, **When** they submit age, weight, and one supported goal option, **Then** the system returns a structured workout plan proposal with sessions and exercises.
3. **Given** a returned proposal, **When** the user reviews it, **Then** they can choose either Accept or Reject before the plan becomes active.
4. **Given** a brand-new user without an accepted program, **When** they try to access program-session tracking, **Then** they are redirected to complete onboarding and accept a plan first.

---

### User Story 2 - Iterate Plan Through Chat-Like Revisions (Priority: P1)

If the user rejects a plan proposal, the system asks what should change. The user provides feedback and receives an updated proposal. This loop continues until the user accepts a proposal.

**Why this priority**: User trust and fit depend on being able to refine the plan instead of being forced to accept the first output.

**Independent Test**: Can be tested by rejecting an initial proposal, submitting change feedback, receiving at least one revised proposal, and then accepting a later version.

**Acceptance Scenarios**:

1. **Given** a user viewing a plan proposal, **When** they select Reject, **Then** the system prompts for specific requested changes.
2. **Given** a rejected proposal and user feedback, **When** feedback is submitted, **Then** the system returns a revised structured plan proposal in the same review format.
3. **Given** multiple revision rounds, **When** the user continues rejecting and providing feedback, **Then** each iteration remains linked to the same onboarding conversation until acceptance.

---

### User Story 3 - Activate Accepted Plan in Existing Workout Tracker (Priority: P1)

Once the user accepts a proposal, the system immediately persists the accepted program and decomposes it into program sessions/exercises that are immediately usable in the existing 001 workout tracking flow.

**Why this priority**: The feature only delivers value if accepted plans can be executed through the current session logging, history, and progression capabilities.

**Independent Test**: Can be tested by accepting a proposal and verifying it is stored as an active program that appears as the next program session in the existing tracker flow.

**Acceptance Scenarios**:

1. **Given** a user accepts a proposal, **When** acceptance is confirmed, **Then** the accepted program is persisted as the user\'s active program.
2. **Given** an accepted program, **When** the user enters the existing program-session logging flow, **Then** sessions and exercises from the accepted plan are presented through existing 001 behavior.
3. **Given** persisted accepted program data, **When** existing history and progression flows consume session logs from that program, **Then** they work without schema or contract conflicts.
4. **Given** a user already has an active program, **When** they accept a newly generated onboarding plan, **Then** the new plan replaces the active one immediately and prior logs/progression data remain intact.

---

### Edge Cases & Simplicity Checks

- What happens when age or weight is missing, non-numeric, or outside allowed limits? The system blocks submission and shows clear correction guidance.
- What happens when the AI returns an unusable or incomplete plan structure? The system reports a recoverable error and allows retry without losing onboarding input.
- What happens if the user exits during revision rounds? The latest proposal and minimal internal context remain available when the user returns to the same in-progress onboarding attempt.
- What happens if the user already has an active program from feature 001? Accepting a new plan replaces the currently active program immediately and preserves previously logged sessions/history/progression.
- Which approach keeps this behavior simple and avoids tricky special cases? Maintain one active program per user and one active proposal thread per onboarding attempt.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST provide a visible entry point labeled "My Profile & Goals" from the first page.
- **FR-002**: Selecting "My Profile & Goals" MUST open an onboarding form that captures age, current weight, and exactly one primary goal selection.
- **FR-003**: Supported goal selections MUST include: lose weight with target bucket (-5%, -10%, -15%, -20%+), build healthy body, strength, and build muscles.
- **FR-004**: The system MUST validate onboarding inputs before submission and prevent plan generation until inputs are valid.
- **FR-005**: On valid submission, the system MUST generate and return a structured workout plan proposal that includes at minimum: proposal identifier, proposal version, at least one ordered session, and for each session at least one exercise entry with exercise name plus either set/rep guidance or duration guidance. The UI MUST display all available exercise target details: for strength exercises, sets, reps, and weight MUST be shown when present; for cardio exercises, duration and distance MUST be shown when present.
- **FR-006**: The system MUST provide Accept and Reject actions for each plan proposal.
- **FR-007**: If the user rejects a proposal, the system MUST request change feedback and generate a revised proposal using the same onboarding context. The user's `requestedChanges` text MUST be passed verbatim to the AI revision prompt; silently ignoring feedback and re-generating an identical plan is a violation of this requirement.
- **FR-008**: The system MUST support repeated reject-feedback-revise cycles until the user accepts a proposal or exits the flow.
- **FR-009**: Once accepted, the system MUST persist the accepted proposal as the user\'s active workout program, replacing any previously active program at acceptance time.
- **FR-010**: The system MUST split accepted program content into program sessions and exercises compatible with the existing workout-tracking model.
- **FR-011**: Accepted programs MUST be available immediately through existing program session logging behavior from feature 001.
- **FR-012**: The system MUST keep minimal internal linkage between proposals and feedback within an onboarding attempt, including at minimum `onboardingAttemptId`, `proposalId`, `proposalVersion`, `previousProposalId` (for revisions), `feedbackId`, `userId`, and `createdAt`; this linkage MUST be retained for active in-progress attempts and for the accepted attempt backing the current active program, while non-accepted attempt linkage older than 30 days may be removed.
- **FR-013**: The system MUST preserve user data isolation so each user can access only their own onboarding answers, proposals, and accepted programs.
- **FR-014**: For brand-new users without an accepted active program, the system MUST require onboarding completion and plan acceptance before allowing program-session tracking flows.
- **FR-015**: The system MUST NOT support deferred or scheduled activation of an accepted onboarding plan in this feature.

### Non-Functional Requirements

- **NFR-001**: Users should receive the initial plan proposal within 10 seconds in at least 95% of onboarding submissions.
- **NFR-002**: Users should receive revised proposals within 10 seconds in at least 95% of revision requests.
- **NFR-003**: Proposal acceptance and activation should complete in under 3 seconds in at least 95% of attempts.
- **NFR-004**: In-progress onboarding and revision attempt state must be server-persisted and recoverable by the same authenticated user after navigation away and full page refresh.

### Integration Constraints with Existing 001-Workout-Tracker *(mandatory for this feature)*

- **IC-001**: Final accepted program data MUST map to existing `WorkoutProgram`, `ProgramSession`, and exercise target concepts defined in `specs/001-workout-tracker/spec.md`.
- **IC-002**: Existing 001 APIs and behaviors for program session retrieval, logging, history, and progression MUST remain backward-compatible.
- **IC-003**: The feature MUST preserve the one-active-program-per-user rule established in feature 001.
- **IC-004**: Replacing an active program MUST NOT alter or delete already logged sessions tied to prior programs.
- **IC-005**: Program sessions produced by this feature MUST honor 001 sequencing behavior (next uncompleted session only, no skipping changes introduced by this feature).
- **IC-006**: Any newly captured profile/goal metadata MUST be optional for existing 001 flows; users with null or absent onboarding metadata MUST continue to use existing 001 APIs/flows without behavior or contract regression.

### Technology Constraints *(mandatory)*

- **TC-001**: Backend services MUST use Java 21.
- **TC-002**: Backend services MUST use Spring Boot conventions and best practices.
- **TC-003**: Web frontend MUST use React.
- **TC-004**: Design MUST prefer simple, explicit logic over clever abstractions.

### Test Coverage Requirements *(mandatory)*

- Map each business rule to at least one unit test and one integration test.
- Document contract-level regression tests proving compatibility with feature 001 APIs.
- Explicitly include FR-013 isolation and IC-006 no-onboarding-metadata compatibility mappings in the matrix.

| Business Rule | Unit Test Reference | Integration Test Reference |
|---|---|---|
| Onboarding input validation for age/weight/goal | OnboardingValidationTest#validateRequiredFields | ProfileGoalsControllerIT#rejectInvalidSubmission |
| Initial proposal generation from onboarding input | PlanProposalServiceTest#generateInitialProposal | ProfileGoalsControllerIT#createProposal |
| Reject/feedback/revise loop continuity | ProposalRevisionServiceTest#createRevisedProposal | PlanConversationControllerIT#reviseProposal |
| Accepted proposal activates as current program | ProgramActivationServiceTest#activateAcceptedProposal | ProgramSessionControllerIT#acceptedProgramBecomesNextSession |
| User data isolation across onboarding attempts/proposals/program activation (FR-013) | ProposalFeedbackPolicyTest#enforceUserOwnershipOnRevisionThread | PlanConversationControllerIT#rejectCrossUserRevisionAccess |
| Existing 001 endpoints remain compatible | ProgramMapperCompatibilityTest#mapToExistingEntities | OnboardingContractCompatibilityIT#existing001EndpointsRemainCompatible |
| Users without onboarding metadata remain compatible with 001 flows (IC-006) | ProgramMapperCompatibilityTest#handleMissingOnboardingMetadata | OnboardingContractCompatibilityIT#userWithoutOnboardingMetadataCanUse001Flows |
| Active program replacement preserves prior logs | ProgramReplacementPolicyTest#preserveHistoricalLogs | SessionHistoryControllerIT#historyUnaffectedAfterProgramReplacement |

### Key Entities *(include if feature involves data)*

- **ProfileGoalResponse**: User-submitted onboarding answers. Key attributes: age, current weight, weight unit, selected primary goal, optional target bucket for weight-loss goal, timestamp.
- **PlanProposal**: AI-generated candidate program before acceptance. Key attributes: proposal identifier, user reference, version number, structured sessions/exercises payload, status (proposed/rejected/accepted), created timestamp.
- **ProposalFeedback**: User-provided revision request after rejection. Key attributes: proposal reference, free-text requested changes, created timestamp.
- **AcceptedProgramActivation**: Accepted proposal mapped into the existing active `WorkoutProgram` and `ProgramSession` records used by feature 001.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: At least 90% of users who open "My Profile & Goals" complete onboarding submission without assistance.
- **SC-002**: At least 85% of onboarding submissions return a reviewable structured proposal on first attempt.
- **SC-003**: At least 80% of users who submit onboarding accept a plan within three proposal iterations.
- **SC-004**: At least 95% of accepted plans become usable in existing program-session tracking immediately after acceptance.
- **SC-005**: Support requests about "accepted plan not visible in tracker" stay below 2% of users who accepted a plan.
- **SC-006**: 100% of brand-new users attempting program-session tracking are routed through onboarding until they accept a plan.

### UX Consistency Outcomes *(mandatory when UI changes exist)*

- **UX-001**: The onboarding and proposal-review flow uses the same terminology for sessions, exercises, and goals as existing 001 workout screens.

### Additional Non-Functional Outcomes *(include when materially relevant)*

- **NF-001**: The onboarding and revision flow is available for 99.5% of user attempts measured monthly.
- **NF-002**: No increase greater than 5% is observed in failed requests for existing 001 program-session APIs after rollout.

## Assumptions

- Existing authentication and user identity from feature 001 are reused without introducing new user roles.
- Onboarding is mandatory for brand-new users before program-session tracking; existing users can rerun onboarding voluntarily through the same entry point.
- The system continues to enforce one active program per user; accepting a new program replaces the active program immediately, not historical logs/progression.
- Weight unit defaults to the user\'s existing preference when available; otherwise a default unit is provided during onboarding.
- The feature is web-first, and native mobile-specific UX behavior is out of scope for this specification.
- Long-term user-visible conversation history is out of scope; only minimal internal proposal-feedback linkage is required.

## Open Questions

- None. Prior questions were resolved in `## Clarifications` / `### Session 2026-05-02`.

