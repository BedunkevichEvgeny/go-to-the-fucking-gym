# Research: Post-Session AI Suggestion

**Feature**: `003-post-session-ai-suggestion`  
**Date**: 2026-05-03

## Resolved Decisions

### RD-001: Flyway Migration Naming

**Decision**: `V003__session_ai_suggestion.sql`  
**Rationale**: Follows existing convention (`V001__initial_schema.sql`, `V002__profile_goal_onboarding.sql`). The `VNNN__description.sql` pattern uses zero-padded three-digit version numbers.  
**Alternatives considered**: None — convention is fixed.

---

### RD-002: Where to Store the Suggestion

**Decision**: Separate table `session_ai_suggestions` with `session_id UUID PRIMARY KEY REFERENCES logged_sessions(id)`.  
**Rationale**:
- FR-002 requires the suggestion to be stored *separately* from user-entered data.
- Using `session_id` as the PK provides a DB-level uniqueness guard (FR-003).
- Avoids widening `logged_sessions` with AI-specific columns.
- Optional one-to-one is naturally nullable — sessions predating this feature simply have no row.

**Alternatives considered**:
- Column `ai_suggestion TEXT` on `logged_sessions`: simpler schema change but mixes concerns and requires handling `NULL` on a table that otherwise avoids nulls.
- Separate entity with generated PK + unique constraint on `session_id`: equivalent safety but unnecessary surrogate key.

---

### RD-003: JPA Fetch Strategy for the `aiSuggestion` Association

**Decision**: `FetchType.LAZY` on the `@OneToOne` from `LoggedSession` to `SessionAiSuggestion`.  
**Rationale**: The history list path (`toHistoryItem`) must not load the suggestion (FR-009). With Hibernate, `@OneToOne` optional associations are not truly lazy without bytecode enhancement or proxy workarounds, but since `LoggedSession` has `optional = true` and `mappedBy`, Hibernate can use a proxy. For the detail path, the association is accessed within a `@Transactional(readOnly = true)` block, so the second SELECT fires correctly.  
**Alternatives considered**: Eager fetch — rejected because it forces a JOIN/secondary SELECT on every history list load.

---

### RD-004: Immutability Enforcement Strategy

**Decision**: Application-layer check (`existsById`) + DB-level PK uniqueness guard.  
**Rationale**: The application check makes intent explicit and avoids catching `DataIntegrityViolationException`. The DB PK provides a safety net for concurrent writes from two parallel AI tasks for the same session (edge case from spec).  
**Alternatives considered**: Only DB constraint — would require exception-catching code, which is less readable.

---

### RD-005: Frontend Polling vs SSE vs Long-Poll

**Decision**: Client-side polling every 3 seconds for up to 15 seconds using a custom `usePollSessionSuggestion` hook that calls the existing `GET /api/logged-sessions/{sessionId}` endpoint.  
**Rationale**:
- No new server endpoint needed.
- Aligns with existing TanStack Query usage in the codebase.
- 3 s interval × 5 attempts = 15 s total, matching SC-001.
- Simple to test with fake timers in Vitest.

**Alternatives considered**:
- Server-Sent Events: requires a new `/stream` endpoint, CORS configuration changes, and higher server-side complexity.
- WebSocket: overkill for a single-use case.
- TanStack Query `refetchInterval`: viable but couples polling to the stable session detail query cache; a separate hook keeps concerns cleaner.

---

### RD-006: `AiHandoffService` Transaction Boundary for Persistence

**Decision**: The `persistSuggestion` call inside `CompletableFuture.whenComplete` runs outside the original session-save transaction (which has already committed). The `sessionAiSuggestionRepository.save()` call runs in its own auto-created transaction (Spring `@Transactional(REQUIRED)` from SimpleJpaRepository).  
**Rationale**: NF-002 requires suggestion storage to be decoupled from the session save transaction. This is already true because `CompletableFuture` executes on the `aiTaskExecutor` thread pool, not the HTTP thread. No additional transaction management is needed.  
**Risk**: If the DB is unavailable at the time of the async write, the suggestion is lost. Acceptable for MVP (spec does not require retry logic).

---

### RD-007: `LoggedSessionRepository.getReferenceById` Usage

**Decision**: Use `loggedSessionRepository.getReferenceById(sessionId)` to set the `session` association on the new `SessionAiSuggestion` entity before saving.  
**Rationale**: `getReferenceById` returns a Hibernate proxy without a DB query. Since `SessionAiSuggestion` uses `@MapsId`, the `session_id` FK value is derived from the proxy ID, so no SELECT is needed.  
**Alternatives considered**: `findById(sessionId).orElseThrow()` — would fire an unnecessary SELECT since the session was just saved and the ID is known.

---

### RD-008: `ProgramSessionPage` Post-Save Navigation Change

**Decision**: Remove the immediate `navigate('/history')` after `logSession.mutateAsync(payload)`. Instead, remain on the page and show a confirmation + AI suggestion panel. Add a "Continue to History" button.  
**Rationale**: FR-004 requires the suggestion to be displayed to the user immediately after saving. This is impossible if the page navigates away before polling completes.  
**UX impact**: Minimal — the user stays on the same page briefly and sees the AI insight before choosing to continue.

