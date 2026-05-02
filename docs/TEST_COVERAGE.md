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

