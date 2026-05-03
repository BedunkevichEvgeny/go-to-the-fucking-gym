# Test Coverage Verification (T109)

Date: 2026-05-02
Feature: `specs/001-workout-tracker`

## Coverage Tooling Executed

- Backend (JaCoCo): `mvn verify` in `backend/`
- Frontend (Vitest + Istanbul): `npm run test:coverage` in `frontend/`

## Coverage Report Locations

- Backend HTML report: `backend/target/site/jacoco/index.html`
- Backend raw summary: `backend/target/site/jacoco/jacoco.csv`
- Frontend HTML report: `frontend/coverage/index.html`
- Frontend JSON summary: `frontend/coverage/coverage-summary.json`

## Layer Coverage Snapshot

### Backend (JaCoCo)

- `com.gymtracker.application`: **92.52%** line coverage
- `com.gymtracker.domain`: **36.36%** line coverage

Target note: the `application` layer meets the >=80% goal. The `domain` layer is currently below target because many entity classes are simple data models with little direct behavioral test coverage.

### Frontend (Vitest + Istanbul)

- Overall lines: **61.19%**
- Overall statements: **60.66%**
- Overall branches: **67.14%**
- Overall functions: **65.38%**

## Spec Matrix Coverage Verification

Matrix source: `specs/001-workout-tracker/spec.md` (Test Coverage Requirements table).

| Business Rule | Unit Test Evidence | Integration Test Evidence | Status |
|---|---|---|---|
| Program session displays AI-predefined exercises | `backend/src/test/java/com/gymtracker/application/ProgramSessionServiceTest.java` (`loadNextUncompletedSessionReturnsTheNextSession`) | `backend/src/test/java/com/gymtracker/api/ProgramSessionControllerIT.java` (`getNextProgramSessionReturnsSessionWithTargets`) | PASS |
| Program exercises cannot be modified in MVP | `backend/src/test/java/com/gymtracker/application/SessionValidatorTest.java` (`validateProgramSessionNotModifiableRejectsAddRemoveOrReorder`) | `backend/src/test/java/com/gymtracker/api/LoggedSessionControllerIT.java` (`postLoggedSessionWithWrongNextSessionReturnsForbidden`) | PASS |
| Free session accepts custom exercise names | `backend/src/test/java/com/gymtracker/application/ExerciseLibraryServiceTest.java` (`searchExerciseLibraryReturnsActiveMatchingExercisesCaseInsensitive`) | `backend/src/test/java/com/gymtracker/api/FreeSessionControllerIT.java` (`postFreeSessionWithCustomExerciseNameReturnsCreated`) | PASS |
| Session saved with all exercise/set data | `backend/src/test/java/com/gymtracker/application/LoggedSessionServiceTest.java` (`saveLoggedSessionPersistsSessionWithEntriesAndSets`) | `backend/src/test/java/com/gymtracker/api/LoggedSessionControllerIT.java` (`postLoggedSessionPersistsEntriesSetsAndFeelings`) | PASS |
| Feelings saved with session | `backend/src/test/java/com/gymtracker/application/LoggedSessionServiceTest.java` (`saveLoggedSessionSavesSessionFeelingsWithOptionalComment`) | `backend/src/test/java/com/gymtracker/api/LoggedSessionControllerIT.java` (`postLoggedSessionPersistsEntriesSetsAndFeelings`) | PASS |
| Body weight sets accepted without weight value | `backend/src/test/java/com/gymtracker/application/SessionValidatorTest.java` (`validateBodyweightSetAllowsNullWeightWhenBodyweight`) | `backend/src/test/java/com/gymtracker/api/LoggedSessionControllerIT.java` (`postLoggedSessionPersistsBodyweightSetWithoutWeight`) | PASS |
| Cardio entries use duration/distance, not sets | `backend/src/test/java/com/gymtracker/application/CardioValidatorTest.java` (`validateCardioEntryRequiresAtLeastOneLap`) | `backend/src/test/java/com/gymtracker/api/FreeSessionControllerIT.java` (`postFreeSessionWithCardioAndBodyweightDataPersistsCorrectly`) | PASS |
| History returned in reverse-chronological order | `backend/src/test/java/com/gymtracker/application/SessionHistoryServiceTest.java` (`getSessionHistoryReturnsReverseChronologicalOrderAndPageMetadata`) | `backend/src/test/java/com/gymtracker/api/SessionHistoryControllerIT.java` (`getHistoryReturnsSessionsInReverseChronologicalOrder`) | PASS |
| Date range filter applied correctly | `backend/src/test/java/com/gymtracker/application/SessionHistoryServiceTest.java` (`getSessionHistoryAppliesDateRangeFilterSpecification`) | `backend/src/test/java/com/gymtracker/api/SessionHistoryControllerIT.java` (`getHistoryFiltersByDateRange`) | PASS |
| Progression data sorted chronologically | `backend/src/test/java/com/gymtracker/application/ProgressionServiceTest.java` (`getExerciseProgressionDelegatesToQueryBuilderAndReturnsPoints`) | `backend/src/test/java/com/gymtracker/api/ProgressionControllerIT.java` (`getProgressionReturnsExerciseNameAndChronologicalPoints`) | PASS |
| User data isolation | `backend/src/test/java/com/gymtracker/infrastructure/UserIsolationTest.java` (`userCannotAccessAnotherUsersLoggedSessionDetails`) | `backend/src/test/java/com/gymtracker/api/ProgramSessionControllerIT.java` (`getNextProgramSessionEnforcesCrossUserIsolation`) | PASS |

## Validation Result

- Coverage tooling execution: PASS
- Spec matrix mapping verification: PASS
- >=80% target for domain+application where possible: PARTIAL PASS (`application` meets target, `domain` does not)

---

# Regression Matrix Verification (T062)

Date: 2026-05-02
Features: `specs/001-workout-tracker` + `specs/002-profile-goal-onboarding`

## Commands Executed

- Backend targeted unblock check: `mvn -Dtest=UserIsolationTest test`
- Backend full regression: `mvn test`
- Frontend lint: `npm run lint`
- Frontend regression tests: `npm test`

## Result Matrix

| Area | Command | Result | Notes |
|---|---|---|---|
| Backend targeted tests | `mvn -Dtest=UserIsolationTest test` | PASS | `UserIsolationTest` compiles and passes with missing Mockito/JPA symbols resolved. |
| Backend full regression | `mvn test` | PASS | 102 tests run, 0 failures, 0 errors, 0 skipped (`backend/test-rerun.log`). |
| Frontend lint | `npm run lint` | PASS | ESLint completes with no reported errors (`frontend/lint-rerun.log`). |
| Frontend full tests | `npm test` | PASS | 15 test files passed; 39 tests passed (`frontend/test-rerun.log`). |

## Remaining Follow-up (Non-blocking)

- Frontend test output still includes React Router future-flag warnings and one React `act(...)` warning in existing hook tests.
- Backend Maven output still includes environment-level warnings (Mockito agent/self-attach, local Maven settings warning about unrecognised `<url>`).

