# API Usage Guide

## Overview

The Workout Tracker API provides endpoints for logging workout sessions, browsing history, and tracking exercise progression. The API uses HTTP Basic Authentication with predefined user credentials for MVP.

**Base URL**: `http://localhost:8080/api` (local development)  
**Authentication**: HTTP Basic Auth  
**Content-Type**: `application/json`

## Authentication

All endpoints require HTTP Basic Authentication. Include the `Authorization` header with base64-encoded credentials:

```
Authorization: Basic base64(username:password)
```

**Example Users**:
| Username | Password | User ID |
|----------|----------|---------|
| `user1` | `password1` | `11111111-1111-1111-1111-111111111111` |
| `user2` | `password2` | `22222222-2222-2222-2222-222222222222` |

### Curl Example
```bash
curl -u user1:password1 http://localhost:8080/api/program-sessions/next
```

### JavaScript/Fetch Example
```javascript
const response = await fetch('http://localhost:8080/api/program-sessions/next', {
  method: 'GET',
  headers: {
    'Authorization': 'Basic ' + btoa('user1:password1'),
    'Content-Type': 'application/json'
  }
});
```

### Axios Example
```javascript
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  auth: {
    username: 'user1',
    password: 'password1'
  }
});

// Usage
const session = await api.get('/program-sessions/next');
```

## Error Responses

All errors return standardized JSON responses with HTTP status codes:

```json
{
  "message": "Descriptive error message",
  "code": "ERROR_CODE",
  "timestamp": "2026-04-27T12:00:00Z"
}
```

### Common Status Codes

| Code | Meaning | Example |
|------|---------|---------|
| `200` | OK | Successful GET request |
| `201` | Created | Successful POST request (resource created) |
| `204` | No Content | Successful GET with no data (e.g., no active program) |
| `400` | Bad Request | Validation error (invalid input format) |
| `401` | Unauthorized | Missing or invalid credentials |
| `403` | Forbidden | Cross-user access attempt or data modification not allowed |
| `404` | Not Found | Resource doesn't exist |
| `500` | Internal Server Error | Server-side error |

## Endpoints

### 1. Get Next Program Session

Retrieve the next uncompleted program session for the authenticated user.

**Endpoint**: `GET /program-sessions/next`  
**Auth**: Required  
**Response**: `200 OK` with `ProgramSessionView` or `204 No Content`

#### Request
```bash
curl -u user1:password1 http://localhost:8080/api/program-sessions/next
```

#### Response (200 OK)
```json
{
  "programSessionId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "sequenceNumber": 1,
  "name": "Upper Body Strength",
  "exercises": [
    {
      "exerciseName": "Bench Press",
      "exerciseType": "STRENGTH",
      "targetSets": 3,
      "targetReps": 8,
      "targetWeight": 70.0,
      "targetWeightUnit": "KG",
      "targetDurationSeconds": null,
      "targetDistance": null,
      "targetDistanceUnit": null
    },
    {
      "exerciseName": "Squats",
      "exerciseType": "STRENGTH",
      "targetSets": 4,
      "targetReps": 6,
      "targetWeight": 100.0,
      "targetWeightUnit": "KG",
      "targetDurationSeconds": null,
      "targetDistance": null,
      "targetDistanceUnit": null
    }
  ]
}
```

#### Response (204 No Content)
No body — indicates no active program or all sessions completed.

---

### 2. Create Logged Session

Log a completed workout session (program or free).

**Endpoint**: `POST /logged-sessions`  
**Auth**: Required  
**Request Body**: `LoggedSessionCreateRequest`  
**Response**: `201 Created` with `LoggedSessionDetail` or error

#### Request (Program Session)
```bash
curl -u user1:password1 -X POST http://localhost:8080/api/logged-sessions \
  -H "Content-Type: application/json" \
  -d '{
    "sessionType": "PROGRAM",
    "programSessionId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "sessionDate": "2026-04-27",
    "name": null,
    "notes": "Felt strong today",
    "totalDurationSeconds": 2400,
    "feelings": {
      "rating": 8,
      "comment": "Great workout, energy was high"
    },
    "exerciseEntries": [
      {
        "exerciseName": "Bench Press",
        "exerciseType": "STRENGTH",
        "sets": [
          {
            "reps": 8,
            "isBodyWeight": false,
            "weightValue": 70.0,
            "weightUnit": "KG"
          },
          {
            "reps": 8,
            "isBodyWeight": false,
            "weightValue": 70.0,
            "weightUnit": "KG"
          },
          {
            "reps": 6,
            "isBodyWeight": false,
            "weightValue": 65.0,
            "weightUnit": "KG"
          }
        ],
        "cardioLaps": []
      }
    ]
  }'
```

