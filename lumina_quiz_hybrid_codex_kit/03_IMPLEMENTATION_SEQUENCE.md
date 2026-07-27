# Implementation Sequence

## Phase 0 — Baseline

- Chạy test hiện tại.
- Ghi lại endpoint và template đang dùng.
- Search toàn repository cho `Submission`, `QuizAttempt`, `QuizAnswer` và quiz routes.

## Phase 1 — Schema

- Add question metadata.
- Add `QuizBlueprintItem`.
- Add `QuizAttemptQuestion`.
- Add indexes/unique constraints.
- Backfill existing data.

## Phase 2 — Runtime engine

- Implement sampler.
- Implement exposure/usage queries.
- Implement snapshot mapper.
- Implement transactional assignment.
- Implement bank readiness.

## Phase 3 — Canonical service

- Implement `QuizAttemptApplicationService`.
- Refactor `AssessmentServiceImpl`.
- Refactor quiz methods in `StudentAssessmentServiceImpl`.
- Keep Coding logic unchanged.
- Preserve lesson DTO/endpoint compatibility.

## Phase 4 — Save/submit/grade

- Upsert `QuizAnswer`.
- Validate assigned membership.
- Grade snapshots.
- Make submit idempotent.
- Preserve lesson progress behavior.

## Phase 5 — Frontend

- Resume fixed attempt.
- Render backend order.
- Autosave.
- Submit guard.
- Clear states/errors.
- No client randomization.

## Phase 6 — Teacher management

- Metadata and filters.
- Review workflow.
- Blueprint editor.
- Readiness check.
- Publish guard.

## Phase 7 — AI batch generation

- Provider abstraction.
- Structured output.
- Validation.
- DRAFT persistence.
- Job status/retry.

## Phase 8 — Verification

- Unit tests.
- Integration tests.
- MockMvc/security tests.
- Regression tests.
- Package build.
