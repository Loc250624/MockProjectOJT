package com.ojtsu26.elearning.controller.api;

import com.ojtsu26.elearning.common.ApiResponse;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.QuizAttemptView;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.QuizDraftPayload;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.QuizOverviewView;
import com.ojtsu26.elearning.service.AssessmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentAssessmentRestController {
    private final AssessmentService assessmentService;

    @GetMapping("/courses/{courseId}/lessons/{lessonId}/quiz-overview")
    public ResponseEntity<ApiResponse<QuizOverviewView>> quizOverview(
            @PathVariable Integer courseId,
            @PathVariable Integer lessonId) {
        return ResponseEntity.ok(ApiResponse.success(
                assessmentService.getStudentQuizOverview(courseId, lessonId)));
    }

    @PostMapping("/quizzes/{quizId}/attempts")
    public ResponseEntity<ApiResponse<QuizAttemptView>> startAttempt(@PathVariable Integer quizId) {
        return ResponseEntity.ok(ApiResponse.success(assessmentService.startQuizAttempt(quizId), "Attempt started"));
    }

    @PutMapping("/quiz-attempts/{attemptId}/draft")
    public ResponseEntity<ApiResponse<QuizAttemptView>> saveDraft(@PathVariable Integer attemptId,
                                                                  @Valid @RequestBody QuizDraftPayload payload) {
        return ResponseEntity.ok(ApiResponse.success(assessmentService.saveQuizDraft(attemptId, payload), "Draft saved"));
    }

    @PostMapping("/quiz-attempts/{attemptId}/submit")
    public ResponseEntity<ApiResponse<QuizAttemptView>> submitAttempt(@PathVariable Integer attemptId,
                                                                      @Valid @RequestBody QuizDraftPayload payload) {
        return ResponseEntity.ok(ApiResponse.success(assessmentService.submitQuizAttempt(attemptId, payload), "Quiz submitted"));
    }

}
