package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.QuestionPayload;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.QuestionView;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.QuestionPageView;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.QuizAttemptView;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.QuizDraftPayload;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.QuizOverviewView;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.QuizPayload;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.QuizView;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.ResultSummaryView;

import java.util.List;

public interface AssessmentService {
    QuizOverviewView getStudentQuizOverview(Integer courseId, Integer lessonId);
    QuizView getStudentQuiz(Integer courseId, Integer quizId);
    QuizAttemptView startQuizAttempt(Integer quizId);
    QuizAttemptView getStudentQuizAttempt(Integer attemptId);
    QuizAttemptView saveQuizDraft(Integer attemptId, QuizDraftPayload payload);
    QuizAttemptView submitQuizAttempt(Integer attemptId, QuizDraftPayload payload);
    ResultSummaryView getStudentResults();
    QuizAttemptView getStudentQuizResult(Integer attemptId);

    List<QuizView> getTeacherCourseQuizzes(Integer courseId);
    QuestionPageView getTeacherQuestions(Integer quizId, Integer page);
    QuizView createTeacherQuiz(Integer courseId, QuizPayload payload);
    QuizView updateTeacherQuiz(Integer quizId, QuizPayload payload);
    void deleteTeacherQuiz(Integer quizId);
    QuestionView createTeacherQuestion(Integer quizId, QuestionPayload payload);
    QuestionView updateTeacherQuestion(Integer questionId, QuestionPayload payload);
    void deleteTeacherQuestion(Integer questionId);
}
