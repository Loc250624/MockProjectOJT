package com.ojtsu26.elearning.controller;

import com.ojtsu26.elearning.dto.response.CourseResponseDTO;
import com.ojtsu26.elearning.dto.response.CourseEnrollmentStateResponseDTO;
import com.ojtsu26.elearning.dto.response.LessonResponseDTO;
import com.ojtsu26.elearning.model.enums.CourseStatus;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.security.CustomUserDetails;
import com.ojtsu26.elearning.service.CategoryService;
import com.ojtsu26.elearning.service.CertificateService;
import com.ojtsu26.elearning.service.CourseEnrollmentService;
import com.ojtsu26.elearning.service.CourseService;
import com.ojtsu26.elearning.service.LessonService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class PublicController {

    private final CourseService courseService;
    private final CategoryService categoryService;
    private final LessonService lessonService;
    private final CourseEnrollmentService courseEnrollmentService;
    private final CertificateService certificateService;

    @GetMapping("/")
    public String homePage() {
        return "public/home";
    }

    @GetMapping({"/courses", "/public/courses"})
    public String coursesPage(@RequestParam(required = false) Integer categoryId,
                              @RequestParam(required = false) String keyword,
                              @RequestParam(required = false, defaultValue = "newest") String sortBy,
                              Model model) {
        List<CourseResponseDTO> courses = courseService.findApprovedCourses(categoryId, keyword, sortBy);
        model.addAttribute("courses", courses);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sortBy", sortBy);
        return "public/courses";
    }

    @GetMapping({"/courses/detail", "/public/courses/detail"})
    public String courseDetailPage(@RequestParam(required = false) Integer id,
                                   Model model,
                                   @AuthenticationPrincipal CustomUserDetails userDetails,
                                   RedirectAttributes redirectAttributes) {
        if (id == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Course ID is required.");
            return "redirect:/courses";
        }
        try {
            CourseResponseDTO course = courseService.findById(id);
            // Security: Only APPROVED courses are viewable through public catalog details route
            if (course == null || course.getStatus() != CourseStatus.APPROVED) {
                redirectAttributes.addFlashAttribute("errorMessage", "Course is not approved or not available.");
                return "redirect:/courses";
            }
            List<LessonResponseDTO> lessons = lessonService.findByCourseId(id, null);
            model.addAttribute("course", course);
            model.addAttribute("lessons", lessons);
            if (userDetails != null && userDetails.getUser().getRole() == Role.STUDENT) {
                CourseEnrollmentStateResponseDTO enrollmentState = courseEnrollmentService.getCurrentStudentCourseState(id);
                model.addAttribute("enrollmentState", enrollmentState);
            }
            return "public/course-detail";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Course not found.");
            return "redirect:/courses";
        }
    }

    @GetMapping("/blogs")
    public String blogsPage() {
        return "public/blogs";
    }

    @GetMapping("/blogs/detail")
    public String blogDetailPage() {
        return "public/blog-detail";
    }

    @GetMapping("/certificates/verify/{verificationCode}")
    public String verifyCertificate(@org.springframework.web.bind.annotation.PathVariable String verificationCode,
                                    Model model) {
        model.addAttribute("verification", certificateService.verifyByCode(verificationCode));
        return "public/certificate-verification";
    }
}
