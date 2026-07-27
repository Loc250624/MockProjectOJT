# Test Plan

## Unit — sampler

1. Exact count for every blueprint bucket.
2. Only APPROVED + active.
3. Prefer unseen.
4. Reuse seen only when unseen pool is insufficient.
5. No wrong-topic/difficulty fallback in strict mode.
6. No duplicate IDs.
7. Avoid identical full set when alternatives exist.
8. Reduce overlap.
9. Stable order after persistence.

## Unit — grader

1. Reject unassigned ID.
2. Missing answer scores zero.
3. Use `pointsSnapshot`.
4. Use `correctAnswerSnapshot`.
5. Ignore current `Question.correctAnswer`.
6. Handle zero total safely.
7. Normalize answers consistently with current option codec.

## Integration — start/resume

1. First start creates one `QuizAttempt`.
2. Creates exactly N `QuizAttemptQuestion`.
3. Refresh returns same attempt ID and ordered question IDs.
4. Two concurrent starts create one draft.
5. `maxAttempts` enforced.
6. Unpublished quiz denied.
7. Unenrolled Student denied.
8. Insufficient bank creates no partial attempt.
9. AI provider interaction count is zero.

## Integration — save/submit

1. Save upserts.
2. Foreign/unassigned question rejected.
3. Grade only assigned set.
4. Repeated submit does not re-grade.
5. Edit live question after start does not alter result.
6. Archive live question after start does not break result.
7. Progress update happens once.
8. Pass/fail behavior matches existing policy.

## Controller/security

- CSRF required for state-changing endpoints.
- Student cannot access Teacher APIs.
- Teacher cannot edit another Teacher's quiz.
- Draft JSON does not contain correct answer.
- Result visibility follows policy.

## Regression

- Coding save/run/submit works.
- Teacher grading works.
- Existing result page works.
- Lesson route and standalone route do not diverge.
- Auth/payment/certificate/chatbot unaffected.

## Manual scenarios

### Refresh stability

1. Start attempt.
2. Record IDs/order.
3. Refresh.
4. Verify unchanged.

### Retry variety

1. Submit attempt.
2. Start next attempt.
3. Verify exact blueprint.
4. Verify unseen preference.

### Teacher update during attempt

1. Start.
2. Teacher edits question.
3. Submit old attempt.
4. Verify snapshot display/grade.

### OpenAI outage

1. Disable key/provider.
2. Open approved quiz.
3. Verify quiz works.
