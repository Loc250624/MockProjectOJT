# Acceptance Criteria

## Student

- [ ] Fixed set assigned at first start.
- [ ] Refresh returns same set.
- [ ] Blueprint counts are exact.
- [ ] Only APPROVED + active questions are assigned.
- [ ] Previous exposure is minimized.
- [ ] Save/submit rejects unassigned IDs.
- [ ] Grade uses snapshots.
- [ ] Attempt can be audited later.
- [ ] Quiz works when OpenAI is unavailable.
- [ ] Assignment happens at start, not enroll.

## Fairness

- [ ] No pure random selection.
- [ ] No complete duplicate set when alternatives exist.
- [ ] Overlap measured and minimized.
- [ ] Shortage causes clear error, not silent unfair fallback.

## Teacher

- [ ] Topic/difficulty/status/source manageable.
- [ ] AI output starts as DRAFT.
- [ ] Approve/reject/archive works.
- [ ] Blueprint configurable.
- [ ] Readiness reports missing buckets.
- [ ] Publish/start guard works.

## Engineering

- [ ] `QuizAttempt` is canonical.
- [ ] No new quiz `Submission`.
- [ ] Lesson endpoints remain compatible.
- [ ] One shared start/save/submit implementation.
- [ ] Migration additive and backfilled.
- [ ] Coding regression passes.
- [ ] `./mvnw test` passes.
- [ ] `./mvnw clean package` passes.
