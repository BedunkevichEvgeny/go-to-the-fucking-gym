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
- [X] T022 [US1] Implement create/current/access-gate onboarding endpoints in `backend/src/main/java/com/gymtracker/api/ProfileGoalOnboardingController.java`
- [X] T023 [US1] Enforce onboarding-required guard for program-session access in `backend/src/main/java/com/gymtracker/api/SessionController.java`
- [X] T024 [US1] Add frontend onboarding types and API client methods in `frontend/src/types/onboarding.ts`
- [X] T025 [US1] Add onboarding query/mutation hooks for submit/current/gate in `frontend/src/hooks/useProfileGoalOnboarding.ts`
- [X] T026 [US1] Implement onboarding page with profile-goal form in `frontend/src/pages/ProfileGoalOnboardingPage.tsx`
- [X] T027 [US1] Add first-page entry route/button for `My Profile & Goals` in `frontend/src/App.tsx`
- [X] T028 [US1] Add mandatory new-user redirect handling before program logging in `frontend/src/pages/ProgramSessionPage.tsx`

**Checkpoint**: US1 is independently functional and testable as MVP.

---

## Phase 4: User Story 2 - Iterate Plan Through Reject/Feedback Revisions (Priority: P1)

**Goal**: Let users reject proposals, submit feedback, and receive revised proposals linked to one onboarding attempt until acceptance.

**Independent Test**: Reject proposal, submit feedback, receive revised version, refresh page, and continue the same revision thread.

### Tests for User Story 2 (MANDATORY)

- [X] T029 [P] [US2] Add proposal revision unit tests for versioning and linkage in `backend/src/test/java/com/gymtracker/application/ProposalRevisionServiceTest.java`
- [X] T030 [P] [US2] Add proposal feedback policy unit tests in `backend/src/test/java/com/gymtracker/application/ProposalFeedbackPolicyTest.java`
- [X] T031 [P] [US2] Add reject-and-revise integration tests in `backend/src/test/java/com/gymtracker/api/PlanConversationControllerIT.java`
- [X] T032 [P] [US2] Add frontend reject/feedback/revise interaction tests in `frontend/src/features/profile-goals/__tests__/ProposalReviewFlow.test.tsx`

### Implementation for User Story 2

- [X] T033 [US2] Implement proposal rejection and revision orchestration service in `backend/src/main/java/com/gymtracker/application/ProposalRevisionService.java`
- [X] T034 [US2] Implement feedback persistence and attempt continuity policy in `backend/src/main/java/com/gymtracker/application/ProposalFeedbackService.java`
- [X] T035 [US2] Implement reject proposal endpoint in `backend/src/main/java/com/gymtracker/api/ProfileGoalOnboardingController.java`
- [X] T036 [US2] Add frontend proposal review card with reject feedback form in `frontend/src/features/profile-goals/ProposalReviewCard.tsx`
- [X] T037 [US2] Wire revision mutations and optimistic state updates in `frontend/src/hooks/useProfileGoalProposalReview.ts`
- [X] T038 [US2] Persist and restore in-progress onboarding attempt state on reload in `frontend/src/pages/ProfileGoalOnboardingPage.tsx`

**Checkpoint**: US2 revision loop is independently functional and testable.

---

## Phase 5: User Story 3 - Activate Accepted Plan in Existing Workout Tracker (Priority: P1)

**Goal**: Accept a proposal, map it to feature 001 entities, replace active program immediately, and preserve history/progression compatibility.

**Independent Test**: Accept proposal and verify existing `/program-sessions/next`, logging, history, and progression flows operate with no contract/behavior regressions.

### Tests for User Story 3 (MANDATORY)

