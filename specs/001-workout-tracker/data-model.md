# Data Model: Workout Tracker

## Overview
The model supports two logging modes (program and free), shared history browsing, and exercise progression analytics while enforcing strict user isolation.

## Entities

### User (external/auth-owned)
- Fields:
  - `id` (UUID, required)
  - `preferredWeightUnit` (enum: `KG`, `LBS`, required)
- Notes:
  - For MVP, identity comes from Spring Security HTTP Basic Auth with predefined in-memory users configured in properties; referenced by all feature entities.

### WorkoutProgram
- Fields:
  - `id` (UUID, required)
  - `userId` (UUID, required, indexed)
  - `name` (string, 1..120, required)
  - `status` (enum: `ACTIVE`, `COMPLETED`, required)
  - `createdAt` (timestamp, required)
  - `completedAt` (timestamp, optional)
- Relationships:
  - One `WorkoutProgram` has many `ProgramSession`.
- Validation rules:
  - A user can have at most one `ACTIVE` program.

### ProgramSession
- Fields:
  - `id` (UUID, required)
  - `programId` (UUID, required, indexed)
  - `sequenceNumber` (int, required, min 1)
  - `name` (string, 1..120, required)
  - `isCompleted` (boolean, required, default false)
- Relationships:
  - One `ProgramSession` belongs to one `WorkoutProgram`.
  - One `ProgramSession` has many `ProgramExerciseTarget`.
- Validation rules:
  - `(programId, sequenceNumber)` unique.
  - User can only access sessions through own program.

### ProgramExerciseTarget
- Fields:
  - `id` (UUID, required)
  - `programSessionId` (UUID, required, indexed)
  - `exerciseName` (string, 1..120, required)
  - `exerciseType` (enum: `STRENGTH`, `BODYWEIGHT`, `CARDIO`, required)
  - `targetSets` (int, optional, min 1)
  - `targetReps` (int, optional, min 1)
  - `targetWeight` (decimal, optional, >0)
  - `targetWeightUnit` (enum: `KG`, `LBS`, optional)
  - `targetDurationSeconds` (int, optional, min 1)
  - `targetDistance` (decimal, optional, >0)
  - `targetDistanceUnit` (enum: `KM`, `MI`, optional)
  - `sortOrder` (int, required, min 0)
- Validation rules:
  - Strength/bodyweight targets require set/rep semantics.
  - Cardio targets require duration and/or distance semantics.

### LoggedSession
- Fields:
  - `id` (UUID, required)
  - `userId` (UUID, required, indexed)
  - `sessionType` (enum: `PROGRAM`, `FREE`, required)
  - `programSessionId` (UUID, optional, indexed)
  - `sessionDate` (date, required)
  - `name` (string, 0..120, optional)
  - `notes` (string, 0..2000, optional)
  - `totalDurationSeconds` (int, optional, min 1)
  - `createdAt` (timestamp, required)
- Relationships:
  - One `LoggedSession` has one `SessionFeelings` (optional until save completion).
  - One `LoggedSession` has many `ExerciseEntry`.
- Validation rules:
  - `PROGRAM` session must reference `programSessionId`.
  - `FREE` session must not reference `programSessionId`.
  - Program session can be logged only for next uncompleted sequence.

### SessionFeelings
- Fields:
  - `sessionId` (UUID, required, PK/FK)
  - `rating` (int, required, range 1..10)
  - `comment` (string, 0..1000, optional)
- Validation rules:
  - Rating required at save time.

### ExerciseEntry
- Fields:
  - `id` (UUID, required)
  - `loggedSessionId` (UUID, required, indexed)
  - `exerciseId` (UUID, optional)
  - `customExerciseName` (string, optional, 1..120)
  - `exerciseNameSnapshot` (string, required, 1..120)
  - `exerciseType` (enum: `STRENGTH`, `BODYWEIGHT`, `CARDIO`, required)
  - `sortOrder` (int, required, min 0)
- Relationships:
  - One `ExerciseEntry` has many `StrengthSet`.
  - One `ExerciseEntry` has many `CardioLap`.
- Validation rules:
  - Exactly one of `exerciseId` or `customExerciseName` for free-session custom flow.
  - Program sessions cannot add/remove/reorder entries from predefined targets.

### StrengthSet
- Fields:
  - `id` (UUID, required)
  - `exerciseEntryId` (UUID, required, indexed)
  - `setOrder` (int, required, min 1)
  - `reps` (int, required, min 1)
  - `weightValue` (decimal, optional, >0)
  - `weightUnit` (enum: `KG`, `LBS`, optional)
  - `isBodyWeight` (boolean, required)
  - `durationSeconds` (int, optional, min 1)
  - `restSeconds` (int, optional, min 0)
- Validation rules:
  - At least one set required for strength/bodyweight entry.
  - If `isBodyWeight=true`, `weightValue` is optional.
  - If `isBodyWeight=false`, `weightValue` required.

### CardioLap
- Fields:
  - `id` (UUID, required)
  - `exerciseEntryId` (UUID, required, indexed)
  - `lapOrder` (int, required, min 1)
  - `durationSeconds` (int, required, min 1)
  - `distanceValue` (decimal, optional, >0)
  - `distanceUnit` (enum: `KM`, `MI`, optional)
- Validation rules:
  - Cardio entry must contain at least one lap.

### Exercise (library)
- Fields:
  - `id` (UUID, required)
  - `name` (string, required, unique)
  - `category` (string, required)
  - `type` (enum: `STRENGTH`, `BODYWEIGHT`, `CARDIO`, required)
  - `description` (string, optional)
  - `isActive` (boolean, required)
- Validation rules:
  - Initial dataset target: >=100 exercises.

## Relationships Summary
- `User 1 -> many WorkoutProgram`
- `WorkoutProgram 1 -> many ProgramSession`
- `ProgramSession 1 -> many ProgramExerciseTarget`
- `User 1 -> many LoggedSession`
- `LoggedSession 1 -> many ExerciseEntry`
- `ExerciseEntry 1 -> many StrengthSet`
- `ExerciseEntry 1 -> many CardioLap`
- `LoggedSession 1 -> 0..1 SessionFeelings`

## State Transitions

### WorkoutProgram
- `ACTIVE -> COMPLETED` when final `ProgramSession` is logged.
- No automatic transition back to `ACTIVE`; new program must be explicitly requested/generated.

### ProgramSession
- `isCompleted=false -> true` when corresponding `LoggedSession` of type `PROGRAM` is saved.

### LoggedSession lifecycle
- `DRAFT` (in UI only) -> `SAVED` (persisted with entries + feelings).
- Deletion/update behavior is out of current scope; MVP focuses on create + read flows.

## Derived Read Models
- `SessionHistoryItem`: date, type, exercise count, total duration, summary tags.
- `ExerciseProgressPoint`: exercise name, session date, metric primary value (weight or duration/distance), detail reference to session.

## Indexing and Query Notes
- `logged_session(user_id, session_date desc)` for fast reverse-chronological history.
- `exercise_entry(logged_session_id, exercise_name_snapshot)` + text index for history search by exercise name.
- `exercise_progress_view(user_id, exercise_name, session_date)` query path for progression charts.
