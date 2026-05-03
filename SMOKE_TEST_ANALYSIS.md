# Smoke Test Analysis: Feature 002 Profile Goal Onboarding

**Date**: May 3, 2026  
**Status**: 🔴 **CRITICAL BUGS BLOCKING USE**  
**Priority**: P0 - Must fix before any production/demo release

---

## Executive Summary

Smoke testing revealed **2 critical implementation gaps** that break core functionality:

1. **BUG #1**: AI proposal generation returns hardcoded fake data instead of calling Azure OpenAI
2. **BUG #2**: Accept/Reject buttons don't work because proposal state persistence is incomplete

**Both bugs prevent the feature from being usable.** Fixes are tracked as **Phase 7 CRITICAL tasks** in `tasks.md`.

---

## Bug #1: AI Chatbot Not Called - Hardcoded Fake Proposal 🤖

### What the User Observes
- Click "Generate Plan" button
- Form submits but AI is not called
- System returns fake proposal with hardcoded exercises: "Back Squat" and "Treadmill Run"
- User cannot get a real AI-generated proposal

### Root Cause Analysis

**File**: `backend/src/main/java/com/gymtracker/infrastructure/ai/OnboardingPlanGenerator.java`

The generator is **hardcoded** to return fake data instead of calling Azure OpenAI API:

```java
@Component
public class OnboardingPlanGenerator {

    private final AzureOpenAiOnboardingProperties properties;

    public PlanProposalResponse generateInitialProposal(UUID userId, OnboardingSubmissionRequest request) {
        UUID attemptId = UUID.randomUUID();
        UUID proposalId = UUID.randomUUID();

        // ❌ HARDCODED - Not using LLM at all
        ProposedExerciseTarget squat = new ProposedExerciseTarget(
            "Back Squat",      // <-- HARDCODED
            ExerciseType.STRENGTH,
            4, 6,
            new BigDecimal("70"),
            request.weightUnit() == null ? WeightUnit.KG : request.weightUnit(),
            null, null, null
        );

        ProposedExerciseTarget run = new ProposedExerciseTarget(
            "Treadmill Run",   // <-- HARDCODED
            ExerciseType.CARDIO,
            null, null, null, null,
            900, null, null
        );

        List<ProposedSession> sessions = List.of(
            new ProposedSession(1, "Strength Foundation", List.of(squat)),
            new ProposedSession(2, "Cardio Builder", List.of(run))
        );

        return new PlanProposalResponse(
            attemptId, proposalId, 1,
            ProposalStatus.PROPOSED,
            new GeneratedBy(ProposalProvider.AZURE_OPENAI, properties.getDeployment()),  // <-- Lying about provider
            sessions
        );
    }
}
```

**The problem**: 
- `AzureOpenAiOnboardingProperties` is injected but **never used**
- No LangChain4j integration exists
- No Azure OpenAI API calls are made
- The `generatedBy` field falsely claims "AZURE_OPENAI" when it's hardcoded

### Impact on Features
- ❌ FR-005 (proposal structure generation) **BROKEN** - no real AI proposals
- ❌ Users receive identical fake proposals regardless of their profile/goals
- ❌ Defeats the entire purpose of AI onboarding

### Tracked Fix
- **Task**: T063-BUG-001 (Phase 7)
- **Requirement**: Implement LangChain4j + Azure OpenAI call to generate real proposals
- **Acceptance**: Exercises in response are NOT "Back Squat" and "Treadmill Run"

---

## Bug #2: Accept/Reject Buttons Don't Work 🔘

### What the User Observes
- User sees "Accept Plan" and "Reject & Revise" buttons
- Clicking buttons does nothing or page state doesn't change
- No visible feedback that the action was processed
- User cannot complete the onboarding flow

### Root Cause Analysis

**File**: `backend/src/main/java/com/gymtracker/application/PlanProposalService.java`

The service has **incomplete stub implementations** that break state management:

#### Problem 1: `getCurrentAttempt()` Returns Empty

```java
@Service
public class PlanProposalService {
    // ...
    
    public Optional<OnboardingAttemptResponse> getCurrentAttempt(UUID userId) {
        return Optional.empty();  // ❌ ALWAYS EMPTY
    }
}
```

**Impact**:
- Frontend calls `useCurrentOnboardingAttempt()` hook
- Backend returns 204 No Content
- Frontend cannot fetch the current proposal state
- Reject/Accept mutations have no state to work with
- UI cannot update buttons or show feedback

