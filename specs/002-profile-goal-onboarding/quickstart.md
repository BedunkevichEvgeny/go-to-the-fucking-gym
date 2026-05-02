# Quickstart: Profile Goal Onboarding

## Purpose
Run the minimal local workflow for profile/goal onboarding, AI proposal generation/revision, and accepted plan activation against the existing 001 stack.

## Prerequisites
- Java 21
- Node.js (frontend local run)
- Docker (if running PostgreSQL/Testcontainers locally)
- Existing 001 baseline dependencies already available in this repository

## Environment Configuration (Backend)
Set Azure OpenAI variables before starting backend:

- `AZURE_OPENAI_ENDPOINT`
- `AZURE_OPENAI_API_KEY`
- `AZURE_OPENAI_DEPLOYMENT`

These map to backend application properties:
- `azure.openai.endpoint=${AZURE_OPENAI_ENDPOINT:...}`
- `azure.openai.api-key=${AZURE_OPENAI_API_KEY:...}`
- `azure.openai.deployment=${AZURE_OPENAI_DEPLOYMENT:...}`

## Minimal Run Sequence
1. Start backend with feature migration applied.
2. Start frontend and open the first page.
3. Open `My Profile & Goals`.
4. Submit valid age/weight/goal and verify proposal appears.
5. Reject proposal once, submit feedback, verify revised proposal appears.
6. Accept proposal and verify existing next program session endpoint returns program derived from accepted plan.

## Validation Focus
- Input validation errors for invalid age/weight/goal.
- Revision continuity after page refresh/navigation.
- Immediate activation behavior and preservation of prior history/progression data.
- Backward compatibility of existing 001 endpoints.

## Test Execution Focus
- Backend unit tests for onboarding validation, revision flow, activation mapping.
- Backend integration tests for onboarding endpoints + 001 regression compatibility.
- Frontend tests for onboarding form/review/revision behavior.

## Smoke Tests
Smoke setup stays intentionally minimal in this plan. Final smoke scenarios/commands will be added by user direction during implementation and task breakdown.

