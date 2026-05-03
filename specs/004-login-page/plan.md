# Implementation Plan: Login Page

**Branch**: `004-login-page` | **Date**: 2026-05-04 | **Spec**: `specs/004-login-page/spec.md`  
**Input**: Feature specification from `specs/004-login-page/spec.md`


## Summary

Add username/password authentication to the Workout Tracker application using Spring Security (server-side `HttpSession` + `JSESSIONID` cookie). The frontend presents a login form at `/login`; credentials are submitted as JSON to `POST /api/auth/login`; after a successful login the user is always redirected to `/`. `GET /api/auth/me` on app mount drives the frontend auth state check (200 = authenticated, 401 = redirect to `/login`). Logout is handled via `POST /api/auth/logout` (Spring Security `.logout()` DSL) — the server invalidates the `HttpSession` and the frontend redirects to `/login`; a logout button is rendered on the main page. Unauthenticated access to any protected route is rejected with HTTP 401 by the backend.

The change is a thin vertical slice:
1. **DB**: Add `username VARCHAR(120) UNIQUE NOT NULL` and `password VARCHAR(255) NOT NULL` columns to the `users` table via Flyway `V005__add_user_credentials.sql`. **Q1 decision**: table is assumed empty at migration time — columns added as NOT NULL directly (no temporary DEFAULT); same migration inserts hard-coded seed user(s) so the app is usable immediately after first deploy.
2. **Backend security**: `SecurityConfig` (`SecurityFilterChain`) + `AppUserDetailsService` (`UserDetailsService` backed by the `users` table) + `JsonLoginFilter` (custom `UsernamePasswordAuthenticationFilter` that reads JSON body) + `AuthController` (`GET /api/auth/me`).
3. **Frontend auth hook**: `useAuth` hook calls `GET /api/auth/me` on mount; result drives a top-level `AuthGuard` component that renders either the `LoginPage` or the protected `App` routes.
4. **Frontend login page**: `LoginPage` component at `/login` with username/password form; posts JSON to `POST /api/auth/login`; shows inline error on 401; redirects to `/` on success.
5. **Logout**: Spring Security `.logout()` DSL configured in `SecurityFilterChain` at `POST /api/auth/logout`; invalidates `HttpSession`; `authApi.ts` exposes `logout()` function; a `LogoutButton` component on the main page calls it and redirects to `/login`.

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.x (frontend)  
**Primary Dependencies**: Spring Boot 4.0.5, Spring Security 6.x, Spring Data JPA, Flyway, React 18, React Router  
**Storage**: PostgreSQL 16 — `users` table gains `username` and `password` columns via Flyway migration  
**Testing**: JUnit 5, Spring Boot Test, MockMvc, Spring Security Test (`@WithMockUser`), React Testing Library + Vitest  
**Target Platform**: Linux container (backend), evergreen browsers (frontend)  
**Project Type**: Web application (Spring Boot backend + React frontend)  
**Performance Goals**: *(Out of scope for MVP — no measurable p95 targets set for this iteration.)*  
**Constraints**: Simple explicit security config; no JWT; no registration flow; server-side session only; all new backend code passes checkstyle before commit; English-only artifacts  
**Scale/Scope**: MVP — single credential pair per user, plain username (not email), plain text password (NoOpPasswordEncoder — intentional MVP simplification, not for production)

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

- [x] Design artifacts maintain thin-slice scope — one DB migration, one security config, one controller endpoint, one React page.
- [x] No constitution violations introduced; no exceptions required.

## Project Structure

### Documentation (this feature)

```text
specs/004-login-page/
├── plan.md              ← this file
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── auth-api.md
└── tasks.md             (created by /speckit.tasks)
```

### Source Code (this feature)

```text
backend/
├── src/main/resources/db/migration/
│   └── V005__add_user_credentials.sql                     NEW
├── src/main/java/com/gymtracker/
│   ├── infrastructure/
│   │   ├── security/
│   │   │   ├── SecurityConfig.java                        NEW
│   │   │   ├── AppUserDetailsService.java                 NEW
│   │   │   └── JsonLoginFilter.java                       NEW
│   │   └── web/
│   │       └── AuthController.java                        NEW
│   └── domain/
│       └── AppUser.java                                   NEW (JPA entity for users table)
└── src/test/java/com/gymtracker/
    ├── infrastructure/security/
    │   ├── JsonLoginFilterTest.java                       NEW
    │   └── AppUserDetailsServiceTest.java                 NEW
    └── integration/
        └── AuthControllerIT.java                          NEW

frontend/
├── src/
│   ├── hooks/
│   │   └── useAuth.ts                                     NEW
│   ├── pages/
│   │   └── LoginPage.tsx                                  NEW
│   ├── components/
│   │   └── AuthGuard.tsx                                  NEW
│   ├── services/
│   │   └── authApi.ts                                     NEW (login + me + logout calls)
│   └── App.tsx                                            MODIFIED (wrap with AuthGuard; add LogoutButton)
└── tests/
    ├── LoginPage.test.tsx                                 NEW
    ├── AuthGuard.test.tsx                                 NEW
    └── LogoutButton.test.tsx                              NEW
```

**Structure Decision**: Option 2 (Web application — Spring Boot backend + React frontend). All security infrastructure goes under `backend/src/main/java/com/gymtracker/infrastructure/security/`. Frontend auth logic is isolated in `useAuth` hook + `AuthGuard` component, keeping `App.tsx` changes minimal.

## Complexity Tracking

> No constitution violations — no exceptions required.

## Clarifications

### Session 2026-05-04

- Q: DB Migration safety — how to handle NOT NULL columns when existing rows may be present? → A: B + seed hard-coded users — assume users table is empty at migration time; add NOT NULL columns directly AND insert hard-coded seed user(s) in V005 migration.
- Q: Password storage approach for MVP? → A: Plain text (no BCrypt); use `NoOpPasswordEncoder` in Spring Security config; column named `password`; seed INSERT stores literal `"admin"`. **Intentional MVP simplification — NOT for production.**
- Q: Logout implementation? → A: `POST /api/auth/logout` via Spring Security `.logout()` DSL in `SecurityFilterChain`; server invalidates `HttpSession` on call; frontend redirects to `/login` after success; logout button rendered on main page.
- Q: `SecurityFilterChain` permit-list — which endpoints are public vs. protected? → A: Only `POST /api/auth/login` and `POST /api/auth/logout` are permitted anonymously. `GET /api/auth/me` is **protected** — it returns HTTP 401 when unauthenticated, which is the intended frontend auth-check signal. All other `/api/**` routes require authentication.

*Clarification session complete — 5/5 questions answered.*
