package com.ojtsu26.elearning.controller.api;

import com.ojtsu26.elearning.common.ApiResponse;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.QuestionPageView;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.QuestionPayload;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.QuestionView;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.QuizPayload;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.QuizView;
import com.ojtsu26.elearning.service.AssessmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
public class TeacherAssessmentRestController {
    private final AssessmentService assessmentService;

    @PostMapping("/courses/{courseId}/quizzes")
    public ResponseEntity<ApiResponse<QuizView>> createQuiz(
            @PathVariable Integer courseId,
            @Valid @RequestBody QuizPayload payload) {
        return ResponseEntity.ok(ApiResponse.success(
                assessmentService.createTeacherQuiz(courseId, payload), "Quiz created"));
    }

    @PutMapping("/quizzes/{quizId}")
    public ResponseEntity<ApiResponse<QuizView>> updateQuiz(
            @PathVariable Integer quizId,
            @Valid @RequestBody QuizPayload payload) {
        return ResponseEntity.ok(ApiResponse.success(
                assessmentService.updateTeacherQuiz(quizId, payload), "Quiz updated"));
    }

    @DeleteMapping("/quizzes/{quizId}")
    public ResponseEntity<ApiResponse<Void>> deleteQuiz(@PathVariable Integer quizId) {
        assessmentService.deleteTeacherQuiz(quizId);
        return ResponseEntity.ok(ApiResponse.success(null, "Quiz deleted"));
    }

    @GetMapping("/quizzes/{quizId}/questions")
    public ResponseEntity<ApiResponse<QuestionPageView>> questions(
            @PathVariable Integer quizId,
            @RequestParam(defaultValue = "0") Integer page) {
        return ResponseEntity.ok(ApiResponse.success(
                assessmentService.getTeacherQuestions(quizId, page)));
    }

    @PostMapping("/quizzes/{quizId}/questions")
    public ResponseEntity<ApiResponse<QuestionView>> createQuestion(
            @PathVariable Integer quizId,
            @Valid @RequestBody QuestionPayload payload) {
        return ResponseEntity.ok(ApiResponse.success(
                assessmentService.createTeacherQuestion(quizId, payload), "Question created"));
    }

    @PutMapping("/questions/{questionId}")
    public ResponseEntity<ApiResponse<QuestionView>> updateQuestion(
            @PathVariable Integer questionId,
            @Valid @RequestBody QuestionPayload payload) {
        return ResponseEntity.ok(ApiResponse.success(
                assessmentService.updateTeacherQuestion(questionId, payload), "Question updated"));
    }

    @DeleteMapping("/questions/{questionId}")
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(@PathVariable Integer questionId) {
        assessmentService.deleteTeacherQuestion(questionId);
        return ResponseEntity.ok(ApiResponse.success(null, "Question deleted"));
    }
}
