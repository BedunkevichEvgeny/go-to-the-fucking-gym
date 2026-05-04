# Research: Login Page

**Feature**: 004-login-page  
**Date**: 2026-05-04

---

## 1. Spring Security JSON Login Filter

**Decision**: Implement a custom `JsonLoginFilter` extending `UsernamePasswordAuthenticationFilter` that reads `username` and `password` from a JSON request body (via `ObjectMapper`) instead of form parameters.

**Rationale**: Spring Security's default `UsernamePasswordAuthenticationFilter` expects `application/x-www-form-urlencoded`. The spec requires `POST /api/auth/login` to accept JSON (TC-007). Extending the filter class reuses the existing `AuthenticationManager` pipeline (plain text password comparison via `NoOpPasswordEncoder` — MVP simplification) with minimal code.

**Alternatives considered**:
- `@PostMapping` controller method with manual `authenticationManager.authenticate()` call — rejected because it reimplements what the filter already does cleanly.
- Form-based login (`formLogin()` DSL) — rejected because it requires URL-encoded body, not JSON.

---

## 2. Session Management

**Decision**: Use Spring Security's default server-side `HttpSession` (no `spring-session`). JSESSIONID cookie is set automatically by the servlet container on session creation. `SecurityFilterChain` configured with `sessionManagement().sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)`.

**Rationale**: Simplest possible stateful session approach matching TC-006. No extra dependency (Spring Session, Redis, etc.) required. Works out of the box with Spring Security.

**Alternatives considered**:
- JWT stateless tokens — rejected by spec (TC-006).
- Spring Session + Redis — rejected; overkill for MVP, no distributed-session requirement.

---

## 3. `/api/auth/me` Endpoint

**Decision**: A `GET /api/auth/me` endpoint in `AuthController` returns HTTP 200 with a `{ "username": "..." }` JSON body when the session is authenticated, or HTTP 401 when unauthenticated (via Spring Security's `authenticationEntryPoint`).

**Rationale**: The spec (TC-008) requires the frontend to call this on app mount to detect auth state. Returning the username allows the UI to display the logged-in user if needed in future.

**Alternatives considered**:
- Return only HTTP status with empty body — simpler but provides no user context for future use.

---

## 4. DB Schema Change

**Decision**: Add `username VARCHAR(120) NOT NULL UNIQUE` and `password VARCHAR(255) NOT NULL` columns to the existing `users` table via Flyway `V005__add_user_credentials.sql`.

**Rationale**: The existing `users` table uses a UUID PK matched to Spring Security. Adding credentials to the same table avoids a new join on every `UserDetailsService.loadUserByUsername()` call. `VARCHAR(255)` for `password` provides headroom for future changes. **For MVP, passwords are stored as plain text** (intentional simplification — not for production).

**Alternatives considered**:
- Separate `user_credentials` table — rejected; unnecessary join, extra complexity, no benefit at this scale.

---

## 5. Password Encoding

**Decision**: Use `NoOpPasswordEncoder` in Spring Security for MVP. Passwords are stored as plain text in the `password` column and compared directly.

**Rationale**: Intentional MVP simplification — removes BCrypt dependency and seed-script complexity for a prototype with no production data. **NOT acceptable for production.** When promoting to production, replace with `BCryptPasswordEncoder` and re-hash all passwords via a new migration.

**Alternatives considered**:
- `BCryptPasswordEncoder` (default strength 10 rounds) — correct for production but adds encoding complexity to the seed migration; deferred to a future hardening task.
- Argon2 — more modern but requires Bouncy Castle dependency; overkill for MVP.

---

## 6. CORS / Cookie Configuration

**Decision**: For local development, configure CORS to allow the frontend dev server origin (`http://localhost:5173`) with `allowCredentials = true` so JSESSIONID cookie is sent cross-origin. In production the frontend is served from the same origin (no CORS needed).

**Rationale**: React dev server runs on port 5173, backend on 8080. Without `allowCredentials` + explicit origin the browser strips the cookie from cross-origin requests.

**Alternatives considered**:
- Proxy via Vite `server.proxy` — viable alternative; documented in quickstart.md as the simpler local dev option (no CORS config needed).

---

## 7. Frontend Auth State Pattern

**Decision**: `useAuth` hook calls `GET /api/auth/me` once on mount (no polling). Result is stored in React state as `{ status: 'loading' | 'authenticated' | 'unauthenticated', username: string | null }`. `AuthGuard` wraps `App` routes and renders `<LoginPage>` when unauthenticated.

**Rationale**: Simple, no extra dependencies (no React Query, no Context API required for this MVP). Single network call on load keeps UX snappy.

**Alternatives considered**:
- React Query for the `/me` call — valid but adds dependency; not justified for a single call.
- Redux auth slice — overengineered for MVP.

---

## 8. Error Handling

**Decision**: The `JsonLoginFilter` configures a failure handler that returns HTTP 401 with `{ "error": "Invalid credentials" }` JSON body. The frontend shows this message inline on the form without revealing which field was wrong (FR-004).

**Rationale**: Generic error message satisfies the security requirement (no credential enumeration). JSON response body lets the frontend display the message without parsing HTML.

**Alternatives considered**:
- Redirect to `/login?error=1` — HTML-redirect approach incompatible with JSON-first API.

