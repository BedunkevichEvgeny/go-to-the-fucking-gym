# Quickstart: Workout Tracker

## Purpose
This quickstart describes the planned development flow for implementing feature `001-workout-tracker` with Java 21 + Spring Boot 4.0.5 backend and React frontend.

## Prerequisites
- Java 21
- Node.js 20+ and npm
- Docker (for local PostgreSQL and Testcontainers support)
- MVP authentication seed users configured in backend properties for Spring Security HTTP Basic Auth (in-memory user store)
- Azure OpenAI deployment details:
  - `AZURE_OPENAI_ENDPOINT`
  - `AZURE_OPENAI_API_KEY`
  - `AZURE_OPENAI_DEPLOYMENT`

## Proposed Repository Layout
- `backend/` - Spring Boot service (REST API, persistence, AI handoff)
- `frontend/` - React web app (logging, history, progression views)
- `specs/001-workout-tracker/contracts/` - API contract source

## Backend Bootstrap (planned)
```powershell
# from repository root
New-Item -ItemType Directory -Path backend -Force
Push-Location backend
# scaffold Spring Boot 4.0.5 app with Java 21 and Maven/Gradle per team standard
# add Azure LangChain Spring Boot starter 1.13.1 and Azure OpenAI integration
# configure Spring Security HTTP Basic Auth with predefined in-memory users in application properties
Pop-Location
```

## Frontend Bootstrap (planned)
```powershell
# from repository root
New-Item -ItemType Directory -Path frontend -Force
Push-Location frontend
# scaffold React + TypeScript app (e.g., Vite)
Pop-Location
```

## Local Development Flow
1. Start PostgreSQL locally (Docker or local service).
2. Run backend with profile `local`, DB migration enabled, and HTTP Basic Auth seed users loaded.
3. Run frontend against local backend base URL.
4. Verify end-to-end flow:
   - Load next program session
   - Save a program session with feelings
   - Save a free session with custom exercise
   - View history and progression chart

## Test Flow
- Backend unit tests: service/domain validation and user isolation rules.
- Backend integration tests: API persistence, filtering, progression, auth scoping.
- Contract tests: backend responses match `contracts/workout-tracker-api.yaml`.
- Frontend tests: form validation, consistent UX interactions, empty states.

## Performance Verification
- Measure p95 latencies for:
  - Session save (target <=1.5s)
  - History list/detail/progression reads (target <=2s)
- Use realistic seed data for two years of sessions in performance checks.

## UX Principle
Keep UI intentionally minimal: plain layout, clear labels, high-contrast inputs, and consistent add/edit/remove interactions across program and free session screens.
