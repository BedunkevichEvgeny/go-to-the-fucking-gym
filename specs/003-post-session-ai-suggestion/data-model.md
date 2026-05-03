# Data Model: Post-Session AI Suggestion

**Feature**: `003-post-session-ai-suggestion`  
**Date**: 2026-05-03

## Entities

### New: `SessionAiSuggestion`

| Attribute | Type | Constraints | Notes |
|---|---|---|---|
| `sessionId` | UUID | PK, FK → `logged_sessions.id`, NOT NULL, immutable | Derived from `@MapsId` — session_id is both PK and FK |
| `suggestion` | String (TEXT) | NOT NULL | Plain text AI coaching output; never empty |
| `generatedAt` | OffsetDateTime | NOT NULL, default now() | Set via `@PrePersist`; read-only after insert |

**Business rules**:
- Exactly one row per program session (FK is PK → DB-enforced uniqueness).
- Once written, the row is never updated or deleted (immutable by convention + application guard).
- Only exists for `SessionType.PROGRAM` sessions — never created for FREE sessions.
- `suggestion` must be non-blank (validated in `AiHandoffService` before persist).

**JPA mapping**:
```java
@Entity
@Table(name = "session_ai_suggestions")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SessionAiSuggestion {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID sessionId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id")
    private LoggedSession session;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String suggestion;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private OffsetDateTime generatedAt;

    @PrePersist
    void prePersist() {
        if (generatedAt == null) {
            generatedAt = OffsetDateTime.now();
        }
    }
}
```

---

### Modified: `LoggedSession`

New association added:

```java
@OneToOne(mappedBy = "session", fetch = FetchType.LAZY, optional = true)
private SessionAiSuggestion aiSuggestion;
```

| Change | Detail |
|---|---|
| New field | `aiSuggestion: SessionAiSuggestion` (nullable, lazy) |
| Cascade | None — `SessionAiSuggestion` is managed independently by `AiHandoffService` |
| OrphanRemoval | Not applicable — suggestion outlives no parent; it is inserted independently |

---

### Modified: `LoggedSessionDetail` (API DTO)

New field:

| Field | Type | Nullable | Notes |
|---|---|---|---|
| `aiSuggestion` | String | Yes | `null` when suggestion not yet generated or for FREE sessions |

`SessionHistoryItem` is **not modified** — no `aiSuggestion` field.

---

## New Repository

### `SessionAiSuggestionRepository`

```java
public interface SessionAiSuggestionRepository extends JpaRepository<SessionAiSuggestion, UUID> {
    // existsById(UUID) inherited from JpaRepository — used for immutability guard
    // save(SessionAiSuggestion) inherited from JpaRepository — used for persistence
}
```

No custom query methods needed.

---

## Database Migration

**File**: `backend/src/main/resources/db/migration/V003__session_ai_suggestion.sql`

```sql
CREATE TABLE session_ai_suggestions (
    session_id   UUID        NOT NULL,
    suggestion   TEXT        NOT NULL,
    generated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_session_ai_suggestions PRIMARY KEY (session_id),
    CONSTRAINT fk_session_ai_suggestions_session
        FOREIGN KEY (session_id) REFERENCES logged_sessions (id)
);
```

---

## Entity Relationship

```
logged_sessions ──────────────── session_ai_suggestions
     (id)         1         0..1       (session_id FK=PK)
                  ──────────────
                  Optional one-to-one
                  User data         AI-generated data
```

---

## Frontend Type Changes

**File**: `frontend/src/types/api.ts`

```typescript
// Before
export interface LoggedSessionDetail {
  sessionId: string;
  sessionType: SessionType;
  programSessionId?: string | null;
  sessionDate: string;
  name?: string;
  notes?: string;
  totalDurationSeconds?: number | null;
  feelings: SessionFeelingsInput;
  exerciseEntries: ExerciseEntryView[];
}

// After (add one field)
export interface LoggedSessionDetail {
  sessionId: string;
  sessionType: SessionType;
  programSessionId?: string | null;
  sessionDate: string;
  name?: string;
  notes?: string;
  totalDurationSeconds?: number | null;
  feelings: SessionFeelingsInput;
  exerciseEntries: ExerciseEntryView[];
  aiSuggestion?: string | null;  // NEW
}
```

`SessionHistoryItem` interface is **not changed**.

---

## Validation Rules

| Rule | Where enforced |
|---|---|
| Suggestion must be non-blank | `AiHandoffService` (existing guard before calling `persistSuggestion`) |
| Suggestion is immutable once stored | `AiHandoffService.persistSuggestion` checks `existsById` before write |
| Only PROGRAM sessions get a suggestion | `AiHandoffService.enqueueSessionForAiAnalysis` early return for non-PROGRAM type |
| `session_id` uniqueness | PostgreSQL PK constraint |

---

## State Transitions

```
Program session saved
    │
    ▼
AiHandoffService enqueues async task
    │
    ├── [AI fails / blank response]
    │       → no row created; session detail aiSuggestion = null
    │
    └── [AI succeeds]
            ├── [suggestion row already exists] → skip (immutability guard)
            └── [no existing row] → INSERT session_ai_suggestions
                    → session detail aiSuggestion = <text>
```

