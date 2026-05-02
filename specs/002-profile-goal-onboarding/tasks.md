# Tasks: Profile Goal Onboarding

**Input**: Design documents from `specs/002-profile-goal-onboarding/`
**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/profile-goal-onboarding-api.yaml`

**Tests**: Business-logic test tasks are mandatory. Each mapped rule includes unit + integration coverage, with explicit compatibility checks against feature 001 contracts/behavior.

**Organization**: Tasks are grouped by user story for independent implementation and validation, while preserving strict dependency order.

**Delivery Governance**: Keep one-task-per-commit discipline for every checklist item.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Prepare feature scaffolding and shared configuration touchpoints without deep quality hardening.

- [X] T001 Add onboarding package skeleton and feature README in `backend/src/main/java/com/gymtracker/application/onboarding/README.md`
- [X] T002 Add onboarding frontend feature folders with placeholder exports in `frontend/src/features/profile-goals/index.ts`
- [X] T003 Register Azure OpenAI onboarding properties in `backend/src/main/resources/application.properties`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Deliver blocking persistence, shared model, and contract-aligned DTO/repository foundations required by all stories.

**Critical**: No user-story implementation starts before this phase is complete.

- [X] T004 Create onboarding schema migration for attempt/proposal/feedback/activation tables in `backend/src/main/resources/db/migration/V002__profile_goal_onboarding.sql`
- [X] T005 [P] Add onboarding domain enums for goal/status/provider in `backend/src/main/java/com/gymtracker/domain/OnboardingEnums.java`
- [X] T006 [P] Add `ProfileGoalOnboardingAttempt` entity mapping in `backend/src/main/java/com/gymtracker/domain/ProfileGoalOnboardingAttempt.java`
- [X] T007 [P] Add `PlanProposal` entity mapping with JSON payload support in `backend/src/main/java/com/gymtracker/domain/PlanProposal.java`
- [X] T008 [P] Add `ProposalFeedback` entity mapping in `backend/src/main/java/com/gymtracker/domain/ProposalFeedback.java`
- [X] T009 [P] Add `AcceptedProgramActivation` entity mapping in `backend/src/main/java/com/gymtracker/domain/AcceptedProgramActivation.java`
- [X] T010 [P] Add onboarding repositories for attempt/proposal/feedback/activation in `backend/src/main/java/com/gymtracker/infrastructure/repository/OnboardingRepositories.java`
- [X] T011 Add onboarding API DTOs aligned to contract schemas in `backend/src/main/java/com/gymtracker/api/dto/ProfileGoalOnboardingDtos.java`
- [X] T012 Add proposal payload model + mapper shared by all onboarding services in `backend/src/main/java/com/gymtracker/infrastructure/mapper/OnboardingProposalMapper.java`
- [X] T013 Add typed Azure OpenAI onboarding config binding in `backend/src/main/java/com/gymtracker/infrastructure/config/AzureOpenAiOnboardingProperties.java`

**Checkpoint**: DB + domain + repositories + DTO/config are ready; user story slices can begin.

---

## Phase 3: User Story 1 - Create Initial Plan from Profile & Goal (Priority: P1) 🎯 MVP

**Goal**: Let users open onboarding, submit valid profile/goal input, receive an initial structured proposal, and enforce mandatory onboarding for new users.

**Independent Test**: From the first page, open `My Profile & Goals`, submit valid inputs, receive proposal, and confirm new users are gated from `/program-session` until acceptance.

### Tests for User Story 1 (MANDATORY)

- [X] T014 [P] [US1] Add onboarding input validation unit tests in `backend/src/test/java/com/gymtracker/application/OnboardingValidationTest.java`
- [X] T015 [P] [US1] Add initial proposal generation unit tests in `backend/src/test/java/com/gymtracker/application/PlanProposalServiceTest.java`
- [X] T016 [P] [US1] Add onboarding submission integration tests for valid/invalid payloads in `backend/src/test/java/com/gymtracker/api/ProfileGoalsControllerIT.java`
- [X] T017 [P] [US1] Add access-gate integration tests for brand-new users in `backend/src/test/java/com/gymtracker/api/TrackingAccessGateControllerIT.java`
- [X] T018 [P] [US1] Add onboarding entry/form/gate frontend tests in `frontend/src/features/profile-goals/__tests__/OnboardingPage.test.tsx`

### Implementation for User Story 1

- [X] T019 [US1] Implement onboarding validation service for age/weight/goal rules in `backend/src/main/java/com/gymtracker/application/OnboardingValidationService.java`
- [X] T020 [US1] Implement initial proposal generation orchestration in `backend/src/main/java/com/gymtracker/application/PlanProposalService.java`
- [X] T021 [US1] Implement onboarding AI generation client with LangChain4j + Azure OpenAI in `backend/src/main/java/com/gymtracker/infrastructure/ai/OnboardingPlanGenerator.java`
- [ ] T022 [US1] Implement create/current/access-gate onboarding endpoints in `backend/src/main/java/com/gymtracker/api/ProfileGoalOnboardingController.java`
- [ ] T023 [US1] Enforce onboarding-required guard for program-session access in `backend/src/main/java/com/gymtracker/api/SessionController.java`
- [ ] T024 [US1] Add frontend onboarding types and API client methods in `frontend/src/types/onboarding.ts`
- [ ] T025 [US1] Add onboarding query/mutation hooks for submit/current/gate in `frontend/src/hooks/useProfileGoalOnboarding.ts`
- [ ] T026 [US1] Implement onboarding page with profile-goal form in `frontend/src/pages/ProfileGoalOnboardingPage.tsx`
- [ ] T027 [US1] Add first-page entry route/button for `My Profile & Goals` in `frontend/src/App.tsx`
- [ ] T028 [US1] Add mandatory new-user redirect handling before program logging in `frontend/src/pages/ProgramSessionPage.tsx`

**Checkpoint**: US1 is independently functional and testable as MVP.

---

## Phase 4: User Story 2 - Iterate Plan Through Reject/Feedback Revisions (Priority: P1)

**Goal**: Let users reject proposals, submit feedback, and receive revised proposals linked to one onboarding attempt until acceptance.

**Independent Test**: Reject proposal, submit feedback, receive revised version, refresh page, and continue the same revision thread.

### Tests for User Story 2 (MANDATORY)

- [ ] T029 [P] [US2] Add proposal revision unit tests for versioning and linkage in `backend/src/test/java/com/gymtracker/application/ProposalRevisionServiceTest.java`
- [ ] T030 [P] [US2] Add proposal feedback policy unit tests in `backend/src/test/java/com/gymtracker/application/ProposalFeedbackPolicyTest.java`
- [ ] T031 [P] [US2] Add reject-and-revise integration tests in `backend/src/test/java/com/gymtracker/api/PlanConversationControllerIT.java`
- [ ] T032 [P] [US2] Add frontend reject/feedback/revise interaction tests in `frontend/src/features/profile-goals/__tests__/ProposalReviewFlow.test.tsx`

### Implementation for User Story 2

- [ ] T033 [US2] Implement proposal rejection and revision orchestration service in `backend/src/main/java/com/gymtracker/application/ProposalRevisionService.java`
- [ ] T034 [US2] Implement feedback persistence and attempt continuity policy in `backend/src/main/java/com/gymtracker/application/ProposalFeedbackService.java`
- [ ] T035 [US2] Implement reject proposal endpoint in `backend/src/main/java/com/gymtracker/api/ProfileGoalOnboardingController.java`
- [ ] T036 [US2] Add frontend proposal review card with reject feedback form in `frontend/src/features/profile-goals/ProposalReviewCard.tsx`
- [ ] T037 [US2] Wire revision mutations and optimistic state updates in `frontend/src/hooks/useProfileGoalProposalReview.ts`
- [ ] T038 [US2] Persist and restore in-progress onboarding attempt state on reload in `frontend/src/pages/ProfileGoalOnboardingPage.tsx`

**Checkpoint**: US2 revision loop is independently functional and testable.

---

## Phase 5: User Story 3 - Activate Accepted Plan in Existing Workout Tracker (Priority: P1)

**Goal**: Accept a proposal, map it to feature 001 entities, replace active program immediately, and preserve history/progression compatibility.

**Independent Test**: Accept proposal and verify existing `/program-sessions/next`, logging, history, and progression flows operate with no contract/behavior regressions.

### Tests for User Story 3 (MANDATORY)

- [ ] T039 [P] [US3] Add proposal-to-001 mapping unit tests in `backend/src/test/java/com/gymtracker/application/ProgramMapperCompatibilityTest.java`
- [ ] T040 [P] [US3] Add accepted proposal activation unit tests in `backend/src/test/java/com/gymtracker/application/ProgramActivationServiceTest.java`
- [ ] T041 [P] [US3] Add active program replacement preservation unit tests in `backend/src/test/java/com/gymtracker/application/ProgramReplacementPolicyTest.java`
- [ ] T042 [P] [US3] Add proposal acceptance integration tests in `backend/src/test/java/com/gymtracker/api/ProfileGoalsAcceptanceControllerIT.java`
- [ ] T043 [P] [US3] Add 001 next-session compatibility integration test after activation in `backend/src/test/java/com/gymtracker/api/ProgramSessionControllerIT.java`
- [ ] T044 [P] [US3] Add history/progression unaffected integration test after replacement in `backend/src/test/java/com/gymtracker/api/SessionHistoryControllerIT.java`
- [ ] T045 [P] [US3] Add onboarding acceptance frontend integration test in `frontend/src/features/profile-goals/__tests__/OnboardingAcceptance.test.tsx`
- [ ] T046 [P] [US3] Add explicit API compatibility assertions between 002 onboarding and 001 tracker contracts in `backend/src/test/java/com/gymtracker/api/OnboardingContractCompatibilityIT.java`
- [ ] T047 [P] [US3] Add IC-006 regression integration test for users without onboarding metadata using 001 flows in `backend/src/test/java/com/gymtracker/api/OnboardingContractCompatibilityIT.java`
- [ ] T048 [P] [US3] Add FR-013 onboarding ownership unit tests for attempts/proposals/feedback/accept authorization policy in `backend/src/test/java/com/gymtracker/application/OnboardingAuthorizationPolicyTest.java`
- [ ] T049 [P] [US3] Add FR-013 cross-user denial integration tests for attempts/proposals/feedback/accept endpoints in `backend/src/test/java/com/gymtracker/api/ProfileGoalOnboardingAuthorizationIT.java`
- [ ] T050 [P] [US3] Add NFR-003 accept/activate p95<=3s integration performance test in `backend/src/test/java/com/gymtracker/api/ProfileGoalsAcceptancePerformanceIT.java`

### Implementation for User Story 3

- [ ] T051 [US3] Implement acceptance transaction and 001 entity activation mapping in `backend/src/main/java/com/gymtracker/application/ProgramActivationService.java`
- [ ] T052 [US3] Implement accept proposal endpoint and response contract mapping in `backend/src/main/java/com/gymtracker/api/ProfileGoalOnboardingController.java`
- [ ] T053 [US3] Integrate activation record persistence and active-program deactivation policy in `backend/src/main/java/com/gymtracker/application/AcceptedProgramActivationService.java`
- [ ] T054 [US3] Update program-session retrieval compatibility mapping for activated plans in `backend/src/main/java/com/gymtracker/application/ProgramSessionService.java`
- [ ] T055 [US3] Implement frontend accept action and post-accept navigation to tracker in `frontend/src/features/profile-goals/ProposalAcceptanceActions.tsx`
- [ ] T056 [US3] Wire acceptance flow and tracker refresh in `frontend/src/pages/ProfileGoalOnboardingPage.tsx`

**Checkpoint**: US3 activation and 001 compatibility are independently functional and testable.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Finish non-critical quality, performance, docs, and broader hardening after core functional slices are complete.

- [ ] T057 Add onboarding latency instrumentation and p95 threshold assertions in `backend/src/test/java/com/gymtracker/infrastructure/ai/OnboardingLatencyIT.java`
- [ ] T058 [P] Add minimal smoke placeholder scenario for onboarding flow in `specs/002-profile-goal-onboarding/quickstart.md`
- [ ] T059 [P] Add rollout/ops notes for new env vars and migration behavior in `DEPLOYMENT.md`
- [ ] T060 [P] Add docs polish for onboarding UX walkthrough in `docs/FEATURE_WALKTHROUGH.md`
- [ ] T061 Run backend/frontend lint and targeted refactoring cleanup in `backend/pom.xml`
- [ ] T062 Run full 001+002 regression matrix and record final results in `docs/TEST_COVERAGE.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- Phase 1 -> no dependencies.
- Phase 2 -> depends on Phase 1; blocks all user stories.
- Phase 3 (US1) -> depends on Phase 2.
- Phase 4 (US2) -> depends on Phase 3 baseline proposal flow.
- Phase 5 (US3) -> depends on Phase 3 proposal creation and Phase 4 revision state integrity.
- Phase 6 -> depends on completion of Phases 3-5.