#### Request (Free Session)
```bash
curl -u user1:password1 -X POST http://localhost:8080/api/logged-sessions \
  -H "Content-Type: application/json" \
  -d '{
    "sessionType": "FREE",
    "programSessionId": null,
    "sessionDate": "2026-04-27",
    "name": "Off-Program Workout",
    "notes": "Tried tire flips today",
    "totalDurationSeconds": 1800,
    "feelings": {
      "rating": 7,
      "comment": "Fun but tiring"
    },
    "exerciseEntries": [
      {
        "exerciseName": "Bench Press",
        "exerciseType": "STRENGTH",
        "sets": [
          {
            "reps": 10,
            "isBodyWeight": false,
            "weightValue": 60.0,
            "weightUnit": "KG"
          }
        ],
        "cardioLaps": []
      },
      {
        "exerciseName": "Tire Flip",
        "exerciseType": "BODYWEIGHT",
        "sets": [
          {
            "reps": 12,
            "isBodyWeight": true,
            "weightValue": null,
            "weightUnit": null
          }
        ],
        "cardioLaps": []
      },
      {
        "exerciseName": "Running",
        "exerciseType": "CARDIO",
        "sets": [],
        "cardioLaps": [
          {
            "durationSeconds": 900,
            "distanceValue": 3.2,
            "distanceUnit": "KM"
          }
        ]
      }
    ]
  }'
```

#### Response (201 Created)
```json
{
  "sessionId": "yyyyyyyy-yyyy-yyyy-yyyy-yyyyyyyyyyyy",
  "sessionType": "FREE",
  "programSessionId": null,
  "sessionDate": "2026-04-27",
  "name": "Off-Program Workout",
  "notes": "Tried tire flips today",
  "totalDurationSeconds": 1800,
  "feelings": {
    "rating": 7,
    "comment": "Fun but tiring"
  },
  "exerciseEntries": [
    {
      "exerciseName": "Bench Press",
      "exerciseType": "STRENGTH",
      "sets": [
        {
          "reps": 10,
          "isBodyWeight": false,
          "weightValue": 60.0,
          "weightUnit": "KG"
        }
      ],
      "cardioLaps": []
    }
  ]
}
```

#### Validation Errors (400 Bad Request)
```json
{
  "message": "Validation failed: exerciseEntries cannot be empty",
  "code": "VALIDATION_ERROR"
}
```

#### Permission Errors (403 Forbidden)
```json
{
  "message": "Cannot log program session: not the next uncompleted session",
  "code": "FORBIDDEN"
}
```

---

### 3. Get Session History

Retrieve paginated history of logged sessions with optional filtering.

**Endpoint**: `GET /logged-sessions/history`  
**Auth**: Required  
**Query Parameters**:
- `page` (int, optional, default: 0) — Page number (0-indexed)
- `size` (int, optional, default: 20, max: 100) — Results per page
- `dateFrom` (string, optional, format: YYYY-MM-DD) — Filter from date (inclusive)
- `dateTo` (string, optional, format: YYYY-MM-DD) — Filter to date (inclusive)
- `exerciseName` (string, optional) — Filter by exercise name (partial match)

**Response**: `200 OK` with `SessionHistoryPage`

#### Request (All Sessions, Paginated)
```bash
curl -u user1:password1 "http://localhost:8080/api/logged-sessions/history?page=0&size=10"
```

#### Request (Date Range Filter)
```bash
curl -u user1:password1 "http://localhost:8080/api/logged-sessions/history?dateFrom=2026-04-01&dateTo=2026-04-30"
```

#### Request (Exercise Name Filter)
```bash
curl -u user1:password1 "http://localhost:8080/api/logged-sessions/history?exerciseName=Bench%20Press"
```