- [X] T039 [P] [US3] Add proposal-to-001 mapping unit tests in `backend/src/test/java/com/gymtracker/application/ProgramMapperCompatibilityTest.java`
- [X] T040 [P] [US3] Add accepted proposal activation unit tests in `backend/src/test/java/com/gymtracker/application/ProgramActivationServiceTest.java`
- [X] T041 [P] [US3] Add active program replacement preservation unit tests in `backend/src/test/java/com/gymtracker/application/ProgramReplacementPolicyTest.java`
- [X] T042 [P] [US3] Add proposal acceptance integration tests in `backend/src/test/java/com/gymtracker/api/ProfileGoalsAcceptanceControllerIT.java`
- [X] T043 [P] [US3] Add 001 next-session compatibility integration test after activation in `backend/src/test/java/com/gymtracker/api/ProgramSessionControllerIT.java`
- [X] T044 [P] [US3] Add history/progression unaffected integration test after replacement in `backend/src/test/java/com/gymtracker/api/SessionHistoryControllerIT.java`
- [X] T045 [P] [US3] Add onboarding acceptance frontend integration test in `frontend/src/features/profile-goals/__tests__/OnboardingAcceptance.test.tsx`
- [X] T046 [P] [US3] Add explicit API compatibility assertions between 002 onboarding and 001 tracker contracts in `backend/src/test/java/com/gymtracker/api/OnboardingContractCompatibilityIT.java`
- [X] T047 [P] [US3] Add IC-006 regression integration test for users without onboarding metadata using 001 flows in `backend/src/test/java/com/gymtracker/api/OnboardingContractCompatibilityIT.java`
- [X] T048 [P] [US3] Add FR-013 onboarding ownership unit tests for attempts/proposals/feedback/accept authorization policy in `backend/src/test/java/com/gymtracker/application/OnboardingAuthorizationPolicyTest.java`
- [X] T049 [P] [US3] Add FR-013 cross-user denial integration tests for attempts/proposals/feedback/accept endpoints in `backend/src/test/java/com/gymtracker/api/ProfileGoalOnboardingAuthorizationIT.java`
- [X] T050 [P] [US3] Add NFR-003 accept/activate p95<=3s integration performance test in `backend/src/test/java/com/gymtracker/api/ProfileGoalsAcceptancePerformanceIT.java`

### Implementation for User Story 3

- [X] T051 [US3] Implement acceptance transaction and 001 entity activation mapping in `backend/src/main/java/com/gymtracker/application/ProgramActivationService.java`
- [X] T052 [US3] Implement accept proposal endpoint and response contract mapping in `backend/src/main/java/com/gymtracker/api/ProfileGoalOnboardingController.java`
- [X] T053 [US3] Integrate activation record persistence and active-program deactivation policy in `backend/src/main/java/com/gymtracker/application/AcceptedProgramActivationService.java`
- [X] T054 [US3] Update program-session retrieval compatibility mapping for activated plans in `backend/src/main/java/com/gymtracker/application/ProgramSessionService.java`
- [X] T055 [US3] Implement frontend accept action and post-accept navigation to tracker in `frontend/src/features/profile-goals/ProposalAcceptanceActions.tsx`
- [X] T056 [US3] Wire acceptance flow and tracker refresh in `frontend/src/pages/ProfileGoalOnboardingPage.tsx`

**Checkpoint**: US3 activation and 001 compatibility are independently functional and testable.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Finish non-critical quality, performance, docs, and broader hardening after core functional slices are complete.

- [X] T057 Add onboarding latency instrumentation and p95 threshold assertions in `backend/src/test/java/com/gymtracker/infrastructure/ai/OnboardingLatencyIT.java`
- [X] T058 [P] Add minimal smoke placeholder scenario for onboarding flow in `specs/002-profile-goal-onboarding/quickstart.md`
- [X] T059 [P] Add rollout/ops notes for new env vars and migration behavior in `DEPLOYMENT.md`
- [X] T060 [P] Add docs polish for onboarding UX walkthrough in `docs/FEATURE_WALKTHROUGH.md`
- [X] T061 Run backend/frontend lint and targeted refactoring cleanup in `backend/pom.xml`
- [X] T062 Run full 001+002 regression matrix and record final results in `docs/TEST_COVERAGE.md`

