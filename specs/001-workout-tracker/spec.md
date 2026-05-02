# Feature Specification: Workout Tracker

**Feature Branch**: `001-workout-tracker`  
**Created**: 2026-04-26  
**Status**: Draft  
**Input**: User description: "Users can log and monitor their daily workouts with structured tracking. This includes storing exercises, sets, reps, weights, duration, and other training parameters. The system maintains a history of all workouts so users can see patterns and progression over time."

**Language Rule**: This specification MUST be written in English.

## Clarifications

### Session 2026-04-26

- Q: Who creates workout programs — users, the system, or both? → A: The AI Coach generates personalized programs based on user requirements. For MVP, users follow the AI-provided program as-is — no adding, removing, or modifying exercises. Users log performance data and subjective feelings per session; the AI Coach uses this to adjust the program over time. Users may also log sessions freely without a program (free session mode).
- Q: How are program sessions structured and scheduled? → A: Sequential numbered sessions (Session 1, Session 2, Session 3...). No calendar binding — the user does the next uncompleted session whenever they choose to train. The AI Coach controls pacing and structure via program adjustment.
- Q: What format does post-session subjective feedback take? → A: A single overall session rating (1–10) plus an optional free-text comment. Both are stored with the session and made available to the AI Coach.
- Q: Should AI target sets/reps/weights be visible to the user while logging a program session? → A: Yes — AI targets are displayed alongside the actual input fields (e.g., "Target: 3 × 8 @ 70 kg — Actual: ___") so users can see what the AI expects while recording what they did.
- Q: What happens when the user completes the last session in their program? → A: The program is marked complete. No automatic action is taken — the user must manually request a new program from the AI Coach. Until a new program is assigned, only free session mode is available.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Log a Program Session (Priority: P1)

A user has an AI-generated workout program. When they are ready to train, they open the app and the system presents the next uncompleted session in their program sequence (e.g., "Session 3 — Pull Day") with exercises already listed by the AI Coach. They fill in the actual performance data for each exercise (sets, reps, weight lifted or duration), then record their subjective feelings about the session (e.g., energy level, perceived effort, soreness). When done, they save the session. The AI Coach later uses this data to adjust future sessions.

**Why this priority**: This is the primary data collection loop for the entire application. Without this, neither the Training Tracker nor the AI Coach can function.

**Independent Test**: Can be fully tested by opening a pre-assigned program session with 3 exercises, filling in data for each, recording feelings, saving, and verifying the session appears in history with all data intact.

**Acceptance Scenarios**:

1. **Given** a logged-in user with an AI-assigned program, **When** they open their next program session, **Then** they see the list of exercises predefined by the AI Coach, each showing the AI target (sets × reps × weight) alongside empty actual-performance fields.
2. **Given** a user filling in a program session, **When** they enter sets, reps, and weight for each exercise, **Then** each entry is stored and associated with the correct exercise in the session.
3. **Given** a user who completed a program session, **When** they record their feelings (e.g., rating + optional text), **Then** the subjective feedback is saved alongside the performance data.
4. **Given** a user on a program session screen, **When** they try to add or remove an exercise, **Then** the system does not allow it (program exercises are fixed in MVP).
5. **Given** a user who finished filling all exercises, **When** they save the session, **Then** the session is stored in history and the AI Coach has access to the data for program adjustment.
6. **Given** a user logging a body weight exercise in a program, **When** they fill in reps without a weight value, **Then** the system accepts it as a body weight set.

---

### User Story 2 - Log a Free Session (Priority: P1)

A user wants to log a workout that is not part of their AI program — for example, a spontaneous training day or a different activity. They start a free session, add exercises from the exercise library or enter custom ones, record sets/reps/weights, and save.

**Why this priority**: Free session mode preserves training data even when the user deviates from their program, and serves as the primary logging mode for users who do not yet have an AI program assigned.

