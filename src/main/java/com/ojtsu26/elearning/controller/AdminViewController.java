package com.ojtsu26.elearning.controller;

import com.ojtsu26.elearning.service.CategoryService;
import com.ojtsu26.elearning.service.CourseService;
import com.ojtsu26.elearning.dto.request.CategoryRequestDTO;
import com.ojtsu26.elearning.dto.response.CategoryResponseDTO;
import com.ojtsu26.elearning.model.enums.CourseStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ojtsu26.elearning.repository.TransactionRepository;
import com.ojtsu26.elearning.model.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminViewController {

    private final CategoryService categoryService;
    private final CourseService courseService;
    private final TransactionRepository transactionRepository;

    @GetMapping("/dashboard")
    public String dashboard() { return "admin/dashboard"; }

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
    public String blogs() { return "admin/blogs"; }

    @GetMapping("/blogs/detail")
    public String blogDetail() { return "admin/blog-detail"; }

    @GetMapping("/blogs/moderation")
    public String blogModeration() { return "admin/blog-moderation"; }

    @GetMapping("/comments")
    public String comments() { return "admin/comments"; }

    @GetMapping("/payments")
    public String payments(@RequestParam(value = "page", defaultValue = "0") int page,
                           @RequestParam(value = "size", defaultValue = "10") int size,
                           @RequestParam(value = "keyword", required = false) String keyword,
                           Model model) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Transaction> txnPage = transactionRepository.searchTransactions(keyword, pageable);

        model.addAttribute("transactions", txnPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", txnPage.getTotalPages());
        model.addAttribute("keyword", keyword);
        return "admin/payments";
    }

    @GetMapping("/transactions")
    public String transactions(@RequestParam(value = "page", defaultValue = "0") int page,
                               @RequestParam(value = "size", defaultValue = "10") int size,
                               @RequestParam(value = "keyword", required = false) String keyword,
                               Model model) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Transaction> txnPage = transactionRepository.searchTransactions(keyword, pageable);

        model.addAttribute("transactions", txnPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", txnPage.getTotalPages());
        model.addAttribute("keyword", keyword);
        return "admin/transactions";
    }

    @GetMapping("/refunds")
    public String refunds() { return "admin/refunds"; }

    @GetMapping("/analytics")
    public String analytics() { return "admin/analytics"; }

    @GetMapping("/statistics/students")
    public String studentStatistics() { return "admin/student-statistics"; }

    @GetMapping("/reports/revenue")
    public String revenueReport() { return "admin/revenue-report"; }

    @GetMapping("/settings")
    public String settings() { return "admin/settings"; }

    @GetMapping("/system-settings")
    public String systemSettings() { return "admin/system-settings"; }
}
