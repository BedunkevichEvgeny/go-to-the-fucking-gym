# Tasks: Login Page

**Input**: Design documents from `specs/004-login-page/`
**Prerequisites**: plan.md ✅, spec.md ✅, data-model.md ✅, research.md ✅, contracts/auth-api.md ✅

**Tests**: Business-logic test tasks are MANDATORY per spec test coverage requirements.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

**Delivery Governance**: Each task SHOULD be mirrored as a GitHub issue for traceability. Each completed task MUST be delivered in its own commit. After merge, the linked issue or task record MUST be reviewed and closed when its acceptance criteria are satisfied.

## Format: `[ID] [P?] [Story?] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[USn]**: Which user story this task belongs to
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: DB migration and Spring Security dependency — these block every subsequent task.

- [X] T001 Create Flyway migration `backend/src/main/resources/db/migration/V005__add_user_credentials.sql` (adds `username VARCHAR(120) NOT NULL UNIQUE`, `password VARCHAR(255) NOT NULL` to `users` table; creates unique index; inserts seed user `admin`/`admin`)
- [X] T002 Add Spring Security dependency to `backend/pom.xml` (`spring-boot-starter-security`) — verify it compiles before writing any security classes

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core security infrastructure that ALL user stories depend on. Must be complete before implementing any user story.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T003 Create JPA entity `backend/src/main/java/com/gymtracker/domain/AppUser.java` (maps `users` table; fields: `id UUID`, `username String`, `password String`, `preferredWeightUnit String`, `createdAt Instant`)
- [X] T004 Create `backend/src/main/java/com/gymtracker/infrastructure/security/AppUserDetailsService.java` (implements `UserDetailsService`; queries `users` table by `username`; returns `UserDetails` with plain text password; throws `UsernameNotFoundException` if not found)
- [X] T005 Create `backend/src/main/java/com/gymtracker/infrastructure/security/JsonLoginFilter.java` (extends `UsernamePasswordAuthenticationFilter`; reads JSON body via `ObjectMapper`; sets `filterProcessesUrl("/api/auth/login")`; success handler returns HTTP 200 `{}`; failure handler returns HTTP 401 `{"error":"Invalid credentials"}`)
- [X] T006 Create `backend/src/main/java/com/gymtracker/infrastructure/security/SecurityConfig.java` (`@Configuration` `SecurityFilterChain` bean; permits only `POST /api/auth/login` and `POST /api/auth/logout` anonymously; all other routes including `GET /api/auth/me` use `.anyRequest().authenticated()`; registers `JsonLoginFilter`; configures `NoOpPasswordEncoder`; disables CSRF; configures CORS for `http://localhost:5173` with `allowCredentials=true`; configures `.logout().logoutUrl("/api/auth/logout")` to return HTTP 200)
- [X] T007 Create `backend/src/main/java/com/gymtracker/infrastructure/web/AuthController.java` (`@RestController`; `@GetMapping("/api/auth/me")`; reads `Authentication` from `SecurityContextHolder`; returns `AuthMeResponse { username }` as HTTP 200)
- [X] T008 [P] Create `frontend/src/services/authApi.ts` (`login(username, password): Promise<void>` → POST `/api/auth/login`; `getMe(): Promise<{ username: string }>` → GET `/api/auth/me`; `logout(): Promise<void>` → POST `/api/auth/logout`; all using `fetch` with `credentials: 'include'`)

**Checkpoint**: Foundation ready — all security infrastructure in place; user story implementation can begin.

---

## Phase 3: User Story 1 — Successful Login (Priority: P1) 🎯 MVP

**Goal**: A registered user can visit the site, enter valid credentials, and be redirected to the main page.

**Independent Test**: Navigate to the site, enter `admin`/`admin`, submit the form, verify redirect to `/` and the main page renders.

### Tests for User Story 1 (MANDATORY) ⚠️

> **Write these tests FIRST — ensure they FAIL before implementing the corresponding feature code.**

- [X] T009 [P] [US1] Create `backend/src/test/java/com/gymtracker/infrastructure/security/AppUserDetailsServiceTest.java` (unit test: loads existing user by username → returns correct `UserDetails`; unknown username → throws `UsernameNotFoundException`)
- [X] T010 [P] [US1] Create `backend/src/test/java/com/gymtracker/infrastructure/security/JsonLoginFilterTest.java` (MockMvc unit test: `POST /api/auth/login` with valid JSON credentials → HTTP 200 `{}`; `POST /api/auth/login` with bad credentials → HTTP 401 `{"error":"Invalid credentials"}`)
- [X] T011 [P] [US1] Create `frontend/tests/LoginPage.test.tsx` (RTL tests: form renders username field, password field, submit button; valid submission calls `authApi.login` and triggers navigation to `/`; password field has `type="password"`)

### Implementation for User Story 1

