# Patch Guide — StudentAssessmentServiceImpl

## Vấn đề hiện tại

Quiz methods đang:

- load toàn bộ live questions;
- tạo `Submission`;
- lưu answers JSON;
- grade live question list.

## Thay đổi bắt buộc

Giữ Coding methods, nhưng thay quiz methods bằng delegation:

```java
@Override
@Transactional
public StudentQuizAttemptDTO getOrStartQuiz(Integer courseId, Integer lessonId) {
    return quizAttemptApplicationService.startOrResumeByLesson(courseId, lessonId);
}

@Override
@Transactional
public StudentQuizAttemptDTO saveQuizDraft(
        Integer courseId,
        Integer lessonId,
        StudentQuizSubmissionRequestDTO request) {
    return quizAttemptApplicationService.saveByLesson(courseId, lessonId, request);
}

@Override
@Transactional
public StudentQuizAttemptDTO submitQuiz(
        Integer courseId,
        Integer lessonId,
        StudentQuizSubmissionRequestDTO request) {
    return quizAttemptApplicationService.submitByLesson(courseId, lessonId, request);
}
```

Ngừng dùng quiz-only helpers dựa trên `Submission`:

- create quiz Submission
- current draft Submission
- quiz JSON state
- live-question scoring
- membership validation against all quiz questions

Không phá Coding payload/helper.
