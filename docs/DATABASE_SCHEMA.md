# Database Schema: Workout Tracker

## Overview

The PostgreSQL database for the Workout Tracker uses a relational model optimized for:
- **Session logging**: Program sessions with predefined targets vs. free-form sessions
- **User isolation**: Strict per-user data access boundaries
- **History querying**: Efficient reverse-chronological retrieval with filtering
- **Progression analytics**: Exercise performance tracking over time

**Database**: PostgreSQL 16+  
**Migration Tool**: Flyway (auto-run on Spring Boot startup)

---

## Schema Diagram (Text Format)

```
┌─────────────────┐
│     users       │
│                 │
│  id (UUID)      │◄──────────┐
│  preferred_     │           │
│  weight_unit    │           │
└─────────────────┘           │
         ▲                     │
         │                     │
    ONE │                      │ MANY
         │                     │
    ┌────┴──────────────────────────────┐
    │   workout_programs                 │
    │                                    │
    │  id (UUID) PK                      │
    │  user_id (UUID) FK ────────────────┘
    │  name                              │
    │  status (ACTIVE/COMPLETED)         │
    │  created_at                        │
    │  completed_at                      │
    └────┬──────────────┬────────────────┘
         │              │
    ONE │              │ ONE
         │              │
    ┌────▼─────────────────────┐
    │  program_sessions        │
    │                          │
    │  id (UUID) PK            │
    │  program_id (UUID) FK    │
    │  sequence_number         │
    │  name                    │
    │  is_completed            │
    └────┬──────────┬───────────┘
         │          │
    ONE │          │ MANY
         │          │
    ┌────▼──────────────────────────────┐
    │  program_exercise_targets          │
    │                                    │
    │  id (UUID) PK                      │
    │  program_session_id (UUID) FK      │
    │  exercise_name                     │
    │  exercise_type                     │
    │  target_sets, target_reps          │
    │  target_weight, target_weight_unit │
    │  target_duration_seconds           │
    │  target_distance, target_distance_ │
    │  unit, sort_order                  │
    └────────────────────────────────────┘

┌──────────────────┐
│  logged_sessions │
│                  │
│  id (UUID) PK    │
│  user_id (UUID)◄─┼────────────┐
│  FK (users)      │            │ MANY
│  session_type    │ ONE        │
│  program_session │            │
│  _id (UUID) FK   │            │
│  (prog._session) │◄───────────┤
│  session_date    │            │
│  name            │            │
│  notes           │            │
│  total_duration_ │            │
│  seconds         │            │
│  created_at      │            │
│                  │            │
│  [INDEXES]       │            │
│  - (user_id,     │            │
│    session_date) │            │
│  - (user_id,     │            │
│    program_      │            │
│    session_id)   │            │
│                  │            │
└──────────┬───────┘            │
           │                    │
      ONE │                MANY │
           │                    │
    ┌──────▼──────────────┐     │
    │ session_feelings    │     │
    │                     │     │
    │ session_id (UUID)   │     │
    │ PK/FK               │     │
    │ rating (1-10)       │     │
    │ comment             │     │
    └─────────────────────┘     │
                                │
                           MANY │
                                │
                    ┌───────────▼─────────────┐
                    │ exercise_entries        │
                    │                         │
                    │ id (UUID) PK            │
                    │ logged_session_id (UUID)│
                    │ FK                      │
                    │ exercise_id (UUID) FK   │
                    │ (exercises table)       │
                    │ custom_exercise_name    │
                    │ exercise_name_snapshot  │
                    │ exercise_type           │
                    │ sort_order              │
                    │                         │
                    │ [INDEXES]               │
                    │ - logged_session_id     │
                    │ - exercise_name_        │
                    │   snapshot              │
                    │                         │
                    └───────┬────────┬────────┘
                            │        │
                        MANY│        │MANY
                            │        │
        ┌───────────────────▼┐  ┌────▼──────────────┐
        │ strength_sets      │  │ cardio_laps       │
        │                    │  │                   │
        │ id (UUID) PK       │  │ id (UUID) PK      │
        │ exercise_entry_id  │  │ exercise_entry_id │
        │ (UUID) FK          │  │ (UUID) FK         │
        │ set_order          │  │ lap_order         │
        │ reps               │  │ duration_seconds  │
        │ weight_value       │  │ distance_value    │
        │ weight_unit        │  │ distance_unit     │
        │ is_body_weight     │  │                   │
        │ duration_seconds   │  │                   │
        │ rest_seconds       │  │                   │
        │                    │  │                   │
        │ [INDEXES]          │  │ [INDEXES]         │
        │ - exercise_entry_id│  │ - exercise_entry_ │
        │                    │  │   id              │
        └────────────────────┘  └───────────────────┘

┌──────────────────┐
│  exercises       │ (Library)
│                  │
│  id (UUID) PK    │
│  name (UNIQUE)   │
│  category        │
│  type            │
│  description     │
│  is_active       │
│                  │
│  [INDEXES]       │
│  - name          │
│  - is_active     │
└──────────────────┘
```

