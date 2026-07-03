package com.ojtsu26.elearning.controller;

import com.ojtsu26.elearning.common.ApiResponse;
import com.ojtsu26.elearning.dto.request.VideoProgressRequestDTO;
import com.ojtsu26.elearning.dto.response.CourseEnrollmentResponseDTO;
import com.ojtsu26.elearning.dto.response.CourseEnrollmentStateResponseDTO;
import com.ojtsu26.elearning.dto.response.LearningProgressDTO;
import com.ojtsu26.elearning.dto.response.StudentLearningCourseDTO;
import com.ojtsu26.elearning.dto.response.StudentLearningLessonDTO;
import com.ojtsu26.elearning.service.CourseEnrollmentService;
import com.ojtsu26.elearning.service.StudentLearningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentActionController {

    private final CourseEnrollmentService courseEnrollmentService;
    private final StudentLearningService studentLearningService;

    @PatchMapping("/profile")
    public ResponseEntity<?> updateProfile() {
        return ResponseEntity.ok(Map.of("message", "Update profile placeholder - not yet implemented"));
    }

    @GetMapping("/courses/{id}/enrollment-state")
    public ResponseEntity<ApiResponse<CourseEnrollmentStateResponseDTO>> enrollmentState(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(courseEnrollmentService.getCurrentStudentCourseState(id)));
    }

    @PostMapping("/courses/{id}/enroll")
    public ResponseEntity<ApiResponse<CourseEnrollmentResponseDTO>> enrollCourse(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(
                courseEnrollmentService.enrollCurrentStudentInFreeCourse(id),
                "Enrollment confirmed"
        ));
    }

    @GetMapping("/courses/{id}/learning")
    public ResponseEntity<ApiResponse<StudentLearningCourseDTO>> learningCourse(@PathVariable Integer id,
                                                                               @RequestParam(required = false) Integer lessonId) {
        return ResponseEntity.ok(ApiResponse.success(studentLearningService.getLearningCourse(id, lessonId)));
    }

    @GetMapping("/courses/{courseId}/lessons/{lessonId}")
    public ResponseEntity<ApiResponse<StudentLearningLessonDTO>> lesson(@PathVariable Integer courseId,
                                                                        @PathVariable Integer lessonId) {
        return ResponseEntity.ok(ApiResponse.success(studentLearningService.openLesson(courseId, lessonId)));
    }

    @GetMapping("/courses/{courseId}/progress")
    public ResponseEntity<ApiResponse<LearningProgressDTO>> courseProgress(@PathVariable Integer courseId) {
        return ResponseEntity.ok(ApiResponse.success(studentLearningService.getCourseProgress(courseId)));
    }

    @PostMapping("/courses/{courseId}/lessons/{lessonId}/video-progress")
    public ResponseEntity<ApiResponse<LearningProgressDTO>> videoProgress(@PathVariable Integer courseId,
                                                                          @PathVariable Integer lessonId,
                                                                          @Valid @RequestBody VideoProgressRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(
                studentLearningService.recordVideoProgress(courseId, lessonId, request.getWatchedSeconds())
        ));
    }

    @PostMapping("/courses/{courseId}/lessons/{lessonId}/complete")
    public ResponseEntity<ApiResponse<LearningProgressDTO>> completeLesson(@PathVariable Integer courseId,
                                                                           @PathVariable Integer lessonId) {
        return ResponseEntity.ok(ApiResponse.success(studentLearningService.completeLesson(courseId, lessonId)));
    }

    @PostMapping("/quiz/{id}/submit")
    public ResponseEntity<?> submitQuiz(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Submit quiz " + id + " placeholder - not yet implemented"));
    }

    @PostMapping("/code-assignment/{id}/submit")
    public ResponseEntity<?> submitCodeAssignment(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Submit code assignment " + id + " placeholder - not yet implemented"));
    }



    @PostMapping("/blogs/{id}/comments")
    public ResponseEntity<?> addComment(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Add comment to blog " + id + " placeholder - not yet implemented"));
    }
}
