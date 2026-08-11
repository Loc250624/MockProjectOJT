package com.ojtsu26.elearning.controller;

import com.ojtsu26.elearning.common.UserFacingErrorMessage;
import com.ojtsu26.elearning.dto.response.CourseResponseDTO;
import com.ojtsu26.elearning.dto.response.CourseEnrollmentStateResponseDTO;
import com.ojtsu26.elearning.dto.response.LessonResponseDTO;
import com.ojtsu26.elearning.dto.request.BlogCommentRequestDTO;
import com.ojtsu26.elearning.dto.response.BlogPostResponseDTO;
import com.ojtsu26.elearning.model.enums.CourseStatus;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.security.CustomUserDetails;
import com.ojtsu26.elearning.service.BlogCommentService;
import com.ojtsu26.elearning.service.BlogPostService;
import com.ojtsu26.elearning.service.CategoryService;
import com.ojtsu26.elearning.service.CertificateService;
import com.ojtsu26.elearning.service.CourseEnrollmentService;
import com.ojtsu26.elearning.service.CourseService;
import com.ojtsu26.elearning.service.LessonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
    private final BlogPostService blogPostService;
    private final BlogCommentService blogCommentService;

    @GetMapping("/")
    public String homePage(Model model) {
        List<CourseResponseDTO> featuredCourses = courseService.findApprovedCourses(null, null, "newest")
                .stream()
                .limit(3)
                .toList();
        model.addAttribute("featuredCourses", featuredCourses);
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

    @GetMapping({"/blogs", "/public/blogs"})
    public String blogsPage(@RequestParam(required = false) String keyword, Model model) {
        model.addAttribute("blogs", blogPostService.findPublished(keyword));
        model.addAttribute("keyword", keyword);
        return "public/blogs";
    }

    @GetMapping({"/blogs/detail", "/public/blogs/detail"})
    public String blogDetailPage(@RequestParam(required = false) Integer id,
                                 Model model,
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        return showBlogDetail(id, model, userDetails, redirectAttributes);
    }

    @GetMapping({"/blogs/{id}", "/public/blogs/{id}"})
    public String blogDetailPath(@PathVariable Integer id,
                                 Model model,
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        return showBlogDetail(id, model, userDetails, redirectAttributes);
    }

    @PostMapping("/blogs/{id}/comments")
    public String addComment(@PathVariable Integer id,
                             @Valid @ModelAttribute("comment") BlogCommentRequestDTO requestDTO,
                             BindingResult bindingResult,
                             @AuthenticationPrincipal CustomUserDetails userDetails,
                             RedirectAttributes redirectAttributes) {
        if (userDetails == null || userDetails.getUser() == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please log in to comment.");
            return "redirect:/auth/login";
        }
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.comment", bindingResult);
            redirectAttributes.addFlashAttribute("comment", requestDTO);
            redirectAttributes.addFlashAttribute("errorMessage", "Please fix the comment before posting.");
            return "redirect:/blogs/" + id;
        }
        try {
            blogCommentService.addComment(id, requestDTO, userDetails.getUser());
            redirectAttributes.addFlashAttribute("successMessage", "Comment posted successfully.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("comment", requestDTO);
            redirectAttributes.addFlashAttribute("errorMessage", UserFacingErrorMessage.from(e));
        }
        return "redirect:/blogs/" + id;
    }

    private String showBlogDetail(Integer id,
                                  Model model,
                                  CustomUserDetails userDetails,
                                  RedirectAttributes redirectAttributes) {
        if (id == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Blog ID is required.");
            return "redirect:/blogs";
        }
        try {
            BlogPostResponseDTO blog = blogPostService.findPublishedById(id);
            model.addAttribute("blog", blog);
            model.addAttribute("comments", blogCommentService.findVisibleForPublishedBlog(id));
            model.addAttribute("canComment", userDetails != null && userDetails.getUser() != null);
            if (!model.containsAttribute("comment")) {
                model.addAttribute("comment", new BlogCommentRequestDTO());
            }
            return "public/blog-detail";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Blog is not published or not available.");
            return "redirect:/blogs";
        }
    }

    @GetMapping("/certificates/verify/{verificationCode}")
    public String verifyCertificate(@org.springframework.web.bind.annotation.PathVariable String verificationCode,
                                    Model model) {
        model.addAttribute("verification", certificateService.verifyByCode(verificationCode));
        return "public/certificate-verification";
    }
}
