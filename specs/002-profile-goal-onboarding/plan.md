# Implementation Plan: Profile Goal Onboarding

**Branch**: `002-profile-goal-onboarding` | **Date**: 2026-05-02 | **Spec**: `specs/002-profile-goal-onboarding/spec.md`
**Input**: Feature specification from `specs/002-profile-goal-onboarding/spec.md`

## Summary

Add a mandatory onboarding flow for brand-new users and an optional rerun flow for existing users where users submit profile + goal inputs, receive AI-generated structured program proposals, iterate through reject/feedback revisions, and activate a proposal on acceptance. The implementation reuses the 001 domain model (`WorkoutProgram`, `ProgramSession`, `ProgramExerciseTarget`) and backend/frontend architecture while introducing a minimal onboarding conversation model and a dedicated onboarding AI plan generation path on LangChain4j + Azure OpenAI.

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.x (frontend)  
**Primary Dependencies**: Spring Boot 4.0.5, Spring Data JPA, Spring Security (existing HTTP Basic MVP), Azure LangChain Spring Boot starter 1.13.1 (LangChain4j), Azure OpenAI SDK, React 18, React Router, TanStack Query, Zod/React Hook Form for onboarding form validation  
**Storage**: PostgreSQL 16 (existing schema + new onboarding/proposal linkage tables via Flyway migration)  
**Testing**: JUnit 5, Spring Boot Test, Testcontainers (PostgreSQL), MockMvc integration tests, OpenAPI contract regression tests, React Testing Library + Vitest  
**Target Platform**: Linux container deployment for backend; modern evergreen browsers for frontend  
**Project Type**: Web application (Spring Boot backend + React frontend)  
**Performance Goals**: Proposal generation/revision p95 <= 10s, acceptance activation p95 <= 3s, no measurable regression in existing 001 program-session endpoints  
**Constraints**: Simple explicit logic, one active program per user, preserve historical logs/progression, English-only artifacts, strict user isolation, minimal internal chat history retention only, AI provider fixed to LangChain4j + Azure OpenAI configured via application properties mapped to env vars (`AZURE_OPENAI_ENDPOINT`, `AZURE_OPENAI_API_KEY`, `AZURE_OPENAI_DEPLOYMENT`)  
**Scale/Scope**: MVP scope for current authenticated user base; one active onboarding attempt per user and proposal revision chain until accept/exit

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

- [x] Design artifacts (`research.md`, `data-model.md`, `quickstart.md`, `contracts/`) maintain thin-slice scope.
- [x] No constitution violations introduced; no exceptions required.

## Project Structure

### Documentation (this feature)

```text
specs/002-profile-goal-onboarding/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── profile-goal-onboarding-api.yaml
└── tasks.md
```

### Source Code (repository root)

```text
backend/
├── src/
│   ├── main/java/com/gymtracker/
│   │   ├── api/
│   │   ├── application/
│   │   ├── domain/
│   │   ├── infrastructure/
│   │   └── infrastructure/ai/
│   └── main/resources/
│       ├── application.properties
│       └── db/migration/
└── src/test/

frontend/
├── src/
│   ├── components/
│   ├── pages/
│   ├── features/
│   └── services/
└── tests/
```

**Structure Decision**: Reuse the existing split web-app structure from feature 001 and add onboarding-specific backend API/application/domain components plus frontend feature pages/services. Keep AI integration in `backend/src/main/java/com/gymtracker/infrastructure/ai` to align with existing LangChain4j/Azure OpenAI integration points.

## Technical Decisions (Phase 0 + Phase 1)

1. **AI chat provider and integration path**: Keep provider fixed to LangChain4j + Azure OpenAI and implement onboarding proposal generation through a dedicated onboarding service that reuses shared AI client wiring patterns.
2. **AI configuration model**: Configure endpoint/api key/deployment via backend application properties mapped to environment variables (`AZURE_OPENAI_ENDPOINT`, `AZURE_OPENAI_API_KEY`, `AZURE_OPENAI_DEPLOYMENT`) and inject through typed Spring configuration.
3. **Chat history scope**: Persist minimal internal revision linkage (attempt -> proposals -> feedback) for continuity and recovery; do not add long-term user-visible transcript UI.
4. **Activation behavior**: On accept, transactionally mark previous active program non-active, create new `WorkoutProgram` + `ProgramSession` + targets, and preserve historical `LoggedSession` rows untouched.
5. **Contract boundary**: Add onboarding-specific API contract under `specs/002-profile-goal-onboarding/contracts/` and keep all 001 endpoints backward-compatible.

## Phase 2 Planning Outlook

`/speckit.tasks` should produce task groups in this sequence:

1. **Database migration + domain**: Add onboarding attempt/proposal/feedback persistence and indexes; include migration rollback/forward notes and integrity constraints.
2. **Backend onboarding APIs + AI service**: Implement create/revise/accept endpoints, onboarding gating checks, LangChain4j + Azure OpenAI prompt pipeline, and error/retry handling.
3. **Program activation integration**: Map accepted proposals into existing 001 entities and enforce replacement-with-history-preservation behavior.
4. **Frontend onboarding flow**: Add entry point, profile/goal form, proposal review card, reject-feedback loop, and mandatory redirect behavior for brand-new users.
5. **Contracts + compatibility tests**: Add contract tests for new onboarding endpoints and regression tests asserting unchanged 001 program/session/history/progression behavior.
6. **Smoke coverage (minimal placeholder)**: Keep smoke-task scaffolding minimal; include only hooks/placeholders so user can provide final smoke scenarios.

**Migration implications for `/speckit.tasks`**: Include tasks for a new Flyway migration adding onboarding/proposal/feedback tables plus foreign keys to users and accepted program references.

**Contracts implications for `/speckit.tasks`**: Include tasks for implementing and validating `contracts/profile-goal-onboarding-api.yaml`, and for explicit backward-compatibility checks against `specs/001-workout-tracker/contracts/workout-tracker-api.yaml`.

**Task decomposition implications for `/speckit.tasks`**: Enforce one issue per task and one task per commit, with separate tasks for migration, API, AI integration, frontend UX, and test matrix completion.

## Complexity Tracking

No constitution violations or elevated complexity justifications are required at plan stage.
