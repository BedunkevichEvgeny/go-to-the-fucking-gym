<!--
Sync Impact Report
- Version change: 0.0.0-template -> 1.0.0
- Modified principles:
  - Template Principle 1 -> I. Fast Prototype Delivery
  - Template Principle 2 -> II. Simplicity Over Cleverness
  - Template Principle 3 -> III. Mandatory Business Logic Test Coverage
  - Template Principle 4 -> IV. Java 21 + Spring Boot Backend Standards
  - Template Principle 5 -> V. React Frontend Standards
  - Added: VI. Consistent User Experience
  - Added: VII. Performance and Low Latency by Default
  - Added: VIII. English-Only Documentation and Specifications
- Added sections:
  - Technology and Quality Constraints
  - Development Workflow and Quality Gates
- Removed sections: none
- Templates requiring updates:
  - ✅ updated: .specify/templates/plan-template.md
  - ✅ updated: .specify/templates/spec-template.md
  - ✅ updated: .specify/templates/tasks-template.md
  - ⚠ pending: none
- Follow-up TODOs: none
-->

# Go To The Fucking Gym Constitution

## Core Principles

### I. Fast Prototype Delivery
Every feature increment MUST prioritize shortest path to a usable outcome.
Teams MUST ship thin vertical slices first, defer non-essential scope, and prove value
with running software before optimization or broad refactoring. Rationale: rapid
feedback lowers product risk and improves requirement accuracy.

### II. Simplicity Over Cleverness
Code and architecture MUST favor straightforward solutions and predictable behavior.
Complex patterns, hidden control flow, and edge-case-heavy abstractions MUST NOT be
introduced without documented justification in the implementation plan. Rationale:
simple systems are faster to build, easier to maintain, and less error-prone.

### III. Mandatory Business Logic Test Coverage
All business logic MUST be covered by automated tests. Coverage MUST include unit
tests and integration tests; contract and end-to-end tests MUST be added when they
validate cross-boundary behavior. A feature is incomplete until its business rules are
verifiably tested in CI. Rationale: test coverage protects correctness and delivery
speed as scope evolves.

### IV. Java 21 + Spring Boot Backend Standards
Backend services MUST use Java 21 and Spring Boot conventions and best practices.
Implementations MUST use idiomatic Spring patterns, explicit configuration,
observability-ready APIs, and secure defaults. Deviations from Java 21 or Spring Boot
stack choices require explicit constitution exception approval. Rationale: consistent
stack decisions improve velocity, hiring fit, and operational reliability.

### V. React Frontend Standards
All web frontend user interfaces MUST be implemented in React. Shared component
patterns, state management conventions, and accessibility expectations MUST be applied
consistently across screens. Rationale: a single frontend paradigm enables consistent
UX and maintainable delivery.

### VI. Consistent User Experience
User journeys MUST be coherent across features, including terminology, navigation,
visual behavior, feedback messages, and error handling. New UI flows MUST align with
established interaction patterns unless a documented UX decision approves divergence.
Rationale: consistency reduces user confusion and support load.

### VII. Performance and Low Latency by Default
Features MUST define measurable latency and performance targets before implementation.
Design and code reviews MUST verify that critical paths meet agreed response-time
budgets (for example, p95/p99 targets) under expected load. Rationale: performance is
a product requirement, not a post-release optimization task.

### VIII. English-Only Documentation and Specifications
All project documentation, specifications, plans, tasks, and architecture notes MUST
be written in English. User-facing localized content may vary by product needs, but
engineering artifacts remain English-only. Rationale: one working language reduces
ambiguity and supports collaboration.

## Technology and Quality Constraints

- Backend runtime MUST target Java 21.
- Backend framework MUST be Spring Boot.
- Frontend framework MUST be React.
- Business logic changes MUST include corresponding unit and integration tests.
- Every feature spec MUST include UX consistency expectations and measurable
  performance criteria.
- All specification artifacts MUST be authored in English.

## Development Workflow and Quality Gates

1. Define the smallest prototype slice that delivers user value.
2. Document simplicity decisions and avoid unnecessary abstraction.
3. Author or update tests for all business rules before merge.
4. Verify backend and frontend stack compliance in plan and review.
5. Validate UX consistency against existing interaction patterns.
6. Verify performance targets with tests, profiling, or benchmark evidence.
7. Reject pull requests that violate English-only engineering documentation.

## Governance

This constitution overrides conflicting local habits and template defaults.

Amendment process:
1. Propose changes through a documented update to
   `.specify/memory/constitution.md`.
2. Include impact assessment for templates, workflows, and existing specs.
3. Obtain explicit maintainer approval in code review before merge.
4. Update dependent artifacts in the same change set or track explicit follow-up
   tasks.

Versioning policy:
- MAJOR: incompatible governance changes or principle removals/redefinitions.
- MINOR: new principle/section or materially expanded mandatory guidance.
- PATCH: wording clarifications, typo fixes, or non-semantic refinements.

Compliance review expectations:
- Every implementation plan MUST pass a constitution check before design and again
  before implementation.
- Every pull request MUST show evidence of testing, stack compliance, UX review,
  and performance consideration when applicable.
- Periodic governance review SHOULD occur at least once per quarter to confirm this
  constitution still reflects project needs.

**Version**: 1.0.0 | **Ratified**: 2026-04-26 | **Last Amended**: 2026-04-26