#### Problem 2: `createRevision()` Breaks Proposal Continuity

```java
public PlanProposalResponse createRevision(UUID userId, UUID proposalId, String requestedChanges) {
    // ❌ Wrong: Uses random UUID for attemptId, breaks chain
    PlanProposalResponse initial = onboardingPlanGenerator.generateInitialProposal(
        userId,
        resolveAttemptSnapshot(userId, UUID.randomUUID())  // ❌ Random UUID!
    );
    
    return new PlanProposalResponse(
        initial.attemptId,
        UUID.randomUUID(),  // ❌ New UUID each time - breaks proposalId chain
        initial.version() + 1,
        ProposalStatus.PROPOSED,
        new GeneratedBy(initial.generatedBy().provider(), initial.generatedBy().deployment()),
        initial.sessions()
    );
}
```

**Impact**:
- Each revision gets a new random `proposalId`
- Proposal chain is broken (v1 → v2 not linked)
- FR-008 (repeated revision cycle) **BROKEN**
- FR-012 (linkage model: onboardingAttemptId, proposalId, feedback) **BROKEN**

#### Problem 3: `resolveAttemptSnapshot()` Returns Hardcoded Data

```java
public OnboardingSubmissionRequest resolveAttemptSnapshot(UUID userId, UUID attemptId) {
    return new OnboardingSubmissionRequest(
        30,  // ❌ HARDCODED age
        java.math.BigDecimal.valueOf(75),  // ❌ HARDCODED weight
        com.gymtracker.domain.WeightUnit.KG,  // ❌ HARDCODED unit
        com.gymtracker.domain.OnboardingEnums.OnboardingPrimaryGoal.STRENGTH,  // ❌ HARDCODED goal
        null
    );
}
```

**Impact**:
- Revision proposals always use same hardcoded inputs (age 30, weight 75)
- Doesn't preserve user's actual profile data
- User cannot revise proposals because system "forgets" their inputs

#### Problem 4: `getTrackingAccessGate()` Has Incomplete Logic

```java
public TrackingAccessGateResponse getTrackingAccessGate(UUID userId) {
    boolean allowed = !UUID.fromString("22222222-2222-2222-2222-222222222222").equals(userId);
    return new TrackingAccessGateResponse(
        allowed,
        allowed ? "ALLOWED" : "ONBOARDING_REQUIRED",
        null
    );
}
```

**Impact**:
- Access gate uses hardcoded "test user" UUID
- No real validation that user has completed onboarding
- FR-014 (mandatory gating) **NOT PROPERLY ENFORCED**

### Call Flow That Breaks

```
User clicks "Accept Plan"
    ↓
ProfileGoalOnboardingPage.tsx calls acceptProposal.mutateAsync()
    ↓
useAcceptProposal hook calls onboardingApi.acceptProposal(proposalId)
    ↓
Backend ProfileGoalOnboardingController.acceptProposal() is called
    ↓
AcceptedProgramActivationService.acceptProposal() tries to find proposal
    ↓
❌ FAILS: PlanProposal not found in DB (never persisted!)
    ❌ OR: activateAcceptedProposal() fails because program creation is incomplete
    ↓
Error bubbles up but frontend doesn't handle it properly
    ↓
User sees no change; button appears broken
```

### Similar Issue with Reject

```
User clicks "Reject & Revise"
    ↓
ProposalReviewCard calls onReject(requestedChanges)
    ↓
useProfileGoalProposalReview hook calls onboardingApi.rejectProposal()
    ↓
Backend ProfileGoalOnboardingController.rejectProposal() called
    ↓
ProposalRevisionService.rejectAndRevise() called
    ↓
ProposalFeedbackService.storeFeedback() tries to save feedback
    ↓
❌ FAILS: ProposalFeedback table not found (DB not initialized)
    ❌ OR: PlanProposal not found (never persisted)
    ↓
createRevision() returns new proposal with broken continuity
    ↓
Frontend's optimistic update fails validation
    ↓
User sees no feedback; button appears broken
```

### Impact on Features
- ❌ FR-006 (Accept/Reject actions) **BROKEN**
- ❌ FR-007 (revision feedback loop) **BROKEN**
- ❌ FR-008 (repeated revision cycle) **BROKEN**
- ❌ FR-009 (proposal persistence) **BROKEN** - nothing is persisted
- ❌ FR-012 (linkage model) **BROKEN** - proposal chains not maintained