- [X] T012 [US1] Create `frontend/src/hooks/useAuth.ts` (calls `authApi.getMe()` on mount; returns `{ status: 'loading' | 'authenticated' | 'unauthenticated', username: string | null }`; handles 200 → `authenticated`, 401 → `unauthenticated`)
- [X] T013 [US1] Create `frontend/src/components/AuthGuard.tsx` (renders loading spinner while `status === 'loading'`; renders `<LoginPage />` when `unauthenticated`; renders children/protected routes when `authenticated`)
- [X] T014 [US1] Create `frontend/src/pages/LoginPage.tsx` (controlled form with username + password fields; client-side trim validation; calls `authApi.login()`; on success navigates to `/`; on 401 displays inline error "Invalid credentials"; password field `type="password"`)
- [X] T015 [US1] Modify `frontend/src/App.tsx` to wrap existing routes with `<AuthGuard>` so the app redirects unauthenticated users to `/login`; add `/login` route pointing to `<LoginPage />`

**Checkpoint**: User Story 1 fully functional and testable — valid login → main page redirect works end-to-end.

---

## Phase 4: User Story 2 — Failed Login with Error Feedback (Priority: P2)

**Goal**: Invalid credentials show a clear error message; the user stays on the login page with no redirect.

**Independent Test**: Enter a wrong username or password, submit, verify error message appears and no redirect occurs. Submit empty fields, verify client-side validation errors appear without an API call.

### Tests for User Story 2 (MANDATORY) ⚠️

> **Write these tests FIRST — ensure they FAIL before implementing the corresponding feature code.**

- [X] T016 [P] [US2] Extend `frontend/tests/LoginPage.test.tsx` with error-feedback cases: invalid credentials → error message rendered; empty username → validation message "Username is required"; empty password → validation message "Password is required"; whitespace-only fields treated as empty
- [X] T017 [P] [US2] Create `backend/src/test/java/com/gymtracker/integration/AuthControllerIT.java` with bad-credentials test cases: bad credentials → HTTP 401 body `{"error":"Invalid credentials"}`; error message does NOT reveal which field was wrong
- [X] T017b [P] [US2] Extend `AuthControllerIT.java` with malformed-request test cases: malformed JSON body → HTTP 400 body `{"error":"Bad request"}`; missing required fields → HTTP 400; covers the `400 Bad Request` contract path in `contracts/auth-api.md`

### Implementation for User Story 2

- [X] T018 [US2] Add client-side validation to `frontend/src/pages/LoginPage.tsx`: trim check for username (show "Username is required") and password (show "Password is required") before calling `authApi.login()`; error display must follow existing app error-state styling (UX-002)
- [X] T019 [US2] Verify `JsonLoginFilter.java` failure handler in `backend/src/main/java/com/gymtracker/infrastructure/security/JsonLoginFilter.java` returns generic `{"error":"Invalid credentials"}` for both unknown username and wrong password (no field-level discrimination per FR-004)

**Checkpoint**: User Story 2 independently verifiable — all error paths show correct messages; no information leakage.

---

## Phase 5: User Story 3 — Protected Main Page + Logout (Priority: P3)

**Goal**: Unauthenticated users are blocked from the main page; authenticated users can log out from the main page.

**Independent Test**: Clear session, navigate to `/` directly, verify redirect to `/login`. Log in, verify logout button appears on main page, click it, verify redirect to `/login` and subsequent visit to `/` redirects again.

### Tests for User Story 3 (MANDATORY) ⚠️

> **Write these tests FIRST — ensure they FAIL before implementing the corresponding feature code.**

- [X] T020 [P] [US3] Create `frontend/tests/AuthGuard.test.tsx` (RTL tests: `status='loading'` → shows spinner; `status='unauthenticated'` → renders `<LoginPage />`; `status='authenticated'` → renders protected children; unauthenticated navigation to `/` redirects to `/login`)
- [X] T021 [P] [US3] Create `frontend/tests/LogoutButton.test.tsx` (RTL tests: button renders; click calls `authApi.logout()`; after logout `window.location` or navigation goes to `/login`)
- [X] T022 [P] [US3] Extend `backend/src/test/java/com/gymtracker/integration/AuthControllerIT.java` (created in T017) with session-lifecycle integration tests: `GET /api/auth/me` without session → HTTP 401; `POST /api/auth/login` (valid) then `GET /api/auth/me` → HTTP 200 `{"username":"admin"}`; `POST /api/auth/logout` then `GET /api/auth/me` → HTTP 401

### Implementation for User Story 3

- [X] T023 [US3] Create `frontend/src/components/LogoutButton.tsx` (calls `authApi.logout()`; on HTTP 200 redirects to `/login`; renders a button element consistent with app design language per UX-001)
- [X] T024 [US3] Modify `frontend/src/App.tsx` to render `<LogoutButton />` on the main page (visible only when authenticated, i.e., inside `<AuthGuard>` protected area)
- [X] T025 [US3] Verify `SecurityConfig.java` `.logout()` DSL in `backend/src/main/java/com/gymtracker/infrastructure/security/SecurityConfig.java`: `logoutUrl("/api/auth/logout")`; `logoutSuccessHandler` returns HTTP 200 `{}`; `invalidateHttpSession(true)`; `deleteCookies("JSESSIONID")`

