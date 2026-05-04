# Data Model: Login Page

**Feature**: 004-login-page  
**Date**: 2026-05-04

---

## Entities

### AppUser (JPA Entity — `users` table)

The existing `users` table gains two new columns for authentication. No new table is introduced.

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | `UUID` | PK | Existing — provided by application on user creation |
| `username` | `VARCHAR(120)` | NOT NULL, UNIQUE | **NEW** — plain text identifier (not email) |
| `password` | `VARCHAR(255)` | NOT NULL | **NEW** — plain text password (MVP simplification; not for production) |
| `preferred_weight_unit` | `VARCHAR(3)` | NOT NULL, CHECK IN ('KG','LBS') | Existing |
| `created_at` | `TIMESTAMP` | NOT NULL DEFAULT NOW() | Existing |

**Flyway migration**: `V005__add_user_credentials.sql`

```sql
-- Assumption (Q1): users table is empty at migration time (MVP / greenfield).
-- Columns are added as NOT NULL directly — no temporary DEFAULT needed.
ALTER TABLE users
  ADD COLUMN username VARCHAR(120) NOT NULL,
  ADD COLUMN password VARCHAR(255) NOT NULL;

CREATE UNIQUE INDEX idx_users_username ON users(username);

-- Seed hard-coded user for MVP access.
-- NOTE: password is stored as plain text — intentional MVP simplification, NOT for production.
INSERT INTO users (id, username, password, preferred_weight_unit, created_at)
VALUES (
  gen_random_uuid(),
  'admin',
  'admin',
  'KG',
  NOW()
);
```

> **Decision (Q1)**: Assume `users` table is empty at migration time; add NOT NULL columns directly. Insert hard-coded seed user(s) in the same migration script so the app is usable immediately after migration.

> **Decision (Q3)**: Passwords are stored as plain text for MVP simplicity. Spring Security is configured with `NoOpPasswordEncoder`. **This is intentional — NOT acceptable for production.**

> **Seed credentials**: username `admin`, password `admin`.

---

### Session (Spring Security — not a DB entity)

Spring Security creates and manages `HttpSession` automatically. No JPA entity required.

| Attribute | Value |
|-----------|-------|
| Mechanism | Server-side `HttpSession` |
| Cookie | `JSESSIONID` (set by servlet container) |
| Lifetime | Until explicit logout (`POST /api/auth/logout`) or server-side expiry (default 30 min idle) |
| Storage | In-memory (JVM) for MVP; no distributed session store |

---

## DTO Contracts

### `LoginRequest` (frontend → backend)

```json
{
  "username": "string (required, non-blank)",
  "password": "string (required, non-blank)"
}
```

Produced by `LoginPage` form. Sent as `Content-Type: application/json` to `POST /api/auth/login`.

### `AuthMeResponse` (backend → frontend)

```json
{
  "username": "string"
}
```

Returned by `GET /api/auth/me` when authenticated (HTTP 200). When unauthenticated, Spring Security's `AuthenticationEntryPoint` returns HTTP 401 with no body (or a standard error body).

### `LoginErrorResponse` (backend → frontend)

```json
{
  "error": "Invalid credentials"
}
```

Returned by the `JsonLoginFilter`'s `AuthenticationFailureHandler` on HTTP 401.

### `BadRequestResponse` (backend → frontend)

```json
{
  "error": "Bad request"
}
```

Returned by `JsonLoginFilter` when the request body is malformed JSON or required fields are missing (HTTP 400). Uses the same `{ "error": "..." }` shape as `LoginErrorResponse` for consistency.

---

## Validation Rules

| Field | Rule | Error |
|-------|------|-------|
| `username` | Not blank, trimmed | Client-side: "Username is required" |
| `password` | Not blank, trimmed | Client-side: "Password is required" |
| `username` + `password` | Must match a `users` record (plain text comparison via NoOpPasswordEncoder) | Server-side HTTP 401: "Invalid credentials" |

- Client-side validation fires before the API call (FR-006).
- Server-side validation never reveals which field was wrong (FR-004).
- Whitespace-only values are treated as empty by the frontend trim check (edge case from spec).

---

## State Transitions

```
[Unauthenticated] ──POST /api/auth/login (valid)──► [Authenticated session]
                                                          │
                                                    JSESSIONID cookie set

[Authenticated session] ──GET /api/auth/me──► HTTP 200

[No session / expired] ──GET /api/auth/me──► HTTP 401 ──► frontend redirects to /login

[Authenticated session] ──POST /api/auth/logout──► [Unauthenticated]
                                                          │
                                                    HttpSession invalidated
                                                    frontend redirects to /login
```

> **Logout** is implemented via Spring Security `.logout()` DSL at `POST /api/auth/logout`. The server invalidates the `HttpSession`; the frontend calls this endpoint and redirects to `/login` on HTTP 200. A logout button is rendered on the main page.

---

## Frontend Auth State Machine

```
         ┌─────────────────────────────┐
         │          loading            │ ← app mount, awaiting GET /api/auth/me
         └──────────────┬──────────────┘
                        │
           ┌────────────┴────────────┐
           ▼                         ▼
  ┌─────────────────┐     ┌─────────────────────┐
  │  authenticated  │     │  unauthenticated     │
  │  (render app)   │     │  (render LoginPage)  │
  └────────┬────────┘     └──────────┬───────────┘
           │                         │
           │                POST /api/auth/login (success)
           │◄────────────────────────┘
           │
  POST /api/auth/logout (success)
           │
           ▼
  ┌─────────────────────┐
  │  unauthenticated    │
  │  (redirect /login)  │
  └─────────────────────┘
```

State values: `'loading' | 'authenticated' | 'unauthenticated'`


