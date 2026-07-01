package com.ojtsu26.elearning.controller;

import com.ojtsu26.elearning.common.ApiResponse;
import com.ojtsu26.elearning.dto.response.UserResponseDTO;
import com.ojtsu26.elearning.model.enums.AuthProvider;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.model.enums.UserStatus;
import com.ojtsu26.elearning.security.CustomUserDetails;
import com.ojtsu26.elearning.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminActionController {

    private static final Set<String> ALLOWED_USER_SORT_FIELDS = Set.of(
            "id", "fullName", "email", "role", "status", "authProvider", "createdAt"
    );

    private final UserService userService;

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<UserResponseDTO>>> searchUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String authProvider,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Role parsedRole = parseEnum(role, Role.class, "role");
        UserStatus parsedStatus = parseEnum(status, UserStatus.class, "status");
        AuthProvider parsedAuthProvider = parseEnum(authProvider, AuthProvider.class, "authProvider");
        Pageable pageable = buildUserSearchPageable(page, size, sortBy, sortDir);

        Page<UserResponseDTO> users = userService.searchUsers(keyword, parsedRole, parsedStatus, parsedAuthProvider, pageable);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    private Pageable buildUserSearchPageable(int page, int size, String sortBy, String sortDir) {
        if (page < 0) {
            throw new IllegalArgumentException("page must not be less than 0");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be at least 1");
        }
        int normalizedSize = Math.min(size, 100);
        if (!ALLOWED_USER_SORT_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException("sortBy is not supported");
        }
        String normalizedSortDir = sortDir.toLowerCase(Locale.ROOT);
        if (!normalizedSortDir.equals("asc") && !normalizedSortDir.equals("desc")) {
            throw new IllegalArgumentException("sortDir must be asc or desc");
        }

        Sort.Direction direction = Sort.Direction.fromString(normalizedSortDir);
        return PageRequest.of(page, normalizedSize, Sort.by(direction, sortBy));
    }

    private <E extends Enum<E>> E parseEnum(String value, Class<E> enumType, String parameterName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(enumType, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(parameterName + " is not valid");
        }
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
    public ResponseEntity<ApiResponse<UserResponseDTO>> blockUser(
            @PathVariable Integer id,
            @AuthenticationPrincipal CustomUserDetails currentAdmin) {
        UserResponseDTO user = userService.blockUser(id, currentAdmin.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success(user, "User blocked successfully"));
    }

    @PatchMapping("/users/{id}/unblock")
    public ResponseEntity<ApiResponse<UserResponseDTO>> unblockUser(
            @PathVariable Integer id,
            @AuthenticationPrincipal CustomUserDetails currentAdmin) {
        UserResponseDTO user = userService.unblockUser(id, currentAdmin.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success(user, "User unblocked successfully"));
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