---

## Table Definitions

### users
Represents authenticated users (Spring Security backed).

```sql
CREATE TABLE users (
  id UUID PRIMARY KEY,
  preferred_weight_unit VARCHAR(3) NOT NULL CHECK (preferred_weight_unit IN ('KG', 'LBS')),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

**Key Points**:
- `id` is provided by Spring Security (HTTP Basic Auth users)
- `preferred_weight_unit` stores user's weight preference (kg or lbs)

---

### workout_programs
Represents a user's training program (e.g., "12-Week Strength", "Summer Endurance").

```sql
CREATE TABLE workout_programs (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name VARCHAR(120) NOT NULL,
  status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'COMPLETED')),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  completed_at TIMESTAMP,
  CONSTRAINT single_active_program_per_user 
    UNIQUE NULLS NOT DISTINCT (user_id, status) 
    WHERE status = 'ACTIVE'
);

CREATE INDEX idx_workout_programs_user_id ON workout_programs(user_id);
```

**Key Points**:
- One user can have at most one `ACTIVE` program (enforced by partial unique index)
- `created_at` tracks program creation
- `completed_at` populated when all sessions completed

---

### program_sessions
Represents sessions within a program (e.g., "Session 1: Upper Body").

```sql
CREATE TABLE program_sessions (
  id UUID PRIMARY KEY,
  program_id UUID NOT NULL REFERENCES workout_programs(id) ON DELETE CASCADE,
  sequence_number INT NOT NULL CHECK (sequence_number >= 1),
  name VARCHAR(120) NOT NULL,
  is_completed BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT unique_program_session_sequence 
    UNIQUE (program_id, sequence_number)
);

CREATE INDEX idx_program_sessions_program_id ON program_sessions(program_id);
```

**Key Points**:
- `sequence_number` defines order within program (1, 2, 3, ...)
- `(program_id, sequence_number)` enforces unique sequences
- `is_completed` tracks session status (marked true when user logs session)

---

### program_exercise_targets
Represents AI-defined targets for exercises in a program session.

```sql
CREATE TABLE program_exercise_targets (
  id UUID PRIMARY KEY,
  program_session_id UUID NOT NULL REFERENCES program_sessions(id) ON DELETE CASCADE,
  exercise_name VARCHAR(120) NOT NULL,
  exercise_type VARCHAR(20) NOT NULL CHECK (exercise_type IN ('STRENGTH', 'BODYWEIGHT', 'CARDIO')),
  target_sets INT CHECK (target_sets IS NULL OR target_sets >= 1),
  target_reps INT CHECK (target_reps IS NULL OR target_reps >= 1),
  target_weight DECIMAL(10, 2),
  target_weight_unit VARCHAR(3) CHECK (target_weight_unit IS NULL OR target_weight_unit IN ('KG', 'LBS')),
  target_duration_seconds INT CHECK (target_duration_seconds IS NULL OR target_duration_seconds >= 1),
  target_distance DECIMAL(10, 2),
  target_distance_unit VARCHAR(3) CHECK (target_distance_unit IS NULL OR target_distance_unit IN ('KM', 'MI')),
  sort_order INT NOT NULL DEFAULT 0,
  CONSTRAINT positive_weight CHECK (target_weight IS NULL OR target_weight > 0),
  CONSTRAINT positive_distance CHECK (target_distance IS NULL OR target_distance > 0)
);

CREATE INDEX idx_program_exercise_targets_program_session_id 
  ON program_exercise_targets(program_session_id);
