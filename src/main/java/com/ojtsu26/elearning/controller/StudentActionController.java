package com.ojtsu26.elearning.controller;

import com.ojtsu26.elearning.common.ApiResponse;
import com.ojtsu26.elearning.dto.request.UpdateProfileRequestDTO;
import com.ojtsu26.elearning.dto.request.StudentQuizSubmissionRequestDTO;
import com.ojtsu26.elearning.dto.response.UserResponseDTO;
import com.ojtsu26.elearning.model.enums.AuthProvider;
import com.ojtsu26.elearning.security.CustomUserDetails;
import com.ojtsu26.elearning.security.JwtCookieService;
import com.ojtsu26.elearning.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import com.ojtsu26.elearning.dto.request.VideoProgressRequestDTO;
import com.ojtsu26.elearning.dto.response.CourseEnrollmentResponseDTO;
import com.ojtsu26.elearning.dto.response.CourseEnrollmentStateResponseDTO;
import com.ojtsu26.elearning.dto.response.LearningProgressDTO;
import com.ojtsu26.elearning.dto.response.StudentLearningCourseDTO;
import com.ojtsu26.elearning.dto.response.StudentLearningLessonDTO;
import com.ojtsu26.elearning.dto.response.StudentQuizAttemptDTO;
import com.ojtsu26.elearning.service.CourseEnrollmentService;
import com.ojtsu26.elearning.service.StudentAssessmentService;
import com.ojtsu26.elearning.service.StudentLearningService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentActionController {

    private final UserService userService;
    private final JwtCookieService jwtCookieService;
    private final CourseEnrollmentService courseEnrollmentService;
    private final StudentLearningService studentLearningService;
    private final StudentAssessmentService studentAssessmentService;

    @PatchMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateProfile(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody UpdateProfileRequestDTO request,
            HttpServletResponse response) {
        String previousEmail = currentUser.getUsername();
        AuthProvider authProvider = currentUser.getUser().getAuthProvider();
        UserResponseDTO updatedProfile = userService.updateCurrentProfile(currentUser.getUser().getId(), request);

        if (authProvider == AuthProvider.LOCAL && !Objects.equals(previousEmail, updatedProfile.getEmail())) {
            jwtCookieService.addJwtCookie(response, updatedProfile.getId());
        }

        return ResponseEntity.ok(ApiResponse.success(updatedProfile, "Profile updated successfully"));
    }

    @PostMapping(value = "/profile/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserResponseDTO>> uploadAvatar(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam("avatar") MultipartFile avatar) throws IOException {
        validateAvatar(avatar);

        Path uploadDir = Paths.get("uploads", "avatars").toAbsolutePath().normalize();
        Files.createDirectories(uploadDir);

        String extension = getExtension(Objects.requireNonNull(avatar.getContentType()));
        String fileName = currentUser.getUser().getId() + "-" + UUID.randomUUID() + extension;
        Path destination = uploadDir.resolve(fileName).normalize();
        avatar.transferTo(destination);

        UserResponseDTO updatedProfile = userService.updateCurrentAvatar(
                currentUser.getUser().getId(),
                "/uploads/avatars/" + fileName
        );

        return ResponseEntity.ok(ApiResponse.success(updatedProfile, "Avatar updated successfully"));
    }

    @PatchMapping("/profile/avatar-url")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateAvatarUrl(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestBody Map<String, String> request) {
        String avatarUrl = validateAvatarUrl(request.get("avatarUrl"));
        UserResponseDTO updatedProfile = userService.updateCurrentAvatar(currentUser.getUser().getId(), avatarUrl);

        return ResponseEntity.ok(ApiResponse.success(updatedProfile, "Avatar updated successfully"));
    }

    private void validateAvatar(MultipartFile avatar) {
        if (avatar == null || avatar.isEmpty()) {
            throw new IllegalArgumentException("Avatar image is required");
        }

        if (avatar.getSize() > 2 * 1024 * 1024) {
            throw new IllegalArgumentException("Avatar image must not exceed 2MB");
        }

        Set<String> allowedContentTypes = Set.of("image/png", "image/jpeg", "image/webp", "image/gif");
        if (!allowedContentTypes.contains(avatar.getContentType())) {
            throw new IllegalArgumentException("Avatar image must be PNG, JPG, WEBP, or GIF");
        }
    }

    private String getExtension(String contentType) {
        String lowerContentType = contentType.toLowerCase(Locale.ROOT);
        if ("image/png".equals(lowerContentType)) {
            return ".png";
        }
        if ("image/jpeg".equals(lowerContentType)) {
            return ".jpg";
        }
        if ("image/webp".equals(lowerContentType)) {
            return ".webp";
        }
        if ("image/gif".equals(lowerContentType)) {
            return ".gif";
        }
        throw new IllegalArgumentException("Avatar image must be PNG, JPG, WEBP, or GIF");
    }

    private String validateAvatarUrl(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) {
            throw new IllegalArgumentException("Avatar URL is required");
        }

        String normalizedAvatarUrl = avatarUrl.trim();
        if (normalizedAvatarUrl.length() > 2048) {
            throw new IllegalArgumentException("Avatar URL must not exceed 2048 characters");
        }

        String lowerAvatarUrl = normalizedAvatarUrl.toLowerCase(Locale.ROOT);
        if (!lowerAvatarUrl.startsWith("http://") && !lowerAvatarUrl.startsWith("https://")) {
            throw new IllegalArgumentException("Avatar URL must start with http:// or https://");
        }

        return normalizedAvatarUrl;
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
                studentLearningService.recordVideoProgress(courseId, lessonId, request)
        ));
    }

    @PostMapping("/courses/{courseId}/lessons/{lessonId}/complete")
    public ResponseEntity<ApiResponse<LearningProgressDTO>> completeLesson(@PathVariable Integer courseId,
                                                                           @PathVariable Integer lessonId) {
        return ResponseEntity.ok(ApiResponse.success(studentLearningService.completeLesson(courseId, lessonId)));
    }

    @GetMapping("/courses/{courseId}/lessons/{lessonId}/quiz")
    public ResponseEntity<ApiResponse<StudentQuizAttemptDTO>> quiz(@PathVariable Integer courseId,
                                                                   @PathVariable Integer lessonId) {
        return ResponseEntity.ok(ApiResponse.success(studentAssessmentService.getOrStartQuiz(courseId, lessonId)));
    }

    @PostMapping("/courses/{courseId}/lessons/{lessonId}/quiz/save")
    public ResponseEntity<ApiResponse<StudentQuizAttemptDTO>> saveQuiz(@PathVariable Integer courseId,
                                                                       @PathVariable Integer lessonId,
                                                                       @Valid @RequestBody StudentQuizSubmissionRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(studentAssessmentService.saveQuizDraft(courseId, lessonId, request)));
    }

    @PostMapping("/courses/{courseId}/lessons/{lessonId}/quiz/submit")
    public ResponseEntity<ApiResponse<StudentQuizAttemptDTO>> submitQuiz(@PathVariable Integer courseId,
                                                                         @PathVariable Integer lessonId,
                                                                         @Valid @RequestBody StudentQuizSubmissionRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(studentAssessmentService.submitQuiz(courseId, lessonId, request)));
    }

    @PostMapping("/quiz/{id}/submit")
    public ResponseEntity<?> submitQuiz(@PathVariable Long id) {
        return ResponseEntity.badRequest().body(Map.of("message", "Use the course lesson quiz endpoint to submit an attempt."));
    }



    @PostMapping("/blogs/{id}/comments")
    public ResponseEntity<?> addComment(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Add comment to blog " + id + " placeholder - not yet implemented"));
    }
}
