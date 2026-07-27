# Patch Guide — AssessmentServiceImpl

Class này đã dùng `QuizAttempt`/`QuizAnswer`, nên là nền tốt hơn.

## Refactor

Các method start/save/submit/result phải dùng canonical application service.

## Start

Thay start đơn giản bằng:

- lock;
- resume;
- max-attempt validation;
- blueprint readiness;
- stratified assignment;
- persist snapshots;
- compute snapshot total.

## Save

Validate answer against `QuizAttemptQuestion`, không phải toàn bộ live quiz bank.

## Submit

Không dùng:

```java
BigDecimal total = totalPoints(attempt.getQuiz());
BigDecimal raw = gradeAttempt(attempt);
```

Dùng ordered assigned rows:

```java
List<QuizAttemptQuestion> assigned =
    attemptQuestionRepository
        .findByAttemptIdOrderByDisplayOrderAsc(attempt.getId());

BigDecimal total = assigned.stream()
    .map(QuizAttemptQuestion::getPointsSnapshot)
    .reduce(BigDecimal.ZERO, BigDecimal::add);

BigDecimal raw = quizGradingService.grade(attempt, assigned, answers);
```

## DTO

- Draft prompt/options lấy từ snapshot.
- Draft response không có correct answer.
- Submitted result dùng snapshot và visibility policy.
