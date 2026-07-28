package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.QuestionPayload;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.QuestionView;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.QuizAttemptView;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.QuizDraftPayload;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.QuizPayload;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.QuizView;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.ResultSummaryView;

import java.util.List;

public interface AssessmentService {
    QuizView getStudentQuiz(Integer courseId, Integer quizId);
    QuizAttemptView startQuizAttempt(Integer quizId);
    QuizAttemptView saveQuizDraft(Integer attemptId, QuizDraftPayload payload);
    QuizAttemptView submitQuizAttempt(Integer attemptId, QuizDraftPayload payload);
    ResultSummaryView getStudentResults();
    QuizAttemptView getStudentQuizResult(Integer attemptId);

    List<QuizView> getTeacherCourseQuizzes(Integer courseId);
    QuizView createTeacherQuiz(Integer courseId, QuizPayload payload);
    QuizView updateTeacherQuiz(Integer quizId, QuizPayload payload);
    void archiveTeacherQuiz(Integer quizId);
    void deleteTeacherQuiz(Integer quizId);
    QuestionView createTeacherQuestion(Integer quizId, QuestionPayload payload);
    QuestionView updateTeacherQuestion(Integer questionId, QuestionPayload payload);
    void deleteTeacherQuestion(Integer questionId);
    List<QuestionView> reorderTeacherQuestions(Integer quizId, List<Integer> questionIdsInOrder);
}
