package com.ojtsu26.elearning.controller.api;

import com.ojtsu26.elearning.common.ApiResponse;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.*;
import com.ojtsu26.elearning.service.AssessmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
public class TeacherAssessmentRestController {
    private final AssessmentService assessmentService;

    @PostMapping("/courses/{courseId}/quizzes")
    public ResponseEntity<ApiResponse<QuizView>> createQuiz(@PathVariable Integer courseId,
                                                            @Valid @RequestBody QuizPayload payload) {
        return ResponseEntity.ok(ApiResponse.success(assessmentService.createTeacherQuiz(courseId, payload), "Quiz created"));
    }

    @PutMapping("/quizzes/{quizId}")
    public ResponseEntity<ApiResponse<QuizView>> updateQuiz(@PathVariable Integer quizId,
                                                            @Valid @RequestBody QuizPayload payload) {
        return ResponseEntity.ok(ApiResponse.success(assessmentService.updateTeacherQuiz(quizId, payload), "Quiz updated"));
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

    @PostMapping("/courses/{courseId}/assignments")
    public ResponseEntity<ApiResponse<AssignmentView>> createAssignment(@PathVariable Integer courseId,
                                                                        @Valid @RequestBody AssignmentPayload payload) {
        return ResponseEntity.ok(ApiResponse.success(
                assessmentService.createTeacherAssignment(courseId, payload),
                "Coding exercise created"));
    }

    @PutMapping("/assignments/{assignmentId}")
    public ResponseEntity<ApiResponse<AssignmentView>> updateAssignment(@PathVariable Integer assignmentId,
                                                                        @Valid @RequestBody AssignmentPayload payload) {
        return ResponseEntity.ok(ApiResponse.success(
                assessmentService.updateTeacherAssignment(assignmentId, payload),
                "Coding exercise updated"));
    }

    @DeleteMapping("/assignments/{assignmentId}")
    public ResponseEntity<ApiResponse<Void>> deleteAssignment(@PathVariable Integer assignmentId) {
        assessmentService.deleteTeacherAssignment(assignmentId);
        return ResponseEntity.ok(ApiResponse.success(null, "Coding exercise deleted"));
    }

    @PostMapping("/assignments/{assignmentId}/questions")
    public ResponseEntity<ApiResponse<QuestionView>> createAssignmentQuestion(@PathVariable Integer assignmentId,
                                                                              @Valid @RequestBody QuestionPayload payload) {
        return ResponseEntity.ok(ApiResponse.success(
                assessmentService.createTeacherAssignmentQuestion(assignmentId, payload),
                "Assignment question created"));
    }

    @PutMapping("/assignments/questions/{questionId}")
    public ResponseEntity<ApiResponse<QuestionView>> updateAssignmentQuestion(@PathVariable Integer questionId,
                                                                              @Valid @RequestBody QuestionPayload payload) {
        return ResponseEntity.ok(ApiResponse.success(
                assessmentService.updateTeacherAssignmentQuestion(questionId, payload),
                "Assignment question updated"));
    }

    @DeleteMapping("/assignments/questions/{questionId}")
    public ResponseEntity<ApiResponse<Void>> deleteAssignmentQuestion(@PathVariable Integer questionId) {
        assessmentService.deleteTeacherAssignmentQuestion(questionId);
        return ResponseEntity.ok(ApiResponse.success(null, "Assignment question deleted"));
    }

    @PostMapping("/quizzes/{quizId}/questions")
    public ResponseEntity<ApiResponse<QuestionView>> createQuestion(@PathVariable Integer quizId,
                                                                    @Valid @RequestBody QuestionPayload payload) {
        return ResponseEntity.ok(ApiResponse.success(assessmentService.createTeacherQuestion(quizId, payload), "Question created"));
    }

    @PutMapping("/questions/{questionId}")
    public ResponseEntity<ApiResponse<QuestionView>> updateQuestion(@PathVariable Integer questionId,
                                                                    @Valid @RequestBody QuestionPayload payload) {
        return ResponseEntity.ok(ApiResponse.success(assessmentService.updateTeacherQuestion(questionId, payload), "Question updated"));
    }

    @DeleteMapping("/questions/{questionId}")
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(@PathVariable Integer questionId) {
        assessmentService.deleteTeacherQuestion(questionId);
        return ResponseEntity.ok(ApiResponse.success(null, "Question deleted"));
    }

    @PostMapping("/quizzes/{quizId}/questions/reorder")
    public ResponseEntity<ApiResponse<List<QuestionView>>> reorderQuestions(@PathVariable Integer quizId,
                                                                            @RequestBody List<Integer> questionIdsInOrder) {
        return ResponseEntity.ok(ApiResponse.success(
                assessmentService.reorderTeacherQuestions(quizId, questionIdsInOrder),
                "Questions reordered"));
    }

    @PostMapping("/submissions/{submissionId}/grade")
    public ResponseEntity<ApiResponse<SubmissionView>> gradeSubmission(@PathVariable Integer submissionId,
                                                                       @Valid @RequestBody GradePayload payload) {
        return ResponseEntity.ok(ApiResponse.success(assessmentService.gradeSubmission(submissionId, payload), "Submission graded"));
    }

    @GetMapping("/submissions/{submissionId}")
    public ResponseEntity<ApiResponse<SubmissionView>> getSubmission(@PathVariable Integer submissionId) {
        return ResponseEntity.ok(ApiResponse.success(assessmentService.getTeacherSubmission(submissionId)));
    }

    @PostMapping("/assignments/{assignmentId}/testcases")
    public ResponseEntity<ApiResponse<TestcaseView>> createTestcase(@PathVariable Integer assignmentId,
                                                                    @Valid @RequestBody TestcasePayload payload) {
        return ResponseEntity.ok(ApiResponse.success(assessmentService.createTeacherTestcase(assignmentId, payload), "Testcase created"));
    }

    @PutMapping("/testcases/{testcaseId}")
    public ResponseEntity<ApiResponse<TestcaseView>> updateTestcase(@PathVariable Integer testcaseId,
                                                                    @Valid @RequestBody TestcasePayload payload) {
        return ResponseEntity.ok(ApiResponse.success(assessmentService.updateTeacherTestcase(testcaseId, payload), "Testcase updated"));
    }

    @DeleteMapping("/testcases/{testcaseId}")
    public ResponseEntity<ApiResponse<Void>> deleteTestcase(@PathVariable Integer testcaseId) {
        assessmentService.deleteTeacherTestcase(testcaseId);
        return ResponseEntity.ok(ApiResponse.success(null, "Testcase deleted"));
    }

    @PostMapping("/submissions/{submissionId}/judge")
    public ResponseEntity<ApiResponse<SubmissionView>> judgeSubmission(@PathVariable Integer submissionId) {
        return ResponseEntity.ok(ApiResponse.success(assessmentService.judgeSubmission(submissionId), "Code judge finished"));
    }
}
