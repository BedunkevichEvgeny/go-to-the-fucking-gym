# Tasks: Post-Session AI Suggestion

**Feature**: `003-post-session-ai-suggestion`  
**Branch**: `003-post-session-ai-suggestion`  
**Input**: `specs/003-post-session-ai-suggestion/` (plan.md, spec.md, data-model.md, research.md, contracts/session-detail-api.md)

**Tests**: Business-logic test tasks are MANDATORY. Each business rule MUST include unit and integration test coverage.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing.

**Delivery Governance**: Each completed task MUST be delivered in its own commit. After merge, the linked issue or task record MUST be reviewed and closed when its acceptance criteria are satisfied.

## Format: `[ID] [P?] [Story?] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[US#]**: Which user story this task belongs to
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Database migration that blocks all backend user story work.

- [X] T001 Create Flyway migration `backend/src/main/resources/db/migration/V003__session_ai_suggestion.sql` — table `session_ai_suggestions` with `session_id UUID PK/FK`, `suggestion TEXT NOT NULL`, `generated_at TIMESTAMPTZ NOT NULL DEFAULT now()`, and FK constraint to `logged_sessions(id)`

**Checkpoint**: Migration applied (`./mvnw flyway:migrate` or app startup) — `session_ai_suggestions` table exists in DB.

---

## Phase 2: Foundational (Blocking Backend Prerequisites)

**Purpose**: JPA entity + repository that all backend user stories depend on. Must complete before Phases 3–5 backend tasks.

- [X] T002 [P] Create `backend/src/main/java/com/gymtracker/domain/SessionAiSuggestion.java` — `@Entity`, `@Table(name = "session_ai_suggestions")`, fields: `sessionId` (UUID, `@Id @Column(name="session_id")`), `session` (`@OneToOne @MapsId @JoinColumn(name="session_id")`), `suggestion` (String, non-null), `generatedAt` (OffsetDateTime, non-null); `@PrePersist` sets `generatedAt = OffsetDateTime.now()` if null
- [X] T003 [P] Add lazy optional `@OneToOne` association to `backend/src/main/java/com/gymtracker/domain/LoggedSession.java`: `@OneToOne(mappedBy = "session", fetch = FetchType.LAZY, optional = true) private SessionAiSuggestion aiSuggestion;` with getter
- [X] T004 Create `backend/src/main/java/com/gymtracker/infrastructure/repository/SessionAiSuggestionRepository.java` — `JpaRepository<SessionAiSuggestion, UUID>`; no custom queries needed beyond inherited `existsById` and `save`

**Checkpoint**: Project compiles (`./mvnw compile`) with no errors after T001–T004.

---

## Phase 3: User Story 1 — See AI Suggestion After Saving a Program Session (Priority: P1) 🎯 MVP

**Goal**: After saving a program session the user sees a coaching suggestion (or a loading/timeout state) before navigating away.

**Independent Test**: Log a program session → confirm AI suggestion panel appears on the post-save screen; loading indicator visible during wait; fallback shown on timeout.

### Backend — Business Logic (US1)

- [X] T005 [US1] Update `backend/src/main/java/com/gymtracker/infrastructure/ai/AiHandoffService.java` — inject `SessionAiSuggestionRepository` and `LoggedSessionRepository`; replace the `whenComplete` log-only handler with a call to `persistSuggestion(sessionId, suggestion)`: check `existsById`, skip if present, otherwise create `SessionAiSuggestion` entity via `loggedSessionRepository.getReferenceById(sessionId)`, call `save`, and log result; keep the existing blank/null guard before calling `persistSuggestion`. **`@MapsId` note**: when constructing `SessionAiSuggestion`, only call `entity.setSession(loggedSession)` — do NOT call `entity.setSessionId(...)`. With `@MapsId` the JPA provider derives the PK (`sessionId`) automatically from the associated `LoggedSession.id` before flush; setting it manually is redundant and can cause `TransientPropertyValueException`.
- [X] T006 [US1] Add `aiSuggestion` field (nullable `String`) as last component to `backend/src/main/java/com/gymtracker/api/dto/LoggedSessionDetail.java` record
- [X] T007 [US1] Update `backend/src/main/java/com/gymtracker/infrastructure/mapper/DtoMapper.java` — in `toDetailDto`, read `loggedSession.getAiSuggestion()`, map to `String` (null if absent), and pass as last argument to `LoggedSessionDetail` constructor; `toHistoryItem` is NOT changed

