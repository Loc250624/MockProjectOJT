package com.ojtsu26.elearning.controller;

import com.ojtsu26.elearning.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminViewController {

    private final ProfileService profileService;

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

    @GetMapping("/courses")
    public String courses() { return "admin/courses"; }

    @GetMapping("/courses/detail")
    public String courseDetail() { return "admin/course-detail"; }

    @GetMapping("/courses/approval")
    public String courseApproval() { return "admin/course-approval"; }

    @GetMapping("/categories")
    public String categories() { return "admin/categories"; }

    @GetMapping("/categories/create")
    public String categoryCreate() { return "admin/category-form"; }

    @GetMapping("/categories/edit")
    public String categoryEdit() { return "admin/category-form"; }

    @GetMapping("/blogs")
    public String blogs() { return "admin/blogs"; }

    @GetMapping("/blogs/detail")
    public String blogDetail() { return "admin/blog-detail"; }

    @GetMapping("/blogs/moderation")
    public String blogModeration() { return "admin/blog-moderation"; }

    @GetMapping("/comments")
    public String comments() { return "admin/comments"; }

    @GetMapping("/payments")
    public String payments() { return "admin/payments"; }

    @GetMapping("/transactions")
    public String transactions() { return "admin/transactions"; }

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
