# Rollback and Compatibility

## Existing questions

Backfill to:

- `GENERAL`
- `MEDIUM`
- `APPROVED`
- active
- version 1
- manual source

## Existing quizzes

Create a default blueprint equal to existing question count so current quizzes stay usable.

Teacher can refine blueprint later.

## Existing quiz Submissions

Do not delete in first release.

Options:

1. Keep legacy result reader.
2. Convert only after schema mapping is verified.
3. Expose old records read-only.

## Endpoint compatibility

Keep lesson endpoints and current DTO fields.

Internally delegate to canonical `QuizAttemptApplicationService`.

## Feature flags

Suggested:

```properties
app.quiz.hybrid-assignment-enabled=true
app.quiz.strict-blueprint=true
```

Rollback must not delete snapshots.

## Deployment order

1. Additive migration.
2. Backend that reads old/new data.
3. Enable hybrid assignment.
4. Teacher UI.
5. AI workflow.
6. Remove dead legacy code in a later release.