### Backend — Unit Tests (US1)

- [X] T008 [P] [US1] Create `backend/src/test/java/com/gymtracker/application/AiHandoffServiceTest.java` — Mockito-based unit tests:
  - PROGRAM session: suggestion persisted when AI returns non-blank text
  - PROGRAM session: blank/whitespace AI response → `save` never called
  - PROGRAM session: `existsById` returns true → `save` never called (immutability guard)
  - FREE session: method returns immediately, no repository interaction
- [X] T009 [P] [US1] Update `backend/src/test/java/com/gymtracker/infrastructure/mapper/DtoMapperTest.java` (or create if absent) — add cases:
  - `toDetailDto` with non-null `SessionAiSuggestion` → DTO `aiSuggestion` equals entity `suggestion`
  - `toDetailDto` with null `aiSuggestion` association → DTO `aiSuggestion` is null
  - `toHistoryItem` → `SessionHistoryItem` does not contain `aiSuggestion` field

### Backend — Integration Tests (US1)

- [X] T010 [US1] Update (or create) `backend/src/test/java/com/gymtracker/integration/LoggedSessionServiceIT.java` — Testcontainers PostgreSQL; use `Awaitility.await().atMost(10, SECONDS).untilAsserted(...)` to wait for async suggestion persistence before asserting:
  - Save program session → `Awaitility` waits up to 10 s → `session_ai_suggestions` row exists with correct `session_id` and non-blank `suggestion`
  - Save free session → assert immediately (no async step) → no row in `session_ai_suggestions`
  - Add `org.awaitility:awaitility` test dependency to `backend/pom.xml` if not already present
- [X] T011 [US1] Update (or create) `backend/src/test/java/com/gymtracker/integration/SessionDetailServiceIT.java` — MockMvc:
  - GET `/api/logged-sessions/{id}` with suggestion in DB → response body contains `aiSuggestion` field with suggestion text
  - GET `/api/logged-sessions/{id}` without suggestion row → response body contains `"aiSuggestion": null`

### Frontend — TypeScript Types (US1)

- [X] T012 [P] [US1] Update `frontend/src/types/api.ts` — add `aiSuggestion?: string | null` to `LoggedSessionDetail` interface

### Frontend — Poll Hook (US1)

- [X] T013 [US1] Create `frontend/src/hooks/usePollSessionSuggestion.ts` — use **TanStack Query** (`useQuery` from `@tanstack/react-query`), consistent with all other hooks in the codebase (e.g., `useSessionDetail.ts`). Accept `sessionId: string | null`; return the query result. Enable the query only when `sessionId` is non-null. Use a `timedOut` flag managed via `useState` + `useEffect`: start a `setTimeout` for 15 000 ms when polling begins; when it fires, set `timedOut = true`. Pass `refetchInterval: timedOut || data?.aiSuggestion ? false : 3000` to `useQuery` — this stops polling once a suggestion is found OR the timeout fires. Set `retry: false` and `refetchIntervalInBackground: false`. **Do NOT use `queryClient.cancelQueries`** — the shared key `['session-detail', sessionId]` is also consumed by `useSessionDetail` on the history detail page; cancelling it would disrupt unrelated components. Expose `{ suggestion: string | null; timedOut: boolean; isPolling: boolean }` derived from the query state. Query key: `['session-detail', sessionId]` — same key as `useSessionDetail` so the cache is shared and the detail view benefits immediately from the polled data.

### Frontend — Post-Save UI (US1)

