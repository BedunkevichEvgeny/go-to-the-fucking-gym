# Feature Walkthrough: Workout Tracker

## Overview

This guide walks through all four user stories of the Workout Tracker feature. Each story represents a complete use case that can be tested independently.

**System Requirements**:
- Backend running at `http://localhost:8080/api`
- Frontend running at `http://localhost:5173`
- Database initialized (PostgreSQL or in-memory H2)
- Test data seeded (see setup instructions)

---

## Setup: Seed Test Data

### Create Test Users & Programs

Use the default authentication users:

| Username | Password | User ID |
|----------|----------|---------|
| `user1` | `password1` | `11111111-1111-1111-1111-111111111111` |
| `user2` | `password2` | `22222222-2222-2222-2222-222222222222` |

### Seed a Program (API Call)

```bash
# Create a workout program for user1
curl -u user1:password1 -X POST http://localhost:8080/api/programs \
  -H "Content-Type: application/json" \
  -d '{
    "name": "12-Week Strength",
    "sessions": [
      {
        "sequenceNumber": 1,
        "name": "Upper Body Strength",
        "exercises": [
          {
            "exerciseName": "Bench Press",
            "exerciseType": "STRENGTH",
            "targetSets": 3,
            "targetReps": 8,
            "targetWeight": 70.0,
            "targetWeightUnit": "KG"
          },
          {
            "exerciseName": "Squats",
            "exerciseType": "STRENGTH",
            "targetSets": 4,
            "targetReps": 6,
            "targetWeight": 100.0,
            "targetWeightUnit": "KG"
          },
          {
            "exerciseName": "Deadlift",
            "exerciseType": "STRENGTH",
            "targetSets": 1,
            "targetReps": 5,
            "targetWeight": 120.0,
            "targetWeightUnit": "KG"
          }
        ]
      },
      {
        "sequenceNumber": 2,
        "name": "Lower Body Strength",
        "exercises": [
          {
            "exerciseName": "Leg Press",
            "exerciseType": "STRENGTH",
            "targetSets": 4,
            "targetReps": 8,
            "targetWeight": 180.0,
            "targetWeightUnit": "KG"
          }
        ]
      }
    ]
  }'
```

**Note**: If program creation API is not available, you can seed via SQL directly into the database.

---

## User Story 1: Log a Program Session

### Goal
Users with an active program can log their next uncompleted program session, recording actual performance (which may differ from targets) and post-session feelings.

### Flow

#### Step 1: Check Active Program
Go to the Program Session page (`/program-sessions` or main dashboard).

The page should display:
- **Session Title**: "Upper Body Strength (Session 1 of 2)"
- **Sequence Number**: 1
- **3 Exercises with AI Targets**:
  - Bench Press — Target: 3 × 8 @ 70 kg
  - Squats — Target: 4 × 6 @ 100 kg
  - Deadlift — Target: 1 × 5 @ 120 kg

#### Step 2: Enter Actual Performance (Different from Targets)

For each exercise, fill in actual performance:

**Bench Press**:
- Set 1: 8 reps @ 70 kg ✓ (matches target)
- Set 2: 8 reps @ 72 kg (exceeded target)
- Set 3: 6 reps @ 70 kg (missed 8 reps target, but acceptable)

**Squats**:
- Set 1: 6 reps @ 100 kg
- Set 2: 5 reps @ 100 kg (only 2 of 4 target sets)
- Toggle "Bodyweight" — leave off (weight required)

**Deadlift**:
- Set 1: 5 reps @ 125 kg (exceeded target by 5 kg)

#### Step 3: Record Feelings

Scroll to feelings section:
- **Rating**: Drag slider to 8/10 (felt strong)
- **Comment**: "Great workout! Felt stronger than last week."

#### Step 4: Save Session

Click "Save Session" button.

**Expected Result**:
- Form disappears
- Success toast message: "Session saved successfully!"
- Redirect to session history page (new session appears at top)

#### Verification

```bash
# Check session was saved
curl -u user1:password1 "http://localhost:8080/api/logged-sessions/history?page=0&size=5"
```

Expected response: Newest session with 3 exercises, PROGRAM type, saved today's date.

```bash
# Check program session marked completed
curl -u user1:password1 "http://localhost:8080/api/program-sessions/next"
```

Expected response: 200 OK with Session 2 (Upper Body now complete, next is Lower Body Strength).

---

## User Story 2: Log a Free Session

### Goal
Users without an active program (or wanting off-program workouts) can create freestyle sessions, adding exercises from the library or custom names, recording performance, and saving.

### Flow

#### Step 1: Navigate to Free Session

Go to "Start Free Session" page (`/free-session`).

#### Step 2: Search & Add Exercises

