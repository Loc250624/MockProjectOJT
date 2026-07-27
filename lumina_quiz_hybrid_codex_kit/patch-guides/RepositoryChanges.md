# Patch Guide — Repositories

## QuizRepository

Add a pessimistic lock lookup:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select q from Quiz q where q.id = :quizId")
Optional<Quiz> findByIdForUpdate(@Param("quizId") Integer quizId);
```

## QuizAttemptRepository

Add:

- draft lookup for student + quiz;
- count attempts;
- ordered history;
- attempt ownership fetch.

Lock quiz before draft lookup to avoid a concurrent no-row race.

## QuestionRepository

Add approved pool query:

```java
@Query("""
    select q from Question q
    where q.quiz.id = :quizId
      and q.reviewStatus = :status
      and q.active = true
""")
List<Question> findApprovedActiveBank(
    @Param("quizId") Integer quizId,
    @Param("status") QuestionReviewStatus status);
```

Do not use DB random order.

## QuizAttemptQuestionRepository

Required queries:

- ordered rows by attempt
- previous exposure IDs
- question usage counts
- previous question sets for overlap
- existence/ownership support

## QuizAnswerRepository

Add:

```java
Optional<QuizAnswer> findByAttemptIdAndQuestionId(
    Integer attemptId,
    Integer questionId);
```

Enforce unique `(attempt_id, question_id)`.