- [X] T014 [US1] Create `frontend/src/components/AiCoachingInsightCard.tsx` — accepts props `{ isPolling: boolean; suggestion: string | null; timedOut: boolean }`; renders:
  - Loading state: spinner + "Generating your coaching insight…" (while `isPolling && !suggestion`)
  - Success state: labelled card "AI Coaching Insight" with suggestion text
  - Timeout/absent state: neutral message "Coaching insight unavailable right now. Check back in session history." (when `timedOut && !suggestion`)
  - Uses same `card stack-sm` CSS class pattern as existing info cards; matches `eyebrow` label convention
- [X] T015 [US1] Update `frontend/src/pages/ProgramSessionPage.tsx` — after successful POST `/api/logged-sessions` (201): store returned `sessionId` in local state (`savedSessionId`); pass `savedSessionId` to `usePollSessionSuggestion` — the hook activates **reactively** (no `startPolling` call needed; `enabled` is `true` when `sessionId` is non-null); destructure `{ suggestion, timedOut, isPolling }` from the hook and pass as props to `<AiCoachingInsightCard />`; render the card in the post-save view; keep "Continue to History" button always enabled (never blocked by AI status); on button click navigate to `/history`

### Frontend — Unit Tests (US1)

- [X] T016 [P] [US1] Create `frontend/tests/AiCoachingInsightCard.test.tsx` — React Testing Library:
  - Loading state: spinner and loading text rendered when `isPolling=true, suggestion=null, timedOut=false`
  - Success state: suggestion text rendered when `suggestion="..."` provided
  - Timeout state: fallback message rendered when `timedOut=true, suggestion=null`
- [X] T017 [P] [US1] Create `frontend/tests/ProgramSessionPage.postSave.test.tsx` — mock `usePollSessionSuggestion`; after save:
  - Loading indicator shown while `isPolling=true`
  - Suggestion text shown when suggestion arrives
  - Fallback shown when `timedOut=true`
  - "Continue to History" button always present and clickable

**Checkpoint**: User Story 1 fully functional — save a program session, see loading indicator, see suggestion (or graceful fallback); navigate to history.

---

## Phase 4: User Story 2 — AI Suggestion Persisted and Accessible in Session History (Priority: P2)

**Goal**: The suggestion stored after session save is visible when the user revisits that session in history. The suggestion is immutable once stored.

**Independent Test**: (1) Save a session, note suggestion text. (2) Navigate away. (3) Return to session detail via history. (4) Same suggestion text appears. (5) Second AI call does not overwrite it.

### Backend — Immutability Integration (US2)

- [X] T018 [P] [US2] Update `backend/src/test/java/com/gymtracker/integration/LoggedSessionServiceIT.java` — add immutability test: save program session → wait for suggestion → trigger `persistSuggestion` a second time with a different text → confirm DB row is unchanged (same suggestion text, `existsById` guard worked)

### Frontend — Session Detail View (US2)

- [X] T019 [P] [US2] Update `frontend/src/features/history/SessionDetailView.tsx` — add AI Coaching Insight section after the session info card: render `<AiCoachingInsightCard suggestion={data.aiSuggestion} isPolling={false} timedOut={false} />` only when `data.sessionType === 'PROGRAM' && data.aiSuggestion`; use `<section aria-label="AI Coaching Insight">`

### Frontend — Unit Tests (US2)

- [X] T020 [P] [US2] Create `frontend/tests/SessionDetailView.suggestion.test.tsx` — React Testing Library:
  - PROGRAM session + non-null `aiSuggestion` → "AI Coaching Insight" section and suggestion text rendered
  - PROGRAM session + `aiSuggestion: null` → suggestion section absent
  - PROGRAM session + `aiSuggestion: ""` (empty string) → suggestion section absent
  - FREE session (any `aiSuggestion` value) → suggestion section absent (covered further in US3 phase)

### Backend — History List Regression (US2)

- [X] T021 [P] [US2] Update (or create) `backend/src/test/java/com/gymtracker/integration/SessionHistoryServiceIT.java` — confirm `GET /api/logged-sessions` list response items do NOT contain an `aiSuggestion` field (shape validation via JSON assertion)

**Checkpoint**: User Story 2 functional — navigate to session history, open a session detail, see the same suggestion stored at save time; immutability guard confirmed by test.

---

## Phase 5: User Story 3 — Free Sessions Do Not Generate AI Suggestions (Priority: P3)

