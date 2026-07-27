# Target Architecture

## Luồng dữ liệu

```text
Teacher hoặc AI tạo câu hỏi
        |
        v
DRAFT question bank
        |
Validation + Teacher Review
        |
        v
APPROVED question bank
        |
Quiz Blueprint
        |
Student start/resume
        |
Transactional Stratified Assignment
        |
QuizAttempt + QuizAttemptQuestion snapshots
        |
QuizAnswer autosave
        |
Submit + Snapshot Grading
        |
Result + Learning Progress
```

## Aggregate chính

```text
QuizAttempt
  ├── QuizAttemptQuestion
  │     ├── original question reference
  │     ├── order
  │     └── immutable grading/display snapshots
  └── QuizAnswer
        └── student response
```

## Service boundaries

### Runtime

- `QuizAttemptApplicationService`
  - authorization
  - start/resume
  - max attempts
  - save
  - submit

- `QuizQuestionAssignmentService`
  - blueprint
  - approved bank
  - prior exposure
  - usage count
  - persist snapshots

- `StratifiedQuestionSampler`
  - pure selection algorithm

- `QuizGradingService`
  - assigned membership
  - answer normalization
  - snapshot grading

### Teacher

- `QuestionBankService`
- `QuizBlueprintService`
- `QuestionBankReadinessService`

### AI

- `QuestionGenerationProvider`
- `OpenAiQuestionGenerationClient`
- `QuestionBankGenerationService`
- `QuestionValidationService`

## Transaction boundary

`startOrResumeAttempt` phải chạy trong transaction:

1. lock quiz hoặc khóa logic tương đương;
2. find active draft;
3. nếu có, load assignment và return;
4. validate max attempts;
5. validate bank readiness;
6. sample;
7. insert attempt;
8. insert all attempt questions;
9. flush;
10. return DTO.

Không để attempt tồn tại mà chưa có đủ assigned questions.
