package com.ojtsu26.elearning.controller;

import com.ojtsu26.elearning.security.CustomUserDetails;
import com.ojtsu26.elearning.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentViewController {

    private final UserService userService;

    @GetMapping("/dashboard")
    public String dashboard() { return "student/dashboard"; }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal CustomUserDetails currentUser, Model model) {
        model.addAttribute("profile", userService.getCurrentProfile(currentUser.getUser().getId()));
        model.addAttribute("profileRole", "student");
        model.addAttribute("profilePortalName", "Student Portal");
        return "student/profile";
    }

    @GetMapping("/courses")
    public String courses() { return "student/courses"; }

    @GetMapping("/courses/detail")
    public String courseDetail() { return "student/course-detail"; }

    @GetMapping("/my-courses")
    public String myCourses() { return "student/my-courses"; }

    @GetMapping("/learning")
    public String learning() { return "student/learning"; }

    @GetMapping("/lessons/view")
    public String lessonView() { return "student/lesson-view"; }

    @GetMapping("/quiz")
    public String quiz() { return "student/quiz"; }

    @GetMapping("/code-assignment")
    public String codeAssignment() { return "student/code-assignment"; }

    @GetMapping("/results")
    public String results() { return "student/results"; }

    @GetMapping("/certificates")
    public String certificates() { return "student/certificates"; }

    @GetMapping("/notifications")
    public String notifications() { return "student/notifications"; }

    @GetMapping("/checkout")
    public String checkout() { return "student/checkout"; }

    @GetMapping("/payment-result")
    public String paymentResult() { return "student/payment-result"; }

    @GetMapping("/payment-history")
    public String paymentHistory() { return "student/payment-history"; }

    @GetMapping("/blogs")
    public String blogs() { return "student/blogs"; }

    @GetMapping("/blogs/detail")
    public String blogDetail() { return "student/blog-detail"; }
}
