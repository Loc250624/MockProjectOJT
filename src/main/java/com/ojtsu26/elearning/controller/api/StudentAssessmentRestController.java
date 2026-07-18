package com.ojtsu26.elearning.controller.api;

import com.ojtsu26.elearning.common.ApiResponse;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.AssignmentSubmissionPayload;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.QuizAttemptView;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.QuizDraftPayload;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.SubmissionView;
import com.ojtsu26.elearning.service.AssessmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentAssessmentRestController {
    private final AssessmentService assessmentService;

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

    @PostMapping("/assignments/{assignmentId}/submissions/draft")
    public ResponseEntity<ApiResponse<SubmissionView>> saveSubmissionDraft(@PathVariable Integer assignmentId,
                                                                          @Valid @RequestBody AssignmentSubmissionPayload payload) {
        return ResponseEntity.ok(ApiResponse.success(assessmentService.saveSubmissionDraft(assignmentId, payload), "Draft saved"));
    }

    @PostMapping("/assignments/{assignmentId}/submissions/run")
    public ResponseEntity<ApiResponse<SubmissionView>> runAssignment(@PathVariable Integer assignmentId,
                                                                     @Valid @RequestBody AssignmentSubmissionPayload payload) {
        return ResponseEntity.ok(ApiResponse.success(assessmentService.runSubmission(assignmentId, payload), "Code judge finished"));
    }

    @PostMapping("/assignments/{assignmentId}/submissions/submit")
    public ResponseEntity<ApiResponse<SubmissionView>> submitAssignment(@PathVariable Integer assignmentId,
                                                                       @Valid @RequestBody AssignmentSubmissionPayload payload) {
        return ResponseEntity.ok(ApiResponse.success(assessmentService.submitAssignment(assignmentId, payload), "Submission sent"));
    }

    @GetMapping("/assignments/{assignmentId}/submissions")
    public ResponseEntity<ApiResponse<List<SubmissionView>>> assignmentHistory(@PathVariable Integer assignmentId) {
        return ResponseEntity.ok(ApiResponse.success(assessmentService.getStudentAssignmentHistory(assignmentId)));
    }
}
