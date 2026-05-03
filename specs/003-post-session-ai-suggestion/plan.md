# Implementation Plan: Post-Session AI Suggestion

**Branch**: `003-post-session-ai-suggestion` | **Date**: 2026-05-03 | **Spec**: `specs/003-post-session-ai-suggestion/spec.md`  
**Input**: Feature specification from `specs/003-post-session-ai-suggestion/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

Wire the existing but unused AI coaching output from `AiHandoffService` (which already calls `LangChainSessionProcessor.process()` and logs the result) into persistent storage and expose it through the API so users can see it in the frontend immediately after saving a program session and again when revisiting that session in history.

The change is a thin vertical slice:
1. **DB**: Add a new `session_ai_suggestions` table (Flyway `V003__session_ai_suggestion.sql`) linked one-to-one with `logged_sessions`.
2. **Backend domain**: New `SessionAiSuggestion` JPA entity + repository; `LoggedSession` gains a navigable lazy `@OneToOne` association.
3. **Backend service**: `AiHandoffService.enqueueSessionForAiAnalysis` persists the suggestion text on success instead of logging it; skips write if a suggestion already exists (immutability guard).
4. **Backend DTO/mapper**: `LoggedSessionDetail` gains a nullable `aiSuggestion` string field; `SessionHistoryItem` is unchanged.
5. **Frontend types**: `LoggedSessionDetail` interface gains `aiSuggestion?: string | null`.
6. **Frontend UX**: `ProgramSessionPage` polls the detail endpoint after save until `aiSuggestion` is non-null (or times out). `SessionDetailView` renders the suggestion in a clearly labelled card when present; renders nothing when absent.

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.x (frontend)  
**Primary Dependencies**: Spring Boot 4.0.5, Spring Data JPA, Flyway, LangChain4j (existing), React 18, TanStack Query  
**Storage**: PostgreSQL 16 — new `session_ai_suggestions` table via Flyway migration `V003__session_ai_suggestion.sql`  
**Testing**: JUnit 5, Spring Boot Test, Testcontainers (PostgreSQL), MockMvc, React Testing Library + Vitest  
**Target Platform**: Linux container (backend), evergreen browsers (frontend)  
**Project Type**: Web application (Spring Boot backend + React frontend)  
**Performance Goals**: Session save response time must not increase; AI suggestion must appear within 15 s in 95% of successful cases (SC-001); suggestion generation is fully async (NF-001)  
**Constraints**: Simple explicit logic (no event bus, no saga, no outbox); suggestion immutable once stored; free sessions excluded; nullable DB column required for sessions predating this feature; all code passes checkstyle before commit  
**Scale/Scope**: MVP — one suggestion per program session, plain text, no user interaction beyond reading

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] Scope is a thin vertical slice that supports fast prototyping.
- [x] Design favors simple, explicit code and avoids tricky abstractions.
- [x] All business logic is mapped to unit and integration tests.
- [x] Backend implementation uses Java 21 and Spring Boot best practices.
- [x] Frontend implementation uses React and shared UX conventions.
- [x] Planned work can be decomposed into discrete tasks that support issue tracking,
  one-task-per-commit delivery, and post-merge closure review.
- [x] Plan and linked artifacts are written in English.

### Post-Design Re-Check

- [x] Design artifacts maintain thin-slice scope — no new abstractions beyond a single entity + repository.
- [x] No constitution violations introduced; no exceptions required.

## Project Structure

### Documentation (this feature)

```text
specs/003-post-session-ai-suggestion/
├── plan.md              ← this file
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── session-detail-api.md
└── tasks.md             (created by /speckit.tasks)
```

### Source Code (this feature)

```text
backend/
├── src/main/resources/db/migration/
│   └── V003__session_ai_suggestion.sql          NEW
├── src/main/java/com/gymtracker/
│   ├── domain/
│   │   └── SessionAiSuggestion.java             NEW
│   ├── infrastructure/
│   │   ├── repository/
│   │   │   └── SessionAiSuggestionRepository.java  NEW
│   │   ├── ai/
│   │   │   └── AiHandoffService.java            MODIFIED (persist instead of log)
│   │   └── mapper/
│   │       └── DtoMapper.java                   MODIFIED (map suggestion field)
│   └── api/dto/
│       └── LoggedSessionDetail.java             MODIFIED (add aiSuggestion field)
└── src/test/java/com/gymtracker/
    ├── application/
    │   └── AiHandoffServiceTest.java            NEW unit tests
    └── integration/
        ├── LoggedSessionServiceIT.java          MODIFIED (suggestion persistence)
        └── SessionDetailServiceIT.java          MODIFIED (suggestion in detail response)