```

**Key Points**:
- Stores AI Coach's prescribed targets for the session
- `exercise_name` is a snapshot (not FK to exercises table) to preserve historical targets
- `sort_order` maintains exercise display order
- Nullable fields for exercise type flexibility (strength vs. cardio)

---

### logged_sessions
Represents a completed workout session (either program-tracked or free).

```sql
CREATE TABLE logged_sessions (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  session_type VARCHAR(20) NOT NULL CHECK (session_type IN ('PROGRAM', 'FREE')),
  program_session_id UUID REFERENCES program_sessions(id) ON DELETE SET NULL,
  session_date DATE NOT NULL,
  name VARCHAR(120),
  notes TEXT,
  total_duration_seconds INT CHECK (total_duration_seconds IS NULL OR total_duration_seconds >= 1),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT program_sessions_require_id 
    CHECK ((session_type = 'PROGRAM' AND program_session_id IS NOT NULL) 
           OR (session_type = 'FREE' AND program_session_id IS NULL))
);

CREATE INDEX idx_logged_sessions_user_id ON logged_sessions(user_id);
CREATE INDEX idx_logged_sessions_user_session_date 
  ON logged_sessions(user_id, session_date DESC);
CREATE INDEX idx_logged_sessions_program_session_id 
  ON logged_sessions(program_session_id);
```

**Key Points**:
- `session_type` determines whether session follows a program (`PROGRAM`) or is freestyle (`FREE`)
- For `PROGRAM` sessions: must reference next uncompleted `program_session_id`
- For `FREE` sessions: `program_session_id` must be null
- `(user_id, session_date DESC)` index optimizes history queries
- User isolation enforced via `user_id` FK

---

### session_feelings
Represents user's post-session feelings/feedback.

```sql
CREATE TABLE session_feelings (
  session_id UUID PRIMARY KEY REFERENCES logged_sessions(id) ON DELETE CASCADE,
  rating INT NOT NULL CHECK (rating >= 1 AND rating <= 10),
  comment VARCHAR(1000)
);
```

**Key Points**:
- 1:1 relationship with `logged_sessions`
- `rating` required (1-10 scale)
- `comment` optional user feedback

---

### exercise_entries
Represents each exercise logged in a session (e.g., "Bench Press", "Squats").

```sql
CREATE TABLE exercise_entries (
  id UUID PRIMARY KEY,
  logged_session_id UUID NOT NULL REFERENCES logged_sessions(id) ON DELETE CASCADE,
  exercise_id UUID REFERENCES exercises(id) ON DELETE SET NULL,
  custom_exercise_name VARCHAR(120),
  exercise_name_snapshot VARCHAR(120) NOT NULL,
  exercise_type VARCHAR(20) NOT NULL CHECK (exercise_type IN ('STRENGTH', 'BODYWEIGHT', 'CARDIO')),
  sort_order INT NOT NULL DEFAULT 0,
  CONSTRAINT library_or_custom 
    CHECK ((exercise_id IS NOT NULL AND custom_exercise_name IS NULL) 
           OR (exercise_id IS NULL AND custom_exercise_name IS NOT NULL))
);

CREATE INDEX idx_exercise_entries_logged_session_id 
  ON exercise_entries(logged_session_id);
CREATE INDEX idx_exercise_entries_exercise_name_snapshot 
  ON exercise_entries(exercise_name_snapshot);
```

**Key Points**:
- Exactly one of `exercise_id` (library) or `custom_exercise_name` (user-defined)
- `exercise_name_snapshot` stores the name at time of logging (for historical consistency)
- `sort_order` maintains display order
- `exercise_name_snapshot` indexed for filtering by exercise name in history

---

### strength_sets
Represents individual sets of a strength/bodyweight exercise.

```sql
CREATE TABLE strength_sets (
  id UUID PRIMARY KEY,
  exercise_entry_id UUID NOT NULL REFERENCES exercise_entries(id) ON DELETE CASCADE,
  set_order INT NOT NULL CHECK (set_order >= 1),
  reps INT NOT NULL CHECK (reps >= 1),
  weight_value DECIMAL(10, 2),
  weight_unit VARCHAR(3) CHECK (weight_unit IS NULL OR weight_unit IN ('KG', 'LBS')),
  is_body_weight BOOLEAN NOT NULL DEFAULT FALSE,
  duration_seconds INT CHECK (duration_seconds IS NULL OR duration_seconds >= 1),
  rest_seconds INT CHECK (rest_seconds IS NULL OR rest_seconds >= 1),
  CONSTRAINT positive_weight CHECK (weight_value IS NULL OR weight_value > 0),
  CONSTRAINT bodyweight_no_weight CHECK (NOT is_body_weight OR weight_value IS NULL)
);