---

## Phase 7: CRITICAL BUG FIXES (Smoke Test Findings) 🚨

**Purpose**: Fix critical bugs discovered in smoke testing that break core functionality.

**Status**: BLOCKING - Must complete before any production or demo use.

### Tests for Bug Fixes (MANDATORY)

- [X] T063-BUG-001-TEST [CRITICAL] Add AI generation integration test verifying real Azure OpenAI calls in `backend/src/test/java/com/gymtracker/infrastructure/ai/AzureOpenAiIntegrationIT.java`
  - Verify non-hardcoded proposal generation from actual LLM
  - Mock or use real Azure OpenAI endpoint
  - Assert exercises are NOT the hardcoded "Back Squat" and "Treadmill Run"

- [X] T064-BUG-002-TEST [CRITICAL] Add proposal state persistence integration tests in `backend/src/test/java/com/gymtracker/application/PlanProposalServiceIT.java`
  - Test `getCurrentAttempt()` returns actual persisted attempt after creation
  - Test proposal version chain: create v1 → reject → create v2 (linked, incrementing version)
  - Test `resolveAttemptSnapshot()` loads actual user inputs, not hardcoded values
  - Test reject/revise cycle maintains proposalId chain and attemptId continuity

### Implementation for Bug Fixes

- [X] T063-BUG-001 [CRITICAL] Implement real Azure OpenAI LLM integration in `OnboardingPlanGenerator`
  - Remove hardcoded "Back Squat" and "Treadmill Run" exercises
  - Implement LangChain4j + Azure OpenAI API call to generate contextual plan
  - Add error handling for LLM timeouts/failures with user-visible retry messaging
  - Use `AzureOpenAiOnboardingProperties` to configure endpoint/key/deployment
  - **Maps to**: FR-005 (proposal structure generation), eliminates fake-data blocker

- [X] T064-BUG-002 [CRITICAL] Complete `PlanProposalService` persistence implementations
  - Implement `getCurrentAttempt(userId)` → query `ProfileGoalOnboardingAttempt` + `PlanProposal` from DB, return latest attempt with proposal chain
  - Implement `resolveAttemptSnapshot(userId, attemptId)` → fetch actual stored `OnboardingSubmissionRequest` from proposal payload, not hardcoded values
  - Fix `createRevision(userId, proposalId, requestedChanges)` → maintain proposalId chain (reuse parent proposalId or link to parent), increment version, preserve attemptId
  - Implement `getTrackingAccessGate(userId)` → query `ProfileGoalOnboardingAttempt` status; return true only if status is `ACCEPTED` or user has active `AcceptedProgramActivation`
  - Add repository queries for attempt/proposal/activation lookups
  - **Maps to**: FR-008 (repeated revision cycle), FR-012 (linkage model), eliminates acceptance/rejection state loss

---

## Phaze 8 cricical bugs fixes for Azure Open AI iteraction

**Purpose**: Replace stub-like onboarding AI paths with explicit LangChain abstractions and strict contract validation.

**Status**: BLOCKING ROLLOUT - Must complete before release promotion.

### Tests for Phase 8 Remediation (MANDATORY)

- [X] T065-BUG-003-TEST [CRITICAL] Add LangChain/Azure integration contract test for onboarding assistant chat path in `backend/src/test/java/com/gymtracker/infrastructure/ai/AzureOpenAiIntegrationIT.java`
  - Assert `LangChainSessionProcessor` uses real `AzureOpenAiChatModel` execution path instead of stubbed provider output
  - Assert returned content is strict JSON consumable by onboarding proposal parser
  - Assert test fails for stub-like plain-text or non-JSON responses