frontend/
└── src/
    ├── types/
    │   └── api.ts                              MODIFIED (add aiSuggestion)
    ├── hooks/
    │   ├── useSessionDetail.ts                 UNCHANGED
    │   └── usePollSessionSuggestion.ts         NEW
    ├── features/history/
    │   └── SessionDetailView.tsx               MODIFIED (render suggestion card)
    └── pages/
        └── ProgramSessionPage.tsx              MODIFIED (poll after save, show result)
```

**Structure Decision**: Web application (Option 2). Existing layout preserved; no new top-level directories.

## Architecture Decisions

### AD-001: Separate `session_ai_suggestions` table (not a column on `logged_sessions`)

**Decision**: Store the AI suggestion in a dedicated `session_ai_suggestions` table with a FK to `logged_sessions`, mapped as a `@OneToOne(optional = true)` association.

**Rationale**:
- Keeps `logged_sessions` focused on user-entered data (FR-002 states suggestion must be stored *separately*).
- Nullable one-to-one relationship naturally represents "no suggestion yet" without a nullable TEXT column polluting the main table.
- Immutability is enforced at the DB level: the `session_id` FK is also the PK (no auto-increment); once a row exists, a second insert fails on the PK constraint.
- Clean separation of user data and AI-generated data.

**Alternatives rejected**:
- Adding `ai_suggestion TEXT` to `logged_sessions`: simpler but mixes concerns and widens the primary session table unnecessarily.
- A generic key-value metadata table: over-engineered for a single typed field.

### AD-002: Frontend polls `GET /api/logged-sessions/{sessionId}` after save

**Decision**: After `POST /api/logged-sessions` returns 201, `ProgramSessionPage` immediately starts polling the detail endpoint every 3 seconds for up to 15 seconds. It stops on the first response where `aiSuggestion` is non-null or on timeout.

**Rationale**:
- AI generation is async (NF-001); the POST response cannot include the suggestion.
- Polling against the existing detail endpoint requires no new endpoint and no WebSocket or SSE.
- 15 s matches SC-001 (95% of suggestions arrive within 15 s).
- On timeout the user sees a graceful fallback and can still navigate; the suggestion will appear on their next visit (FR-007, FR-006).

**Alternatives rejected**:
- SSE / WebSocket: higher server complexity for a simple MVP.
- Include suggestion in POST response body: impossible because generation is async and completes after the HTTP response is sent.

### AD-003: Immutability enforced at both application layer and DB level

**Decision**: `AiHandoffService` calls `sessionAiSuggestionRepository.existsById(sessionId)` before writing. If a row already exists, the write is silently skipped. Additionally, the DB PK constraint on `session_id` provides a safety net.

**Rationale**: Satisfies FR-003 and SC-002. The application-layer check avoids a `DataIntegrityViolationException` and makes the intent explicit in the code.

### AD-004: `AiHandoffService` injects `SessionAiSuggestionRepository` directly

**Decision**: No new service layer is introduced. `AiHandoffService` writes the suggestion entity directly via the new repository.

**Rationale**: Adding a thin `SuggestionPersistenceService` wrapping a single `save()` call would be an unnecessary abstraction (constitution principle II). The persistence logic is three lines.

### AD-005: `LoggedSession.aiSuggestion` association is `FetchType.LAZY`

**Decision**: The `@OneToOne` association from `LoggedSession` to `SessionAiSuggestion` uses `fetch = FetchType.LAZY, optional = true`.

**Rationale**: History list queries (`toHistoryItem`) must not trigger an extra JOIN or secondary SELECT for the suggestion (FR-009). `toDetailDto` accesses the association explicitly and triggers a lazy load only in that path.

## Data Model Changes

### New DB table: `session_ai_suggestions`

```sql
-- V003__session_ai_suggestion.sql
CREATE TABLE session_ai_suggestions (
    session_id   UUID        NOT NULL,
    suggestion   TEXT        NOT NULL,
    generated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_session_ai_suggestions PRIMARY KEY (session_id),
    CONSTRAINT fk_session_ai_suggestions_session
        FOREIGN KEY (session_id) REFERENCES logged_sessions (id)
);
```

Key design choices:
- `session_id` is the PK — enforces exactly-one-per-session at DB level.
- `suggestion` is `NOT NULL TEXT` — empty strings are rejected at the application layer before the row is written (existing guard in `AiHandoffService`).
- `generated_at` defaults to `now()` for observability.

### New JPA entity: `SessionAiSuggestion`

Fields: `sessionId` (UUID, PK/FK), `suggestion` (TEXT, non-null), `generatedAt` (OffsetDateTime, non-null).  
Association: `@OneToOne @MapsId` back to `LoggedSession`.  
`@PrePersist` sets `generatedAt = OffsetDateTime.now()` if null.

### `LoggedSession` association (MODIFIED)

Add:
```java
@OneToOne(mappedBy = "session", fetch = FetchType.LAZY, optional = true)
private SessionAiSuggestion aiSuggestion;
```

### `LoggedSessionDetail` DTO (MODIFIED)

New field added as last component of the record:
```java
String aiSuggestion  // nullable; null when not yet generated or N/A (free session)
```

`SessionHistoryItem` is **not** changed.

### Frontend type change: `LoggedSessionDetail` (MODIFIED)

```typescript
export interface LoggedSessionDetail {
  // ...existing fields unchanged...
  aiSuggestion?: string | null;
}
```

## Backend Design

### `AiHandoffService` changes (MODIFIED)

Current `whenComplete` handler logs the response. New behaviour: persist on success, silently skip if suggestion already exists.

```java
private void persistSuggestion(UUID sessionId, String suggestion) {
    if (sessionAiSuggestionRepository.existsById(sessionId)) {
        log.debug("Suggestion already stored for session {}; skipping.", sessionId);
        return;
    }
    SessionAiSuggestion entity = new SessionAiSuggestion();
    entity.setSession(loggedSessionRepository.getReferenceById(sessionId));
    entity.setSuggestion(suggestion);
    sessionAiSuggestionRepository.save(entity);
    log.info("AI suggestion stored for session {}", sessionId);
}
```

`AiHandoffService` gains two new constructor-injected dependencies:
- `SessionAiSuggestionRepository`
- `LoggedSessionRepository` (already in the codebase; used for `getReferenceById` — no DB hit, just an entity proxy)

### `DtoMapper.toDetailDto` changes (MODIFIED)

```java
String suggestion = loggedSession.getAiSuggestion() == null
        ? null
        : loggedSession.getAiSuggestion().getSuggestion();