#### Request (Combined Filters)
```bash
curl -u user1:password1 "http://localhost:8080/api/logged-sessions/history?page=0&size=20&dateFrom=2026-04-01&dateTo=2026-04-30&exerciseName=Deadlift"
```

#### Response (200 OK)
```json
{
  "items": [
    {
      "sessionId": "zzzzzzzz-zzzz-zzzz-zzzz-zzzzzzzzzzzz",
      "sessionDate": "2026-04-27",
      "sessionType": "FREE",
      "exerciseCount": 3,
      "totalDurationSeconds": 1800
    },
    {
      "sessionId": "yyyyyyyy-yyyy-yyyy-yyyy-yyyyyyyyyyyy",
      "sessionDate": "2026-04-26",
      "sessionType": "PROGRAM",
      "exerciseCount": 2,
      "totalDurationSeconds": 2400
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 25
}
```

---

### 4. Get Session Details

Retrieve full details (with all exercises, sets, and feelings) for a specific session.

**Endpoint**: `GET /logged-sessions/{sessionId}`  
**Auth**: Required  
**Path Parameters**:
- `sessionId` (UUID) — Session ID

**Response**: `200 OK` with `LoggedSessionDetail`, `404 Not Found`, or `403 Forbidden`

#### Request
```bash
curl -u user1:password1 "http://localhost:8080/api/logged-sessions/yyyyyyyy-yyyy-yyyy-yyyy-yyyyyyyyyyyy"
```

#### Response (200 OK)
```json
{
  "sessionId": "yyyyyyyy-yyyy-yyyy-yyyy-yyyyyyyyyyyy",
  "sessionType": "PROGRAM",
  "programSessionId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "sessionDate": "2026-04-26",
  "name": null,
  "notes": "Felt strong today",
  "totalDurationSeconds": 2400,
  "feelings": {
    "rating": 8,
    "comment": "Great workout, energy was high"
  },
  "exerciseEntries": [
    {
      "exerciseName": "Bench Press",
      "exerciseType": "STRENGTH",
      "sets": [
        {
          "reps": 8,
          "isBodyWeight": false,
          "weightValue": 70.0,
          "weightUnit": "KG"
        },
        {
          "reps": 8,
          "isBodyWeight": false,
          "weightValue": 70.0,
          "weightUnit": "KG"
        },
        {
          "reps": 6,
          "isBodyWeight": false,
          "weightValue": 65.0,
          "weightUnit": "KG"
        }
      ],
      "cardioLaps": []
    },
    {
      "exerciseName": "Running",
      "exerciseType": "CARDIO",
      "sets": [],
      "cardioLaps": [
        {
          "durationSeconds": 600,
          "distanceValue": 5.0,
          "distanceUnit": "KM"
        }
      ]
    }
  ]
}
```

#### 404 Not Found
```json
{
  "message": "Session not found",
  "code": "NOT_FOUND"
}
```

#### 403 Forbidden (Cross-User Access)
```json
{
  "message": "Access denied: this session belongs to another user",
  "code": "FORBIDDEN"
}
```

---

### 5. Get Exercise Progression

Retrieve progression data points for an exercise across all user's sessions.

**Endpoint**: `GET /progression/{exerciseName}`  
**Auth**: Required  
**Path Parameters**:
- `exerciseName` (string) — Exercise name (URL-encoded)

**Response**: `200 OK` with `ProgressionResponse` (may have empty points if no data)

#### Request
```bash
curl -u user1:password1 "http://localhost:8080/api/progression/Deadlift"
```

#### Request (URL-Encoded Name)
```bash
curl -u user1:password1 "http://localhost:8080/api/progression/Bench%20Press"
```

#### Response (200 OK)
```json
{
  "exerciseName": "Deadlift",
  "points": [
    {
      "sessionId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
      "sessionDate": "2026-03-15",
      "metricType": "WEIGHT",
      "metricValue": 100.0
    },
    {
      "sessionId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
      "sessionDate": "2026-03-22",
      "metricType": "WEIGHT",
      "metricValue": 105.0
    },
    {
      "sessionId": "cccccccc-cccc-cccc-cccc-cccccccccccc",
      "sessionDate": "2026-03-29",
      "metricType": "WEIGHT",
      "metricValue": 110.0
    },
    {
      "sessionId": "dddddddd-dddd-dddd-dddd-dddddddddddd",
      "sessionDate": "2026-04-05",
      "metricType": "WEIGHT",
      "metricValue": 115.0
    }
  ]
}
```

