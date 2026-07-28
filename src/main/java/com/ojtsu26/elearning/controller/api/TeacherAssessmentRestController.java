package com.ojtsu26.elearning.controller.api;

import com.ojtsu26.elearning.common.ApiResponse;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.BlueprintItemPayload;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.QuestionPayload;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.QuestionView;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.QuizPayload;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.QuizReadinessView;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.QuizView;
import com.ojtsu26.elearning.service.AssessmentService;
import com.ojtsu26.elearning.service.quiz.TeacherQuestionBankService;
import com.ojtsu26.elearning.model.enums.QuestionDifficulty;
import com.ojtsu26.elearning.model.enums.QuestionGenerationSource;
import com.ojtsu26.elearning.model.enums.QuestionReviewStatus;
import com.ojtsu26.elearning.service.ai.question.QuestionBankGenerationService;
import com.ojtsu26.elearning.service.ai.question.QuestionGenerationRequest;
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

import java.util.List;

@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
public class TeacherAssessmentRestController {
    private final AssessmentService assessmentService;
    private final TeacherQuestionBankService questionBankService;
    private final QuestionBankGenerationService questionGenerationService;

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

    @PostMapping("/quizzes/{quizId}/archive")
    public ResponseEntity<ApiResponse<Void>> archiveQuiz(@PathVariable Integer quizId) {
        assessmentService.archiveTeacherQuiz(quizId);
        return ResponseEntity.ok(ApiResponse.success(null, "Quiz archived"));
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

    @PostMapping("/quizzes/{quizId}/questions/reorder")
    public ResponseEntity<ApiResponse<List<QuestionView>>> reorderQuestions(
            @PathVariable Integer quizId,
            @RequestBody List<Integer> questionIdsInOrder) {
        return ResponseEntity.ok(ApiResponse.success(
                assessmentService.reorderTeacherQuestions(quizId, questionIdsInOrder),
                "Questions reordered"));
    }
    @GetMapping("/quizzes/{quizId}/question-bank")
    public ResponseEntity<ApiResponse<List<TeacherQuestionBankService.QuestionBankItem>>> questionBank(
            @PathVariable Integer quizId,
            @RequestParam(required = false) QuestionReviewStatus status,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) QuestionDifficulty difficulty,
            @RequestParam(required = false) QuestionGenerationSource source) {
        return ResponseEntity.ok(ApiResponse.success(
                questionBankService.list(quizId, status, topic, difficulty, source)));
    }

    @PostMapping("/questions/{questionId}/approve")
    public ResponseEntity<ApiResponse<TeacherQuestionBankService.QuestionBankItem>> approveQuestion(
            @PathVariable Integer questionId) {
        return ResponseEntity.ok(ApiResponse.success(
                questionBankService.approve(questionId), "Question approved"));
    }

    @PostMapping("/questions/{questionId}/reject")
    public ResponseEntity<ApiResponse<TeacherQuestionBankService.QuestionBankItem>> rejectQuestion(
            @PathVariable Integer questionId) {
        return ResponseEntity.ok(ApiResponse.success(
                questionBankService.reject(questionId), "Question rejected"));
    }

    @PostMapping("/questions/{questionId}/archive")
    public ResponseEntity<ApiResponse<TeacherQuestionBankService.QuestionBankItem>> archiveQuestion(
            @PathVariable Integer questionId) {
        return ResponseEntity.ok(ApiResponse.success(
                questionBankService.archive(questionId), "Question archived"));
    }

    @PostMapping("/quizzes/{quizId}/questions/bulk-approve")
    public ResponseEntity<ApiResponse<List<TeacherQuestionBankService.QuestionBankItem>>> bulkApprove(
            @PathVariable Integer quizId,
            @RequestBody List<Integer> questionIds) {
        return ResponseEntity.ok(ApiResponse.success(
                questionBankService.bulkApprove(quizId, questionIds), "Questions approved"));
    }

    @PutMapping("/quizzes/{quizId}/blueprint")
    public ResponseEntity<ApiResponse<QuizReadinessView>> configureBlueprint(
            @PathVariable Integer quizId,
            @Valid @RequestBody List<BlueprintItemPayload> payload) {
        return ResponseEntity.ok(ApiResponse.success(
                questionBankService.configureBlueprint(quizId, payload), "Blueprint saved"));
    }

    @GetMapping("/quizzes/{quizId}/readiness")
    public ResponseEntity<ApiResponse<QuizReadinessView>> readiness(@PathVariable Integer quizId) {
        return ResponseEntity.ok(ApiResponse.success(questionBankService.readiness(quizId)));
    }

    @PostMapping("/quizzes/{quizId}/question-generation-jobs")
    public ResponseEntity<ApiResponse<QuestionBankGenerationService.JobView>> generateQuestions(
            @PathVariable Integer quizId,
            @Valid @RequestBody QuestionGenerationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                questionGenerationService.start(quizId, request),
                "Question generation queued"));
    }

    @GetMapping("/question-generation-jobs/{jobId}")
    public ResponseEntity<ApiResponse<QuestionBankGenerationService.JobView>> generationJob(
            @PathVariable Integer jobId) {
        return ResponseEntity.ok(ApiResponse.success(questionGenerationService.get(jobId)));
    }
}