### Tracked Fixes
- **Task**: T064-BUG-002 (Phase 7)
- **Requirements**:
  1. Implement `getCurrentAttempt(userId)` - query DB for latest attempt + proposal chain
  2. Fix `createRevision()` - maintain proposal linkage, increment version properly
  3. Implement `resolveAttemptSnapshot()` - load actual user inputs from DB, not hardcoded
  4. Fix `getTrackingAccessGate()` - query attempt status and acceptance records
  5. Add proper repository queries for attempt/proposal/activation lookups

---

## Dependency Chain of Root Causes

```
T064-BUG-002 blocks:
    ├─ T035 (reject endpoint implementation)
    ├─ T052 (accept endpoint implementation)
    └─ T049 (cross-user isolation tests)

T063-BUG-001 blocks:
    ├─ US1 acceptance criteria (real AI proposals required)
    └─ T057 (latency instrumentation - needs real LLM to measure)

Both must complete before:
    └─ Phase 6 (polish & hardening)
    └─ Production release / demo
```

---

## What Was Supposed to Work

### Expected Behavior When Fixed

#### Accept Flow
```
1. User clicks "Accept Plan"
2. POST /api/profile-goals/proposals/{proposalId}/accept
3. Backend:
   a. Loads PlanProposal from DB
   b. Validates user owns proposal
   c. Maps proposal to WorkoutProgram entity
   d. Creates AcceptedProgramActivation record
   e. Deactivates any prior active program
   f. Returns ProposalAcceptanceResponse with activated program ID
4. Frontend:
   a. Receives 200 OK with program ID
   b. Clears localStorage
   c. Navigates to /program-session
   d. New program is now active

✅ Result: User's accepted plan is active in the tracker
```

#### Reject & Revise Flow
```
1. User enters feedback and clicks "Reject & Revise"
2. POST /api/profile-goals/proposals/{proposalId}/reject
3. Backend:
   a. Stores feedback in ProposalFeedback table
   b. Loads original OnboardingSubmissionRequest from parent proposal
   c. Calls Azure OpenAI with feedback context
   d. Returns v2 proposal (linked to v1 via attemptId)
4. Frontend:
   a. Receives PlanProposalResponse with version=2
   b. Updates local state optimistically
   c. User sees new proposal exercises
   d. User can accept v2 or reject again for v3

✅ Result: User gets revised plan based on feedback; chain is maintained
```

---

## Why This Matters for Phase Execution

### Current Blockers

| Phase | Status | Reason |
|-------|--------|--------|
| Phase 1 | ✅ Complete | Setup and scaffolding done |
| Phase 2 | ✅ Complete | DB schema, entities, DTOs done |
| Phase 3 (US1) | ⚠️ **INCOMPLETE** | T063-BUG-001 needed (AI generation broken) |
| Phase 4 (US2) | ❌ **BLOCKED** | T064-BUG-002 needed (revision state broken) |
| Phase 5 (US3) | ❌ **BLOCKED** | Depends on US2 completion + T064-BUG-002 |
| Phase 6 | ❌ **BLOCKED** | Can't harden until core flows work |
| **Smoke Test** | ❌ **FAILS** | Both critical bugs must be fixed |

### What Needs to Happen

**IMMEDIATE (Today)**:
1. Create Phase 7 with T063-BUG-001 and T064-BUG-002 tasks
2. Deprioritize all other work
3. Assign to senior backend engineer

**SHORT TERM (Next 1-2 days)**:
1. T064-BUG-002: Complete PlanProposalService persistence
2. T063-BUG-001: Implement Azure OpenAI integration with LangChain4j
3. Re-run smoke test

**VALIDATION**:
1. Verify T063: AI generates contextual exercises (not hardcoded)
2. Verify T064: Accept proposal creates program; reject creates v2 with linkage
3. Re-run full test suite for US1/US2/US3

---

## Implementation Checkpoints

### For T064-BUG-002 Completion

✅ Done:
- [x] Profile goals form validates and submits
- [x] API endpoints exist and route to services
- [x] DTOs are defined
- [x] Domain entities created
- [x] Database schema migrated

❌ Missing:
- [ ] Repository queries for ProfileGoalOnboardingAttempt
- [ ] Repository queries for PlanProposal lookup
- [ ] Service implementations for state persistence
- [ ] Proposal versioning logic (maintain proposalId chain)
- [ ] Attempt snapshot loading from DB
- [ ] Access gate validation against DB