**Independent Test**: Can be fully tested by starting a free session, adding 3 custom exercises, filling data, saving, and verifying the session appears in history marked as a free session.

**Acceptance Scenarios**:

1. **Given** a logged-in user starting a free session, **When** they search for "Bench Press" in the exercise library and add it, **Then** it appears in their session ready for set data entry.
2. **Given** a user in a free session, **When** they type a custom exercise name not in the library (e.g., "Tire Flip"), **Then** it is accepted and saved as a custom exercise.
3. **Given** a user logging a cardio activity in a free session, **When** they enter duration and distance instead of sets/reps, **Then** the system stores it as a cardio entry.
4. **Given** a user mid-logging a free session, **When** they remove an exercise they added by mistake, **Then** the exercise and all its sets are removed.
5. **Given** a user who completed a free session, **When** they save it, **Then** it appears in workout history labeled as a free session (not linked to any program).

---

### User Story 3 - Browse Workout History (Priority: P2)

A user wants to see what workouts they have done in the past. They can view a chronological list of all past sessions (both program sessions and free sessions), see date, session type, and exercise summary, and open any session for full details.

**Why this priority**: Browsing history closes the loop on logged data and enables users and the AI Coach to see patterns over time.

**Independent Test**: Can be tested by logging 5 sessions of mixed types, verifying they appear in reverse-chronological order with correct labels and that opening each shows full details.

**Acceptance Scenarios**:

1. **Given** a user who has logged 10 sessions, **When** they open workout history, **Then** sessions appear in reverse-chronological order showing date, session type (program or free), and exercise count.
2. **Given** a user viewing history, **When** they tap a program session, **Then** they see all exercises, performance data, and feelings recorded for that session.
3. **Given** a user with no sessions logged, **When** they open the history view, **Then** a clear empty state with a call-to-action to start a session is displayed.
4. **Given** a user who logged sessions over 2 years, **When** they filter by date range (e.g., last 30 days), **Then** only sessions in that range are shown.
5. **Given** a user searching for "Squats", **When** they apply the search, **Then** only sessions containing that exercise are shown.

---

### User Story 4 - Track Exercise Progression (Priority: P3)

A user wants to see how a specific exercise has improved over time across all sessions (program and free). They select an exercise and view a progression chart showing performance over time.

**Why this priority**: Progression tracking transforms raw data into motivating insight. It depends on P1 and P2 being in place.

**Independent Test**: Can be tested by logging 6 sessions with "Deadlift" at increasing weights and verifying the chart shows all data points chronologically.

**Acceptance Scenarios**:

1. **Given** a user who logged the same exercise in at least 2 sessions, **When** they view progression for that exercise, **Then** they see a chart with performance data points over time in chronological order.
2. **Given** a user viewing the "Bench Press" chart, **When** they tap a data point, **Then** they see the full set/rep/weight breakdown for that session.
3. **Given** a user who has only 1 session for an exercise, **When** they view its progression, **Then** the system shows the single data point with a message that more sessions are needed to show a trend.
4. **Given** a user viewing progression for a cardio exercise, **When** they check the chart, **Then** distance or duration is shown as the primary metric.

---

### Edge Cases & Simplicity Checks