**Search Library**:
1. Type "Bench" in search box
2. Results appear: "Bench Press (Chest)", "Incline Bench Press (Chest)"
3. Click "Bench Press" → exercise added to session

**Add Custom Exercise**:
1. Type "Tire Flip" (not in library)
2. Press Enter or click "Add as custom"
3. Exercise added to session with type selector (STRENGTH/BODYWEIGHT/CARDIO)
4. Select "BODYWEIGHT"

**Add Cardio**:
1. Search for "Running" or type custom
2. Add "Running" as CARDIO exercise

#### Step 3: Enter Performance Data

**Bench Press (Strength)**:
- Set 1: 8 reps @ 60 kg
- Set 2: 8 reps @ 60 kg
- Click "Add Set" if more sets needed
- Remove Set if needed

**Tire Flip (Bodyweight)**:
- Set 1: 12 reps (no weight, bodyweight toggle on)
- Verify weight field is disabled

**Running (Cardio)**:
- Lap 1: 600 seconds (10 min) @ 3.2 km
- If adding distance unit, select KM

#### Step 4: Record Feelings

- **Rating**: 7/10 (good workout)
- **Comment**: "Fun session, tried new exercises"

#### Step 5: Save

Click "Save Session".

**Expected Result**:
- Session saved with `sessionType: "FREE"`
- Redirect to history
- New session appears at top, marked as FREE
- No program session marked as complete

#### Verification

```bash
# Check free session exists
curl -u user1:password1 "http://localhost:8080/api/logged-sessions/history"
```

Expected response: Latest session with `sessionType: "FREE"`, 3 exercises, no `programSessionId`.

```bash
# Check program session still points to Session 2
curl -u user1:password1 "http://localhost:8080/api/program-sessions/next"
```

Expected response: Session 2 data (program session not affected by free session).

---

## User Story 3: Browse Workout History

### Goal
Users view a reverse-chronological list of all past sessions (program + free), filter by date or exercise, and drill into detailed session data.

### Flow

#### Step 1: View History

Go to "Workout History" page (`/history`).

