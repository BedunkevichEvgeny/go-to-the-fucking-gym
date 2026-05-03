# Research: Profile Goal Onboarding

## Decision 1: Reuse 001 layered backend architecture and add onboarding-specific services
- Decision: Implement onboarding with the existing `api -> application -> domain -> infrastructure` structure and keep business logic in explicit services.
- Rationale: Maintains consistency with feature 001, keeps the slice thin, and avoids introducing a second architectural style.
- Alternatives considered: New standalone onboarding microservice (rejected due to unnecessary operational and integration complexity); controller-heavy implementation with inline logic (rejected due to reduced testability).

## Decision 2: AI proposal generation and revision use LangChain4j + Azure OpenAI
- Decision: Build onboarding proposal and revision generation with LangChain4j using Azure OpenAI as the only provider for this feature.
- Rationale: Matches the mandated stack and existing repository direction where AI integration already uses LangChain/Azure patterns.
- Alternatives considered: Direct custom HTTP client to Azure OpenAI without LangChain4j (rejected because it violates the requested integration approach); non-Azure model provider (rejected by constraint).

## Decision 3: Azure OpenAI configuration stays property-driven and env-mapped
- Decision: Configure Azure endpoint, API key, and deployment/model name in `application.properties` using environment-variable mapping (`AZURE_OPENAI_ENDPOINT`, `AZURE_OPENAI_API_KEY`, `AZURE_OPENAI_DEPLOYMENT`).
- Rationale: Keeps deployment-safe configuration, aligns with existing backend configuration conventions, and avoids hardcoded secrets.
- Alternatives considered: Hardcoded values in Java config (rejected for security and operability risks); separate config file not env-backed (rejected for container deployment friction).

## Decision 4: Persist minimal internal chat history linkage only
- Decision: Store onboarding attempt metadata plus proposal and feedback records sufficient for revise/continue/accept flow continuity; do not expose long-term conversation transcript in UI.
- Rationale: Satisfies FR-012 and clarified scope while keeping storage and API shape simple.
- Alternatives considered: Full transcript retention and retrieval APIs (rejected as out of scope); no persistence for rejected iterations (rejected because revision continuity would break after refresh/navigation).

## Decision 5: Activation maps directly into existing 001 entities
- Decision: Accepted proposals are transformed into `WorkoutProgram`, `ProgramSession`, and `ProgramExerciseTarget` records in a transactional activation step that immediately replaces the active program.
- Rationale: Directly satisfies integration constraints IC-001..IC-005 and avoids introducing parallel program models.
- Alternatives considered: New onboarding-only program schema with later synchronization (rejected for dual-model complexity and compatibility risk).

## Decision 6: API contract strategy adds onboarding endpoints without breaking 001 endpoints
- Decision: Define a dedicated OpenAPI contract (`contracts/profile-goal-onboarding-api.yaml`) for onboarding/revision/accept/gating endpoints and preserve existing 001 contract semantics.
- Rationale: Enables parallel implementation and explicit compatibility testing.
- Alternatives considered: Merge onboarding paths into 001 contract only (rejected because feature boundaries become less clear and tasks become harder to trace).

## Decision 7: Validation and UX guardrails remain simple and explicit
- Decision: Apply strict backend validation for age/weight/goal and mirror validation on frontend forms; use clear error messages and keep interaction flow linear.
- Rationale: Supports completion rates and reduces support friction while preserving predictable behavior.
- Alternatives considered: AI-side validation only (rejected because invalid inputs should be blocked before AI calls); permissive submission with post-hoc cleanup (rejected due to unstable outcomes).

## Decision 8: Smoke coverage remains minimal in planning artifacts
- Decision: Keep smoke-test planning as a minimal placeholder in this phase and defer final smoke details to user-provided inputs.
- Rationale: Respects user-specific planning constraint while preserving traceability for `/speckit.tasks`.
- Alternatives considered: Fully detailed smoke matrix now (rejected per explicit user request).

## Resolved Clarifications and Unknowns
- AI provider for chat/revision flow: Fixed to LangChain4j + Azure OpenAI.
- AI runtime configuration: Property-driven and env-mapped for endpoint, key, and deployment/model.
- Chat history scope: Minimal internal linkage persisted; no long-term user-visible transcript.
- Integration compatibility: Activation must map to existing 001 program/session entities without endpoint regression.
- Smoke planning depth: Minimal placeholder only at this planning phase.