- What happens when a user logs two sessions on the same day? Both are stored independently — date uniqueness is not enforced.
- What if the user has no AI program assigned yet? They can only use free session mode; the program-based option shows an empty state prompting them to get a program from the AI Coach.
- What if the user has not completed the previous program session and tries to start a new one? The system always presents the next uncompleted session in sequence — users cannot skip ahead.
- What happens when the user logs the final session in their program? The program status is set to complete. The program session mode shows a "Program complete" state with a prompt to request a new program from the AI Coach. Only free session mode is available until a new program is assigned.
- How does the system handle an exercise with 0 sets? At least 1 set is required before saving.
- What if a user switches between metric (kg) and imperial (lbs)? The preferred unit is a user-level setting; previously logged data retains the unit used at entry.
- What is the maximum number of exercises per session or sets per exercise? No hard limit — governed by usability.
- What if a user searches for an exercise not in their history? An empty result with a "no results" message is shown.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Users MUST be able to start a new session in one of two modes: (a) program session — based on an AI-provided workout program, or (b) free session — where the user adds exercises freely.
- **FR-002**: In program session mode, the system MUST automatically present the next uncompleted session in the user's program sequence, displaying each AI Coach-defined exercise with its target sets, reps, and weight alongside empty actual-performance fields. Users cannot skip sessions — they always log the next in sequence.
- **FR-002a**: When the user completes the final session in their program, the system MUST mark the program as complete and display a prompt directing the user to request a new program from the AI Coach.
- **FR-003**: In program session mode, users MUST NOT be able to add, remove, or reorder the AI-provided exercises (MVP constraint).
- **FR-004**: In free session mode, users MUST be able to add exercises by selecting from the exercise library or entering a custom exercise name.
- **FR-005**: For each strength or bodyweight exercise, users MUST be able to record one or more sets, each containing: rep count, weight (optional for bodyweight), and weight unit (kg or lbs).
- **FR-006**: Body weight exercises MUST be supported — weight field MUST be optional; users can mark a set as body weight.
- **FR-007**: For each cardio exercise, users MUST be able to record duration and optionally distance.
- **FR-008**: Users MUST be able to edit or remove individual sets within a session before saving (both modes).
- **FR-009**: After completing exercises, users MUST be able to record a post-session rating (integer 1–10) and an optional free-text comment describing how the session felt.
- **FR-010**: System MUST persist each saved session (program or free) to the user's personal workout history.
- **FR-011**: System MUST make logged performance data and session feelings available to the AI Coach for program adjustment.
- **FR-012**: Users MUST be able to add optional notes to any session.
- **FR-013**: Users MUST be able to view a paginated, reverse-chronological list of their past sessions.
- **FR-014**: Each session in the history list MUST display: date, session type (program/free), exercise count, and total duration if recorded.
- **FR-015**: Users MUST be able to open any past session to view its full details, including performance data and feelings.
- **FR-016**: Users MUST be able to filter workout history by date range.
- **FR-017**: Users MUST be able to search workout history by exercise name.
- **FR-018**: Users MUST be able to view a progression chart for any exercise logged more than once, showing performance over time across all session types.
- **FR-019**: System MUST associate all session data with the authenticated user and MUST NOT expose one user's data to another.

### Technology Constraints *(mandatory)*

- **TC-001**: Backend services MUST use Java 21.
- **TC-002**: Backend services MUST use Spring Boot conventions and best practices.
- **TC-003**: Web frontend MUST use React.
- **TC-004**: Design MUST prefer simple, explicit logic over clever abstractions.

### Test Coverage Requirements *(mandatory)*

- Map each business rule to at least one unit test and one integration test.

| Business Rule | Unit Test Reference | Integration Test Reference |
|---|---|---|
| Program session displays AI-predefined exercises | ProgramSessionServiceTest#loadProgramExercises | SessionApiIT#getProgramSession |
| Program exercises cannot be modified in MVP | ProgramSessionValidatorTest#rejectExerciseChange | SessionApiIT#modifyProgramExerciseForbidden |
| Free session accepts custom exercise names | ExerciseServiceTest#customExercise | SessionApiIT#freeSessionCustomExercise |
| Session saved with all exercise/set data | SessionServiceTest#saveSession | SessionApiIT#postSessionReturns201 |
| Feelings saved with session | FeelingServiceTest#saveFeelings | SessionApiIT#feelingsStoredWithSession |
| Body weight sets accepted without weight value | SetValidatorTest#bodyWeightSet | SessionApiIT#bodyWeightExercise |
| Cardio entries use duration/distance, not sets | ExerciseTypeTest#cardioEntry | SessionApiIT#cardioExercise |
| History returned in reverse-chronological order | SessionQueryTest#sortOrder | SessionApiIT#historyOrder |
| Date range filter applied correctly | SessionQueryTest#dateRangeFilter | SessionApiIT#filterByDateRange |
| Progression data sorted chronologically | ProgressionServiceTest#chartOrder | ProgressionApiIT#progressionChart |
| User data isolation | UserIsolationTest#cannotAccessOtherUserData | SessionApiIT#unauthorizedAccess |