**Expected Display**:
- List of 3 sessions (User Story 1 + US2 + older sessions if any)
- Reverse-chronological order (newest first, today's free session at top)
- Each row shows:
  - Session date: "Today" or formatted date
  - Type badge: "PROGRAM" (green) or "FREE" (blue)
  - Exercise count: "3 exercises"
  - Optional: Total duration "40 min"

#### Step 2: Apply Filters

**Date Range Filter**:
1. Click filter section
2. Select "From": Today (or 1 month ago)
3. Select "To": Today
4. Click "Apply Filters"
5. History updates to show only sessions in range (still reverse-chronological)

**Exercise Name Filter**:
1. Type "Bench" in exercise name field
2. Click "Apply Filters"
3. History shows only sessions containing Bench Press (both US1 and US2 had it)
4. Exercise count remains accurate for filtered results

**Combined Filters**:
1. Set date range + exercise name
2. Only sessions matching both criteria appear

**Clear Filters**:
1. Click "Clear All Filters"
2. Full history reappears

#### Step 3: View Session Detail

Click on any session row (e.g., today's free session).

**Expected Detail View**:
- Session metadata:
  - Date: "April 27, 2026"
  - Type: "FREE"
  - Total duration: "30 minutes"
  - Name/Notes: if provided
- Exercise breakdown (table):
  | Exercise | Type | Set 1 | Set 2 | Set 3 |
  |----------|------|-------|-------|-------|
  | Bench Press | Strength | 8 reps @ 60kg | 8 reps @ 60kg | — |
  | Tire Flip | Bodyweight | 12 reps | — | — |
  | Running | Cardio | 600s / 3.2 km | — | — |

- Feelings section:
  - Rating: ⭐⭐⭐⭐⭐⭐⭐ (7/10 stars)
  - Comment: "Fun session, tried new exercises"

#### Step 4: Pagination

If > 20 sessions:
1. Pagination controls appear at bottom (Previous/Next)
2. Click "Next" to load older sessions
3. Page number updates
4. Sessions on new page are in correct reverse-chronological order

#### Verification

```bash
# Check history endpoint
curl -u user1:password1 "http://localhost:8080/api/logged-sessions/history?page=0&size=20"
```

Expected: `SessionHistoryPage` with 3 items, correct order.

```bash
# Check detail endpoint
curl -u user1:password1 "http://localhost:8080/api/logged-sessions/{sessionId}"
```

Expected: `LoggedSessionDetail` with full exercise/set data.

---

## User Story 4: Track Exercise Progression

### Goal
Users select an exercise and view a chart showing performance improvement over time (weight increases, duration gains, etc.).

### Flow

#### Step 1: Navigate to Progression Chart

From session detail view (US3), click "View Progression" link on an exercise (e.g., "Bench Press").

Or directly visit `/progression/Bench Press` (if supported).

#### Step 2: View Chart

**Expected Chart Display**:
- Title: "Bench Press Progression"
- X-axis: Session dates (chronological, oldest to newest)
- Y-axis: Max weight (kg or lbs)
- 2+ data points connected with line
- Sample data points:
  - March 15: 60 kg (from old session)
  - March 22: 65 kg
  - March 29: 70 kg (program target)
  - April 27: 72 kg (US1 actual)

#### Step 3: Interact with Chart

**Hover over point**: Tooltip shows:
- Date: "April 27, 2026"
- Weight: "72 kg"
- "View session" link

**Click data point**: Navigates to session detail view for that session.

#### Step 4: Chart Edge Cases

**Single Data Point**:
- Show message: "Not enough data to show trend (need at least 2 points)"
- Point still displayed

**No Data**:
- Show message: "No progression data for [exercise name]"
- Empty chart

**Mixed Exercise Types**:
- If progression queries multiple types (e.g., "Running" as CARDIO):
  - Y-axis label: "Duration (minutes)"
  - Points show duration instead of weight

#### Verification

```bash
# Check progression endpoint
curl -u user1:password1 "http://localhost:8080/api/progression/Bench%20Press"
```

Expected response: `ProgressionResponse` with 3+ points, sorted by date ascending.

```json
{
  "exerciseName": "Bench Press",
  "points": [
    {
      "sessionId": "...",
      "sessionDate": "2026-03-15",
      "metricType": "WEIGHT",
      "metricValue": 60.0
    },
    {
      "sessionId": "...",
      "sessionDate": "2026-04-27",
      "metricType": "WEIGHT",
      "metricValue": 72.0
    }
  ]
}
```

---

## Complete End-to-End Scenario

### Ideal 30-Minute Test Flow

1. **Setup** (5 min):
   - Ensure backend + frontend running
   - Login as `user1`

2. **US1: Log Program Session** (10 min):
   - Navigate to program sessions
   - Fill in actual vs. target performance
   - Record feelings (rating 8)
   - Save and verify in history

3. **US2: Log Free Session** (8 min):
   - Navigate to free session
   - Search + add library exercises
   - Add custom exercise (bodyweight)
   - Record feelings (rating 7)
   - Save and verify separate from program

4. **US3: Browse History & Filter** (5 min):
   - View history (2 sessions visible)
   - Apply date filter (shows both)
   - Apply exercise filter (shows relevant)
   - Click session detail view
   - Verify full data displayed

5. **US4: View Progression** (2 min):
   - From detail view, click "View Progression"
   - Chart displays with 2 data points
   - Hover/click to verify interactions

---

## Troubleshooting

### "No active program" when starting US1

**Solution**:
- Seed a program via API or SQL
- Verify `programSessionId` is not null in database
- Check program status is `ACTIVE` (not `COMPLETED`)

### Session save fails with 403 error

**Causes**:
1. Trying to log program session but not the next uncompleted one
   - **Fix**: Call `GET /api/program-sessions/next` to confirm correct `programSessionId`
2. Cross-user access (user1 trying to log user2's program)
   - **Fix**: Ensure authenticated user matches program owner

### Charts show no data

**Causes**:
1. Exercise name doesn't match exactly (case-sensitive after first session)
   - **Fix**: Use exact name from first logged session
2. Only 1 data point logged
   - **Fix**: Log same exercise in another session

### Filters return empty results

**Causes**:
1. Date range excludes all sessions
   - **Fix**: Check session dates, expand date range
2. Exercise filter too specific (typo or case mismatch)
   - **Fix**: Verify exact exercise name

### API authentication fails (401)

**Solution**:
```bash
# Verify credentials work
curl -u user1:password1 http://localhost:8080/api/program-sessions/next -v

# Check Authorization header is included
# Output should show: "< HTTP/1.1 200 OK" or "204 No Content"
```

---

## Performance Expectations

During walkthrough, expect these latencies:

| Operation | Expected Latency | Success Criteria |
|-----------|-------------------|------------------|
| Session save (3 ex, 3 sets) | < 1.5s | Form submitted, loading spinner < 1.5s |
| History load (20 items) | < 2s | List populated, scrollable |
| Session detail load | < 1s | Full nested data displayed |
| Progression chart render | < 2s | Chart with all data points visible |

If any operation exceeds criteria, check:
- Database query performance (run `EXPLAIN ANALYZE` on relevant queries)
- Network latency (check DevTools Network tab in browser)
- Backend logs for slow endpoints

---

## Next Steps After Walkthrough

1. **Collect feedback** on UX, data model, responsiveness
2. **File bugs** if any features don't work as documented
3. **Request feature requests** (e.g., edit session, delete session, program management)
4. **Plan next release** (AI Coach integration, program generation, social features)