**Goal**: Free sessions produce no suggestion, no AI call, and no suggestion section in the UI.

**Independent Test**: Log a free session → confirm no suggestion panel in post-save view and no suggestion section in its session detail.

### Backend — Free Session Guard (US3)

_The existing gate in `AiHandoffService` already skips FREE sessions. No new backend code needed; coverage is verified via unit test T008 (AiHandoffServiceTest — FREE session case) and integration test T010 (LoggedSessionServiceIT — free session save, no suggestion row). This phase adds the explicit frontend guard test._

### Frontend — Free Session UI Guard (US3)

- [X] T022 [US3] Update `frontend/tests/SessionDetailView.suggestion.test.tsx` — add explicit test cases:
  - FREE session + `aiSuggestion: null` → no suggestion section rendered
  - FREE session + `aiSuggestion: "some text"` (defensive) → no suggestion section rendered (guard is `sessionType === 'PROGRAM'`)
- [X] T023 [P] [US3] Update `frontend/tests/ProgramSessionPage.postSave.test.tsx` (or add `FreeSessionPage.test.tsx` if separate page exists) — confirm no `AiCoachingInsightCard` rendered after saving a free session

**Checkpoint**: User Story 3 verified — free sessions show no suggestion at any point.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Code quality, documentation, and final validation.

- [X] T024 Run `./mvnw checkstyle:check` in `backend/`; fix any style violations in new/modified files (`AiHandoffService.java`, `SessionAiSuggestion.java`, `SessionAiSuggestionRepository.java`, `DtoMapper.java`, `LoggedSessionDetail.java`, `LoggedSession.java`)
- [X] T025 [P] Run `npm run lint` in `frontend/`; fix any lint errors in new/modified files (`api.ts`, `usePollSessionSuggestion.ts`, `AiCoachingInsightCard.tsx`, `ProgramSessionPage.tsx`, `SessionDetailView.tsx`)
- [X] T026 [P] Verify all backend tests pass: `./mvnw test` in `backend/` — confirm `AiHandoffServiceTest`, `DtoMapperTest`, `LoggedSessionServiceIT`, `SessionDetailServiceIT`, `SessionHistoryServiceIT` are green
- [X] T027 [P] Verify all frontend tests pass: `npm run test` in `frontend/` — confirm `AiCoachingInsightCard.test.tsx`, `ProgramSessionPage.postSave.test.tsx`, `SessionDetailView.suggestion.test.tsx` are green
- [X] T028 Execute manual smoke test per `specs/003-post-session-ai-suggestion/quickstart.md` — save a program session end-to-end in local dev, verify suggestion appears, navigate to history, verify suggestion visible in detail view
  - ~~**FAILED**: `suggestion CHARACTER VARYING(255)` truncation error — AI response exceeds 255 chars; see T029/T030 for fix.~~
  - **PASSED** after T029/T030 hotfix applied.

---

## Phase 7: Hotfix — Suggestion Column Truncation (T028 Smoke Test Failure)

**Root Cause**: `String suggestion` in `SessionAiSuggestion.java` lacked `columnDefinition = "TEXT"`. Hibernate's `ddl-auto=update` silently maps unmapped `String` fields to `CHARACTER VARYING(255)`, overriding the Flyway `TEXT` DDL on any environment where Hibernate ran first or `ddl-auto` mutated the schema. Real AI responses routinely exceed 255 characters.

**Fix**:
- [X] T029 Create `backend/src/main/resources/db/migration/V004__fix_suggestion_column_text.sql` — `ALTER TABLE session_ai_suggestions ALTER COLUMN suggestion TYPE TEXT;` (idempotent in PostgreSQL; repairs all existing deployed DBs where the column was downgraded to `VARCHAR(255)`)
- [X] T030 Update `backend/src/main/java/com/gymtracker/domain/SessionAiSuggestion.java` — change `@Column(nullable = false)` to `@Column(nullable = false, columnDefinition = "TEXT")` on the `suggestion` field; prevents Hibernate from ever inferring `VARCHAR(255)` regardless of `ddl-auto` setting

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — start immediately.
- **Phase 2 (Foundational)**: Depends on Phase 1 (migration must exist before entity compiles). BLOCKS all backend user story work.
- **Phase 3 (US1)**: Depends on Phase 2 completion. All US1 tasks can begin once entity + repository compile.
- **Phase 4 (US2)**: Depends on Phase 2. T018 also depends on T005 (AiHandoffService change) from Phase 3. T019 depends on T014 (AiCoachingInsightCard) from Phase 3.
- **Phase 5 (US3)**: Depends on Phase 3 frontend tasks (T014, T015) being complete; backend guard pre-exists.
- **Phase 6 (Polish)**: Depends on all prior phases complete.

