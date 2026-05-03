# Auth API Contract

**Feature**: 004-login-page  
**Date**: 2026-05-04

---

## Endpoints

### POST /api/auth/login

Submit credentials and establish an authenticated session.

**Request**

```
POST /api/auth/login
Content-Type: application/json
```

```json
{
  "username": "alice",
  "password": "s3cr3t"
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `username` | string | yes | Non-blank |
| `password` | string | yes | Non-blank |

**Responses**

| Status | When | Body |
|--------|------|------|
| `200 OK` | Authentication succeeded | `{}` (empty JSON object) |
| `401 Unauthorized` | Bad credentials | `{ "error": "Invalid credentials" }` |
| `400 Bad Request` | Malformed JSON or missing fields | `{ "error": "Bad request" }` |

**Side effects on success**:
- Spring Security creates an `HttpSession` and sets a `JSESSIONID` cookie in the response.
- Subsequent requests that include the `JSESSIONID` cookie are treated as authenticated.

**Notes**:
- The error body never distinguishes between "username not found" and "password wrong" (FR-004).
- The frontend always redirects to `/` after a 200 response (TC-009).

---

### GET /api/auth/me

Check whether the current session is authenticated and retrieve the username.

**Request**

```
GET /api/auth/me
Cookie: JSESSIONID=<value>
```

No request body.

**Responses**

| Status | When | Body |
|--------|------|------|
| `200 OK` | Session is authenticated | `{ "username": "alice" }` |
| `401 Unauthorized` | No session / expired session / unauthenticated | *(empty or Spring Security default error body)* |

**Access control**: `GET /api/auth/me` is a **protected** endpoint — `SecurityFilterChain` does NOT permit it anonymously. An unauthenticated request returns HTTP 401. This is the intentional auth-check signal used by the frontend.

**Frontend behaviour**:
- Called once on app mount inside `useAuth` hook.
- `200` → render protected routes (main application).
- `401` → render `LoginPage` at `/login`.

---

### POST /api/auth/logout

Invalidate the current authenticated session.

**Request**

```
POST /api/auth/logout
Cookie: JSESSIONID=<value>
```

No request body.

**Responses**

| Status | When | Body |
|--------|------|------|
| `200 OK` | Session successfully invalidated | `{}` (empty JSON object) |
| `401 Unauthorized` | No active session (already logged out or session expired) | *(Spring Security default)* |

**Side effects on success**:
- Spring Security's `.logout()` DSL invalidates the `HttpSession` and clears the `SecurityContext`.
- The `JSESSIONID` cookie is cleared from the browser.
- Frontend redirects to `/login` after receiving HTTP 200.

**Notes**:
- Configured via `http.logout().logoutUrl("/api/auth/logout").logoutSuccessHandler(...)` in `SecurityFilterChain`.
- A logout button on the main page triggers this endpoint.

---

## SecurityFilterChain Permit-List

| Endpoint | Method | Access |
|----------|--------|--------|
| `/api/auth/login` | `POST` | **Public** (anonymous) |
| `/api/auth/logout` | `POST` | **Public** (anonymous — Spring Security handles this before auth check) |
| `/api/auth/me` | `GET` | **Protected** — returns `401` when unauthenticated; this is the intended frontend auth-check signal |
| All other `/api/**` | any | **Protected** — requires an authenticated `HttpSession` |

> **Rule**: `SecurityFilterChain` calls `.permitAll()` only for `POST /api/auth/login` and `POST /api/auth/logout`. Every other route — including `GET /api/auth/me` — falls under `.anyRequest().authenticated()`.

---

| Rule | Detail |
|------|--------|
| Only `POST /api/auth/login` and `POST /api/auth/logout` are public | All other routes — including `GET /api/auth/me` — require authentication; `SecurityFilterChain` uses `.anyRequest().authenticated()` |
| Session cookie is `HttpOnly` | Browser JS cannot read `JSESSIONID` |
| CORS for local dev | Backend allows `http://localhost:5173` with `allowCredentials=true` |
| CSRF | Disabled for the stateless API surface (REST clients send `Content-Type: application/json`; Spring Security CSRF token not applicable to JSON API callers) |

---

## Implementation Notes

### Backend components

| Component | Role |
|-----------|------|
| `SecurityConfig` | `SecurityFilterChain` bean — permits `POST /api/auth/login` and `POST /api/auth/logout` anonymously; all other routes (including `GET /api/auth/me`) fall under `.anyRequest().authenticated()`; registers `JsonLoginFilter`; sets session creation policy; configures `NoOpPasswordEncoder` (plain text comparison — MVP only, not for production) |
| `JsonLoginFilter` | Extends `UsernamePasswordAuthenticationFilter`; reads JSON body with `ObjectMapper`; sets `filterProcessesUrl("/api/auth/login")`; success handler returns HTTP 200; failure handler returns HTTP 401 JSON |
| `AppUserDetailsService` | Implements `UserDetailsService`; queries `users` table by `username`; returns `UserDetails` with plain text `password` value; throws `UsernameNotFoundException` if not found |
| `AuthController` | `@GetMapping("/api/auth/me")`; reads `Authentication` from `SecurityContextHolder`; returns `AuthMeResponse` |

### Frontend components

| Component | Role |
|-----------|------|
| `useAuth` hook | Calls `GET /api/auth/me` on mount; returns `{ status, username: string | null }` |
| `AuthGuard` | Renders loading state, `LoginPage`, or child routes based on `useAuth` result |
| `LoginPage` | Form with username + password; calls `POST /api/auth/login`; handles success (redirect to `/`) and error (inline message) |
| `authApi.ts` | `login(username, password): Promise<void>`, `getMe(): Promise<{ username: string }>`, and `logout(): Promise<void>` service functions |
| `LogoutButton` | Calls `POST /api/auth/logout`; on HTTP 200 redirects to `/login`; rendered on the main page inside the `<AuthGuard>` protected area (FR-009) |

