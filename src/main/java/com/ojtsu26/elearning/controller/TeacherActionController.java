package com.ojtsu26.elearning.controller;

import com.ojtsu26.elearning.common.ApiResponse;
import com.ojtsu26.elearning.dto.request.UpdateProfileRequestDTO;
import com.ojtsu26.elearning.dto.response.UserResponseDTO;
import com.ojtsu26.elearning.model.enums.AuthProvider;
import com.ojtsu26.elearning.security.CustomUserDetails;
import com.ojtsu26.elearning.security.JwtCookieService;
import com.ojtsu26.elearning.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/teacher")
@RequiredArgsConstructor
public class TeacherActionController {

    private final UserService userService;
    private final JwtCookieService jwtCookieService;

    @PatchMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateProfile(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody UpdateProfileRequestDTO request,
            HttpServletResponse response) {
        String previousEmail = currentUser.getUsername();
        AuthProvider authProvider = currentUser.getUser().getAuthProvider();
        UserResponseDTO updatedProfile = userService.updateCurrentProfile(currentUser.getUser().getId(), request);

        if (authProvider == AuthProvider.LOCAL && !Objects.equals(previousEmail, updatedProfile.getEmail())) {
            jwtCookieService.addJwtCookie(response, updatedProfile.getEmail());
        }

        return ResponseEntity.ok(ApiResponse.success(updatedProfile, "Profile updated successfully"));
    }

    @PostMapping("/courses")
    public ResponseEntity<?> createCourse() {
        return ResponseEntity.ok(Map.of("message", "Create course placeholder - not yet implemented"));
    }

    @PatchMapping("/courses/{id}")
    public ResponseEntity<?> updateCourse(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Update course " + id + " placeholder - not yet implemented"));
    }

    @DeleteMapping("/courses/{id}")
    public ResponseEntity<?> deleteCourse(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Delete course " + id + " placeholder - not yet implemented"));
    }

    @PostMapping("/roadmap")
    public ResponseEntity<?> createRoadmap() {
        return ResponseEntity.ok(Map.of("message", "Create roadmap placeholder - not yet implemented"));
    }

    @PatchMapping("/roadmap/{id}")
    public ResponseEntity<?> updateRoadmap(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Update roadmap " + id + " placeholder - not yet implemented"));
    }

    @DeleteMapping("/roadmap/{id}")
    public ResponseEntity<?> deleteRoadmap(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Delete roadmap " + id + " placeholder - not yet implemented"));
    }

    @PostMapping("/lessons")
    public ResponseEntity<?> createLesson() {
        return ResponseEntity.ok(Map.of("message", "Create lesson placeholder - not yet implemented"));
    }

    @PatchMapping("/lessons/{id}")
    public ResponseEntity<?> updateLesson(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Update lesson " + id + " placeholder - not yet implemented"));
    }

    @DeleteMapping("/lessons/{id}")
    public ResponseEntity<?> deleteLesson(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Delete lesson " + id + " placeholder - not yet implemented"));
    }

    @PostMapping("/videos")
    public ResponseEntity<?> uploadVideo() {
        return ResponseEntity.ok(Map.of("message", "Upload video placeholder - not yet implemented"));
    }

    @DeleteMapping("/videos/{id}")
    public ResponseEntity<?> deleteVideo(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Delete video " + id + " placeholder - not yet implemented"));
    }

    @PostMapping("/quizzes")
    public ResponseEntity<?> createQuiz() {
        return ResponseEntity.ok(Map.of("message", "Create quiz placeholder - not yet implemented"));
    }

    @PatchMapping("/quizzes/{id}")
    public ResponseEntity<?> updateQuiz(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Update quiz " + id + " placeholder - not yet implemented"));
    }

    @DeleteMapping("/quizzes/{id}")
    public ResponseEntity<?> deleteQuiz(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Delete quiz " + id + " placeholder - not yet implemented"));
    }

    @PostMapping("/testcases")
    public ResponseEntity<?> createTestcase() {
        return ResponseEntity.ok(Map.of("message", "Create testcase placeholder - not yet implemented"));
    }

    @PatchMapping("/testcases/{id}")
    public ResponseEntity<?> updateTestcase(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Update testcase " + id + " placeholder - not yet implemented"));
    }

    @DeleteMapping("/testcases/{id}")
    public ResponseEntity<?> deleteTestcase(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Delete testcase " + id + " placeholder - not yet implemented"));
    }

    @PatchMapping("/assignments/{id}/grade")
    public ResponseEntity<?> gradeAssignment(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Grade assignment " + id + " placeholder - not yet implemented"));
    }

    @PostMapping("/blogs")
    public ResponseEntity<?> createBlog() {
        return ResponseEntity.ok(Map.of("message", "Create blog placeholder - not yet implemented"));
    }

    @PatchMapping("/blogs/{id}")
    public ResponseEntity<?> updateBlog(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Update blog " + id + " placeholder - not yet implemented"));
    }

    @DeleteMapping("/blogs/{id}")
    public ResponseEntity<?> deleteBlog(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Delete blog " + id + " placeholder - not yet implemented"));
    }

    @PostMapping("/blogs/{id}/submit")
    public ResponseEntity<?> submitBlog(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Submit blog " + id + " for review placeholder - not yet implemented"));
    }
}