### Key Entities *(include if feature involves data)*

- **WorkoutProgram**: An AI-generated training program for a specific user. Key attributes: unique identifier, user reference, program name, list of program sessions in sequence, creation timestamp, status (active / completed). A user has at most one active program at a time.
- **ProgramSession**: A single planned training session within a workout program, ordered by sequence number. Key attributes: unique identifier, program reference, sequence number (1-based), session name (e.g., "Session 1 — Upper Body"), list of predefined exercises each with AI-defined target sets, target reps, and target weight. Sessions have no calendar date — the user logs them in order at their own pace. Targets are displayed to the user while logging actual performance.
- **LoggedSession**: A user's recorded training event, linked either to a ProgramSession or marked as a free session. Key attributes: unique identifier, user reference, optional program session reference, session type (program / free), date, optional name, optional notes, feelings reference, list of exercise entries, total duration, creation timestamp.
- **SessionFeelings**: Subjective post-session feedback. Key attributes: session reference, overall rating (integer 1–10), optional free-text comment.
- **ExerciseEntry**: A specific exercise performed within a logged session. Key attributes: exercise reference or custom name, exercise type (strength / cardio / bodyweight), ordered list of sets or cardio laps.
- **Set**: A single set within a strength or bodyweight exercise entry. Key attributes: rep count, weight value (optional), weight unit (kg or lbs), is-body-weight flag, optional duration, optional rest time.
- **CardioLap**: A single effort within a cardio exercise entry. Key attributes: duration, distance (optional), distance unit (km or miles).
- **Exercise**: A reusable exercise definition in the shared library. Key attributes: name, category (e.g., chest, back, legs, cardio), type (strength / cardio / bodyweight), description.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can complete logging a full program session (5 exercises, 3 sets each, plus feelings) in under 5 minutes from opening the session screen to saving.
- **SC-002**: 90% of users successfully save their first session (program or free) without assistance on their first attempt.
- **SC-003**: Users can locate and open any specific past session within 30 seconds using search or filter.
- **SC-004**: Progression chart for any exercise loads and displays within 2 seconds regardless of history length.
- **SC-005**: The exercise library covers at least 100 common exercises at launch, reducing the need for custom entries in 80% of free sessions.

### UX Consistency Outcomes *(mandatory when UI changes exist)*

- **UX-001**: Program session and free session logging screens share the same interaction patterns for exercise data entry (add set, edit set, remove set) to minimize cognitive switching.

### Additional Non-Functional Outcomes *(include when materially relevant)*

- **NF-001**: Session history list and session detail screens load within 2 seconds for users with up to 2 years of training data.

## Assumptions

- Each user has a single personal account; all session data belongs to the authenticated user.
- The application is a web application accessible via browser (React frontend); native mobile apps are out of scope for v1.
- The AI Coach component is responsible for generating and adjusting workout programs; this feature only handles reading program structure and writing logged session data back for AI consumption.
- A pre-populated exercise library of common exercises is provided at launch; custom exercise creation is supported in free session mode.
- The preferred weight unit (kg or lbs) is a user-level setting; individual sets retain the unit used at time of entry.
- Social and sharing features are out of scope for this story.
- Offline support is out of scope for v1.
- Data export/import is out of scope for this story.
- The existing authentication system handles user login and session management; this feature relies on an authenticated user context being available.