- [X] T066-BUG-004-TEST [CRITICAL] Add fail-fast malformed/empty model-output tests in `backend/src/test/java/com/gymtracker/infrastructure/ai/LangChainSessionProcessorTest.java`
  - Assert no hardcoded fallback proposal/session data is emitted on parse failure
  - Assert explicit error is surfaced for malformed and empty model output

- [X] T067-BUG-005-TEST [CRITICAL] Add onboarding generator integration tests for assistant prompt path in `backend/src/test/java/com/gymtracker/application/PlanProposalServiceIT.java`
  - Assert onboarding generation uses assistant `chat` prompt path directly
  - Assert no fake `SessionSummaryDTO` bridge is used in onboarding generation flow

### Implementation for Phase 8 Remediation

- [X] T065-BUG-003 [CRITICAL] Replace stubbed Azure call path with real LangChain/Azure execution in `backend/src/main/java/com/gymtracker/infrastructure/ai/LangChainSessionProcessor.java`
  - Remove synthetic/default response branch and route calls through runtime model invocation
  - Ensure the processing path consumes `ChatModel` backed by `AzureOpenAiChatModel`

- [X] T066-BUG-004 [CRITICAL] Implement explicit onboarding LangChain abstractions in `backend/src/main/java/com/gymtracker/infrastructure/ai/ChatMemoryProvider.java`
  - Add `ChatMemoryProvider` contract
  - Add onboarding assistant interface via `AiServices` with `chat` method
  - Add `ChatModel` and `AzureOpenAiChatModel` wiring used by onboarding assistant

- [X] T067-BUG-005 [CRITICAL] Refactor onboarding proposal generation to assistant prompt path in `backend/src/main/java/com/gymtracker/infrastructure/ai/OnboardingPlanGenerator.java`
  - Remove fake `SessionSummaryDTO` bridge usage from onboarding generation
  - Keep fail-fast behavior: malformed/empty output surfaces explicit error
  - Keep zero hardcoded fallback proposal payloads

---

## Dependencies & Execution Order

### Phase Dependencies

- Phase 1 -> no dependencies.
- Phase 2 -> depends on Phase 1; blocks all user stories.
- Phase 3 (US1) -> depends on Phase 2.
- Phase 4 (US2) -> depends on Phase 3 baseline proposal flow.
- Phase 5 (US3) -> depends on Phase 3 proposal creation and Phase 4 revision state integrity.
- Phase 6 -> depends on completion of Phases 3-5.
- **Phase 7 (CRITICAL BUG FIXES) -> BLOCKING all use until complete; must be prioritized immediately**
  - T063-BUG-001 must complete before smoke test acceptance
  - T064-BUG-002 must complete before T035 (reject endpoint), T052 (accept endpoint) can pass integration tests
- **Phaze 8 cricical bugs fixes for Azure Open AI iteraction -> depends on Phase 7 and BLOCKS rollout**
  - T065-BUG-003-TEST/T065-BUG-003 must pass before AI integration sign-off
  - T066-BUG-004-TEST/T066-BUG-004 must pass before fail-fast behavior is compliant
  - T067-BUG-005-TEST/T067-BUG-005 must pass before onboarding rollout approval

### User Story Dependency Graph

- `US1 -> US2 -> US3`
- Rationale: revision and acceptance require initial proposal/gating foundation; activation relies on accepted proposal lifecycle and continuity.

### Task-Level Critical Chain

- `T004 -> T006/T007/T008/T009/T010 -> T020/T022 -> T033/T035 -> T046/T049/T050 -> T051/T052 -> T062`
- `T063-BUG-001/T064-BUG-002 -> T065-BUG-003 -> T066-BUG-004 -> T067-BUG-005 -> rollout`
- `T068-BUG-006-TEST -> T069-BUG-006 -> T070-BUG-006-ENUM`
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
- Phase 8 test tasks `T065-BUG-003-TEST` and `T066-BUG-004-TEST` can run in parallel before implementation.
- Phase 9: `T068-BUG-006-TEST` and `T070-BUG-006-ENUM` can run in parallel; `T069-BUG-006` depends on `T068-BUG-006-TEST`.

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

