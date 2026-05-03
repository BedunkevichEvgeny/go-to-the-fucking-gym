# Quickstart: Post-Session AI Suggestion

**Feature**: `003-post-session-ai-suggestion`  
**Date**: 2026-05-03

## Prerequisites

- Docker (for Testcontainers PostgreSQL in integration tests)
- Java 21 JDK
- Node.js 20+

## Backend Setup

```bash
cd backend
# Build and run all tests
./mvnw verify

# Run checkstyle before committing
./mvnw checkstyle:check

# Start backend locally
./mvnw spring-boot:run
```

## Frontend Setup

```bash
cd frontend
npm install
npm test          # unit tests (Vitest)
npm run dev       # dev server at http://localhost:5173
```

## Environment Variables

The AI generation requires Azure OpenAI credentials configured via:

```
AZURE_OPENAI_ENDPOINT=https://<your-instance>.openai.azure.com/
AZURE_OPENAI_API_KEY=<your-key>
AZURE_OPENAI_DEPLOYMENT=<your-deployment>
```

For local testing without real AI output, mock `LangChainSessionProcessor` in tests (see `AiHandoffServiceTest`).

## End-to-End Smoke Test

1. Start the backend (`./mvnw spring-boot:run`).
2. Start the frontend (`npm run dev`).
3. Log in as a test user with an active program.
4. Navigate to **Program Session** and complete the form.
5. Submit the session.
6. Observe the **AI Coaching Insight** card appearing within ~15 seconds with a loading indicator followed by suggestion text.
7. Click **Continue to History**.
8. Find the saved session in history and open its detail view.
9. Confirm the same suggestion text appears in the **AI Coaching Insight** card.
10. For a free session: confirm no AI Coaching Insight section appears.

## Key Files Modified / Created

| File | Status | Purpose |
|---|---|---|
| `backend/src/main/resources/db/migration/V003__session_ai_suggestion.sql` | NEW | Creates `session_ai_suggestions` table |
| `backend/…/domain/SessionAiSuggestion.java` | NEW | JPA entity for the suggestion |
| `backend/…/repository/SessionAiSuggestionRepository.java` | NEW | Spring Data JPA repository |
| `backend/…/ai/AiHandoffService.java` | MODIFIED | Persists suggestion instead of logging |
| `backend/…/api/dto/LoggedSessionDetail.java` | MODIFIED | Adds `aiSuggestion` field |
| `backend/…/mapper/DtoMapper.java` | MODIFIED | Maps `aiSuggestion` to DTO |
| `frontend/src/types/api.ts` | MODIFIED | Adds `aiSuggestion` to `LoggedSessionDetail` |
| `frontend/src/hooks/usePollSessionSuggestion.ts` | NEW | Polling hook |
| `frontend/src/pages/ProgramSessionPage.tsx` | MODIFIED | Polls and renders suggestion after save |
| `frontend/src/features/history/SessionDetailView.tsx` | MODIFIED | Renders suggestion card |

## Running New Tests Only

```bash
# Backend: run only AI-related tests
cd backend
./mvnw test -Dtest="AiHandoffServiceTest,DtoMapperTest,SessionDetailServiceIT,LoggedSessionServiceIT,SessionHistoryServiceIT"

# Frontend: run only modified test files
cd frontend
npx vitest run src/features/history/__tests__/SessionDetailView.test.tsx
npx vitest run tests/ProgramSessionPage.ai-suggestion.test.tsx
```

