---
description: "Dependency-ordered implementation tasks for Workout Tracker (001-workout-tracker) feature"
---

# Tasks: Workout Tracker

**Feature**: `001-workout-tracker`  
**Branch**: `001-workout-tracker` | **Created**: 2026-04-26  
**Input**: Design documents from `/specs/001-workout-tracker/`  
**Tech Stack**: Java 21 + Spring Boot 4.0.5 | React 18 | PostgreSQL 16 | Azure LangChain 1.13.1 + OpenAI  
**Prerequisites**: plan.md (tech stack, structure), spec.md (user stories P1-P3), research.md (decisions), data-model.md (entities), quickstart.md (dev flow), contracts/workout-tracker-api.yaml (API boundary)

**Format**: `- [ ] [TaskID] [P?] [Story?] Description with file path`
- `[P]`: Parallelizable (different files, no blocking dependencies)
- `[Story]`: User story label (US1, US2, US3, US4) for traceability
- **Critical Path**: Setup → Foundational → US1/US2 (parallel P1) → US3 (P2) → US4 (P3) → AI Handoff → Polish

**Tests**: All business-rule tests are MANDATORY (unit + integration per spec.md test coverage matrix). Tests are part of implementation tasks; they fail before code.

---

## Phase 1: Setup (Shared Infrastructure & Initialization)

**Purpose**: Create project structure, initialize dependencies, establish dev tooling

**Constraint**: Linear dependency - each task sets foundation for next phase

- [X] T001 Create project directory structure per plan.md: `backend/`, `frontend/`, `backend/src/main/java/com/gymtracker/{api,application,domain,infrastructure,ai}`, `backend/src/test/{unit,integration}`, `frontend/src/{components,pages,features,services,hooks}`, `frontend/tests/` in repository root
- [X] T002 Initialize Java 21 Spring Boot 4.0.5 backend with Maven/Gradle build file in `backend/pom.xml` (or Gradle equivalent) including: spring-boot-starter-web, spring-boot-starter-data-jpa, spring-boot-starter-validation, spring-boot-starter-security, com.langchain4j:langchain4j-azure-open-ai:1.13.1 (pinned version), postgresql:postgresql (driver), testcontainers:postgresql (testing)
- [X] T003 Initialize React 18 frontend with TypeScript 5.x in `frontend/package.json`: react, react-router-dom, @tanstack/react-query, recharts (or chart.js), typescript, vitest, @testing-library/react, axios
- [X] T004 [P] Create base linting configuration: `backend/.editorconfig`, `backend/checkstyle.xml`, `frontend/.eslintrc.json`, `frontend/.prettierrc.json`
- [X] T005 [P] Create local development documentation: `backend/LOCAL_DEV.md` (database setup, auth seed users), `frontend/LOCAL_DEV.md` (API base URL, HMR setup)

---

## Phase 2: Foundational (Blocking Prerequisites - Must Complete Before Any User Story)

**Purpose**: Core backend domain model, persistence layer, security bootstrap, logging/validation framework

**⚠️ CRITICAL**: No user story work can begin until this phase is complete. This phase enables all downstream tasks.

### Data Model & Persistence Foundation

- [X] T006 [P] Create PostgreSQL schema migration file in `backend/src/main/resources/db/migration/V001__initial_schema.sql` with tables: `users`, `workout_programs`, `program_sessions`, `program_exercise_targets`, `logged_sessions`, `session_feelings`, `exercise_entries`, `strength_sets`, `cardio_laps`, `exercises` (library); include indexes per data-model.md (user_id, session_date desc, exercise_name_snapshot)
- [X] T007 [P] Create base JPA entity classes in `backend/src/main/java/com/gymtracker/domain/`:
  - `User.java` (id UUID, preferredWeightUnit enum)
  - `WorkoutProgram.java` (id, userId, name, status enum, createdAt, completedAt)
  - `ProgramSession.java` (id, programId, sequenceNumber, name, isCompleted)
  - `ProgramExerciseTarget.java` (id, programSessionId, exerciseName, exerciseType enum, targets for sets/reps/weight/cardio)
  - `LoggedSession.java` (id, userId, sessionType enum, programSessionId optional, sessionDate, name optional, notes optional, totalDurationSeconds, createdAt)
  - `SessionFeelings.java` (sessionId PK/FK, rating 1-10, comment optional)
  - `ExerciseEntry.java` (id, loggedSessionId, exerciseId optional, customExerciseName optional, exerciseNameSnapshot, exerciseType enum, sortOrder)
  - `StrengthSet.java` (id, exerciseEntryId, setOrder, reps, weightValue optional, weightUnit enum optional, isBodyWeight, durationSeconds optional, restSeconds optional)
  - `CardioLap.java` (id, exerciseEntryId, lapOrder, durationSeconds, distanceValue optional, distanceUnit enum optional)
  - `Exercise.java` (id, name unique, category, type enum, description optional, isActive boolean)
  - Ensure all entities have @Entity, @Table, @Column annotations and Hibernate/JPA best practices
- [X] T008 [P] Create remaining JPA entities in `backend/src/main/java/com/gymtracker/domain/`:
  - `LoggedSession.java` (id, userId, sessionType enum, programSessionId optional, sessionDate, name optional, notes optional, totalDurationSeconds, createdAt)
  - `SessionFeelings.java` (sessionId PK/FK, rating 1-10, comment optional)
  - `ExerciseEntry.java` (id, loggedSessionId, exerciseId optional, customExerciseName optional, exerciseNameSnapshot, exerciseType enum, sortOrder)
  - `StrengthSet.java` (id, exerciseEntryId, setOrder, reps, weightValue optional, weightUnit enum optional, isBodyWeight, durationSeconds optional, restSeconds optional)
  - `CardioLap.java` (id, exerciseEntryId, lapOrder, durationSeconds, distanceValue optional, distanceUnit enum optional)
  - `Exercise.java` (id, name unique, category, type enum, description optional, isActive boolean)
  - Include all JPA annotations and validation constraints from data-model.md
- [X] T009 Create JPA repositories in `backend/src/main/java/com/gymtracker/infrastructure/repository/`:
  - `UserRepository extends JpaRepository<User, UUID>` with method `findById(UUID)`
  - `WorkoutProgramRepository extends JpaRepository<WorkoutProgram, UUID>` with methods `findActiveByUserId(UUID)`, `findById(UUID)`
  - `ProgramSessionRepository extends JpaRepository<ProgramSession, UUID>` with methods `findNextUncompletedByProgram(UUID)`, `findById(UUID)`
  - `LoggedSessionRepository extends JpaRepository<LoggedSession, UUID>` with methods `findByUserIdOrderBySessionDateDesc(UUID, Pageable)`, `findByUserIdAndExerciseName(UUID, String, Pageable)`, `findByUserIdAndSessionDateBetween(UUID, LocalDate, LocalDate, Pageable)`
  - `ExerciseRepository extends JpaRepository<Exercise, UUID>` with method `findByNameIgnoreCase(String)`
  - Implement custom @Query methods for filtering, pagination, and text search as required
- [X] T010 Create Spring Data Specification/Predicate helper for filtering in `backend/src/main/java/com/gymtracker/infrastructure/repository/SessionSpecifications.java` to support date range filter, exercise name search, and reverse-chronological sort per FR-016 and FR-017

### Security & Authentication

- [X] T011 Configure Spring Security HTTP Basic Auth with in-memory user store in `backend/src/main/resources/application.properties` (or application.yml): define seed users (e.g., user1/password1, user2/password2) with ROLE_USER; configure basic auth header requirement
- [X] T012 Create Spring Security configuration class in `backend/src/main/java/com/gymtracker/infrastructure/config/SecurityConfig.java`: enable HTTP Basic Auth, disable CSRF for MVP, configure AuthenticationManager, ensure authenticated user context is available in all endpoints via SecurityContextHolder.getContext().getAuthentication()
- [X] T013 Create `AuthenticationService` in `backend/src/main/java/com/gymtracker/application/security/AuthenticationService.java` with method `getCurrentUserId(): UUID` that extracts user ID from Spring Security principal (map Spring username to UUID) and enforces strict per-user data isolation (all service methods validate authenticated user matches queried data)

### Logging & Validation Framework

- [X] T014 [P] Configure SLF4J logging in `backend/src/main/resources/application.properties`: set log level to INFO for `com.gymtracker`, DEBUG for Spring core (development profile); ensure logs include request/response timing for performance monitoring
- [X] T015 [P] Create custom validation annotations in `backend/src/main/java/com/gymtracker/infrastructure/validation/`:
  - `@ValidSessionType` - ensures PROGRAM or FREE
  - `@ValidRating` - ensures 1-10 range
  - `@ValidExerciseType` - ensures STRENGTH, BODYWEIGHT, or CARDIO
  - `@ValidWeightUnit` - ensures KG or LBS
  - Each annotation includes corresponding ConstraintValidator implementation
