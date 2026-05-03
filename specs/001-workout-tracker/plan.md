# Implementation Plan: Workout Tracker

**Repository**: `https://github.com/BedunkevichEvgeny/go-to-the-fucking-gym`
**Branch**: `001-workout-tracker` | **Date**: 2026-04-26 | **Spec**: `specs/001-workout-tracker/spec.md`
**Input**: Feature specification from `specs/001-workout-tracker/spec.md`

## Summary

Deliver a thin vertical slice for workout logging and analysis: users can log program sessions and free sessions, browse history, and view progression charts. The solution uses a Java 21 + Spring Boot backend with LangChain4j + Azure OpenAI integration to expose workout data to an AI Coach pipeline, and a React frontend focused on a simple, modern, no-frills UI with consistent interaction patterns.

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.x (frontend)  
**Primary Dependencies**: Spring Boot 4.0.5, Spring Data JPA, Spring Validation, Spring Security (HTTP Basic Auth for MVP with predefined in-memory users), Azure LangChain Spring Boot starter 1.13.1 (LangChain4j integration), Azure OpenAI SDK, React 18, React Router, TanStack Query, Chart.js/Recharts (progression)  
**Storage**: PostgreSQL 16 (relational model for sessions/history/filtering)  
**Testing**: JUnit 5, Spring Boot Test, Testcontainers (PostgreSQL), REST-assured/MockMvc integration tests, React Testing Library + Vitest for UI logic  
**Target Platform**: Linux container deployment for backend; modern evergreen browsers for frontend  
**Project Type**: Web application (Spring Boot backend + React frontend)  
**Performance Goals**: p95 <= 2s for history/detail/progression reads up to 2 years of data; p95 <= 1.5s for session save; AI handoff enqueue <= 500ms p95  
**Constraints**: Simple explicit logic, no premature abstraction, English-only artifacts, strict per-user data isolation, MVP uses HTTP Basic Auth with predefined in-memory users (no bearer/JWT), no-frills UI prioritizing usability, Java 21 + Spring Boot + React mandated  
**Scale/Scope**: MVP for single-user personal training logs, target up to 10k MAU and up to 5k sessions per user history

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Pre-Design Gate

- [x] Scope is a thin vertical slice that supports fast prototyping.
- [x] Design favors simple, explicit code and avoids tricky abstractions.
- [x] All business logic is mapped to unit and integration tests.
- [x] Backend implementation uses Java 21 and Spring Boot best practices.
- [x] Frontend implementation uses React and shared UX conventions.
- [x] Planned work can be decomposed into discrete tasks that support issue tracking,
  one-task-per-commit delivery, and post-merge closure review.
- [x] Plan and linked artifacts are written in English.

### Post-Design Re-Check

- [x] Design artifacts (`research.md`, `data-model.md`, `quickstart.md`, `contracts/`) maintain thin-slice scope.
- [x] No constitution violations introduced; no exceptions required.

## Project Structure

### Documentation (this feature)

```text
specs/001-workout-tracker/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── workout-tracker-api.yaml
└── tasks.md
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/gymtracker/
│   ├── api/
│   ├── application/
│   ├── domain/
│   ├── infrastructure/
│   └── ai/
├── src/main/resources/
└── src/test/
    ├── unit/
    └── integration/

frontend/
├── src/
│   ├── components/
│   ├── pages/
│   ├── features/
│   ├── services/
│   └── hooks/
└── tests/
```

**Structure Decision**: Use a split web-application layout (`backend/` + `frontend/`) to keep Spring Boot and React concerns isolated while preserving a direct, thin-slice implementation path.

## Phase 2 Planning Outlook

Phase 2 (`/speckit.tasks`) should produce work items in this order: (1) backend domain + persistence, (2) security bootstrap with HTTP Basic Auth + predefined in-memory users, (3) logging APIs + validation, (4) frontend logging/history/progression screens, (5) AI handoff integration with LangChain4j + Azure OpenAI adapters, (6) test matrix completion per FR-to-test mapping. The generated task list should support one GitHub issue per task, one commit per completed task, and post-merge review and closure of the linked issue/task.

## Complexity Tracking

No constitution violations or elevated complexity justifications are required at plan stage.
