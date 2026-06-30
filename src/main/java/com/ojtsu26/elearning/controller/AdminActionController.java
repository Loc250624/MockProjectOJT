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
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminActionController {

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

    @PostMapping("/users")
    public ResponseEntity<?> createUser() {
        return ResponseEntity.ok(Map.of("message", "Create user placeholder - not yet implemented"));
    }

    @PatchMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Update user " + id + " placeholder - not yet implemented"));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Delete user " + id + " placeholder - not yet implemented"));
    }

    @PatchMapping("/users/{id}/block")
    public ResponseEntity<?> blockUser(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Block user " + id + " placeholder - not yet implemented"));
    }

    @PatchMapping("/users/{id}/unblock")
    public ResponseEntity<?> unblockUser(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Unblock user " + id + " placeholder - not yet implemented"));
    }

    @PostMapping("/categories")
    public ResponseEntity<?> createCategory() {
        return ResponseEntity.ok(Map.of("message", "Create category placeholder - not yet implemented"));
    }

    @PatchMapping("/categories/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Update category " + id + " placeholder - not yet implemented"));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Delete category " + id + " placeholder - not yet implemented"));
    }

    @PatchMapping("/courses/{id}/approve")
    public ResponseEntity<?> approveCourse(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Approve course " + id + " placeholder - not yet implemented"));
    }

    @PatchMapping("/courses/{id}/hide")
    public ResponseEntity<?> hideCourse(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Hide course " + id + " placeholder - not yet implemented"));
    }

    @DeleteMapping("/courses/{id}")
    public ResponseEntity<?> deleteCourse(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Delete course " + id + " placeholder - not yet implemented"));
    }

    @PatchMapping("/blogs/{id}/approve")
    public ResponseEntity<?> approveBlog(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Approve blog " + id + " placeholder - not yet implemented"));
    }

    @PatchMapping("/blogs/{id}/reject")
    public ResponseEntity<?> rejectBlog(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Reject blog " + id + " placeholder - not yet implemented"));
    }

    @DeleteMapping("/blogs/{id}")
    public ResponseEntity<?> deleteBlog(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Delete blog " + id + " placeholder - not yet implemented"));
    }

    @DeleteMapping("/comments/{id}")
    public ResponseEntity<?> deleteComment(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Delete comment " + id + " placeholder - not yet implemented"));
    }

    @PostMapping("/refunds")
    public ResponseEntity<?> createRefund() {
        return ResponseEntity.ok(Map.of("message", "Create refund placeholder - not yet implemented"));
    }

    @PatchMapping("/system-settings")
    public ResponseEntity<?> updateSystemSettings() {
        return ResponseEntity.ok(Map.of("message", "Update system settings placeholder - not yet implemented"));
    }
}