### For T063-BUG-001 Completion

✅ Done:
- [x] AzureOpenAiOnboardingProperties configured
- [x] OnboardingPlanGenerator component created
- [x] GeneratedBy DTO supports AZURE_OPENAI provider

❌ Missing:
- [ ] LangChain4j dependency in pom.xml
- [ ] Azure OpenAI client bean initialization
- [ ] Prompt engineering for proposal generation
- [ ] JSON parsing of LLM response → ProposedSession objects
- [ ] Error handling (timeout, quota, malformed response)
- [ ] Retry logic with exponential backoff

---

## Test Requirements

### T064-BUG-002-TEST (Backend Integration)

```java
@SpringBootTest
class PlanProposalServiceIT {
    
    @Test
    void getCurrentAttempt_ReturnsPersistedAttempt() {
        // Create attempt via API
        // Query getCurrentAttempt
        // Assert: returns same attempt with proposal chain
    }
    
    @Test
    void createRevision_MaintainsProposalChain() {
        // Create v1 proposal
        // Call createRevision with feedback
        // Assert: v2.version == 2
        // Assert: v2.attemptId == v1.attemptId (same attempt)
        // Assert: v2.proposalId != v1.proposalId (different proposal)
        // Assert: v2 linked to v1 in DB
    }
    
    @Test
    void resolveAttemptSnapshot_LoadsActualUserInputs() {
        // Create attempt with specific age=35, weight=80
        // Call resolveAttemptSnapshot
        // Assert: returns age=35, weight=80 (not hardcoded 30, 75)
    }
    
    @Test
    void getTrackingAccessGate_RealValidation() {
        // User WITHOUT onboarding: canAccessProgramTracking == false
        // User WITH onboarding ACCEPTED: canAccessProgramTracking == true
        // Assert proper state transitions
    }
}
```

### T063-BUG-001-TEST (Backend Integration)

```java
@SpringBootTest
class AzureOpenAiIntegrationIT {
    
    @Test
    @WithMockAzureOpenAi  // Mock or use test endpoint
    void generateInitialProposal_CallsRealLlm() {
        OnboardingSubmissionRequest request = new OnboardingSubmissionRequest(
            30, BigDecimal.valueOf(75), KG, STRENGTH, null
        );
        
        PlanProposalResponse proposal = generator.generateInitialProposal(userId, request);
        
        // Assert: NOT hardcoded
        List<String> exerciseNames = proposal.sessions()
            .flatMap(s -> s.exercises().stream())
            .map(ProposedExerciseTarget::exerciseName)
            .toList();
        
        assertThat(exerciseNames)
            .doesNotContain("Back Squat", "Treadmill Run")  // ← Key assertion
            .hasSize(2);
        
        assertThat(proposal.generatedBy().provider())
            .isEqualTo(ProposalProvider.AZURE_OPENAI);
    }
    
    @Test
    void generateInitialProposal_ErrorHandling() {
        // Test timeout handling
        // Test quota exhaustion
        // Test malformed JSON response
        // Assert: returns RetryableException or user-visible error
    }
}
```

---

## Next Steps

1. ✅ **DONE**: Phase 7 critical tasks added to `tasks.md`
2. ⏭️ **TODO**: Execute T064-BUG-002 (PlanProposalService persistence)
3. ⏭️ **TODO**: Execute T063-BUG-001 (Azure OpenAI + LangChain4j)
4. ⏭️ **TODO**: Re-run smoke test
5. ⏭️ **TODO**: Unlock Phase 4/5 for continued development

---

## Attached Context

**Smoke Test Report Location**: `SMOKE_TEST_ANALYSIS.md` (this file)  
**Tasks File Location**: `specs/002-profile-goal-onboarding/tasks.md` (Phase 7 section added)  
**Affected Files**:
- `backend/src/main/java/com/gymtracker/infrastructure/ai/OnboardingPlanGenerator.java` (hardcoded fake data)
- `backend/src/main/java/com/gymtracker/application/PlanProposalService.java` (incomplete stubs)
- `backend/src/main/java/com/gymtracker/api/ProfileGoalOnboardingController.java` (works correctly; waiting for service fix)

---

**Analysis Complete** ✅  
**Status**: Ready for remediation  
**Severity**: 🔴 P0 - CRITICAL (blocks all use)