- [X] T016 [P] Create global exception handler in `backend/src/main/java/com/gymtracker/api/exception/GlobalExceptionHandler.java` with @ControllerAdvice: handle ValidationException (400), UnauthorizedException (401), ForbiddenException (403), ResourceNotFoundException (404), all returning standardized error JSON with message + code

### Base API & Shared Utilities

- [X] T017 [P] Create DTO classes in `backend/src/main/java/com/gymtracker/api/dto/`:
  - `ProgramSessionView` (programSessionId, sequenceNumber, name, exercises list)
  - `ProgramExerciseTargetView` (exerciseName, exerciseType, target sets/reps/weight/cardio fields)
  - `LoggedSessionCreateRequest` (sessionType, programSessionId optional, sessionDate, name optional, notes optional, totalDurationSeconds optional, feelings, exerciseEntries list)
  - `SessionFeelings` (rating 1-10, comment optional)
  - `ExerciseEntryInput` (exerciseName, exerciseType, sets array, cardioLaps array)
  - `StrengthSetInput` (reps, isBodyWeight, weightValue optional, weightUnit optional)
  - `CardioLapInput` (durationSeconds, distanceValue optional, distanceUnit optional)
  - `SessionHistoryPage` (items array, page, size, totalItems)
  - `SessionHistoryItem` (sessionId, sessionDate, sessionType, exerciseCount, totalDurationSeconds optional)
  - `LoggedSessionDetail` (extends LoggedSessionCreateRequest with sessionId)
  - `ProgressionResponse` (exerciseName, points array)
  - `ProgressionPoint` (sessionId, sessionDate, metricType enum WEIGHT/DURATION/DISTANCE, metricValue)
  - Use @Valid annotations and bean validation per contracts/workout-tracker-api.yaml
- [X] T018 [P] Create ModelMapper/DTO converter utility in `backend/src/main/java/com/gymtracker/infrastructure/mapper/DtoMapper.java` with methods:
  - `toDto(ProgramSession ps, List<ProgramExerciseTarget> targets): ProgramSessionView`
  - `toDomain(LoggedSessionCreateRequest req): LoggedSession` (validates user_id matches auth context)
  - `toDetailDto(LoggedSession ls): LoggedSessionDetail`
  - `toHistoryItem(LoggedSession ls): SessionHistoryItem`
  - `toProgressionPoint(LoggedSession ls, ExerciseEntry ee, StrengthSet ss or CardioLap cl): ProgressionPoint`
- [X] T019 Create base controller class in `backend/src/main/java/com/gymtracker/api/BaseController.java` that:
  - Provides convenient method `extractUserId()` that calls AuthenticationService.getCurrentUserId()
  - Configures default @RequestMapping("/api")
  - Ensures all endpoints log request start/end with userId + operation for audit trail

**Checkpoint: Foundation Complete** ✓  
All downstream user story tasks can now proceed. Shared domain model, security, persistence, and DTOs are locked. All Phase 2 tasks are independent and can be parallelized by role (backend persistence specialist, security lead, framework integrator).

---

## Phase 3: User Story 1 - Log a Program Session (Priority: P1) 🎯 MVP

**Goal**: Users with an AI-assigned program can log their next uncompleted program session, filling in actual performance data for each predefined exercise and recording post-session feelings. Data is immediately persisted and made available for AI Coach program adjustment.

**Independent Test Scenario**: 
1. Pre-populate user1 with active program (3 sessions, first with 3 exercises: Bench Press 3×8@70kg, Squats 4×6@100kg, Deadlift 1×5@120kg)
2. Call GET /api/program-sessions/next as user1 → verify returns Session 1 with 3 exercises and targets
3. POST /api/logged-sessions with PROGRAM type, filled performance data (actual sets/reps/weights differ from targets), feeling rating 7 + comment
4. Verify session saved with all data intact, program-session marked completed, feelings persisted
5. GET /api/logged-sessions/history as user1 → verify new session appears in reverse-chronological order

### Unit Tests for User Story 1 (MANDATORY - Write First, Fail Before Implementation)

- [X] T020 [P] [US1] Create `backend/src/test/java/com/gymtracker/domain/ProgramSessionTest.java`: unit tests for ProgramSession entity:
  - Test creating program session with valid sequenceNumber, name, isCompleted=false
  - Test that exercises list is populated from ProgramExerciseTarget relationship
  - Test validation: sequenceNumber min 1, name 1-120 chars
  - Test state transition: isCompleted false → true
- [X] T021 [P] [US1] Create `backend/src/test/java/com/gymtracker/application/ProgramSessionServiceTest.java`: 
  - Test `loadNextUncompletedSession(userId)` returns next uncompleted session in sequence
  - Test `loadNextUncompletedSession(userId)` returns null when program completed
  - Test `loadNextUncompletedSession(userId)` enforces user isolation (rejects cross-user access)
  - Test `markProgramSessionCompleted(sessionId, userId)` transitions to completed
  - Test `markProgramSessionCompleted(sessionId, userId)` throws ForbiddenException for wrong user
- [X] T022 [P] [US1] Create `backend/src/test/java/com/gymtracker/application/LoggedSessionServiceTest.java`:
  - Test `saveLoggedSession(userId, request)` persists LoggedSession with all exercise entries and sets
  - Test `saveLoggedSession(userId, request)` validates programSessionId matches next uncompleted session for PROGRAM type
  - Test `saveLoggedSession(userId, request)` saves SessionFeelings with rating 1-10 and optional comment
  - Test `saveLoggedSession(userId, request)` rejects empty exercise list (minItems 1)
  - Test `saveLoggedSession(userId, request)` rejects exercise with 0 sets
  - Test `saveLoggedSession(userId, request)` accepts body-weight sets without weight value
  - Test `saveLoggedSession(userId, request)` enforces user ownership (request user_id must match auth)
- [X] T023 [P] [US1] Create `backend/src/test/java/com/gymtracker/application/SessionValidatorTest.java`:
  - Test `validateProgramSessionNotModifiable(programSessionId, exerciseList)` rejects attempts to add/remove/reorder exercises in PROGRAM mode
  - Test `validateBodyweightSet(set)` allows weight=null when isBodyWeight=true
  - Test `validateBodyweightSet(set)` rejects weight=null when isBodyWeight=false
  - Test `validateSessionFeelings(feelings)` requires rating 1-10
  - Test `validateSessionFeelings(feelings)` allows null comment
  - Test `validateExerciseEntry(entry)` requires at least 1 set for strength entries, at least 1 lap for cardio
- [X] T024 [P] [US1] Create `backend/src/test/java/com/gymtracker/infrastructure/UserIsolationTest.java`:
  - Test user1 cannot access user2's logged sessions (throws ForbiddenException on read/write)
  - Test user1 cannot update another user's program session status
  - Test that authenticated user context is properly extracted and validated in all data access paths

### Integration Tests for User Story 1 (MANDATORY)

- [X] T025 [P] [US1] Create `backend/src/test/java/com/gymtracker/api/ProgramSessionControllerIT.java` (Spring Boot integration test with @SpringBootTest + @DataJpaTest):
  - Test GET /api/program-sessions/next returns 200 + ProgramSessionView with 3 exercises and targets
  - Test GET /api/program-sessions/next returns 204 No Content when no active program
  - Test GET /api/program-sessions/next returns 204 when program completed
  - Test HTTP Basic Auth: unauthenticated request returns 401
  - Test cross-user isolation: user1 cannot see user2's program (even if both have programs)
  - Use Testcontainers to spin up PostgreSQL, seed test data via repository