### User Story Dependency Graph

- `US1 -> US2 -> US3`
- Rationale: revision and acceptance require initial proposal/gating foundation; activation relies on accepted proposal lifecycle and continuity.

### Task-Level Critical Chain

- `T004 -> T006/T007/T008/T009/T010 -> T020/T022 -> T033/T035 -> T046/T049/T050 -> T051/T052 -> T062`
- `T023` and `T028` must complete before US1 is considered done for mandatory new-user gating.

### Within Each User Story

- Tests first and failing before implementation (`T014-T018`, `T029-T032`, `T039-T050`).
- Backend domain/service changes before controller wiring.
- Backend API readiness before frontend flow integration.
- Story acceptance checkpoint must pass before next story phase begins.

---

## Parallel Opportunities

- Phase 2 entity files `T006-T009` can run in parallel after `T004`.
- US1 test tasks `T014-T018` can run in parallel.
- US2 test tasks `T029-T032` can run in parallel.
- US3 test tasks `T039-T050` can run in parallel.
- Docs/ops polish tasks `T058-T060` can run in parallel.

## Parallel Example: User Story 1

```text
Task T014 backend validation unit tests in backend/src/test/java/com/gymtracker/application/OnboardingValidationTest.java
Task T015 backend proposal unit tests in backend/src/test/java/com/gymtracker/application/PlanProposalServiceTest.java
Task T018 frontend onboarding behavior tests in frontend/src/features/profile-goals/__tests__/OnboardingPage.test.tsx
```

