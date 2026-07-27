# API Contract

## Lesson API — preserve

### Start/resume

`GET /student/courses/{courseId}/lessons/{lessonId}/quiz`

Behavior:

- return existing DRAFT attempt if present;
- otherwise create attempt and fixed assignment;
- never run AI;
- never rerun sampler for a draft.

Keep current response fields where possible:

```json
{
  "courseId": 1,
  "lessonId": 10,
  "quizId": 5,
  "quizTitle": "Chapter Quiz",
  "attemptId": 91,
  "attemptState": "DRAFT",
  "questions": [
    {
      "id": 1001,
      "questionText": "Question snapshot",
      "optionsJson": "[...]"
    }
  ],
  "answers": {},
  "submitted": false,
  "unavailable": false
}
```

Allowed additive fields:

```json
{
  "startedAt": "2026-07-27T20:00:00",
  "durationMinutes": 20,
  "totalQuestions": 10
}
```

### Save

`POST /student/courses/{courseId}/lessons/{lessonId}/quiz/save`

```json
{
  "attemptId": 91,
  "answers": {
    "1001": "A"
  }
}
```

Rules:

- IDs must belong to assigned set.
- Upsert answer.
- Return same ordered set.
- Submitted attempt cannot be edited.

### Submit

`POST /student/courses/{courseId}/lessons/{lessonId}/quiz/submit`

Rules:

- idempotent;
- grade snapshots;
- no duplicate completion update;
- result follows existing visibility policy.

## Canonical assessment API

Existing `/api/student/quizzes/...` endpoints may remain, but they must call the same application service as lesson endpoints.

## Suggested Teacher APIs

- `GET /api/teacher/quizzes/{quizId}/question-bank`
- `POST /api/teacher/quizzes/{quizId}/question-bank`
- `POST /api/teacher/questions/{questionId}/approve`
- `POST /api/teacher/questions/{questionId}/reject`
- `POST /api/teacher/questions/{questionId}/archive`
- `POST /api/teacher/quizzes/{quizId}/question-bank/bulk-approve`
- `GET /api/teacher/quizzes/{quizId}/blueprint`
- `PUT /api/teacher/quizzes/{quizId}/blueprint`
- `GET /api/teacher/quizzes/{quizId}/readiness`
- `POST /api/teacher/quizzes/{quizId}/question-generation-jobs`

Readiness example:

```json
{
  "ready": false,
  "requiredTotal": 10,
  "approvedTotal": 47,
  "buckets": [
    {
      "topicCode": "TOPIC_A",
      "difficulty": "EASY",
      "required": 2,
      "available": 1,
      "ready": false
    }
  ]
}
```