**Checkpoint**: All user stories independently functional — unauthenticated access blocked; logout invalidates session.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Non-functional hardening and delivery hygiene across all user stories.

- [X] T026 [P] Run Checkstyle on all new backend files (`SecurityConfig.java`, `AppUserDetailsService.java`, `JsonLoginFilter.java`, `AuthController.java`, `AppUser.java`) and fix any violations — all new backend code must pass Checkstyle before commit (plan.md constraint)
- [X] T027 [P] Validate CORS configuration in `SecurityConfig.java`: confirm `http://localhost:5173` origin is allowed with `allowCredentials=true`; test from Vite dev server that login and `/api/auth/me` requests succeed with cookies
- [X] T028 [P] Validate that `GET /api/auth/me` returns HTTP 401 (not a redirect) when unauthenticated — Spring Security `AuthenticationEntryPoint` must return JSON 401, not a redirect to a login page (backend sends 401 so frontend JS can detect it)
- [X] T029 [P] Verify login page visual consistency with rest of app: uses same colour palette, typography, spacing, and button styles (UX-001); error messages follow existing error-state pattern (UX-002)
- [X] T030 Run `specs/004-login-page/quickstart.md` validation scenarios end-to-end (if quickstart.md exists) and confirm all smoke tests pass

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 (DB migration must exist; Security dep must compile) — **BLOCKS all user stories**
- **User Story Phases (3–5)**: All depend on Phase 2 completion; can proceed in priority order or in parallel (if staffed)
- **Polish (Phase 6)**: Depends on all desired user stories being complete

### User Story Dependencies

- **US1 (P1)**: Can start after Phase 2 — no dependency on US2 or US3
- **US2 (P2)**: Can start after Phase 2 — builds on `LoginPage.tsx` introduced in US1 (add error cases); US1 should be done first for practical incremental delivery
- **US3 (P3)**: Can start after Phase 2 — `AuthGuard.tsx` started in US1; `LogoutButton` is new; logout endpoint configuration is in `SecurityConfig.java` created in Phase 2

### Within Each User Story

- Tests → Models/Services → Endpoints/Components → Integration
- Tests MUST be written and FAIL before implementation
- Each task delivered in its own commit

### Parallel Opportunities

Within Phase 2 after T003 (AppUser entity) is complete:
- T004 (`AppUserDetailsService`) and T005 (`JsonLoginFilter`) can run in parallel
- T008 (`authApi.ts`) can run in parallel with all backend Phase 2 tasks (different codebase)

Within Phase 3:
- T009, T010, T011 (tests) can all run in parallel
- T012 (`useAuth`), T013 (`AuthGuard`), T014 (`LoginPage`) can run in parallel after tests exist

Within Phase 5:
- T020, T021, T022 (tests) can all run in parallel

---

## Parallel Example: User Story 1

```text
# Write all US1 tests together (parallel):
T009 — AppUserDetailsServiceTest.java
T010 — JsonLoginFilterTest.java
T011 — LoginPage.test.tsx

# Then implement (frontend components can be parallel):
T012 — useAuth.ts
T013 — AuthGuard.tsx
T014 — LoginPage.tsx

# Finally integrate:
T015 — App.tsx modifications (depends on T013, T014)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001–T002)
2. Complete Phase 2: Foundational (T003–T008) — CRITICAL, blocks all stories
3. Complete Phase 3: User Story 1 (T009–T015)
4. **STOP and VALIDATE**: Enter `admin`/`admin`, confirm redirect to `/`, confirm unauthenticated visit to `/` redirects to `/login`
5. Deploy/demo if ready

### Incremental Delivery

1. Phase 1 + Phase 2 → Security infrastructure ready
2. Phase 3 (US1 — Successful Login) → Test independently → Deploy (MVP!)
3. Phase 4 (US2 — Error Feedback) → Test independently → Deploy
4. Phase 5 (US3 — Protected Route + Logout) → Test independently → Deploy
5. Phase 6 (Polish) → Checkstyle, CORS, visual review

---

## Notes

- **NoOpPasswordEncoder** is intentional for this MVP — plain text password comparison. Do NOT replace with BCrypt without a separate migration task.
- **Seed credentials**: username `admin`, password `admin` (inserted by V005 migration).
- `GET /api/auth/me` is a **protected** endpoint — the HTTP 401 it returns when unauthenticated is the intended auth-check signal; do NOT permit it anonymously.
- CSRF is disabled — REST JSON API clients do not use CSRF tokens; document this decision in `SecurityConfig.java` with a comment.
- Each completed task must be committed separately (one task = one commit).
- After merge, review and close the linked GitHub issue when acceptance criteria are met.
- Stop at each checkpoint to validate the story works independently before proceeding.