## Phase 9: LLM Response Type Safety (Bug BUG-006)

**Trigger**: Smoke test revealed `ExerciseType.valueOf("FLEXIBILITY")` crash caused by
prompt/enum mismatch and unguarded raw-string LLM parsing. `buildPrompt()` lists
`FLEXIBILITY` as a valid type, but `ExerciseType` enum has no such constant. Additionally,
`BODYWEIGHT` exists in the enum but is absent from the prompt.

**Status**: BLOCKING — `generateInitialProposal()` crashes in production on any plan containing flexibility exercises.

### Tests for Phase 9 (MANDATORY — write first, run failing)

- [X] T068-BUG-006-TEST [CRITICAL] Add regression unit tests in
  `backend/src/test/java/com/gymtracker/infrastructure/ai/OnboardingPlanGeneratorTest.java`
  - Assert LLM response with `"type": "FLEXIBILITY"` does NOT throw `IllegalArgumentException`
  - Assert `"type": "BODYWEIGHT"` is parsed correctly to `ExerciseType.BODYWEIGHT`
  - Assert `"type": "UNKNOWN_VALUE_XYZ"` is handled gracefully (fallback to `STRENGTH`, not crash)
  - Assert session list is non-empty after parsing a response containing unknown types
  - Use `@MockBean`/stub for `LangChainSessionProcessor` returning controlled JSON

### Implementation Tasks for Phase 9

- [X] T069-BUG-006 [CRITICAL] Replace raw-string LLM response and manual JSON parsing with
  LangChain4j structured output via typed DTOs in `backend/src/main/java/com/gymtracker/infrastructure/ai/`
  - Create `backend/src/main/java/com/gymtracker/infrastructure/ai/dto/OnboardingPlanDto.java`
    (record with `List<SessionDto> sessions`)
  - Create `backend/src/main/java/com/gymtracker/infrastructure/ai/dto/SessionDto.java`
    (record with `int sequenceNumber`, `String name`, `List<ExerciseDto> exercises`)
  - Create `backend/src/main/java/com/gymtracker/infrastructure/ai/dto/ExerciseDto.java`
    (record with `String name`, `ExerciseType type`, `Integer targetSets`, `Integer targetReps`,
    `BigDecimal targetWeight`, `WeightUnit weightUnit`, `Integer durationSeconds`) —
    `ExerciseType` field enforces valid enum values at deserialisation time
  - Change `LangChainSessionProcessor.process()` return type from `String` to `OnboardingPlanDto`
    (LangChain4j `AiServices` handles structured output / JSON extraction + enum mapping automatically)
  - Update `AiChatModelConfig` bean registration if needed for the new return type
  - Remove `parseLlmResponse()`, `parseExercise()`, `extractJsonPayload()` from `OnboardingPlanGenerator`
  - Map `OnboardingPlanDto` directly to `List<ProposedSession>` in `generateInitialProposal()`
  - Update `buildPrompt()`: replace `STRENGTH|CARDIO|FLEXIBILITY` with `STRENGTH|BODYWEIGHT|CARDIO`;
    replace "flexibility work" prose with "mobility/bodyweight work"
  - Dependency: T068-BUG-006-TEST must exist (failing) before this task begins

- [X] T070-BUG-006-ENUM [HIGH] Audit `ExerciseType` enum and prompt alignment in
  `backend/src/test/java/com/gymtracker/infrastructure/ai/OnboardingPlanGeneratorPromptTest.java`
  - Add unit test asserting the `buildPrompt()` output contains every `ExerciseType` constant name
  - Verify no enum constant is missing from the prompt template after T069
  - Verify `FLEXIBILITY` does NOT appear anywhere in the prompt after T069

---

## Phase 7: Smoke-Test Gap Fixes (Bug Fixes)

**Purpose**: Close two Constitution §III violations identified during smoke testing — missing test coverage for implemented business logic.

