package com.ojtsu26.elearning.controller;

import com.ojtsu26.elearning.dto.response.CourseResponseDTO;
import com.ojtsu26.elearning.dto.response.CourseEnrollmentStateResponseDTO;
import com.ojtsu26.elearning.dto.response.LessonResponseDTO;
import com.ojtsu26.elearning.dto.response.StudentLearningCourseDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.model.entity.Order;
import com.ojtsu26.elearning.model.enums.CourseStatus;
import com.ojtsu26.elearning.model.enums.OrderStatus;
import com.ojtsu26.elearning.model.enums.PaymentMethod;
import com.ojtsu26.elearning.model.enums.UserStatus;
import com.ojtsu26.elearning.dto.response.PaymentResponse;
import com.ojtsu26.elearning.repository.CourseEnrollmentRepository;
import com.ojtsu26.elearning.repository.OrderRepository;
import com.ojtsu26.elearning.security.CustomUserDetails;
import com.ojtsu26.elearning.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentViewController {

    private final CourseService courseService;
    private final CategoryService categoryService;
    private final LessonService lessonService;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final CourseEnrollmentService courseEnrollmentService;
    private final ProfileService profileService;
    private final StudentLearningService studentLearningService;
    private final CurrencyConversionService currencyConversionService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<com.ojtsu26.elearning.dto.response.StudentCourseProgressCardDTO> courseCards =
                studentLearningService.getCurrentStudentCourseCards();
        long completed = courseCards.stream().filter(card -> Boolean.TRUE.equals(card.getCompleted())).count();
        model.addAttribute("courseCards", courseCards.stream().limit(3).toList());
        model.addAttribute("enrolledCourseCount", courseCards.size());
        model.addAttribute("completedCourseCount", completed);
        model.addAttribute("inProgressCourseCount", Math.max(0, courseCards.size() - completed));
        return "student/dashboard";
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        model.addAttribute("profile", profileService.getCurrentProfile());
        model.addAttribute("overview", profileService.getCurrentProfileOverview());
        model.addAttribute("profileRole", "student");
        model.addAttribute("profilePortalName", "Student Portal");
        return "student/profile";
    }

    @GetMapping("/courses")
    public String courses(@RequestParam(required = false) Integer categoryId,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false, defaultValue = "newest") String sortBy,
                          @RequestParam(value = "page", defaultValue = "0") int page,
                          @RequestParam(value = "size", defaultValue = "6") int size,
                          Model model,
                          @org.springframework.security.core.annotation.AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getUser().getStatus() != UserStatus.ACTIVE) {
            return "redirect:/auth/login?error=blocked";
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<CourseResponseDTO> coursePage = courseService.findApprovedCourses(categoryId, keyword, sortBy, pageable);
        
        Integer studentId = userDetails.getUser().getId();
        
        // Fetch student's enrollments and map to a set of course IDs
        java.util.Set<Integer> enrolledCourseIds = courseEnrollmentRepository.findByStudentId(studentId).stream()
                .filter(e -> e.getCourse() != null)
                .map(e -> e.getCourse().getId())
                .collect(java.util.stream.Collectors.toSet());
                
        // Fetch student's pending order course IDs in a single query
        java.util.List<Integer> pendingCourseIds = orderRepository.findCourseIdsByStudentIdAndStatus(studentId, OrderStatus.PENDING);
        
        coursePage.getContent().forEach(c -> {
            if (enrolledCourseIds.contains(c.getId())) {
                c.setEnrollmentStatus("ENROLLED");
            } else if (pendingCourseIds.contains(c.getId())) {
                c.setEnrollmentStatus("PENDING");
            } else {
                c.setEnrollmentStatus("NOT_ENROLLED");
            }
        });
        
        model.addAttribute("courses", coursePage.getContent());
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", coursePage.getTotalPages());
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
            CourseEnrollmentStateResponseDTO enrollmentState = courseEnrollmentService.getCurrentStudentCourseState(id);
            model.addAttribute("course", course);
            model.addAttribute("lessons", lessons);
            model.addAttribute("enrollmentState", enrollmentState);
            return "student/course-detail";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Course not found.");
            return "redirect:/student/courses";
        }
    }

    @GetMapping("/my-courses")
    public String myCourses(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getUser().getStatus() != UserStatus.ACTIVE) {
            return "redirect:/auth/login?error=blocked";
        }
        model.addAttribute("courseCards", studentLearningService.getCurrentStudentCourseCards());
        return "student/my-courses";
    }

    @GetMapping("/learning")
    public String learning(@RequestParam(required = false) Integer courseId,
                           @RequestParam(required = false) Integer lessonId,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (courseId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Course ID is required.");
            return "redirect:/student/my-courses";
        }
        try {
            StudentLearningCourseDTO learningCourse = studentLearningService.getLearningCourse(courseId, lessonId);
            model.addAttribute("learningCourse", learningCourse);
            model.addAttribute("courseId", courseId);
            model.addAttribute("lessonId", learningCourse.getActiveLessonId());
            return "student/learning";
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/student/courses/detail?id=" + courseId;
        }
    }

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
    public String checkout(@RequestParam(value = "courseId", required = false) Integer courseId,
                           Model model,
                           @AuthenticationPrincipal CustomUserDetails userDetails,
                           RedirectAttributes redirectAttributes) {
        if (courseId == null) {
            model.addAttribute("cartEmpty", true);
            return "student/checkout";
        }

        try {
            CourseResponseDTO course = courseService.findById(courseId);
            if (course == null || course.getStatus() != CourseStatus.APPROVED) {
                redirectAttributes.addFlashAttribute("errorMessage", "This course is not available for purchase.");
                return "redirect:/student/courses";
            }

            Integer studentId = userDetails.getUser().getId();
            
            // Check student account status
            if (userDetails.getUser().getStatus() == UserStatus.BLOCKED) {
                redirectAttributes.addFlashAttribute("errorMessage", "Your account is blocked. Please contact support.");
                return "redirect:/student/courses";
            }

            // Check owned/already enrolled
            if (courseEnrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
                redirectAttributes.addFlashAttribute("errorMessage", "You are already enrolled in this course.");
                return "redirect:/student/courses/detail?id=" + courseId;
            }

            BigDecimal price = course.getPrice() != null ? course.getPrice() : BigDecimal.ZERO;
            BigDecimal discount = BigDecimal.ZERO; // Default discount placeholder
            BigDecimal total = price.subtract(discount);
            // Compute VND equivalent for display on checkout (MoMo payment amount)
            BigDecimal vndAmount = currencyConversionService.convertUsdToVnd(total);
            BigDecimal exchangeRate = currencyConversionService.getExchangeRate();

            model.addAttribute("cartEmpty", false);
            model.addAttribute("courseId", courseId);
            model.addAttribute("course", course);
            model.addAttribute("courseName", course.getTitle());
            model.addAttribute("coursePrice", price);
            model.addAttribute("subtotal", price);
            model.addAttribute("discount", discount);
            model.addAttribute("total", total);
            model.addAttribute("vndAmount", vndAmount);
            model.addAttribute("exchangeRate", exchangeRate);

            return "student/checkout";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to load checkout details.");
            return "redirect:/student/courses";
        }
    }

    @PostMapping("/checkout")
    public String handleCheckout(@RequestParam("courseId") Integer courseId,
                                 @RequestParam("pay") String payMethodStr,
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        try {
            PaymentMethod method = PaymentMethod.valueOf(payMethodStr.toUpperCase());
            Order order = orderService.createOrder(userDetails.getUser(), courseId, method);
            PaymentResponse response = paymentService.initiatePayment(order);

            if (response.isSuccess() && response.getPaymentUrl() != null) {
                return "redirect:" + response.getPaymentUrl();
            } else {
                String errMsg = response.getErrorMessage() != null ? response.getErrorMessage() : "Unknown gateway error";
                redirectAttributes.addFlashAttribute("errorMessage", "Payment gateway error: " + errMsg);
                return "redirect:/student/checkout?courseId=" + courseId;
            }
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid payment method selected.");
            return "redirect:/student/checkout?courseId=" + courseId;
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/student/checkout?courseId=" + courseId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred: " + e.getMessage());
            return "redirect:/student/checkout?courseId=" + courseId;
        }
    }



    @GetMapping("/payment-result")
    public String paymentResult(
            @RequestParam java.util.Map<String, String> params,
            Model model) {

        String orderId = params.get("orderId");
        String resultCode = params.get("resultCode");
        String message = params.get("message");

        // Support VNPAY query params mapping
        if (params.containsKey("vnp_TxnRef")) {
            orderId = params.get("vnp_TxnRef");
            String vnpResponseCode = params.get("vnp_ResponseCode");
            resultCode = "00".equals(vnpResponseCode) ? "0" : (vnpResponseCode != null ? vnpResponseCode : "99");
            message = "0".equals(resultCode) ? "Payment successful!" : "Payment failed (code: " + vnpResponseCode + ")";
        }

        if (orderId == null) {
            // No order context — just show generic page
            model.addAttribute("paymentSuccess", false);
            model.addAttribute("paymentMessage", "No payment information found.");
            return "student/payment-result";
        }

        // MoMo and VNPAY mapped success code
        boolean success = "0".equals(resultCode);
        model.addAttribute("paymentSuccess", success);
        model.addAttribute("orderId", orderId);
        model.addAttribute("paymentMessage",
                success ? "Payment successful! Your enrollment has been activated."
                        : (message != null ? message : "Payment failed or was cancelled."));

        if (success) {
            // Redirect to My Courses after short delay via meta-refresh
            model.addAttribute("redirectToMyCourses", true);
        }

        return "student/payment-result";
    }

    @GetMapping("/payment-history")
    public String paymentHistory(@RequestParam(value = "page", defaultValue = "0") int page,
                                 @RequestParam(value = "size", defaultValue = "10") int size,
                                 Model model,
                                 @AuthenticationPrincipal CustomUserDetails userDetails) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orderPage = orderRepository.findByUserId(userDetails.getUser().getId(), pageable);
        
        model.addAttribute("orders", orderPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", orderPage.getTotalPages());
        return "student/payment-history";
    }

    @GetMapping("/blogs")
    public String blogs() { return "student/blogs"; }

    @GetMapping("/blogs/detail")
    public String blogDetail() { return "student/blog-detail"; }
}