#### Response (200 OK, No Data)
```json
{
  "exerciseName": "Unknown Exercise",
  "points": []
}
```

---

### 6. Search Exercise Library

Get exercises from the library (for free session creation).

**Endpoint**: `GET /exercises`  
**Auth**: Required  
**Query Parameters**:
- `query` (string, optional) — Search term to filter exercises by name
- (No query param returns top 50 most-used exercises)

**Response**: `200 OK` with list of `Exercise`

#### Request (Top Exercises)
```bash
curl -u user1:password1 "http://localhost:8080/api/exercises"
```

#### Request (Search)
```bash
curl -u user1:password1 "http://localhost:8080/api/exercises?query=bench"
```

#### Response (200 OK)
```json
[
  {
    "id": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "name": "Bench Press",
    "category": "Chest",
    "type": "STRENGTH",
    "description": "Barbell bench press on flat bench",
    "isActive": true
  },
  {
    "id": "yyyyyyyy-yyyy-yyyy-yyyy-yyyyyyyyyyyy",
    "name": "Incline Bench Press",
    "category": "Chest",
    "type": "STRENGTH",
    "description": "Incline dumbbell bench press",
    "isActive": true
  }
]
```

---

## Data Types & Enums

### SessionType
- `PROGRAM` — Session following a predefined program
- `FREE` — Freestyle session without predefined targets

### ExerciseType
- `STRENGTH` — Weight training exercise (use `sets` with reps and weight)
- `BODYWEIGHT` — Bodyweight exercise (use `sets` with reps, no weight)
- `CARDIO` — Cardiovascular exercise (use `cardioLaps` with duration and optional distance)

### MetricType (Progression)
- `WEIGHT` — Weight lifted (kg or lbs)
- `DURATION` — Time spent (seconds, typically for cardio)
- `DISTANCE` — Distance covered (km or miles)

### WeightUnit
- `KG` — Kilograms
- `LBS` — Pounds

### DistanceUnit
- `KM` — Kilometers
- `MI` — Miles

---

## Constraints & Validation Rules

1. **Session Type Rules**:
   - `PROGRAM` sessions must include valid `programSessionId` (must be next uncompleted)
   - `FREE` sessions must have `programSessionId` as null

2. **Exercise Rules**:
   - Each session must have at least 1 exercise
   - STRENGTH/BODYWEIGHT exercises must have at least 1 set
   - CARDIO exercises must have at least 1 lap

3. **Set Rules**:
   - `reps` must be >= 1
   - For non-bodyweight sets: `weightValue` must be provided
   - For bodyweight sets: `weightValue` must be null

4. **Feelings Rules**:
   - `rating` must be between 1 and 10 (required)
   - `comment` is optional (max 1000 chars)

5. **User Isolation**:
   - All endpoints enforce strict per-user data access
   - Attempting to access another user's data returns `403 Forbidden`

---

## JavaScript Integration Example

```javascript
import axios from 'axios';

// Create authenticated API client
const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  auth: {
    username: 'user1',
    password: 'password1'
  }
});

// Get next program session
async function getNextSession() {
  try {
    const response = await api.get('/program-sessions/next');
    if (response.status === 200) {
      console.log('Next session:', response.data);
    } else if (response.status === 204) {
      console.log('No active program');
    }
  } catch (error) {
    console.error('Error:', error.response?.data || error.message);
  }
}

// Log a session
async function logSession(sessionData) {
  try {
    const response = await api.post('/logged-sessions', sessionData);
    console.log('Session saved:', response.data);
    return response.data.sessionId;
  } catch (error) {
    console.error('Validation error:', error.response?.data);
  }
}

// Get history
async function getHistory(filters = {}) {
  try {
    const response = await api.get('/logged-sessions/history', { params: filters });
    console.log('History:', response.data);
  } catch (error) {
    console.error('Error:', error.response?.data);
  }
}

// Get progression
async function getProgression(exerciseName) {
  try {
    const response = await api.get(`/progression/${encodeURIComponent(exerciseName)}`);
    console.log('Progression:', response.data);
  } catch (error) {
    console.error('Error:', error.response?.data);
  }
}
```

