# Expected File Change Manifest

Codex must verify exact paths in the latest checkout.

## Existing files likely modified

### Entities

- `model/entity/Question.java`
- `model/entity/Quiz.java`
- `model/entity/QuizAttempt.java`
- `model/entity/QuizAnswer.java`

### Repositories

- `repository/QuestionRepository.java`
- `repository/QuizRepository.java`
- `repository/QuizAttemptRepository.java`
- `repository/QuizAnswerRepository.java`

### Services

- `service/impl/AssessmentServiceImpl.java`
- `service/impl/StudentAssessmentServiceImpl.java`
- related service interfaces

### Controllers

- `controller/StudentActionController.java`
- `controller/api/StudentAssessmentRestController.java`
- `controller/api/TeacherAssessmentRestController.java`

### DTOs

- `StudentQuizAttemptDTO`
- `StudentQuizQuestionDTO`
- assessment DTOs
- teacher bank/blueprint DTOs

### Frontend

- actual Student learning template
- actual quiz JavaScript module
- teacher quiz/question management UI
- related CSS only when necessary

### Migration

- new file in `src/main/resources/db/migration`

## New files suggested

- `QuizAttemptQuestion.java`
- `QuizBlueprintItem.java`
- question metadata enums
- `QuizAttemptQuestionRepository.java`
- `QuizBlueprintItemRepository.java`
- `QuizAttemptApplicationService.java`
- `QuizQuestionAssignmentService.java`
- `QuizGradingService.java`
- `StratifiedQuestionSampler.java`
- question-bank services
- AI question-generation package
- focused tests

## Avoid unrelated changes

- authentication/security
- payment
- certificate
- chatbot client/prompt
- coding judge
- global layout/sidebar