- [X] T026 [P] [US1] Create `backend/src/test/java/com/gymtracker/api/LoggedSessionControllerIT.java`:
  - Test POST /api/logged-sessions with valid PROGRAM request returns 201 + LoggedSessionDetail with sessionId
  - Test POST /api/logged-sessions persists all exercise entries, sets, and feelings to database
  - Test POST /api/logged-sessions with exercise entry containing 0 sets returns 400 (validation error)
  - Test POST /api/logged-sessions with PROGRAM type validates programSessionId is next uncompleted (reject if already completed)
  - Test POST /api/logged-sessions with body-weight set (isBodyWeight=true, weight=null) persists successfully
  - Test POST /api/logged-sessions marks corresponding ProgramSession as completed after save
  - Test POST /api/logged-sessions with wrong programSessionId (not user's next) returns 403 Forbidden
  - Verify completed program transitions to COMPLETED status in WorkoutProgram
  - Use HTTP Basic Auth header with seed user credentials
  - Verify response schema matches contracts/workout-tracker-api.yaml
- [X] T027 [P] [US1] Create `backend/src/test/java/com/gymtracker/api/SessionHistoryControllerIT.java` (sufficient for now; can also be part of US3 integration):
  - Test GET /api/logged-sessions/history returns 200 + SessionHistoryPage with created session
  - Verify session appears in reverse-chronological order
  - Verify response schema matches SessionHistoryPage contract

### Frontend Tests for User Story 1 (MANDATORY)

- [X] T028 [P] [US1] Create `frontend/src/features/program-session/__tests__/ProgramSessionForm.test.tsx`:
  - Test form renders "Next Program Session: Session 1 — Upper Body"
  - Test form displays 3 exercises with AI targets (e.g., "Target: 3 × 8 @ 70 kg")
  - Test form renders input fields for actual performance (sets, reps, weight, unit)
  - Test form allows user to enter different values than targets (e.g., only 2 reps instead of 8)
  - Test form allows user to mark sets as body-weight (isBodyWeight toggle)
  - Test form allows user to enter optional body-weight sets without weight value
  - Test form renders feelings section: rating 1-10 slider + optional comment text area
  - Test form validates: at least 1 set required per exercise
  - Test form validates: rating required before save
  - Test form submit calls API endpoint with correct payload structure
  - Test form shows loading state and success message after save
- [X] T029 [P] [US1] Create `frontend/src/hooks/__tests__/useLogSession.test.ts` (custom hook for session mutation):
  - Test `useLogSession` hook returns `mutate(data)` and `isLoading` state
  - Test `mutate(data)` calls POST /api/logged-sessions with payload
  - Test `mutate(data)` returns session ID on success
  - Test `mutate(data)` sets error state on 400/403 responses
  - Test hook integrates with React Query (or equivalent state management)

### Implementation for User Story 1

- [X] T030 [P] [US1] Create `backend/src/main/java/com/gymtracker/application/ProgramSessionService.java`:
  - Implement `loadNextUncompletedSession(UUID userId): Optional<ProgramSessionView>` - queries WorkoutProgram.status=ACTIVE, finds next uncompleted ProgramSession by sequenceNumber, loads ProgramExerciseTargets, returns DTOified view
  - Implement `markProgramSessionCompleted(UUID sessionId, UUID userId): void` - validates session belongs to user's program, marks isCompleted=true, checks if all program sessions completed → marks program status=COMPLETED
  - All queries filtered by userId (injected from AuthenticationService)
  - Log method entry/exit with userId for audit trail
- [X] T031 [P] [US1] Create `backend/src/main/java/com/gymtracker/application/LoggedSessionService.java`:
  - Implement `saveLoggedSession(UUID userId, LoggedSessionCreateRequest request): LoggedSessionDetail` - validates request, creates LoggedSession + ExerciseEntry + StrengthSet/CardioLap + SessionFeelings records, persists to DB, returns DTO
  - Validate programSessionId for PROGRAM type (query next uncompleted, must match, must belong to user)
  - Call ProgramSessionService.markProgramSessionCompleted after successful save for PROGRAM type
  - Validate all exercise entries have at least 1 set (strength) or lap (cardio)
  - Log operation: "[userId] saved [sessionType] session with [exerciseCount] exercises"
- [X] T032 [P] [US1] Create `backend/src/main/java/com/gymtracker/application/SessionValidatorService.java`:
  - Implement `validateProgramSessionNotModifiable(UUID programSessionId, List<ExerciseEntry> entries): void` - fetch actual targets, verify entries match in count and exercise name, throw ForbiddenException if modified
  - Implement `validateBodyweightSet(StrengthSetInput set): void` - if isBodyWeight=true, reject if weight!=null (or warn); if isBodyWeight=false, require weight!=null
  - Implement `validateExerciseEntry(ExerciseEntryInput entry): void` - if STRENGTH/BODYWEIGHT, require sets.size()>=1; if CARDIO, require cardioLaps.size()>=1
  - Implement `validateSessionFeelings(SessionFeelings feelings): void` - require rating 1-10
  - Throw ValidationException with specific error messages for each violation
- [X] T033 [US1] Create `backend/src/main/java/com/gymtracker/api/SessionController.java` with endpoints:
  - `GET /program-sessions/next` - calls ProgramSessionService.loadNextUncompletedSession, returns 200 ProgramSessionView or 204 No Content
  - `POST /logged-sessions` - calls LoggedSessionService.saveLoggedSession, returns 201 LoggedSessionDetail or 400/403 on error
  - Extract userId from AuthenticationService in both endpoints
  - Implement proper error handling via GlobalExceptionHandler
  - Add @Valid annotations to request bodies to trigger bean validation
  - Log request/response via logging framework
- [X] T034 [US1] Create React page component in `frontend/src/pages/ProgramSessionPage.tsx`:
  - Fetch next program session on page load: `GET /api/program-sessions/next`
  - Display "No active program" empty state if 204 response
  - Render session name + sequence number as page header
  - Render `<ProgramSessionForm exercises={session.exercises} onSubmit={handleSave} />` component
  - On form submit, call `useLogSession().mutate(formData)`
  - Show loading spinner during submit
  - Show success toast + navigate to history page on successful save
  - Show error toast with validation messages on 400/403 errors
  - Include back button to main menu
- [X] T035 [US1] Create React form component in `frontend/src/features/program-session/ProgramSessionForm.tsx`:
  - Accept exercises array prop (with AI targets) + onSubmit callback
  - Render exercise list, each with:
    - Exercise name + AI target (e.g., "Bench Press — Target: 3 × 8 @ 70 kg")
    - Set-by-set input form: reps, weight, unit, isBodyWeight toggle
    - "Add Set" button to add blank set row
    - "Remove Set" button per set
    - Validation: at least 1 set required before save
  - Render feelings section below exercises:
    - Rating slider 1-10 (required)
    - Comment text area (optional)
  - Render submit button ("Save Session") + cancel button
  - Use React Hook Form for form state management
  - Validate form before submit, show error messages inline
- [X] T036 [US1] Create custom React hook in `frontend/src/hooks/useLogSession.ts`:
  - Wrap `useMutation` from TanStack Query
  - Define mutation function: POST `/api/logged-sessions` with LoggedSessionCreateRequest payload
  - Configure error handling, loading state
  - Return `{ mutate, isLoading, isError, error }`
- [X] T037 [US1] Create React hook in `frontend/src/hooks/useNextProgramSession.ts`:
  - Wrap `useQuery` from TanStack Query
  - Query: GET `/api/program-sessions/next`
  - Handle 204 response as "no program" state
  - Return `{ data: ProgramSessionView | null, isLoading, error }`
- [X] T038 [US1] Add UI polish and consistent interaction patterns in ProgramSessionForm:
  - Visual feedback: disabled state on submit button while loading
  - Input validation feedback: show error icon + message under each invalid field
  - Consistent styling: use CSS-in-JS or Tailwind for spacing, colors, typography
  - Accessibility: label all inputs, use semantic HTML, ensure tab order is logical
  - Empty state handling: if no exercises in program session, show "No exercises in this session" message

**Checkpoint: User Story 1 Complete** ✓  
Program session logging fully functional end-to-end (backend + frontend). Feelings + exercise data persisted. Program completion tracked. Can be tested independently without US2/US3.

---

## Phase 4: User Story 2 - Log a Free Session (Priority: P1)

**Goal**: Users without a program (or who want to log off-program workouts) can create a free session, add exercises from the library or custom exercises, record performance data, and save. Free sessions enable data collection outside the AI program flow.

**Independent Test Scenario**:
1. User1 (no active program) clicks "Start Free Session"
2. Search library for "Bench Press" → add it
3. Type "Tire Flip" (custom) → add it as custom exercise
4. For Bench Press: enter 3 sets (8 reps @ 70kg, 8 reps @ 70kg, 6 reps @ 65kg)
5. For Tire Flip: mark as bodyweight, enter 1 set (12 reps, no weight)
6. Record feeling: rating 8, comment "Felt strong"
7. Save → verify session appears in history, marked as FREE

### Unit Tests for User Story 2 (MANDATORY - Write First)

- [X] T039 [P] [US2] Create `backend/src/test/java/com/gymtracker/application/FreeSessionServiceTest.java`:
  - Test `saveFreeSession(userId, request)` persists LoggedSession with sessionType=FREE, programSessionId=null
  - Test `saveFreeSession(userId, request)` accepts custom exercise names (not in library)
  - Test `saveFreeSession(userId, request)` accepts library exercise IDs
  - Test `saveFreeSession(userId, request)` supports both strength and cardio entries in same session
  - Test `saveFreeSession(userId, request)` validates at least 1 exercise required
  - Test `saveFreeSession(userId, request)` enforces user ownership
- [X] T040 [P] [US2] Create `backend/src/test/java/com/gymtracker/application/ExerciseLibraryServiceTest.java`:
  - Test `searchExerciseLibrary(query)` returns list of Exercise entities matching name (case-insensitive)
  - Test `searchExerciseLibrary(query)` returns active exercises only (isActive=true)
  - Test `createCustomExercise(userId, name)` stores custom exercise reference in ExerciseEntry (customExerciseName field)
  - Test `getTopExercises()` returns most-used exercises for UI suggestions
- [X] T041 [P] [US2] Create `backend/src/test/java/com/gymtracker/application/CardioValidatorTest.java`:
  - Test `validateCardioLap(cardioLap)` requires durationSeconds >= 1
  - Test `validateCardioLap(cardioLap)` allows distanceValue=null
  - Test `validateCardioEntry(entry)` requires at least 1 lap
  - Test cardio entries can coexist with strength entries in same session

### Integration Tests for User Story 2 (MANDATORY)

- [X] T042 [P] [US2] Create `backend/src/test/java/com/gymtracker/api/ExerciseLibraryControllerIT.java`:
  - Test GET /api/exercises?query=bench returns list of Exercise with matching name
  - Test GET /api/exercises returns top 50 exercises (for initial UI population) ordered by usage
  - Test response includes exercise name, category, type, description
  - Test only active exercises (isActive=true) returned
- [X] T043 [P] [US2] Create `backend/src/test/java/com/gymtracker/api/FreeSessionControllerIT.java`:
  - Test POST /api/logged-sessions with sessionType=FREE, library exercise IDs, returns 201
  - Test POST /api/logged-sessions with sessionType=FREE, custom exercise names, returns 201
  - Test POST /api/logged-sessions with mixed library + custom exercises persists all correctly
  - Test POST /api/logged-sessions with cardio laps (duration + optional distance) persists correctly
  - Test POST /api/logged-sessions with body-weight sets in free session persists correctly
  - Test POST /api/logged-sessions free session does NOT require programSessionId (must be null)
  - Test sessionType=FREE session does NOT mark any program session as completed
  - Verify created session appears in history with exerciseCount and correct sessionType

### Frontend Tests for User Story 2 (MANDATORY)

- [X] T044 [P] [US2] Create `frontend/src/features/free-session/__tests__/FreeSessionForm.test.tsx`:
  - Test form renders "Start Free Session" heading
  - Test form renders exercise search/add section
  - Test search input filters library exercises dynamically
  - Test clicking "Add Exercise" adds exercise to session with empty set inputs
  - Test form allows typing custom exercise name if not in library (e.g., "Tire Flip")
  - Test form renders exercise list with add/remove buttons per exercise
  - Test form allows adding strength sets (reps, weight, unit, isBodyWeight)
  - Test form allows adding cardio laps (duration, optional distance)
  - Test form allows removing individual sets/laps
  - Test feelings section: rating 1-10 slider + optional comment
  - Test form validates: at least 1 exercise required
  - Test form validates: at least 1 set/lap per exercise required
  - Test form submit calls API with correct payload (sessionType=FREE, exerciseEntries, no programSessionId)
- [X] T045 [P] [US2] Create `frontend/src/hooks/__tests__/useExerciseLibrary.test.ts`:
  - Test `useExerciseLibrary(query)` hook queries GET /api/exercises?query=[query]
  - Test hook returns exercises list with name, category, type
  - Test hook debounces search to avoid excessive requests
- [X] T046 [P] [US2] Create `frontend/src/features/free-session/__tests__/ExerciseLibrarySearch.test.tsx`:
  - Test search input triggers useExerciseLibrary hook
  - Test displays search results as clickable list
  - Test results show exercise name + category (e.g., "Bench Press (Chest)")
  - Test clicking result or typing custom name adds exercise to session form

### Implementation for User Story 2

- [X] T047 [P] [US2] Create `backend/src/main/java/com/gymtracker/application/ExerciseLibraryService.java`:
  - Implement `searchExerciseLibrary(String query): List<ExerciseDTO>` - query Exercise table with name LIKE, filter isActive=true, return paginated results (top 20)
  - Implement `getTopExercises(): List<ExerciseDTO>` - query exercises ordered by frequency in recent sessions, return top 50
  - Implement `getExerciseById(UUID id): ExerciseDTO` - fetch single exercise by ID
  - Implement `validateExerciseExists(UUID id)` - throw ResourceNotFoundException if not found
  - Cache results for 1 hour to reduce DB load (optional but recommended)
- [X] T048 [P] [US2] Create `backend/src/main/java/com/gymtracker/application/FreeSessionService.java`:
  - Implement `saveFreeSession(UUID userId, LoggedSessionCreateRequest request): LoggedSessionDetail` - similar to saveLoggedSession but validates sessionType=FREE, programSessionId=null, accepts custom exercise names, persists ExerciseEntry with customExerciseName field
  - Validate at least 1 exercise entry required
  - Validate each exercise has at least 1 set (strength) or lap (cardio)
  - Support mixed exercise types in single session
- [X] T049 [US2] Extend `backend/src/main/java/com/gymtracker/api/SessionController.java`:
  - Extend POST /api/logged-sessions endpoint logic to handle both PROGRAM and FREE session types via a router/factory pattern or conditional dispatch
  - Add GET /api/exercises?query={query} endpoint - calls ExerciseLibraryService.searchExerciseLibrary, returns 200 + list of ExerciseDTO
  - Add optional GET /api/exercises endpoint (no query param) to return top exercises
- [X] T050 [US2] Create React page component in `frontend/src/pages/FreeSessionPage.tsx`:
  - Display "Start Free Session" heading
  - Render `<FreeSessionForm />` component
  - On successful save, show toast + navigate to history page
  - Include back button to main menu
- [X] T051 [US2] Create React form component in `frontend/src/features/free-session/FreeSessionForm.tsx`:
  - Render exercise search/library section at top
  - Render exercise list with add/remove buttons
  - For each exercise: support exercise-specific input form (strength sets or cardio laps)
  - Use shared UX components (Set Input Row, Cardio Lap Input Row) for consistency with US1
  - Render feelings section: rating 1-10 + optional comment
  - Render submit button ("Save Session") + cancel button
  - Use React Hook Form for form state
  - Validate form before submit
- [X] T052 [US2] Create React component in `frontend/src/features/free-session/ExerciseLibrarySearch.tsx`:
  - Render search input for exercise library
  - Call useExerciseLibrary hook with search query (debounced)
  - Display search results as clickable list
  - Allow clicking result to add exercise to form
  - Show "Type to search or enter custom exercise name" placeholder
- [X] T053 [US2] Create custom React hook in `frontend/src/hooks/useExerciseLibrary.ts`:
  - Use TanStack Query `useQuery` to fetch exercises: GET /api/exercises?query=[query]
  - Debounce query changes (300ms)
  - Return `{ data: Exercise[], isLoading, error }`
- [X] T054 [US2] Create custom React hook in `frontend/src/hooks/useLogFreeSession.ts`:
  - Similar to useLogSession but specialized for FREE session type
  - Ensure programSessionId is not sent or is null
  - Return `{ mutate, isLoading, isError, error }`
- [X] T055 [US2] Add reusable form components for consistent UX:
  - Create `frontend/src/components/SetInputRow.tsx` - input row for single set (reps, weight, unit, isBodyWeight toggle, remove button)
  - Create `frontend/src/components/CardioLapInputRow.tsx` - input row for single cardio lap (duration, distance, unit, remove button)
  - Use in both ProgramSessionForm and FreeSessionForm for UX consistency per PUX-002 requirement
  - Ensure styling and interaction patterns match across both components

**Checkpoint: User Stories 1 & 2 Complete** ✓  
Both program and free session logging fully functional. Can be tested independently. MVP scope could end here if time-boxed. Estimated 50% of feature scope.

---

## Phase 5: User Story 3 - Browse Workout History (Priority: P2)

**Goal**: Users can view a reverse-chronological list of all past sessions (program + free), filter by date range or exercise name, and drill into individual session details.

**Independent Test Scenario**:
1. User1 has logged 10 sessions (mix of program and free) over past month
2. GET /api/logged-sessions/history returns page 0, size 20 → verify all 10 in reverse-chronological order
3. GET /api/logged-sessions/history?dateFrom=2026-04-01&dateTo=2026-04-15 → verify only 2 sessions in range appear
4. GET /api/logged-sessions/history?exerciseName=Bench%20Press → verify only sessions containing Bench Press appear
5. GET /api/logged-sessions/{sessionId} → verify full session details (all exercises, sets, feelings, performance data)

### Unit Tests for User Story 3 (MANDATORY)

- [X] T056 [P] [US3] Create `backend/src/test/java/com/gymtracker/application/SessionHistoryServiceTest.java`:
  - Test `getSessionHistory(userId, page, size)` returns sessions in reverse-chronological order
  - Test `getSessionHistory()` returns SessionHistoryPage with totalItems, items array, pagination metadata
  - Test `getSessionHistory(userId, pageIndex, pageSize)` with pageSize limits result (e.g., size=10 returns max 10 items)
  - Test `getSessionHistory()` calculates exerciseCount correctly (counts ExerciseEntry records per LoggedSession)
  - Test `filterByDateRange(userId, dateFrom, dateTo)` returns only sessions within date range (inclusive)
  - Test `filterByExerciseName(userId, exerciseName)` returns only sessions containing exercise (searches ExerciseEntry.exerciseNameSnapshot)
  - Test filter methods combine (dateRange + exerciseName both applied)
- [X] T057 [P] [US3] Create `backend/src/test/java/com/gymtracker/application/SessionDetailServiceTest.java`:
  - Test `getSessionDetails(userId, sessionId)` returns LoggedSessionDetail with all exercise entries, sets/laps, feelings
  - Test `getSessionDetails()` validates user ownership (throws ForbiddenException if wrong user)
  - Test `getSessionDetails()` throws ResourceNotFoundException if session not found

### Integration Tests for User Story 3 (MANDATORY)

- [X] T058 [P] [US3] Create `backend/src/test/java/com/gymtracker/api/SessionHistoryControllerIT.java` (extend if started in US1, or create new):
  - Test GET /api/logged-sessions/history returns 200 + SessionHistoryPage
  - Test GET /api/logged-sessions/history returns sessions in reverse-chronological order (newest first)
  - Test GET /api/logged-sessions/history?page=0&size=5 respects pagination
  - Test GET /api/logged-sessions/history?dateFrom=2026-04-01&dateTo=2026-04-30 filters by date range correctly
  - Test GET /api/logged-sessions/history?exerciseName=Bench%20Press filters by exercise name (partial match OK)
  - Test GET /api/logged-sessions/history combines dateFrom, dateTo, exerciseName filters when all provided
  - Test empty history returns SessionHistoryPage with items=[] and totalItems=0
  - Verify SessionHistoryItem includes: sessionId, sessionDate, sessionType, exerciseCount, totalDurationSeconds optional
- [X] T059 [P] [US3] Create `backend/src/test/java/com/gymtracker/api/SessionDetailControllerIT.java`:
  - Test GET /api/logged-sessions/{sessionId} returns 200 + LoggedSessionDetail with all data
  - Test GET /api/logged-sessions/{sessionId} includes all exercise entries, sets/laps, feelings
  - Test GET /api/logged-sessions/{sessionId} returns 404 if session not found
  - Test GET /api/logged-sessions/{sessionId} returns 403 if requesting user is not session owner
  - Test response schema matches contracts/workout-tracker-api.yaml LoggedSessionDetail

### Frontend Tests for User Story 3 (MANDATORY)

- [X] T060 [P] [US3] Create `frontend/src/features/history/__tests__/SessionHistoryPage.test.tsx`:
  - Test page renders "Workout History" heading
  - Test page fetches sessions on mount
  - Test page displays list of sessions with date, session type badge (PROGRAM/FREE), exercise count
  - Test sessions appear in reverse-chronological order (newest first)
  - Test clicking session opens detail view
  - Test empty state shows "No workouts logged yet" with link to start session
  - Test pagination controls appear if more than pageSize items
  - Test filter section: date range picker (from/to) + exercise name search input
  - Test clicking filter button re-fetches sessions with new filters
  - Test clearing filters resets to all sessions
- [X] T061 [P] [US3] Create `frontend/src/features/history/__tests__/SessionDetailView.test.tsx`:
  - Test detail view renders full session information: date, type, total duration
  - Test detail view renders all exercises with their sets/laps
  - For strength exercise: displays reps, weight, unit, isBodyWeight flag
  - For cardio exercise: displays duration, optional distance
  - Test detail view renders feelings: rating (1-10) displayed as stars or numeric value, comment text
  - Test detail view includes back button to history list
- [X] T062 [P] [US3] Create `frontend/src/hooks/__tests__/useSessionHistory.test.ts`:
  - Test `useSessionHistory(page, dateFrom, dateTo, exerciseName)` hook queries GET /api/logged-sessions/history
  - Test hook builds query params correctly
  - Test hook returns `{ data: SessionHistoryPage, isLoading, error }`
- [X] T063 [P] [US3] Create `frontend/src/hooks/__tests__/useSessionDetail.test.ts`:
  - Test `useSessionDetail(sessionId)` hook queries GET /api/logged-sessions/{sessionId}
  - Test hook returns `{ data: LoggedSessionDetail, isLoading, error }`

### Implementation for User Story 3

- [X] T064 [P] [US3] Create `backend/src/main/java/com/gymtracker/application/SessionHistoryService.java`:
  - Implement `getSessionHistory(UUID userId, int page, int size, LocalDate dateFrom, LocalDate dateTo, String exerciseName): SessionHistoryPageDTO` - queries LoggedSession by userId, applies filters, sorts by sessionDate DESC, paginates, returns DTO with metadata
  - Implement `filterByDateRange(UUID userId, LocalDate from, LocalDate to): Specification` - creates JPA Specification for date filtering
  - Implement `filterByExerciseName(UUID userId, String name): Specification` - creates JPA Specification for exercise name search (text index on ExerciseEntry.exerciseNameSnapshot)
  - Calculate exerciseCount from ExerciseEntry rows per LoggedSession
  - Combine all filter specifications with Specification.and()
  - Log queries and result counts for performance monitoring
- [X] T065 [P] [US3] Create `backend/src/main/java/com/gymtracker/application/SessionDetailService.java`:
  - Implement `getSessionDetails(UUID userId, UUID sessionId): LoggedSessionDetailDTO` - fetches LoggedSession, validates user ownership, loads nested ExerciseEntry + StrengthSet/CardioLap + SessionFeelings, maps to detail DTO
  - Implement `validateSessionOwnership(UUID userId, UUID sessionId): void` - throw ForbiddenException if mismatch
  - Eager load related entities to avoid N+1 queries (use JOIN FETCH or explicit queries)
- [X] T066 [US3] Extend `backend/src/main/java/com/gymtracker/api/SessionController.java`:
  - Add GET /api/logged-sessions/history endpoint - parse query params (page, size, dateFrom, dateTo, exerciseName), call SessionHistoryService, return 200 + SessionHistoryPageDTO
  - Add GET /api/logged-sessions/{sessionId} endpoint - extract userId from auth, call SessionDetailService, return 200 + LoggedSessionDetailDTO or 404/403
  - Add proper error handling and logging
- [X] T067 [US3] Create React page component in `frontend/src/pages/SessionHistoryPage.tsx`:
  - Render "Workout History" heading
  - Render filter section: date range picker (from/to dates) + exercise name search input + filter button
  - Render session list (call useSessionHistory hook with filter params)
  - Display each session as card with date, type badge, exercise count, total duration
  - On session click, navigate to detail view with sessionId
  - Render empty state if no sessions
  - Render pagination controls (Previous/Next buttons) if applicable
  - Include back button to main menu
- [X] T068 [US3] Create React component in `frontend/src/features/history/SessionHistoryList.tsx`:
  - Accept sessions array, isLoading flag, onSessionClick callback
  - Render list of SessionHistoryItem cards
  - Each card displays: date (formatted), session type badge (PROGRAM/FREE), exercise count, total duration
  - Card is clickable and calls onSessionClick(sessionId)
  - Show loading skeleton while loading
- [X] T069 [US3] Create React component in `frontend/src/features/history/SessionDetailView.tsx`:
  - Accept sessionId prop
  - Call useSessionDetail hook to fetch full session data
  - Render session metadata: date, type, total duration, optional name/notes
  - Render exercise list with exercise-specific data:
    - Strength/bodyweight: sets table with columns (Set #, Reps, Weight, Unit, Bodyweight)
    - Cardio: laps table with columns (Lap #, Duration, Distance, Unit)
  - Render feelings section: rating displayed as star rating or numeric value, comment text
  - Include back button to history list
- [X] T070 [US3] Create React component in `frontend/src/features/history/SessionFilterSection.tsx`:
  - Render date range picker (from/to dates, accept locale date format)
  - Render exercise name search input (with autocomplete/suggestions optional)
  - Render filter button to apply filters
  - Render clear button to reset all filters
  - Accept onChange callback to update parent state
- [X] T071 [US3] Create custom React hook in `frontend/src/hooks/useSessionHistory.ts`:
  - Use TanStack Query `useQuery` to fetch GET /api/logged-sessions/history
  - Accept params: page, size, dateFrom, dateTo, exerciseName
  - Build query params correctly (date format, URL encoding)
  - Return `{ data: SessionHistoryPage, isLoading, error, refetch }`
- [X] T072 [US3] Create custom React hook in `frontend/src/hooks/useSessionDetail.ts`:
  - Use TanStack Query `useQuery` to fetch GET /api/logged-sessions/{sessionId}
  - Accept sessionId param
  - Return `{ data: LoggedSessionDetail, isLoading, error }`
- [X] T073 [US3] Create shared utility components:
  - Create `frontend/src/components/DateRangePicker.tsx` - component for selecting date range (from/to)
  - Create `frontend/src/components/SessionTypeBadge.tsx` - component to display PROGRAM/FREE badge with different styling
  - Create `frontend/src/components/ExerciseTable.tsx` - reusable table component for displaying exercise sets/laps
  - Use consistent styling and UX patterns

**Checkpoint: User Story 3 Complete** ✓  
History browsing, filtering, and detail views fully functional. Users can navigate past workouts. All features so far can be tested independently.

---

## Phase 6: User Story 4 - Track Exercise Progression (Priority: P3)

**Goal**: Users select an exercise and view a chart showing performance progression over time (weight increases, duration improvements, etc.) across all sessions (program + free).

**Independent Test Scenario**:
1. User1 has logged 6 sessions with "Deadlift": 100kg, 105kg, 110kg, 110kg (repeat), 115kg, 120kg over 2 months
2. GET /api/progression/Deadlift returns 6 ProgressionPoint objects with sessionDate, metricValue (weight), sessionId
3. Frontend renders chart with 6 data points in chronological order, weight values on Y-axis
4. User can hover/click data point to see session detail

### Unit Tests for User Story 4 (MANDATORY)

- [X] T074 [P] [US4] Create `backend/src/test/java/com/gymtracker/application/ProgressionServiceTest.java`:
  - Test `getExerciseProgression(userId, exerciseName)` returns list of ProgressionPoint sorted chronologically (oldest first)
  - Test each ProgressionPoint includes sessionId, sessionDate, metricType (WEIGHT/DURATION/DISTANCE), metricValue
  - Test `getExerciseProgression()` aggregates data from all sessions (program + free)
  - Test `getExerciseProgression()` returns empty list if exercise never logged
  - Test `getExerciseProgression()` returns single point if exercise only logged once (with message in response?)
  - Test for strength exercise: metricValue = max weight across all sets in session OR average, per design decision
  - Test for cardio exercise: metricValue = total duration OR total distance, per design decision
  - Test case-insensitive exercise name matching

### Integration Tests for User Story 4 (MANDATORY)

- [X] T075 [P] [US4] Create `backend/src/test/java/com/gymtracker/api/ProgressionControllerIT.java`:
  - Test GET /api/progression/{exerciseName} returns 200 + ProgressionResponse
  - Test response includes exerciseName + points array
  - Test points are sorted chronologically (oldest to newest)
  - Test points include metricType and metricValue
  - Test GET /api/progression/Unknown%20Exercise returns 200 with empty points array (or 404, decide)
  - Test response schema matches contracts/workout-tracker-api.yaml ProgressionResponse

### Frontend Tests for User Story 4 (MANDATORY)

- [X] T076 [P] [US4] Create `frontend/src/features/progression/__tests__/ProgressionChart.test.tsx`:
  - Test chart renders with exercise name in title
  - Test chart displays data points in chronological order
  - Test chart renders Y-axis label based on metric type (Weight, Duration, Distance)
  - Test chart shows at least 2 points to visualize trend
  - Test chart shows message if only 1 data point ("Not enough data to show trend")
  - Test hovering data point shows tooltip with date, metricValue, session link
  - Test clicking data point navigates to session detail
- [X] T077 [P] [US4] Create `frontend/src/features/history/__tests__/ExerciseProgressionLink.test.tsx`:
  - Test component renders "View Progression" link/button for exercise name
  - Test clicking link navigates to progression chart view
- [X] T078 [P] [US4] Create `frontend/src/hooks/__tests__/useExerciseProgression.test.ts`:
  - Test `useExerciseProgression(exerciseName)` hook queries GET /api/progression/{exerciseName}
  - Test hook returns `{ data: ProgressionResponse, isLoading, error }`

### Implementation for User Story 4

- [X] T079 [P] [US4] Create `backend/src/main/java/com/gymtracker/application/ProgressionService.java`:
  - Implement `getExerciseProgression(UUID userId, String exerciseName): ProgressionResponseDTO` - queries LoggedSession + ExerciseEntry + StrengthSet/CardioLap where exerciseNameSnapshot matches (case-insensitive), groups by LoggedSession, calculates metric value per session:
    - For strength: max weight per session (or average, decide based on UX preference) — metricType=WEIGHT
    - For cardio: sum of duration per session — metricType=DURATION; or sum of distance if distance entries exist — metricType=DISTANCE
  - Sort results by sessionDate ASC (chronological)
  - Return DTO with exerciseName + points array
  - Handle case where exercise never logged (return empty points or 404)
- [X] T080 [P] [US4] Create `backend/src/main/java/com/gymtracker/infrastructure/query/ProgressionQueryBuilder.java`:
  - Implement native SQL or JPQL query to efficiently fetch progression data without N+1
  - Query joins LoggedSession → ExerciseEntry → StrengthSet/CardioLap, filters by userId + exerciseName, groups by session, calculates aggregates
  - Return List<ProgressionPoint> projection with sessionId, sessionDate, metricType, metricValue
- [X] T081 [US4] Extend `backend/src/main/java/com/gymtracker/api/SessionController.java`:
  - Add GET /api/progression/{exerciseName} endpoint - URL-decode exerciseName, call ProgressionService, return 200 + ProgressionResponseDTO
  - Add error handling for invalid exercise names
- [X] T082 [US4] Create React page component in `frontend/src/pages/ProgressionChartPage.tsx`:
  - Accept exerciseName as URL param (from history detail view or direct navigation)
  - Call useExerciseProgression hook to fetch data
  - Render page with exercise name as heading
  - Render `<ProgressionChart data={progression.points} />` component
  - Include back button to history
- [X] T083 [US4] Create React component in `frontend/src/features/progression/ProgressionChart.tsx`:
  - Accept progressionPoints array prop
  - Use Recharts or Chart.js to render line/area chart
  - X-axis: sessionDate (formatted)
  - Y-axis: metricValue (weight in kg/lbs, duration in minutes, distance in km/miles)
  - Y-axis label based on metricType prop
  - Show loading skeleton while data loading
  - Show message if < 2 points ("Not enough data for trend visualization")
  - Render tooltip on hover: date, metricValue, link to view session
  - Render data point markers
  - Optional: show trend line or average line
- [X] T084 [US4] Create React component in `frontend/src/features/history/ExerciseProgressionLink.tsx`:
  - Accept exerciseName prop, onViewProgression callback
  - Render "View Progression" button/link
  - On click, navigate to progression chart page with exerciseName param
- [X] T085 [US4] Extend `frontend/src/features/history/SessionDetailView.tsx`:
  - For each exercise in detail view, add "View Progression" link/button
  - Clicking link navigates to ProgressionChartPage with exerciseName param
- [X] T086 [US4] Create custom React hook in `frontend/src/hooks/useExerciseProgression.ts`:
  - Use TanStack Query `useQuery` to fetch GET /api/progression/{exerciseName}
  - URL-encode exerciseName properly
  - Return `{ data: ProgressionResponse, isLoading, error }`

**Checkpoint: User Story 4 Complete** ✓  
Full feature scope now includes logging (program + free) + history + progression. Core functionality feature-complete. All 4 user stories independently functional and testable.

---

## Phase 7: AI Handoff Integration (Priority: P3+ Cross-Cutting)

**Goal**: Logged session data (especially program sessions) is made available asynchronously to the AI Coach pipeline via LangChain4j + Azure OpenAI integration for program adjustment. This phase adds async enqueue logic without blocking session saves.

### Unit Tests for AI Handoff (MANDATORY)

- [X] T087 [P] Create `backend/src/test/java/com/gymtracker/infrastructure/ai/AiHandoffServiceTest.java`:
  - Test `enqueueSessionForAiAnalysis(UUID userId, LoggedSession session)` queues session summary to async handler
  - Test async queue returns immediately (non-blocking, p95 <= 500ms)
  - Test session summary includes: exerciseEntries with actual vs. target comparison (if program session), sessionFeelings, user preferences
  - Test queue handles errors gracefully (logs error, does not throw to caller)
  - Test only program sessions are enqueued (free sessions optionally enqueued or skipped)

### Integration Tests for AI Handoff (MANDATORY)

- [X] T088 [P] Create `backend/src/test/java/com/gymtracker/infrastructure/ai/AzureOpenAiIntegrationIT.java`:
  - Test Azure OpenAI client initialization with AZURE_OPENAI_* environment variables
  - Test LangChain4j workflow accepts session summary DTO
  - Test workflow calls Azure OpenAI with session data in prompt
  - Test response is captured (even if analysis is not used in MVP)
  - Test async execution does not timeout (configure reasonable timeout, e.g., 30s)
  - Mock or use Azure SDK test containers if needed

### Implementation for AI Handoff

- [X] T089 Create `backend/src/main/java/com/gymtracker/infrastructure/ai/AiHandoffService.java`:
  - Implement `enqueueSessionForAiAnalysis(UUID userId, LoggedSession session): void` - creates SessionSummaryDTO, submits to async queue (Spring TaskExecutor or dedicated queue), returns immediately
  - Implement `buildSessionSummary(LoggedSession session): SessionSummaryDTO` - extracts exercise entries, actual performance, feelings, formats for AI consumption
  - For program sessions: include target vs. actual comparison
  - For free sessions: include exercise list + performance
  - Catch and log errors to prevent throwing exceptions to request thread
  - Ensure queue is non-blocking (use @Async or ExecutorService with async submission)
- [X] T090 Create `backend/src/main/java/com/gymtracker/infrastructure/ai/LangChainSessionProcessor.java`:
  - Implement LangChain4j integration: accept SessionSummaryDTO, construct prompt with session data
  - Use Azure LangChain Spring Boot starter 1.13.1 to configure LangChain4j + Azure OpenAI
  - Implement workflow: prompt construction → Azure OpenAI call → log response
  - Handle transient failures (retry logic optional but recommended)
  - Ensure execution completes within timeout (30s recommended)
- [X] T091 Configure Spring Boot dependency for Azure LangChain in `backend/pom.xml`:
  - Add dependency: `<artifactId>azure-open-ai-langchain4j-spring-boot-starter</artifactId>` version `1.13.1`
  - Ensure compatible Spring Boot 4.0.5 version
  - Add Azure OpenAI SDK dependencies if not transitively included
- [X] T092 Configure Azure OpenAI properties in `backend/src/main/resources/application.properties`:
  - Add `azure.openai.endpoint=${AZURE_OPENAI_ENDPOINT:http://localhost:8080}`
  - Add `azure.openai.api-key=${AZURE_OPENAI_API_KEY:fake-key}`
  - Add `azure.openai.deployment=${AZURE_OPENAI_DEPLOYMENT:gpt-35-turbo}`
  - Document expected environment variables in LOCAL_DEV.md + deployment guide
- [X] T093 Extend `backend/src/main/java/com/gymtracker/application/LoggedSessionService.java`:
  - After successfully saving LoggedSession (in saveLoggedSession method), call `aiHandoffService.enqueueSessionForAiAnalysis(userId, savedSession)` asynchronously
  - Ensure save operation completes and returns to client before async queue processes
  - Log enqueueing event
- [X] T094 Create dto in `backend/src/main/java/com/gymtracker/infrastructure/ai/SessionSummaryDTO.java`:
  - Fields: userId, sessionId, sessionType, sessionDate, exercises (with actual/target for program), totalDuration, feelings (rating + comment), metadata (user preferences)
  - Implement serialization for LangChain prompt construction
- [X] T095 Configure Spring async executor in `backend/src/main/java/com/gymtracker/infrastructure/config/AsyncConfig.java`:
  - Define `@Bean TaskExecutor aiTaskExecutor()` with thread pool (corePoolSize=2, maxPoolSize=5, queueCapacity=100)
  - Configure exception handling (log failures without throwing)
  - Apply @Async("aiTaskExecutor") to async handoff methods

**Checkpoint: AI Handoff Integrated** ✓  
Session data now enqueued for AI analysis. Non-blocking, < 500ms p95. Async pipeline ready for future program adjustment features.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Final testing, performance validation, documentation, and feature refinements

### Performance & Load Testing

- [X] T096 [P] Create `backend/src/test/java/com/gymtracker/performance/PerformanceTest.java`:
  - Test session save latency (p95 <= 1.5s) with realistic payload: 3 exercises, 3 sets each, feelings
  - Test history list query (p95 <= 2s) with 2 years of data (~500 sessions)
  - Test progression chart query (p95 <= 2s) with 50+ data points
  - Use JMH or SimpleTimer for benchmarking
  - Seed realistic data (SQL script or test fixtures)
  - Document results and highlight any p95 violations
- [X] T097 [P] Create performance test script in `backend/docs/performance-validation.md`:
  - Document how to run performance tests locally
  - Include expected p95 targets from plan.md
  - Include sample data seed instructions
  - Track performance over time (add to CI/CD if possible)

### End-to-End Integration Test

- [X] T098 Create `backend/src/test/java/com/gymtracker/e2e/WorkoutTrackerE2ETest.java`:
  - Complete user journey: login → get program session → save program session → view history → view progression → save free session → verify all data consistent
  - Use Testcontainers (PostgreSQL)
  - Seed test data via repository
  - Assert at each step
  - Verify no data leakage between authenticated users

### Frontend End-to-End Test (Optional but recommended)

- [X] T099 Create `frontend/tests/e2e.test.ts` (or Playwright/Cypress config):
  - Test full user flow: navigate to app → login (HTTP Basic Auth via header or form) → open program session → fill form → save → view history → click session detail → view progression
  - Mock API responses or use test backend
  - Verify UI renders correctly at each step
  - Verify form validation works
  - Verify navigation works

### Security & User Isolation Validation

- [X] T100 Create `backend/src/test/java/com/gymtracker/security/UserIsolationE2ETest.java`:
  - Test user1 cannot access user2's sessions via direct ID (403)
  - Test user1 cannot filter history by user2's exercise patterns
  - Test API rejects requests without authentication (401)
  - Test API rejects requests with wrong user in programSessionId
  - Verify all data access paths validated with authenticated userId

### Documentation & Cleanup

- [X] T101 [P] Update `backend/LOCAL_DEV.md`:
   - Document PostgreSQL setup (Docker or local)
   - Document seed users for HTTP Basic Auth
   - Document running backend locally (mvn spring-boot:run or gradle bootRun)
   - Document running tests (mvn test, gradle test)
   - Document running performance tests
   - Add troubleshooting section
- [X] T102 [P] Update `frontend/LOCAL_DEV.md`:
   - Document npm install + npm run dev
   - Document test running (npm run test)
   - Document API base URL configuration
   - Add troubleshooting section
- [X] T103 [P] Create `docs/API_USAGE.md`:
   - Copy contract from contracts/workout-tracker-api.yaml
   - Add examples for each endpoint (curl, JavaScript fetch, etc.)
   - Document authentication method (HTTP Basic Auth)
   - Document error responses
- [X] T104 [P] Create `docs/DATABASE_SCHEMA.md`:
   - Document entity relationships
   - Document indexes
   - Include entity-relationship diagram (text format or link)
   - Document query performance considerations
- [X] T105 [P] Create `docs/FEATURE_WALKTHROUGH.md`:
   - Step-by-step guide for using all 4 user stories
   - Screenshots or descriptions of each screen
   - Example data + expected outcomes
   - Troubleshooting tips
- [X] T106 Create `DEPLOYMENT.md` at repository root:
   - Document Docker build for backend (Dockerfile)
   - Document environment variables required (Azure credentials, DB connection, etc.)
   - Document database migration strategy (Flyway/Liquibase)
   - Document scaling considerations

### Code Quality & Refactoring

- [X] T107 [P] Review and refactor code for consistency:
  - Ensure all service methods have consistent error handling
  - Ensure all DTOs have consistent naming (Create, Detail, List, etc.)
  - Ensure all endpoints follow REST conventions (POST for create, GET for read, etc.)
  - Run linting checks (checkstyle for backend, ESLint for frontend)
  - Fix any style violations
- [X] T108 [P] Add missing JSDoc/TypeDoc comments:
  - Document all public service methods with @param, @return, @throws
  - Document all React components with PropTypes or TypeScript interfaces
  - Document all endpoints with descriptions of parameters and responses
- [X] T109 [P] Verify test coverage:
  - Check that all business rules from spec.md test coverage matrix are covered
  - Run code coverage tool (JaCoCo for backend, Istanbul for frontend)
  - Aim for >= 80% coverage on domain + application layers
  - Document coverage report location
- [ ] T110 [P] Set up CI/CD pipeline (GitHub Actions example):
  - Create `.github/workflows/test.yml` to run backend tests + frontend tests on PR
  - Run linting checks
  - Run performance tests (optional)
  - Run code coverage checks
  - Publish coverage reports

### Quickstart Validation

- [ ] T111 Execute `specs/001-workout-tracker/quickstart.md` end-to-end:
  - Follow PostgreSQL setup instructions
  - Follow backend bootstrap instructions
  - Follow frontend bootstrap instructions
  - Follow local development flow: program session → history → progression
  - Verify all steps work as documented
  - Update quickstart.md if any steps fail or are unclear
  - Document any prerequisites not listed

### Feature Sign-Off Checklist

- [ ] T112 Verify all user story acceptance scenarios pass:
  - User Story 1 (Program Session): All 6 acceptance scenarios pass manually or via test
  - User Story 2 (Free Session): All 5 acceptance scenarios pass
  - User Story 3 (History): All 5 acceptance scenarios pass
  - User Story 4 (Progression): All 4 acceptance scenarios pass
- [ ] T113 Verify performance targets met:
  - Session save p95 <= 1.5s
  - History/detail/progression reads p95 <= 2s
  - AI handoff enqueue p95 <= 500ms
  - Document results in PERFORMANCE.md
- [ ] T114 Verify security & user isolation:
  - User1 cannot access user2 data (403 on all unauthorized attempts)
  - Unauthenticated requests rejected (401)
  - Cross-user attacks tested and mitigated
- [ ] T115 Verify UI/UX consistency:
  - Program + free session forms use identical add/edit/remove patterns (PUX-002)
  - All buttons, inputs, messages use consistent styling
  - Empty states clear and actionable
  - Error messages helpful and specific

**Final Checkpoint: Feature Complete & Production-Ready** ✓

---

## Dependencies & Execution Order

### Critical Path (Blocking Dependencies)

```
Phase 1 (Setup)
    ↓
Phase 2 (Foundational: Domain + Security + Logging)
    ↓
    ├─→ Phase 3 (User Story 1: Program Session) ←─┐
    ├─→ Phase 4 (User Story 2: Free Session) ←────┤ Can run in parallel
    ├─→ Phase 5 (User Story 3: History) ←─────────┤ (after Foundational)
    └─→ Phase 6 (User Story 4: Progression) ←────┘
    ↓
Phase 7 (AI Handoff: Async Integration)
    ↓
Phase 8 (Polish & Cross-Cutting)
```

### Within Each Phase

- **Phase 1**: Linear (T001 → T002 → T003 → T004 [P] → T005 [P])
  - T004 and T005 can run in parallel
- **Phase 2**: All T006-T019 can run in parallel except:
  - T006 (schema) should complete first
  - T009 depends on T007-T008 (entities)
  - Recommended order: schema → entities → repositories → security → validation → DTOs
- **Phase 3/4/5/6**: Can start in parallel after Foundational (Phase 2) completes
  - Each user story has independent test → implementation pipeline
  - User stories can be delivered sequentially or in parallel (if team capacity)
- **Phase 7**: Depends on Phase 3/4 completion (needs LoggedSessionService)
- **Phase 8**: Depends on Phase 3/4/5/6 completion

### Parallelization Opportunities

**Setup Phase (T001-T005)**:
- T004, T005 can run in parallel (both setup)

**Foundational Phase (T006-T019)**:
- Parallel groups:
  - T006 (schema) - should complete first
  - T007, T008 (entities) - can run in parallel
  - T009 (repositories) - after T007/T008
  - T011, T012, T013 (security) - independent
  - T014, T015, T016 (logging/validation) - independent
  - T017, T018, T019 (DTOs) - can run in parallel

**User Story Phases (T020-T115)**:
- All "Tests" tasks for a story marked [P] can run in parallel
- All "Implementation" domain models marked [P] can run in parallel
- Different user stories (US1, US2, US3, US4) can be worked on by different developers in parallel after Foundational
- Suggested team allocation:
  - Developer A: Phase 3 (US1 Program Session) + US4 Progression
  - Developer B: Phase 4 (US2 Free Session) + History integration
  - Developer C: Phase 5 (US3 History) + Polish/testing
  - Tech Lead: Phase 7 (AI Handoff) + Phase 8 validation

### MVP Scope (Time-Boxed Release)

If constrained to 2-3 weeks:

1. **Week 1-2**: Complete Phase 1, Phase 2, Phase 3 (Program Session), Phase 4 (Free Session)
   - MVP = "Users can log both program and free sessions, data persists"
   - Ready to deploy to staging for user feedback
2. **Week 3**: Complete Phase 5 (History)
   - Add history browsing
   - Handle feedback from Phase 1+2 deployment
   - Deploy v0.1.1
3. **Later**: Phase 6 (Progression), Phase 7 (AI), Phase 8 (Polish)

---

## Implementation Strategy

### MVP First (Recommended for User Validation)

1. ✅ Phase 1: Setup (2-3 hours)
2. ✅ Phase 2: Foundational (6-8 hours) — **CRITICAL GATE**
3. ✅ Phase 3: User Story 1 — Program Session (6-8 hours)
4. ✅ Phase 4: User Story 2 — Free Session (4-6 hours)
5. 🛑 **STOP AND VALIDATE**: Deploy MVP (Phase 1+2+3+4) to staging
   - Test with real users or stakeholders
   - Gather feedback on UX, data model, performance
   - Fix critical bugs before continuing
6. Phase 5: User Story 3 — History (6-8 hours)
7. Phase 6: User Story 4 — Progression (4-6 hours)
8. Phase 7: AI Handoff (4-6 hours)
9. Phase 8: Polish & Validation (4-6 hours)

**Estimated Total**: 42-50 hours for complete feature (1 developer, 1-2 weeks)

### Incremental Delivery

- **Delivery 1** (Week 1-2): Phase 1 + 2 + 3 + 4 = MVP ready
- **Delivery 2** (Week 3): Phase 5 = Full user data visibility
- **Delivery 3** (Week 4): Phase 6 = Progression insights
- **Delivery 4** (Week 5): Phase 7 + 8 = AI integration complete

Each delivery is independently deployable and testable.

### Parallel Team Strategy (3+ Developers)

With 3 developers:

1. **Sprint 0** (Day 1): Everyone completes Phase 1 + Phase 2 together (foundational lockdown)
2. **Sprint 1** (Days 2-4):
   - Dev A: Phase 3 (Program Session) — backend + frontend
   - Dev B: Phase 4 (Free Session) — backend + frontend
   - Dev C: Phase 5 (History) — backend + frontend (some overlap with A/B for shared endpoints)
3. **Sprint 2** (Days 5-6):
   - Dev A: Phase 6 (Progression) — backend + frontend
   - Dev B: Phase 7 (AI Handoff) — backend async integration
   - Dev C: Phase 8 (Polish, Testing, Docs)
4. **Sprint 3** (Day 7): Merge + validate all features + ship

Each developer can work independently on their assigned stories after Phase 2 completes.

---

## Notes

- **[P] tasks** = Different files, no blocking dependencies; can run in parallel
- **[Story] labels** = US1, US2, US3, US4 for traceability to user stories
- **Test-First Approach**: For each user story, tests MUST be written first and FAIL before implementation begins
- **Thin-Slice Principle**: No unnecessary abstractions; explicit, simple code
- **User Isolation**: All data access must validate authenticated user ownership — no exceptions
- **Performance Targets** (from plan.md):
  - Session save: p95 <= 1.5s
  - History/detail/progression reads: p95 <= 2s
  - AI handoff enqueue: p95 <= 500ms
- **Stop at Checkpoints**: After each user story phase, verify independently before moving to next
- **Commit Frequently**: Aim for 1 commit per task or logical grouping
- **Avoid Cross-Story Dependencies**: Each user story should be independently completable; if integration needed, flag it explicitly
- **Documentation**: Keep LOCAL_DEV.md + API docs + schema docs up-to-date as tasks progress
- **Tech Stack Locked**: Java 21 + Spring Boot 4.0.5, React 18, PostgreSQL 16, Azure LangChain 1.13.1, HTTP Basic Auth (MVP)

---

## Summary Statistics

- **Total Tasks**: 115
- **Phase 1 (Setup)**: 5 tasks
- **Phase 2 (Foundational)**: 14 tasks
- **Phase 3 (US1 - Program Session)**: 19 tasks (tests + implementation)
- **Phase 4 (US2 - Free Session)**: 17 tasks (tests + implementation)
- **Phase 5 (US3 - History)**: 18 tasks (tests + implementation)
- **Phase 6 (US4 - Progression)**: 16 tasks (tests + implementation)
- **Phase 7 (AI Handoff)**: 7 tasks
- **Phase 8 (Polish)**: 19 tasks

**Parallelizable Tasks (marked [P])**: ~60 tasks  
**Sequential Critical Path Tasks**: ~55 tasks  
**Est. Total Effort**: 40-50 hours (1 developer), 2-3 weeks  
**Est. Parallel Delivery** (3 devs): 1-2 weeks + Phase 2 synchronization

---


