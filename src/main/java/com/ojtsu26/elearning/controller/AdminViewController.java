package com.ojtsu26.elearning.controller;

import com.ojtsu26.elearning.service.CategoryService;
import com.ojtsu26.elearning.service.AdminTeacherPayoutService;
import com.ojtsu26.elearning.service.BlogCommentService;
import com.ojtsu26.elearning.service.BlogPostService;
import com.ojtsu26.elearning.service.CourseService;
import com.ojtsu26.elearning.service.ProfileService;
import com.ojtsu26.elearning.service.TransactionService;
import com.ojtsu26.elearning.dto.request.CategoryRequestDTO;
import com.ojtsu26.elearning.dto.response.CategoryResponseDTO;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.CourseStatus;
import com.ojtsu26.elearning.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.validation.BindingResult;
import jakarta.validation.Valid;

import com.ojtsu26.elearning.model.entity.Transaction;
import com.ojtsu26.elearning.model.enums.PaymentMethod;
import com.ojtsu26.elearning.model.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminViewController {

    private static final String CSV_UTF8_BOM = "\uFEFF";
    private static final java.time.format.DateTimeFormatter CSV_TIMESTAMP_FORMATTER =
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CategoryService categoryService;
    private final CourseService courseService;
    private final ProfileService profileService;
    private final BlogPostService blogPostService;
    private final BlogCommentService blogCommentService;
    private final TransactionService transactionService;
    private final AdminTeacherPayoutService adminTeacherPayoutService;

    @GetMapping("/dashboard")
    public String dashboard() { return "admin/dashboard"; }

    @GetMapping("/profile")
    public String profile(Model model) {
        model.addAttribute("profile", profileService.getCurrentProfile());
        model.addAttribute("overview", profileService.getCurrentProfileOverview());
        model.addAttribute("profileRole", "admin");
        model.addAttribute("profilePortalName", "Admin Portal");
        return "student/profile";
    }

    @GetMapping("/users")
    public String users() { return "admin/users"; }

    @GetMapping("/users/detail")
    public String userDetail() { return "admin/user-detail"; }

    @GetMapping("/users/create")
    public String userCreate() { return "admin/user-form"; }

    @GetMapping("/users/edit")
    public String userEdit() { return "admin/user-form"; }

    /**
     * Admin: All courses overview (all statuses).
     */
    @GetMapping("/courses")
    public String courses(Model model) {
        model.addAttribute("courses", courseService.findAll());
        return "admin/courses";
    }

    @GetMapping("/courses/detail")
    public String courseDetail() { return "admin/course-detail"; }

    /**
     * CRS-09: Course moderation queue.
     * Defaults to PENDING_APPROVAL. Accepts ?status= to switch tabs.
     */
    @GetMapping("/courses/approval")
    public String courseApproval(@RequestParam(required = false, defaultValue = "PENDING_APPROVAL") String status,
                                 Model model) {
        CourseStatus selectedStatus;
        try {
            selectedStatus = CourseStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            selectedStatus = CourseStatus.PENDING_APPROVAL;
        }

        model.addAttribute("courses", courseService.findByStatus(selectedStatus));
        model.addAttribute("selectedStatus", selectedStatus.name());

        // Counts for tab badges
        model.addAttribute("pendingCount",  courseService.findByStatus(CourseStatus.PENDING_APPROVAL).size());
        model.addAttribute("approvedCount", courseService.findByStatus(CourseStatus.APPROVED).size());
        model.addAttribute("hiddenCount",   courseService.findByStatus(CourseStatus.HIDDEN).size());
        model.addAttribute("draftCount",    courseService.findByStatus(CourseStatus.DRAFT).size());

        return "admin/course-approval";
    }

    @GetMapping("/categories")
    public String categories(Model model) {
        model.addAttribute("categories", categoryService.findAll());
        return "admin/categories";
    }

    @GetMapping("/categories/create")
    public String categoryCreate(Model model) {
        if (!model.containsAttribute("category")) {
            model.addAttribute("category", new CategoryRequestDTO());
        }
        return "admin/category-form";
    }

    @GetMapping("/categories/edit/{id}")
    public String categoryEdit(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        try {
            if (!model.containsAttribute("category")) {
                CategoryResponseDTO categoryDTO = categoryService.findById(id);
                CategoryRequestDTO form = new CategoryRequestDTO();
                form.setName(categoryDTO.getName());
                form.setDescription(categoryDTO.getDescription());
                model.addAttribute("category", form);
            }
            model.addAttribute("id", id);
            return "admin/category-form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Category not found: " + e.getMessage());
            return "redirect:/admin/categories";
        }
    }

    @GetMapping("/blogs")
    public String blogs(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        model.addAttribute("blogs", blogPostService.findAllForAdmin(currentUser(userDetails)));
        return "admin/blogs";
    }

    @GetMapping("/blogs/detail")
    public String blogDetail() { return "admin/blog-detail"; }

    @GetMapping("/blogs/moderation")
    public String blogModeration(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        model.addAttribute("blogs", blogPostService.findPendingReviewForAdmin(currentUser(userDetails)));
        return "admin/blog-moderation";
    }

    @PostMapping("/blogs/approve/{id}")
    public String approveBlog(@PathVariable Integer id,
                              @AuthenticationPrincipal CustomUserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        try {
            blogPostService.approve(id, currentUser(userDetails));
            redirectAttributes.addFlashAttribute("successMessage", "Blog approved and published successfully.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/blogs/moderation";
    }

    @PostMapping("/blogs/reject/{id}")
    public String rejectBlog(@PathVariable Integer id,
                             @RequestParam(required = false) String rejectionReason,
                             @AuthenticationPrincipal CustomUserDetails userDetails,
                             RedirectAttributes redirectAttributes) {
        try {
            blogPostService.reject(id, rejectionReason, currentUser(userDetails));
            redirectAttributes.addFlashAttribute("successMessage", "Blog rejected with feedback.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/blogs/moderation";
    }

    @PostMapping("/blogs/hide/{id}")
    public String hideBlog(@PathVariable Integer id,
                           @RequestParam(required = false) String reason,
                           @AuthenticationPrincipal CustomUserDetails userDetails,
                           RedirectAttributes redirectAttributes) {
        try {
            blogPostService.hide(id, reason, currentUser(userDetails));
            redirectAttributes.addFlashAttribute("successMessage", "Blog hidden from public pages.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/blogs";
    }

    @PostMapping("/blogs/archive/{id}")
    public String archiveBlog(@PathVariable Integer id,
                              @RequestParam(required = false) String reason,
                              @AuthenticationPrincipal CustomUserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        try {
            blogPostService.archive(id, reason, currentUser(userDetails));
            redirectAttributes.addFlashAttribute("successMessage", "Blog archived.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/blogs";
    }

    @GetMapping("/comments")
    public String comments(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        model.addAttribute("comments", blogCommentService.findAllForAdmin(currentUser(userDetails)));
        return "admin/comments";
    }

    @PostMapping("/comments/hide/{id}")
    public String hideComment(@PathVariable Integer id,
                              @RequestParam(required = false) String reason,
                              @AuthenticationPrincipal CustomUserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        try {
            blogCommentService.hide(id, reason, currentUser(userDetails));
            redirectAttributes.addFlashAttribute("successMessage", "Comment hidden from public pages.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/comments";
    }

    @PostMapping("/comments/delete/{id}")
    public String deleteComment(@PathVariable Integer id,
                                @RequestParam(required = false) String reason,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        try {
            blogCommentService.delete(id, reason, currentUser(userDetails));
            redirectAttributes.addFlashAttribute("successMessage", "Comment deleted from public pages.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/comments";
    }

    @GetMapping("/payments")
    public String payments(@RequestParam(value = "page", defaultValue = "0") int page,
                           @RequestParam(value = "size", defaultValue = "20") int size,
                           @RequestParam(value = "keyword", required = false) String keyword,
                           @RequestParam(value = "date", required = false) String date,
                           @RequestParam(value = "tab", defaultValue = "platform") String tab,
                           Model model) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        java.time.LocalDate selectedDate = parsePaymentDate(date);
        java.time.LocalDateTime fromDate = selectedDate == null ? null : selectedDate.atStartOfDay();
        java.time.LocalDateTime toDate = selectedDate == null ? null : selectedDate.plusDays(1).atStartOfDay();
        Page<Transaction> txnPage = transactionService.searchTransactions(keyword, fromDate, toDate, pageable);
        String activeTab = "teacher".equalsIgnoreCase(tab) ? "teacher" : "platform";

        model.addAttribute("transactions", txnPage.getContent());
        model.addAttribute("paymentSummary", transactionService.getAdminPaymentSummary(fromDate, toDate));
        model.addAttribute("teacherPayouts", adminTeacherPayoutService.findPayoutReadiness(fromDate, toDate));
        model.addAttribute("activeTab", activeTab);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", txnPage.getTotalPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("invalidDateFilter", date != null && !date.isBlank() && selectedDate == null);
        return "admin/payments";
    }

    @GetMapping(value = "/payments/export/csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportPaymentsCsv(@RequestParam(value = "keyword", required = false) String keyword,
                                                    @RequestParam(value = "date", required = false) String date) {
        java.time.LocalDate selectedDate = parsePaymentDate(date);
        java.time.LocalDateTime fromDate = selectedDate == null ? null : selectedDate.atStartOfDay();
        java.time.LocalDateTime toDate = selectedDate == null ? null : selectedDate.plusDays(1).atStartOfDay();
        java.util.List<Transaction> transactions = transactionService.findTransactionsForAdminPaymentsExport(
                keyword, fromDate, toDate, Sort.by(Sort.Direction.DESC, "createdAt"));
        String filename = selectedDate == null
                ? "lumina-platform-payments.csv"
                : "lumina-platform-payments-" + selectedDate + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(buildPaymentsCsv(transactions).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @GetMapping("/transactions")
    public String transactions(Model model) {
        model.addAttribute("transactionStatuses", TransactionStatus.values());
        model.addAttribute("paymentMethods", PaymentMethod.values());
        return "admin/transactions";
    }

    @GetMapping("/refunds")
    public String refunds() { return "admin/refunds"; }

    @GetMapping("/analytics")
    public String analytics() { return "admin/analytics"; }

    @GetMapping("/statistics/students")
    public String studentStatistics() { return "admin/analytics"; }

    @GetMapping("/reports/revenue")
    public String revenueReport() { return "admin/revenue-report"; }

    @GetMapping("/analytics/revenue")
    public String analyticsRevenueReport() { return "admin/revenue-report"; }

    @GetMapping("/settings")
    public String settings() { return "admin/settings"; }

    @GetMapping("/system-settings")
    public String systemSettings() { return "admin/settings"; }

    @PostMapping("/categories/create")
    public String createCategory(@Valid @ModelAttribute("category") CategoryRequestDTO requestDTO, 
                                 BindingResult bindingResult, 
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.category", bindingResult);
            redirectAttributes.addFlashAttribute("category", requestDTO);
            return "redirect:/admin/categories/create";
        }
        try {
            categoryService.create(requestDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Category created successfully!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("category", requestDTO);
            return "redirect:/admin/categories/create";
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/edit/{id}")
    public String updateCategory(@PathVariable Integer id,
                                 @Valid @ModelAttribute("category") CategoryRequestDTO requestDTO, 
                                 BindingResult bindingResult, 
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.category", bindingResult);
            redirectAttributes.addFlashAttribute("category", requestDTO);
            return "redirect:/admin/categories/edit/" + id;
        }
        try {
            categoryService.update(id, requestDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Category updated successfully!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("category", requestDTO);
            return "redirect:/admin/categories/edit/" + id;
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/delete/{id}")
    public String deleteCategory(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Category deleted successfully!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/courses/approve/{id}")
    public String approveCourse(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            courseService.approveCourse(id);
            redirectAttributes.addFlashAttribute("successMessage", "Course approved and published successfully.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/courses/approval?status=PENDING_APPROVAL";
    }

    @PostMapping("/courses/reject/{id}")
    public String rejectCourse(@PathVariable Integer id,
                               @RequestParam(required = false) String rejectReason,
                               RedirectAttributes redirectAttributes) {
        try {
            courseService.rejectCourse(id, rejectReason);
            redirectAttributes.addFlashAttribute("successMessage", "Course rejected. The teacher will be notified.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/courses/approval?status=PENDING_APPROVAL";
    }

    @PostMapping("/courses/hide/{id}")
    public String hideCourse(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            courseService.hideCourse(id);
            redirectAttributes.addFlashAttribute("successMessage", "Course is now hidden from public catalog.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/courses/approval?status=APPROVED";
    }

    @PostMapping("/courses/unhide/{id}")
    public String unhideCourse(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            courseService.unhideCourse(id);
            redirectAttributes.addFlashAttribute("successMessage", "Course is now visible in the public catalog.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/courses/approval?status=HIDDEN";
    }

    @PostMapping("/courses/delete/{id}")
    public String deleteCourse(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            courseService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Course deleted permanently.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/courses/approval";
    }

    private User currentUser(CustomUserDetails userDetails) {
        return userDetails == null ? null : userDetails.getUser();
    }

    private java.time.LocalDate parsePaymentDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return java.time.LocalDate.parse(value.trim());
        } catch (java.time.format.DateTimeParseException ex) {
            return null;
        }
    }

    private String buildPaymentsCsv(java.util.List<Transaction> transactions) {
        StringBuilder csv = new StringBuilder(CSV_UTF8_BOM);
        csv.append("Transaction ID,Order ID,Customer Name,Customer Email,Amount,Currency,Payment Method,Status,Transaction Date/Time\r\n");
        for (Transaction transaction : transactions) {
            csv.append(csvText(transaction.getTransactionRef())).append(',')
                    .append(csvText(orderIdentifier(transaction))).append(',')
                    .append(csvText(transaction.getStudent() == null ? null : transaction.getStudent().getFullName())).append(',')
                    .append(csvText(transaction.getStudent() == null ? null : transaction.getStudent().getEmail())).append(',')
                    .append(csvNumber(transaction.getAmount())).append(',')
                    .append(csvText("VND")).append(',')
                    .append(csvText(transaction.getPaymentMethod())).append(',')
                    .append(csvText(transaction.getStatus())).append(',')
                    .append(csvText(formatCsvTimestamp(csvTransactionTimestamp(transaction))))
                    .append("\r\n");
        }
        return csv.toString();
    }

    private String orderIdentifier(Transaction transaction) {
        if (transaction == null || transaction.getOrder() == null) {
            return null;
        }
        if (transaction.getOrder().getOrderCode() != null && !transaction.getOrder().getOrderCode().isBlank()) {
            return transaction.getOrder().getOrderCode();
        }
        return transaction.getOrder().getId() == null ? null : String.valueOf(transaction.getOrder().getId());
    }

    private String formatCsvTimestamp(java.time.LocalDateTime timestamp) {
        return timestamp == null ? "" : CSV_TIMESTAMP_FORMATTER.format(timestamp);
    }

    private java.time.LocalDateTime csvTransactionTimestamp(Transaction transaction) {
        if (transaction == null) {
            return null;
        }
        if (transaction.getCreatedAt() != null) {
            return transaction.getCreatedAt();
        }
        if (transaction.getUpdatedAt() != null) {
            return transaction.getUpdatedAt();
        }
        return transaction.getOrder() == null ? null : transaction.getOrder().getCreatedAt();
    }

    private String csvNumber(java.math.BigDecimal value) {
        return value == null
                ? ""
                : value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private String csvText(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        if (!text.isEmpty() && (text.charAt(0) == '=' || text.charAt(0) == '+'
                || text.charAt(0) == '-' || text.charAt(0) == '@')) {
            text = "'" + text;
        }
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}
