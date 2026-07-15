package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.*;
import com.ojtsu26.elearning.model.enums.SubmissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AssessmentService {
    QuizView getStudentQuiz(Integer courseId, Integer quizId);
    QuizAttemptView startQuizAttempt(Integer quizId);
    QuizAttemptView saveQuizDraft(Integer attemptId, QuizDraftPayload payload);
    QuizAttemptView submitQuizAttempt(Integer attemptId, QuizDraftPayload payload);
    ResultSummaryView getStudentResults();
    QuizAttemptView getStudentQuizResult(Integer attemptId);
    SubmissionView getStudentSubmissionResult(Integer submissionId);
    SubmissionView getAssignmentForSubmission(Integer assignmentId);
    SubmissionView saveSubmissionDraft(Integer assignmentId, AssignmentSubmissionPayload payload);
    SubmissionView submitAssignment(Integer assignmentId, AssignmentSubmissionPayload payload);

    List<QuizView> getTeacherCourseQuizzes(Integer courseId);
    QuizView createTeacherQuiz(Integer courseId, QuizPayload payload);
    QuizView updateTeacherQuiz(Integer quizId, QuizPayload payload);
    void deleteTeacherQuiz(Integer quizId);
    QuestionView createTeacherQuestion(Integer quizId, QuestionPayload payload);
    QuestionView updateTeacherQuestion(Integer questionId, QuestionPayload payload);
    void deleteTeacherQuestion(Integer questionId);
    List<QuestionView> reorderTeacherQuestions(Integer quizId, List<Integer> questionIdsInOrder);
    List<SubmissionView> getTeacherAssignmentSubmissions(Integer assignmentId);
    Page<AssignmentView> getTeacherAssignments(Integer courseId, String status, String search, Pageable pageable);
    Page<SubmissionView> getTeacherSubmissions(Integer courseId, Integer assignmentId, SubmissionStatus status,
                                               String search, Pageable pageable);
    GradingSummaryView getTeacherGradingSummary(Integer courseId, Integer assignmentId, String search);
    SubmissionView gradeSubmission(Integer submissionId, GradePayload payload);
    List<TestcaseView> getTeacherTestcases(Integer assignmentId);
    TestcaseView createTeacherTestcase(Integer assignmentId, TestcasePayload payload);
    TestcaseView updateTeacherTestcase(Integer testcaseId, TestcasePayload payload);
    void deleteTeacherTestcase(Integer testcaseId);
    SubmissionView judgeSubmission(Integer submissionId);
}