CREATE INDEX idx_strength_sets_exercise_entry_id 
  ON strength_sets(exercise_entry_id);
```

**Key Points**:
- `set_order` defines set sequence (1, 2, 3, ...)
- `reps` required (how many reps performed)
- `weight_value` optional (null for bodyweight exercises)
- `is_body_weight` boolean flag (if true, weight_value must be null)
- `duration_seconds` optional (for time-under-tension tracking)
- `rest_seconds` optional (rest between sets)

---

### cardio_laps
Represents laps/intervals of a cardio exercise.

```sql
CREATE TABLE cardio_laps (
  id UUID PRIMARY KEY,
  exercise_entry_id UUID NOT NULL REFERENCES exercise_entries(id) ON DELETE CASCADE,
  lap_order INT NOT NULL CHECK (lap_order >= 1),
  duration_seconds INT NOT NULL CHECK (duration_seconds >= 1),
  distance_value DECIMAL(10, 2),
  distance_unit VARCHAR(3) CHECK (distance_unit IS NULL OR distance_unit IN ('KM', 'MI')),
  CONSTRAINT positive_distance CHECK (distance_value IS NULL OR distance_value > 0)
);

CREATE INDEX idx_cardio_laps_exercise_entry_id 
  ON cardio_laps(exercise_entry_id);
