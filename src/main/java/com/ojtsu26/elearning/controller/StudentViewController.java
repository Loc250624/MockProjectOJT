package com.ojtsu26.elearning.controller;

import com.ojtsu26.elearning.dto.response.CourseResponseDTO;
import com.ojtsu26.elearning.dto.response.LessonResponseDTO;
import com.ojtsu26.elearning.model.enums.CourseStatus;
import com.ojtsu26.elearning.service.CategoryService;
import com.ojtsu26.elearning.service.CourseService;
import com.ojtsu26.elearning.service.LessonService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentViewController {

    private final CourseService courseService;
    private final CategoryService categoryService;
    private final LessonService lessonService;

    @GetMapping("/dashboard")
    public String dashboard() { return "student/dashboard"; }

    @GetMapping("/profile")
    public String profile() { return "student/profile"; }

    @GetMapping("/courses")
    public String courses(@RequestParam(required = false) Integer categoryId,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false, defaultValue = "newest") String sortBy,
                          Model model) {
        List<CourseResponseDTO> courses = courseService.findApprovedCourses(categoryId, keyword, sortBy);
        model.addAttribute("courses", courses);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sortBy", sortBy);
        return "student/courses";
    }

    @GetMapping("/courses/detail")
    public String courseDetail(@RequestParam(required = false) Integer id,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (id == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Course ID is required.");
            return "redirect:/student/courses";
        }
        try {
            CourseResponseDTO course = courseService.findById(id);
            // Security: Only APPROVED courses are viewable through student catalog detail route
            if (course == null || course.getStatus() != CourseStatus.APPROVED) {
                redirectAttributes.addFlashAttribute("errorMessage", "Course is not approved or not available.");
                return "redirect:/student/courses";
            }
            List<LessonResponseDTO> lessons = lessonService.findByCourseId(id, null);
            model.addAttribute("course", course);
            model.addAttribute("lessons", lessons);
            return "student/course-detail";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Course not found.");
            return "redirect:/student/courses";
        }
    }

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
