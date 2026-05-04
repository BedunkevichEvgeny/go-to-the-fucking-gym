# Quickstart: Login Page

**Feature**: 004-login-page  
**Date**: 2026-05-04

---

## Prerequisites

- Java 21 + Maven (backend)
- Node 20 + npm (frontend)
- PostgreSQL 16 running locally (or Docker)
- Existing application up and running (`backend/LOCAL_DEV.md`)

---

## Step 1 — Run the DB Migration

The migration adds `username` and `password_hash` columns to the `users` table.

```powershell
# From repo root
cd backend
mvn flyway:migrate
```

After migration the `users` table has two new columns. You can verify:

```sql
SELECT column_name FROM information_schema.columns
WHERE table_name = 'users';
-- should include: username, password_hash
```

---

## Step 2 — Seed a Test User

There is no registration flow. Use psql or a migration seed to create a user with a BCrypt-hashed password.

**Option A — psql one-liner** (generates BCrypt hash for password `password123`):

```sql
INSERT INTO users (id, username, password_hash, preferred_weight_unit)
VALUES (
  gen_random_uuid(),
  'testuser',
  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh/.',  -- BCrypt of "password123"
  'KG'
);
```

> The hash above is a known BCrypt encoding of `password123`. Replace it for a real deployment.

**Option B — Spring Boot ApplicationRunner** (dev-only bean, disabled in prod):

Add a `DataInitializer` component annotated `@Profile("dev")` that encodes and inserts credentials at startup. This is outside the scope of this feature but documented here for convenience.

---

## Step 3 — Start the Backend

```powershell
cd backend
mvn spring-boot:run
```

Spring Security is now active. All API endpoints except `POST /api/auth/login` require authentication.

---

## Step 4 — Start the Frontend

```powershell
cd frontend
npm install
npm run dev
```

The app opens at `http://localhost:5173`. Because no session exists, `GET /api/auth/me` returns 401 and the login page is displayed.

---

## Step 5 — Log In

1. Enter `testuser` / `password123` in the login form.
2. Click **Sign in**.
3. The app posts to `http://localhost:8080/api/auth/login` and receives a `JSESSIONID` cookie.
4. The frontend detects success (HTTP 200) and navigates to `/`.

---

## Local Dev CORS Note

In local development the React dev server (`localhost:5173`) and backend (`localhost:8080`) are on different ports. The `SecurityConfig` includes a CORS configuration:

```java
.cors(cors -> cors.configurationSource(request -> {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("http://localhost:5173"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);
    return config;
}))
```

This allows the browser to send the `JSESSIONID` cookie cross-origin.

**Alternative** — configure Vite proxy (avoids CORS entirely):

```ts
// vite.config.ts
server: {
  proxy: {
    '/api': 'http://localhost:8080'
  }
}
```

If using the Vite proxy, remove the CORS configuration from `SecurityConfig`.

---

## Verify the Auth Endpoints Manually

```powershell
# Should return 401 (no session)
Invoke-WebRequest -Uri http://localhost:8080/api/auth/me -Method GET

# Login — should return 200 + Set-Cookie: JSESSIONID=...
$response = Invoke-WebRequest -Uri http://localhost:8080/api/auth/login `
  -Method POST `
  -ContentType "application/json" `
  -Body '{"username":"testuser","password":"password123"}' `
  -SessionVariable session

# Auth check with session — should return 200 + { "username": "testuser" }
Invoke-WebRequest -Uri http://localhost:8080/api/auth/me -Method GET -WebSession $session
```

---

## Running Tests

```powershell
# Backend security + integration tests
cd backend
mvn test -Dtest="JsonLoginFilterTest,AppUserDetailsServiceTest,AuthControllerIT"

# Frontend login page tests
cd frontend
npx vitest run tests/LoginPage.test.tsx tests/AuthGuard.test.tsx
```

