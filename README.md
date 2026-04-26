# Workout Tracker

Thin-slice workout tracking application for logging program and free sessions, browsing history, and viewing exercise progression.

## Layout
- `backend/` - Spring Boot 4 backend
- `frontend/` - React 18 + Vite frontend
- `specs/001-workout-tracker/` - feature specs and tasks

## Quick try
```powershell
Set-Location backend
mvn spring-boot:run
```

```powershell
Set-Location frontend
npm install
npm run dev
```

The frontend defaults to calling `http://localhost:8080/api` with HTTP Basic credentials `user1/password1`.