// pass as last argument to LoggedSessionDetail constructor
```

The `SessionDetailService.getSessionDetails` method is already `@Transactional(readOnly = true)`, so accessing the lazy `@OneToOne` within that transaction triggers a secondary SELECT automatically. No query change needed.

For `toHistoryItem`, `loggedSession.getAiSuggestion()` is never accessed, so no extra query is issued.

## Frontend Design

### New hook: `usePollSessionSuggestion`

`frontend/src/hooks/usePollSessionSuggestion.ts`:

- Accepts `sessionId: string | null` and options `{ maxWaitMs = 15000, intervalMs = 3000 }`.
- Returns `{ suggestion: string | null, timedOut: boolean, isPolling: boolean, startPolling: () => void }`.
- On `startPolling()`: sets a `setInterval` that calls `GET /api/logged-sessions/{sessionId}` every `intervalMs` ms.
- Stops (clears interval) when `aiSuggestion` is non-null or when `maxWaitMs` elapsed.
- Sets `timedOut = true` if `maxWaitMs` elapsed without a suggestion.
- Cleans up interval on unmount.

### `ProgramSessionPage` flow (MODIFIED)

New post-save flow (replaces immediate `navigate('/history')`):

```
User submits form
  → POST /api/logged-sessions (201) → sessionId in response
  → savedSessionId state = sessionId
  → startPolling(savedSessionId)
  → Render: session saved confirmation card + AI Coaching Insight card
      - While isPolling: spinner + "Generating your coaching insight…"
      - On suggestion: suggestion text displayed
      - On timedOut: "Coaching insight unavailable right now. Check back in session history."
  → "Continue to History" button always visible (not blocked by AI status)
      → onClick: navigate('/history')