### User Story Dependencies

- **US1 (P1)**: Can start after Phase 2 — no dependency on US2 or US3.
- **US2 (P2)**: Backend immutability test depends on T005 (US1). Frontend detail view depends on T014 (US1 component). Otherwise independent.
- **US3 (P3)**: Frontend guard tests depend on T014 (US1 component). Backend guard already exists. Independent of US2.

### Within Each User Story

- T001 → T002, T003, T004 (migration before entity)
- T002 + T003 → T004 (entity before repository — or parallel since repo just imports entity type)
- T002 + T003 + T004 → T005 (AiHandoffService needs repository)
- T005 + T006 → T007 (DtoMapper needs updated entity association and DTO)
- T006 + T007 → T011 (integration tests validate complete backend stack)
- T012 → T013 → T014 → T015 (types before hook before component before page)

### Parallel Opportunities (within phases)

```text
Phase 2:
  T002 [entity] ──┐
  T003 [assoc]  ──┼──> T004 [repository]
  (T002 and T003 can run in parallel — different files)

Phase 3 — backend and frontend are fully parallel tracks:
  Backend track:  T005 → T006 → T007 → T010 → T011
  Frontend track: T012 → T013 → T014 → T015
  Tests:          T008 [P], T009 [P], T016 [P], T017 [P]  (parallel within their track)

Phase 4:
  T018 (backend IT) — parallel with T019 (frontend) and T020 (FE tests) and T021 (history IT)

Phase 5:
  T022, T023 — parallel with each other
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Migration (T001)
2. Complete Phase 2: Entity + association + repository (T002–T004)
3. Complete Phase 3: US1 — backend service change, DTO, mapper, poll hook, UI card, page update, tests (T005–T017)
4. **STOP and VALIDATE**: Save a program session in local dev — suggestion appears, loading state works, fallback on timeout works.
5. Deploy / demo if ready.

### Incremental Delivery

1. Complete Phases 1–2 → Foundation ready
2. Complete Phase 3 (US1) → Test independently → Deploy/Demo (**MVP**)
3. Complete Phase 4 (US2) → Test persistence and history view → Deploy/Demo
4. Complete Phase 5 (US3) → Confirm free session boundary → Deploy/Demo
5. Complete Phase 6 → Polish, lint, smoke test → Ready for review

### Parallel Team Strategy

With two developers available after Phase 2 completes:

- **Developer A** (backend): T005 → T006 → T007 → T008 → T009 → T010 → T011 → T018 → T021
- **Developer B** (frontend): T012 → T013 → T014 → T015 → T016 → T017 → T019 → T020 → T022 → T023

Both tracks converge at Phase 6 for linting and smoke testing.

---

## Notes

- `[P]` tasks operate on different files with no dependencies on incomplete tasks in the same phase.
- `[US#]` label maps each task to a specific user story for traceability.
- The `AiHandoffService` blank-output guard already exists — T005 preserves it, does not rewrite it.
- The `FetchType.LAZY` on `LoggedSession.aiSuggestion` (T003) is essential — do not change to EAGER or history list queries will N+1.
- `existsById` in the immutability guard (T005) avoids a `DataIntegrityViolationException` and makes the intent explicit; the DB PK constraint is the safety net.
- Commit each task separately; do not combine multiple task IDs in one commit.
- After merge, review and close the linked GitHub issue when acceptance criteria are met.
- Stop at any checkpoint to validate the story independently before continuing.

