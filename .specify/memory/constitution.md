<!--
Sync Impact Report
- Version change: 2.0.0 -> 2.1.0
- Modified principles: none
- Added sections: VIII. Checkstyle Enforcement
- Removed sections: none
- Templates requiring updates:
  - ✅ updated: .specify/templates/plan-template.md
  - ✅ updated: .specify/templates/spec-template.md
  - ✅ updated: .specify/templates/tasks-template.md
  - ⚠ pending: none
- Runtime guidance updated:
  - ✅ updated: specs/001-workout-tracker/plan.md
  - ✅ updated: specs/001-workout-tracker/spec.md
  - ✅ updated: specs/001-workout-tracker/tasks.md
- Follow-up TODOs: none
--> Constitution

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

### VII. English-Only Documentation and Specifications
All project documentation, specifications, plans, tasks, and architecture notes MUST
be written in English. User-facing localized content may vary by product needs, but
engineering artifacts remain English-only. Rationale: one working language reduces
ambiguity and supports collaboration.

### VIII. Checkstyle Enforcement
All new backend code MUST pass checkstyle validation before being committed or merged.
Developers MUST run the checkstyle task locally before every commit. Pull requests that
introduce checkstyle violations MUST NOT be merged. Rationale: automated style
enforcement keeps the codebase consistent, reduces review friction, and prevents
style debt accumulation over time.

## Technology and Quality Constraints

- Backend runtime MUST target Java 21.
- Backend framework MUST be Spring Boot.
- Frontend framework MUST be React.
- Business logic changes MUST include corresponding unit and integration tests.
- Every feature spec MUST include UX consistency expectations and any
  materially relevant non-functional constraints required to define done.
- All specification artifacts MUST be authored in English.
- All new backend code MUST pass checkstyle before commit and merge.

## Development Workflow and Quality Gates

1. Define the smallest prototype slice that delivers user value.
2. Document simplicity decisions and avoid unnecessary abstraction.
3. Author or update tests for all business rules before merge.
4. Verify backend and frontend stack compliance in plan and review.
5. Validate UX consistency against existing interaction patterns.
6. Create or link a GitHub issue for each planned task before implementation when the
   work will be executed through the standard delivery workflow; if a task is not
   tracked as an issue, the reason MUST be documented in the task list or pull
   request.
7. Complete each planned task in its own commit; a commit MUST NOT claim completion
   for multiple task IDs.
8. After merge, review each linked issue or task against acceptance criteria and close
   it when the delivered change satisfies the documented scope.
9. Reject pull requests that violate English-only engineering documentation.
10. Run the checkstyle task locally before every commit; MUST NOT commit code that
    fails checkstyle validation.

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
  checkstyle compliance, and linked task or issue traceability.
- Each planned task SHOULD be created as a GitHub issue so planning, review, and
  closure remain auditable; if a task is not created as an issue, the exception MUST
  be documented in the delivery record.
- Each completed task MUST land in a separate commit that references the relevant task
  ID or linked issue.
- After merge, maintainers MUST review linked issues or task records and close only
  the items whose acceptance criteria are satisfied by the merged change.
- Maintainers MUST review this constitution at least once per quarter to confirm it
  still reflects project needs.

**Version**: 2.1.0 | **Ratified**: 2026-04-26 | **Last Amended**: 2026-05-03