### B1 — Integration test: `requestedChanges` reaches AI prompt

- [ ] T071-BUG-B1 [US2] Add integration test asserting `requestedChanges` is forwarded to the AI revision prompt in `backend/src/test/java/com/gymtracker/application/PlanRevisionFeedbackIT.java`
  - Use a `@SpyBean` or `@MockBean` on `OnboardingPlanGenerator` (or `LangChainSessionProcessor`) to capture the prompt argument
  - Call `POST /api/onboarding/{attemptId}/proposals/{proposalId}/reject` with a non-blank `requestedChanges` value via `MockMvc` or `TestRestTemplate`
  - Assert the captured prompt string **contains** the exact feedback text submitted
  - Verify `PlanProposalService.createRevision()` takes the feedback-present branch (not the blank branch)
  - Test must run against the embedded H2 test database — no real Azure calls
  - **Acceptance criteria**: Test goes red without the `buildRevisionPrompt()` method and green with it; CI pipeline stays green

### B2 — Frontend component test: exercise detail rendering in `ProposalReviewCard`

- [ ] T072-BUG-B2 [P] [US2] Add Vitest/RTL component tests for exercise detail fields in `frontend/tests/ProposalReviewCard.exercise-details.test.tsx`
  - Render `ProposalReviewCard` with a proposal fixture containing `targetSets`, `targetReps`, `targetWeight`, and `targetWeightUnit`; assert all four values appear in the DOM
  - Render with `targetDurationSeconds` set; assert the formatted duration string is visible
  - Render with `targetDistance` and `targetDistanceUnit` set; assert both appear in the DOM
  - Render with none of the optional fields set; assert no empty/broken detail rows are shown
  - Use `@testing-library/react` `render` + `screen.getByText` / `screen.queryByText` patterns consistent with existing frontend tests
  - **Acceptance criteria**: All four cases pass; component must not regress when any single optional field is absent

---

### B3 — Frontend test: no stale localStorage proposal rendered when backend returns 204

- [ ] T073-BUG-B3 [P] Add Vitest/RTL page-level test asserting stale localStorage proposal is NOT rendered when `useCurrentOnboardingAttempt` returns `null` in `frontend/tests/ProfileGoalOnboardingPage.stale-localStorage.test.tsx`
  - Before rendering, seed `localStorage` with key `profile-goals.proposal` containing a serialised proposal fixture
  - Mock `useCurrentOnboardingAttempt` to return `{ data: null, isLoading: false }` (simulates 204 from backend)
  - Mock `useCreateInitialProposal` to return `{ data: undefined, isPending: false, mutateAsync: vi.fn() }`
  - Assert that NO proposal card / proposal content is visible in the DOM after render
  - Assert that `localStorage.getItem('profile-goals.proposal')` returns `null` after mount (cleanup side-effect ran)
  - **Acceptance criteria**: Test goes red if the stale `storedProposal` fallback path is re-introduced; test goes green with the current server-only derivation; CI pipeline stays green

---

### B4 — Frontend test: accept-button error message shown and navigation suppressed on 404

- [ ] T074-BUG-B4 [P] Add Vitest/RTL page-level test asserting error message is displayed and navigation does NOT occur when `acceptProposal.mutateAsync` rejects in `frontend/tests/ProfileGoalOnboardingPage.accept-error.test.tsx`
  - Render page with a valid in-progress attempt fixture (proposal visible, Accept button enabled)
  - Mock `acceptProposal.mutateAsync` to reject with a simulated 404 error
  - Fire a click on the Accept button
  - Assert the text `"Failed to accept the plan. Please try again."` appears in the DOM
  - Assert that `useNavigate`'s mock function was NOT called (navigation suppressed)
  - **Acceptance criteria**: Test goes red if the `try/catch` error handler or `acceptError` render path is removed; test goes green with the current implementation; CI pipeline stays green

---



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