```

**Key Points**:
- `lap_order` defines lap sequence
- `duration_seconds` required (time spent in this lap)
- `distance_value` optional (distance covered in this lap)
- Used for tracking running, cycling, swimming, etc.

---

### exercises (Library)
Reference table for exercise definitions (optional, populated by admin).

```sql
CREATE TABLE exercises (
  id UUID PRIMARY KEY,
  name VARCHAR(120) NOT NULL UNIQUE,
  category VARCHAR(50) NOT NULL,
  type VARCHAR(20) NOT NULL CHECK (type IN ('STRENGTH', 'BODYWEIGHT', 'CARDIO')),
  description TEXT,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_exercises_name ON exercises(name);
CREATE INDEX idx_exercises_is_active ON exercises(is_active);
```

**Key Points**:
- Centralized library of exercises for UI suggestions
- `is_active` allows soft-delete / archiving exercises
- Not required for session logging (custom names always allowed)
- Used in `/api/exercises` search endpoint

---

## Query Performance Considerations

### Indexes

**User Isolation**:
- All queries filtered by `user_id` (indexed on every per-user table)
- Ensures O(log n) lookups within user's data

**Session History (Reverse-Chronological)**:
- `(user_id, session_date DESC)` composite index on `logged_sessions`
- Enables efficient `ORDER BY session_date DESC` pagination
- Typical query: 20 items in < 5ms on 500-session dataset

**Exercise Name Search**:
- `exercise_name_snapshot` indexed on `exercise_entries`
- Enables partial-match filtering within history
- Combine with `user_id` filter for efficient results

**Program Session Lookups**:
- `program_session_id` indexed on `logged_sessions`
- Quick validation that program session is already logged
- Enforce single-log-per-session constraint

### N+1 Prevention

**Recommended Query Patterns**:

1. **Session Detail** (eager-load nested data):
   ```sql
   SELECT ls.*, sf.rating, sf.comment
   FROM logged_sessions ls
   LEFT JOIN session_feelings sf ON ls.id = sf.session_id
   WHERE ls.id = ? AND ls.user_id = ?;
   
   SELECT * FROM exercise_entries WHERE logged_session_id = ?;
   SELECT * FROM strength_sets WHERE exercise_entry_id = ?;
   SELECT * FROM cardio_laps WHERE exercise_entry_id = ?;
   ```
   Use Spring JPA `@EntityGraph` or explicit JOIN FETCH to avoid N+1.

2. **History Query** (efficient pagination):
   ```sql
   SELECT ls.id, ls.session_date, ls.session_type, 
          (SELECT COUNT(*) FROM exercise_entries WHERE logged_session_id = ls.id) as exercise_count,
          ls.total_duration_seconds
   FROM logged_sessions ls
   WHERE ls.user_id = ?
   ORDER BY ls.session_date DESC
   LIMIT 20 OFFSET 0;
   ```

3. **Progression Query** (aggregate):
   ```sql
   SELECT ls.id, ls.session_date, 
          CASE WHEN ee.exercise_type = 'CARDIO' 
               THEN SUM(cl.duration_seconds)
               ELSE MAX(ss.weight_value)
          END as metric_value
   FROM logged_sessions ls
   JOIN exercise_entries ee ON ls.id = ee.logged_session_id
   LEFT JOIN strength_sets ss ON ee.id = ss.exercise_entry_id
   LEFT JOIN cardio_laps cl ON ee.id = cl.exercise_entry_id
   WHERE ls.user_id = ? AND ee.exercise_name_snapshot = ?
   GROUP BY ls.id, ls.session_date, ee.exercise_type
   ORDER BY ls.session_date ASC;
   ```

### Performance Targets

- **Session Save**: p95 <= 1.5s (3 exercises, 3 sets each + feelings)
- **History Query**: p95 <= 2s (pagination, 20 items from 500-session dataset)
- **Detail Query**: p95 <= 1s (single session with nested data)
- **Progression Query**: p95 <= 2s (50+ data points)

---

## Data Isolation & Security

### User-Based Row Security

**All queries must include user_id filter**:
```sql
-- ✓ Correct
SELECT * FROM logged_sessions WHERE user_id = $1;

-- ✗ Incorrect (data leak!)
SELECT * FROM logged_sessions;
```

**Spring Security Integration**:
- Extract authenticated user UUID from `SecurityContextHolder`
- Pass as query parameter to all repository methods
- Service layer enforces `userId` match before returning data

### Referential Integrity

- All FKs use `ON DELETE CASCADE` (when user deleted, cascade to programs/sessions/entries)
- Exception: `exercise_entries.exercise_id` uses `SET NULL` (library exercises persist)

### Constraints

- `session_type` + `program_session_id` constraint prevents invalid combinations
- `is_body_weight` + `weight_value` constraint prevents weight on bodyweight exercises
- `(program_id, sequence_number)` uniqueness prevents duplicate sessions

---

## Migration & Versioning

Migrations are managed by **Flyway**:

**File**: `backend/src/main/resources/db/migration/V001__initial_schema.sql`

**Auto-runs on Spring Boot startup**:
```powershell
mvn spring-boot:run
# Flyway checks for pending migrations and applies them
```

**To add new migrations**:
1. Create file: `V002__add_new_column.sql` (increment version)
2. Spring Boot auto-applies on next startup

---

## Example Queries

### Get User's Latest 10 Sessions
```sql
SELECT ls.id, ls.session_date, ls.session_type, 
       COUNT(ee.id) as exercise_count,
       ls.total_duration_seconds
FROM logged_sessions ls
LEFT JOIN exercise_entries ee ON ls.id = ee.logged_session_id
WHERE ls.user_id = '11111111-1111-1111-1111-111111111111'
GROUP BY ls.id, ls.session_date, ls.session_type, ls.total_duration_seconds
ORDER BY ls.session_date DESC
LIMIT 10;
```

### Get Bench Press Progression for User
```sql
SELECT ls.id as session_id, ls.session_date, 
       MAX(ss.weight_value) as max_weight,
       ss.weight_unit
FROM logged_sessions ls
JOIN exercise_entries ee ON ls.id = ee.logged_session_id
JOIN strength_sets ss ON ee.id = ss.exercise_entry_id
WHERE ls.user_id = '11111111-1111-1111-1111-111111111111'
  AND ee.exercise_name_snapshot = 'Bench Press'
GROUP BY ls.id, ls.session_date, ss.weight_unit
ORDER BY ls.session_date ASC;
```

### Count Sessions by Exercise Type
```sql
SELECT ee.exercise_type, COUNT(DISTINCT ls.id) as session_count
FROM logged_sessions ls
JOIN exercise_entries ee ON ls.id = ee.logged_session_id
WHERE ls.user_id = '11111111-1111-1111-1111-111111111111'
GROUP BY ee.exercise_type
ORDER BY session_count DESC;
```

### Find Sessions Within Date Range
```sql
SELECT ls.id, ls.session_date, ls.session_type
FROM logged_sessions ls
WHERE ls.user_id = '11111111-1111-1111-1111-111111111111'
  AND ls.session_date BETWEEN '2026-04-01' AND '2026-04-30'
ORDER BY ls.session_date DESC;
```