## Parallel Example: User Story 2

```text
Task T029 backend revision service tests in backend/src/test/java/com/gymtracker/application/ProposalRevisionServiceTest.java
Task T030 backend feedback policy tests in backend/src/test/java/com/gymtracker/application/ProposalFeedbackPolicyTest.java
Task T032 frontend reject/revise tests in frontend/src/features/profile-goals/__tests__/ProposalReviewFlow.test.tsx
```

## Parallel Example: User Story 3

```text
Task T039 backend mapping compatibility tests in backend/src/test/java/com/gymtracker/application/ProgramMapperCompatibilityTest.java
Task T042 backend accept integration tests in backend/src/test/java/com/gymtracker/api/ProfileGoalsAcceptanceControllerIT.java
Task T049 backend cross-user isolation endpoint tests in backend/src/test/java/com/gymtracker/api/ProfileGoalOnboardingAuthorizationIT.java
Task T050 backend acceptance performance threshold test in backend/src/test/java/com/gymtracker/api/ProfileGoalsAcceptancePerformanceIT.java
Task T045 frontend acceptance flow tests in frontend/src/features/profile-goals/__tests__/OnboardingAcceptance.test.tsx
```

---

## Implementation Strategy

### MVP First (US1)

1. Complete Phase 1 and Phase 2 foundations.
2. Complete Phase 3 for onboarding form + initial proposal + mandatory gating.
3. Validate US1 independent test criteria before proceeding.

### Incremental Delivery

1. Deliver US1 as the first usable slice.
2. Add US2 revision loop as the second slice.
3. Add US3 acceptance activation + 001 compatibility as the third slice.
4. Move deeper quality/perf/docs hardening to Phase 6.

### One-Task-Per-Commit Rule

1. Open one implementation branch thread per task ID.
2. Commit exactly one task ID per commit message and scope.
3. Do not combine multiple checklist IDs in one commit.
4. Mark checklist item done only after its tests pass for that scope.