```

### `SessionDetailView` — AI Coaching Insight section (MODIFIED)

Added after the session info card, before the exercise list:

```tsx
{data.sessionType === 'PROGRAM' && data.aiSuggestion && (
  <section className="card stack-sm" aria-label="AI Coaching Insight">
    <p className="eyebrow">AI Coaching Insight</p>
    <p>{data.aiSuggestion}</p>
  </section>
)}
```

- Renders only when `sessionType === 'PROGRAM'` and `aiSuggestion` is non-null/non-empty (FR-008).
- Uses same `card stack-sm` pattern as other sections (UX-001).
- `eyebrow` class matches existing labelling conventions (UX-002).
- Free sessions and sessions without a suggestion: section is absent (FR-008, Story 2 AC-3).

## API Contract

### `GET /api/logged-sessions/{sessionId}` — MODIFIED response

New `aiSuggestion` field added. Value is `null` when not yet generated or when session type is FREE.

See `specs/003-post-session-ai-suggestion/contracts/session-detail-api.md` for full contract.

### `GET /api/logged-sessions` (history list) — UNCHANGED

`SessionHistoryItem` response shape does not change.

### `POST /api/logged-sessions` — response shape MODIFIED (backward-compatible)

The POST response (`LoggedSessionDetail`) now includes `aiSuggestion`, but it is always `null` at creation time. The frontend ignores this field on the POST response and polls the GET endpoint instead.

## Test Plan

### Backend unit tests

| Test class | Scenario |
|---|---|
| `AiHandoffServiceTest` | PROGRAM session: suggestion is persisted on successful AI response |
| `AiHandoffServiceTest` | PROGRAM session: blank/null AI response is not persisted (existing guard) |
| `AiHandoffServiceTest` | FREE session: returns immediately, no repository call |
| `AiHandoffServiceTest` | Suggestion already exists: `existsById` true → no second write |
| `DtoMapperTest` | `toDetailDto` with non-null `aiSuggestion` → DTO includes suggestion text |
| `DtoMapperTest` | `toDetailDto` with null `aiSuggestion` → DTO `aiSuggestion` is null |
| `DtoMapperTest` | `toHistoryItem` → DTO does not contain `aiSuggestion` field |

### Backend integration tests

| Test class | Scenario |
|---|---|
| `LoggedSessionServiceIT` | Save program session → suggestion row created in DB after async step |
| `LoggedSessionServiceIT` | Save program session twice → suggestion unchanged (immutability) |
| `LoggedSessionServiceIT` | Save free session → no `session_ai_suggestions` row created |
| `SessionDetailServiceIT` | GET detail with suggestion → `aiSuggestion` field populated |
| `SessionDetailServiceIT` | GET detail without suggestion → `aiSuggestion` is null |
| `SessionHistoryServiceIT` | GET history list → items do not include `aiSuggestion` |

### Frontend unit tests

| Test | Assertion |
|---|---|
| `SessionDetailView` — PROGRAM + suggestion | "AI Coaching Insight" section and text rendered |
| `SessionDetailView` — PROGRAM + null suggestion | No "AI Coaching Insight" section |
| `SessionDetailView` — FREE session | No "AI Coaching Insight" section regardless of `aiSuggestion` value |
| `ProgramSessionPage` — after save | Loading indicator shown while polling |
| `ProgramSessionPage` — suggestion arrives | Suggestion text rendered, loading gone |
| `ProgramSessionPage` — poll timeout | Fallback message shown, "Continue to History" enabled |

## Complexity Tracking

No constitution violations. All Constitution Check items are satisfied. No exceptions required.

## Research Notes

All open questions resolved:

| Question | Resolution |
|---|---|
| Flyway version number | `V003__session_ai_suggestion.sql` — follows `V002__profile_goal_onboarding.sql` |
| Does `SessionDetailService` need a transaction change? | No. `@Transactional(readOnly = true)` already wraps the call; the lazy `@OneToOne` load fires within the same transaction automatically. |
| Does the history list query accidentally fetch suggestions? | No — `toHistoryItem` never accesses `getAiSuggestion()`, so the lazy association is never initialised. |
| Is the `CompletableFuture` in `AiHandoffService` in the original Spring transaction? | No — it runs on `aiTaskExecutor` outside the original transaction. The persistence call in `persistSuggestion` executes in its own transaction. This is correct per NF-002. |
| Polling vs SSE | Polling with TanStack Query interval is simpler for this MVP scope and aligns with existing query patterns in the codebase. |
