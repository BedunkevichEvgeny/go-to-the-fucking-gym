# API Contract: Session Detail — AI Suggestion Field

**Feature**: `003-post-session-ai-suggestion`  
**Date**: 2026-05-03

## Modified Endpoint

### `GET /api/logged-sessions/{sessionId}`

**Authentication**: HTTP Basic (existing)  
**Authorization**: Session must belong to the authenticated user (existing ownership check)

#### Path Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `sessionId` | UUID | Yes | Identifier of the logged session |

#### Response: 200 OK

```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "sessionType": "PROGRAM",
  "programSessionId": "550e8400-e29b-41d4-a716-446655440001",
  "sessionDate": "2026-05-03",
  "name": "Session 1 — Upper Body",
  "notes": null,
  "totalDurationSeconds": 3600,
  "feelings": {
    "rating": 8,
    "comment": "Felt strong today"
  },
  "exerciseEntries": [
    {
      "exerciseId": "550e8400-e29b-41d4-a716-446655440002",
      "customExerciseName": null,
      "exerciseName": "Bench Press",
      "exerciseType": "STRENGTH",
      "sets": [
        {
          "setOrder": 1,
          "reps": 8,
          "weightValue": 80.0,
          "weightUnit": "KG",
          "isBodyWeight": false,
          "durationSeconds": null,
          "restSeconds": null
        }
      ],
      "cardioLaps": []
    }
  ],
  "aiSuggestion": "Great bench press session! You hit your target reps at 80 kg. Consider increasing to 82.5 kg next session to continue progressive overload."
}
```

#### `aiSuggestion` Field Semantics

| Value | Meaning |
|---|---|
| Non-empty string | AI suggestion successfully generated and stored |
| `null` | Suggestion not yet generated, AI service failed, or session is FREE type |

**Consumer responsibilities**:
- Frontend MUST handle `null` gracefully — render nothing in the suggestion area.
- Frontend MUST NOT assume a non-null value immediately after a POST — suggestion is generated asynchronously.
- Frontend MUST poll this endpoint after session save to detect when `aiSuggestion` becomes non-null.

---

## Unchanged Endpoint

### `GET /api/logged-sessions` (history list)

**Response shape**: unchanged. `SessionHistoryItem` does **not** include `aiSuggestion`.

```json
{
  "items": [
    {
      "sessionId": "550e8400-e29b-41d4-a716-446655440000",
      "sessionDate": "2026-05-03",
      "sessionType": "PROGRAM",
      "exerciseCount": 4,
      "totalDurationSeconds": 3600,
      "name": "Session 1 — Upper Body"
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 1
}
```

---

## Modified Endpoint (backward-compatible)

### `POST /api/logged-sessions`

**Response**: `LoggedSessionDetail` — same structure as `GET /api/logged-sessions/{sessionId}`.

The `aiSuggestion` field is **always `null`** in the POST response because AI generation is asynchronous and has not yet completed at the time the 201 response is returned.

Frontend consumers MUST NOT use the `aiSuggestion` value from the POST response. They MUST poll the GET endpoint instead.

---

## Error Responses (unchanged)

| Status | Scenario |
|---|---|
| 401 | No or invalid authentication |
| 403 | Session belongs to another user |
| 404 | Session not found |

