package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.request.StudentCodeSubmissionRequestDTO;
import com.ojtsu26.elearning.dto.request.StudentQuizSubmissionRequestDTO;
import com.ojtsu26.elearning.dto.response.StudentCodingAssignmentDTO;
import com.ojtsu26.elearning.dto.response.StudentQuizAttemptDTO;

public interface StudentAssessmentService {
    StudentQuizAttemptDTO getOrStartQuiz(Integer courseId, Integer lessonId);
    StudentQuizAttemptDTO saveQuizDraft(Integer courseId, Integer lessonId, StudentQuizSubmissionRequestDTO request);
    StudentQuizAttemptDTO submitQuiz(Integer courseId, Integer lessonId, StudentQuizSubmissionRequestDTO request);
    StudentCodingAssignmentDTO getCodingAssignment(Integer courseId, Integer lessonId);
    StudentCodingAssignmentDTO saveCodeDraft(Integer courseId, Integer lessonId, StudentCodeSubmissionRequestDTO request);
    StudentCodingAssignmentDTO submitCode(Integer courseId, Integer lessonId, StudentCodeSubmissionRequestDTO request);
}
