package com.ojtsu26.elearning.service.quiz;

import com.ojtsu26.elearning.dto.request.StudentQuizSubmissionRequestDTO;
import com.ojtsu26.elearning.dto.response.StudentQuizAttemptDTO;

public interface QuizAttemptApplicationService {

    StudentQuizAttemptDTO startOrResumeByLesson(
        Integer courseId,
        Integer lessonId);

    StudentQuizAttemptDTO saveByLesson(
        Integer courseId,
        Integer lessonId,
        StudentQuizSubmissionRequestDTO request);

    StudentQuizAttemptDTO submitByLesson(
        Integer courseId,
        Integer lessonId,
        StudentQuizSubmissionRequestDTO request);
}
