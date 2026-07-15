package com.ojtsu26.elearning.controller;

import com.ojtsu26.elearning.common.ApiResponse;
import com.ojtsu26.elearning.dto.request.UpdateProfileRequestDTO;
import com.ojtsu26.elearning.dto.response.AdminPaymentSummaryDTO;
import com.ojtsu26.elearning.dto.response.AdminTransactionDTO;
import com.ojtsu26.elearning.dto.response.AdminTransactionDetailDTO;
import com.ojtsu26.elearning.dto.response.UserResponseDTO;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.AuthProvider;
import com.ojtsu26.elearning.model.enums.PaymentMethod;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.model.enums.TransactionStatus;
import com.ojtsu26.elearning.model.enums.UserStatus;
import com.ojtsu26.elearning.security.CustomUserDetails;
import com.ojtsu26.elearning.security.JwtCookieService;
import com.ojtsu26.elearning.service.TransactionService;
import com.ojtsu26.elearning.service.UserService;
import com.ojtsu26.elearning.service.BlogCommentService;
import com.ojtsu26.elearning.service.BlogPostService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminActionController {

    private static final Set<String> ALLOWED_USER_SORT_FIELDS = Set.of(
            "id", "fullName", "email", "role", "status", "authProvider", "createdAt"
    );
    private static final Set<String> ALLOWED_TRANSACTION_SORT_FIELDS = Set.of(
            "id", "amount", "paymentMethod", "transactionRef", "status", "createdAt", "updatedAt"
    );

    private final UserService userService;
    private final JwtCookieService jwtCookieService;
    private final BlogPostService blogPostService;
    private final BlogCommentService blogCommentService;
    private final TransactionService transactionService;

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
        return buildPageable(page, size, sortBy, sortDir, ALLOWED_USER_SORT_FIELDS);
    }

    private Pageable buildTransactionSearchPageable(int page, int size, String sortBy, String sortDir) {
        return buildPageable(page, size, sortBy, sortDir, ALLOWED_TRANSACTION_SORT_FIELDS);
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortDir, Set<String> allowedSortFields) {
        if (page < 0) {
            throw new IllegalArgumentException("page must not be less than 0");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be at least 1");
        }
        int normalizedSize = Math.min(size, 100);
        if (!allowedSortFields.contains(sortBy)) {
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

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<Page<AdminTransactionDTO>>> searchTransactions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        TransactionStatus parsedStatus = parseEnum(status, TransactionStatus.class, "status");
        PaymentMethod parsedPaymentMethod = parseEnum(paymentMethod, PaymentMethod.class, "paymentMethod");
        LocalDate fromDate = parseDate(from, "from");
        LocalDate toDate = parseDate(to, "to");
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("from must be before or equal to to");
        }

        LocalDateTime fromDateTime = fromDate == null ? null : fromDate.atStartOfDay();
        LocalDateTime toDateExclusive = toDate == null ? null : toDate.plusDays(1).atStartOfDay();
        Pageable pageable = buildTransactionSearchPageable(page, size, sortBy, sortDir);

        Page<AdminTransactionDTO> transactions = transactionService.searchAdminTransactions(
                keyword, parsedStatus, parsedPaymentMethod, fromDateTime, toDateExclusive, pageable);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @GetMapping("/transactions/{id}")
    public ResponseEntity<ApiResponse<AdminTransactionDetailDTO>> transactionDetail(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(transactionService.findAdminTransactionDetail(id)));
    }

    @GetMapping("/transactions/summary")
    public ResponseEntity<ApiResponse<AdminPaymentSummaryDTO>> transactionSummary() {
        return ResponseEntity.ok(ApiResponse.success(transactionService.getAdminPaymentSummary()));
    }

    private LocalDate parseDate(String value, String parameterName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(parameterName + " must use yyyy-MM-dd");
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
    public ResponseEntity<ApiResponse<UserResponseDTO>> softDeleteUser(
            @PathVariable Integer id,
            @AuthenticationPrincipal CustomUserDetails currentAdmin) {
        UserResponseDTO user = userService.softDeleteUser(id, currentAdmin.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success(user, "User account has been soft-deleted. Historical data remains preserved."));
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
    public ResponseEntity<?> approveBlog(@PathVariable Integer id,
                                         @AuthenticationPrincipal CustomUserDetails currentAdmin) {
        return ResponseEntity.ok(ApiResponse.success(
                blogPostService.approve(id, currentUser(currentAdmin)),
                "Blog approved and published successfully"
        ));
    }

    @PatchMapping("/blogs/{id}/reject")
    public ResponseEntity<?> rejectBlog(@PathVariable Integer id,
                                        @RequestBody Map<String, String> request,
                                        @AuthenticationPrincipal CustomUserDetails currentAdmin) {
        return ResponseEntity.ok(ApiResponse.success(
                blogPostService.reject(id, request == null ? null : request.get("rejectionReason"), currentUser(currentAdmin)),
                "Blog rejected with feedback"
        ));
    }

    @PatchMapping("/blogs/{id}/hide")
    public ResponseEntity<?> hideBlog(@PathVariable Integer id,
                                      @RequestBody(required = false) Map<String, String> request,
                                      @AuthenticationPrincipal CustomUserDetails currentAdmin) {
        return ResponseEntity.ok(ApiResponse.success(
                blogPostService.hide(id, request == null ? null : request.get("reason"), currentUser(currentAdmin)),
                "Blog hidden from public pages"
        ));
    }

    @PatchMapping("/blogs/{id}/archive")
    public ResponseEntity<?> archiveBlog(@PathVariable Integer id,
                                         @RequestBody(required = false) Map<String, String> request,
                                         @AuthenticationPrincipal CustomUserDetails currentAdmin) {
        return ResponseEntity.ok(ApiResponse.success(
                blogPostService.archive(id, request == null ? null : request.get("reason"), currentUser(currentAdmin)),
                "Blog archived"
        ));
    }

    @DeleteMapping("/blogs/{id}")
    public ResponseEntity<?> deleteBlog(@PathVariable Integer id,
                                        @RequestBody(required = false) Map<String, String> request,
                                        @AuthenticationPrincipal CustomUserDetails currentAdmin) {
        return ResponseEntity.ok(ApiResponse.success(
                blogPostService.archive(id, request == null ? null : request.get("reason"), currentUser(currentAdmin)),
                "Blog archived"
        ));
    }

    @GetMapping("/comments")
    public ResponseEntity<?> comments(@AuthenticationPrincipal CustomUserDetails currentAdmin) {
        return ResponseEntity.ok(ApiResponse.success(blogCommentService.findAllForAdmin(currentUser(currentAdmin))));
    }

    @PatchMapping("/comments/{id}/hide")
    public ResponseEntity<?> hideComment(@PathVariable Integer id,
                                         @RequestBody(required = false) Map<String, String> request,
                                         @AuthenticationPrincipal CustomUserDetails currentAdmin) {
        return ResponseEntity.ok(ApiResponse.success(
                blogCommentService.hide(id, request == null ? null : request.get("reason"), currentUser(currentAdmin)),
                "Comment hidden from public pages"
        ));
    }

    @DeleteMapping("/comments/{id}")
    public ResponseEntity<?> deleteComment(@PathVariable Integer id,
                                           @RequestBody(required = false) Map<String, String> request,
                                           @AuthenticationPrincipal CustomUserDetails currentAdmin) {
        return ResponseEntity.ok(ApiResponse.success(
                blogCommentService.delete(id, request == null ? null : request.get("reason"), currentUser(currentAdmin)),
                "Comment deleted from public pages"
        ));
    }

    @PatchMapping("/system-settings")
    public ResponseEntity<?> updateSystemSettings() {
        return ResponseEntity.ok(Map.of("message", "Update system settings placeholder - not yet implemented"));
    }

    private User currentUser(CustomUserDetails userDetails) {
        return userDetails == null ? null : userDetails.getUser();
    }
}
