# Research: Workout Tracker

## Decision 1: Backend architecture uses Spring Boot modular layers with explicit service boundaries
- Decision: Implement backend with clear `api -> application -> domain -> infrastructure` flow, avoiding deep abstraction.
- Rationale: Aligns with constitution principle "Simplicity Over Cleverness" and supports fast prototype delivery while preserving testability.
- Alternatives considered: Hexagonal architecture with many ports/adapters (rejected for MVP complexity); anemic single-layer controllers (rejected due to weak separation for growing rules).

## Decision 2: Persistence uses PostgreSQL 16 relational schema
- Decision: Store programs, sessions, entries, sets/laps, and feelings in PostgreSQL 16.
- Rationale: Feature requires filtering, pagination, and timeline/progression queries with strict user isolation, which map naturally to relational indexes and joins.
- Alternatives considered: MongoDB document model (rejected because complex cross-session progression/filter queries become less predictable); flat file/event log (rejected for transactional integrity and query performance).

## Decision 3: LLM integration uses Azure LangChain Spring Boot starter 1.13.1 with LangChain4j + Azure OpenAI asynchronous handoff
- Decision: Use Azure LangChain Spring Boot starter `1.13.1` in a Spring Boot `4.0.5` backend, and implement an AI handoff service that packages logged session summaries into a LangChain4j workflow backed by Azure OpenAI.
- Rationale: Keeps LangChain4j integration first-class while pinning the requested starter/version combination, maintains a fast session save path, and supports future adaptive program generation without blocking user actions.
- Alternatives considered: Direct synchronous Azure OpenAI calls in request thread (rejected due to latency risk); custom HTTP wrapper without LangChain4j (rejected because it conflicts with stack requirement).

## Decision 4: API style is REST + OpenAPI contract-first for frontend/backend boundary
- Decision: Define REST endpoints for session logging, history queries, progression data, and program-session retrieval in `contracts/workout-tracker-api.yaml`.
- Rationale: Clear contract supports parallel React development and contract/integration testing.
- Alternatives considered: GraphQL (rejected for MVP overhead); undocumented controller-first API (rejected due to higher integration risk).

## Decision 5: Frontend stack and UX conventions prioritize simple no-frills usability
- Decision: Use React 18 + TypeScript, React Router, TanStack Query, and form components with consistent interaction patterns for program and free sessions.
- Rationale: Satisfies React mandate and user requirement for a simple modern UI; shared input patterns reduce cognitive load.
- Alternatives considered: Redux-heavy architecture (rejected as unnecessary for MVP state size); UI framework with high visual complexity (rejected due to "no frills" requirement).

## Decision 6: Testing strategy follows business-rule matrix from spec
- Decision: Implement unit + integration tests for every business rule; add contract tests for API schema compatibility.
- Rationale: Constitution requires mandatory business-logic coverage and CI-verifiable correctness.
- Alternatives considered: Integration-only coverage (rejected because debugging/regression isolation is weaker); unit-only coverage (rejected because boundary behavior is unverified).

## Decision 7: Performance strategy uses indexed read paths and pre-aggregated progression metrics
- Decision: Add indexes for `(user_id, session_date desc)`, exercise name search keys, and progression query fields; compute chart-ready points in query layer with pagination windows.
- Rationale: Required outcomes demand <=2s loads for two years of data and smooth history browsing.
- Alternatives considered: Full table scans with in-memory filtering (rejected for latency risk); immediate caching layer dependency (rejected for MVP complexity).

## Decision 8: MVP security uses HTTP Basic Auth with predefined in-memory users and strict user scoping
- Decision: Use Spring Security HTTP Basic Auth (no bearer/JWT) with a small predefined in-memory user set configured in application properties for MVP, and enforce authenticated `user_id` scoping on every read/write operation.
- Rationale: It is the simplest non-destructive MVP authentication approach, minimizes setup complexity, and still directly fulfills FR-019 and constitution quality gates.
- Alternatives considered: Bearer/JWT setup (rejected for MVP complexity); external identity provider integration (rejected as out of slice); client-side filtering (rejected as insecure).

## Resolved Clarifications
- Storage technology: PostgreSQL 16 selected.
- Backend integration pattern for AI Coach: async LangChain4j workflow via Azure LangChain Spring Boot starter 1.13.1 with Azure OpenAI selected.
- Frontend state/query strategy: TanStack Query + localized component state selected.
- Performance target definition: p95 budgets established for save/read critical paths.
- Project split decision: dedicated `backend/` and `frontend/` workspaces selected.
