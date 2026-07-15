package com.ojtsu26.elearning.controller;

import com.ojtsu26.elearning.model.entity.*;
import com.ojtsu26.elearning.model.enums.*;
import com.ojtsu26.elearning.repository.*;
import com.ojtsu26.elearning.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import jakarta.servlet.http.Cookie;
import com.ojtsu26.elearning.service.PaymentService;
import com.ojtsu26.elearning.service.OrderService;
import com.ojtsu26.elearning.common.HmacUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:student_view_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class StudentViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CourseEnrollmentRepository courseEnrollmentRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private LessonProgressRepository lessonProgressRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private RefundTransactionRepository refundTransactionRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PaymentService paymentService;

    private String studentToken;
    private User student;
    private Course course;
    private User instructor;
    private Category category;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        refundTransactionRepository.deleteAll();
        transactionRepository.deleteAll();
        orderRepository.deleteAll();
        lessonProgressRepository.deleteAll();
        courseEnrollmentRepository.deleteAll();
        lessonRepository.deleteAll();
        courseRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        // Create student
        student = User.builder()
                .fullName("John Student")
                .email("student@example.com")
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .authProvider(AuthProvider.LOCAL)
                .passwordHash("123456")
                .build();
        student = userRepository.save(student);
        com.ojtsu26.elearning.security.CustomUserDetails userDetails = new com.ojtsu26.elearning.security.CustomUserDetails(student);
        org.springframework.security.core.Authentication authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        studentToken = jwtUtils.generateJwtToken(authentication);

        // Create instructor
        User instructor = User.builder()
                .fullName("Teacher Jack")
                .email("teacher@example.com")
                .role(Role.TEACHER)
                .status(UserStatus.ACTIVE)
                .authProvider(AuthProvider.LOCAL)
                .passwordHash("123456")
                .build();
        instructor = userRepository.save(instructor);

        // Create category
        Category category = Category.builder()
                .name("Development")
                .description("Coding courses")
                .build();
        category = categoryRepository.save(category);

        // Create approved course
        course = Course.builder()
                .title("Java Beginners")
                .description("Intro to Java")
                .price(BigDecimal.valueOf(49.99))
                .status(CourseStatus.APPROVED)
                .category(category)
                .instructor(instructor)
                .build();
        course = courseRepository.save(course);

        // Create enrollment
        CourseEnrollment enrollment = CourseEnrollment.builder()
                .student(student)
                .course(course)
                .isCompleted(false)
                .enrolledAt(LocalDateTime.now())
                .progressPercentage(BigDecimal.ZERO)
                .build();
        courseEnrollmentRepository.save(enrollment);
    }

    @Test
    void testGetStudentCoursesPage() throws Exception {
        mockMvc.perform(get("/student/courses")
                        .cookie(new Cookie("jwt_token", studentToken)))
                .andExpect(status().isOk())
                .andExpect(view().name("student/courses"))
                .andExpect(model().attributeExists("courses"))
                .andExpect(model().attributeExists("categories"));
    }

    @Test
    void testGetStudentMyCoursesPage() throws Exception {
        mockMvc.perform(get("/student/my-courses")
                        .cookie(new Cookie("jwt_token", studentToken)))
                .andExpect(status().isOk())
                .andExpect(view().name("student/my-courses"))
                .andExpect(model().attributeExists("courseCards"));
    }

    @Test
    void dashboardDoesNotRollbackWhenAutoCertificateIsNotYetEligible() throws Exception {
        CourseEnrollment enrollment = courseEnrollmentRepository
                .findByStudentIdAndCourseId(student.getId(), course.getId())
                .orElseThrow();

        Lesson contentLesson = lessonRepository.save(Lesson.builder()
                .course(course)
                .title("Required content")
                .type(LessonType.VIDEO)
                .orderIndex(1)
                .build());
        lessonRepository.save(Lesson.builder()
                .course(course)
                .title("Required assessment")
                .type(LessonType.QUIZ)
                .orderIndex(2)
                .build());
        lessonProgressRepository.save(LessonProgress.builder()
                .enrollment(enrollment)
                .lesson(contentLesson)
                .isCompleted(true)
                .completedAt(LocalDateTime.now())
                .lastAccessedAt(LocalDateTime.now())
                .watchedSeconds(1)
                .lastPositionSeconds(1)
                .maxReachedSeconds(1)
                .build());

        mockMvc.perform(get("/student/dashboard")
                        .cookie(new Cookie("jwt_token", studentToken)))
                .andExpect(status().isOk())
                .andExpect(view().name("student/dashboard"))
                .andExpect(model().attributeExists("courseCards"));

        CourseEnrollment updated = courseEnrollmentRepository.findById(enrollment.getId()).orElseThrow();
        assertTrue(Boolean.TRUE.equals(updated.getIsCompleted()));
        assertEquals(0, new BigDecimal("100.00").compareTo(updated.getProgressPercentage()));
    }

    @Test
    void testGetStudentCoursesPageWithPagination() throws Exception {
        mockMvc.perform(get("/student/courses")
                        .param("page", "0")
                        .param("size", "2")
                        .cookie(new Cookie("jwt_token", studentToken)))
                .andExpect(status().isOk())
                .andExpect(view().name("student/courses"))
                .andExpect(model().attributeExists("courses"))
                .andExpect(model().attribute("currentPage", 0))
                .andExpect(model().attributeExists("totalPages"));
    }

    @Test
    void testBlockedStudentRedirectsToLogin() throws Exception {
        User u = userRepository.findById(student.getId()).orElseThrow();
        u.setStatus(UserStatus.BLOCKED);
        userRepository.save(u);

        mockMvc.perform(get("/student/courses")
                        .cookie(new Cookie("jwt_token", studentToken)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"));

        mockMvc.perform(get("/student/my-courses")
                        .cookie(new Cookie("jwt_token", studentToken)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"));
    }

    @Test
    void testPaymentE2EFlow() throws Exception {
        // 1. Create a second approved course that student is NOT enrolled in
        Course course2 = Course.builder()
                .title("React Advanced")
                .description("React Hooks and State Management")
                .price(new BigDecimal("99.99")) // USD
                .status(CourseStatus.APPROVED)
                .category(category)
                .instructor(instructor)
                .build();
        course2 = courseRepository.save(course2);

        // 2. Perform checkout POST to buy course2 via VNPAY
        org.springframework.test.web.servlet.MvcResult result = mockMvc.perform(post("/student/checkout")
                        .param("courseId", course2.getId().toString())
                        .param("pay", "vnpay")
                        .cookie(new Cookie("jwt_token", studentToken)))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String redirectUrl = result.getResponse().getHeader("Location");
        assertNotNull(redirectUrl);
        assertTrue(redirectUrl.contains("sandbox.vnpayment.vn"));

        // 3. Find created order in database
        List<Order> orders = orderRepository.findByStudentIdAndCourseIdAndStatus(student.getId(), course2.getId(), OrderStatus.PENDING);
        assertFalse(orders.isEmpty());
        Order order = orders.get(0);
        assertEquals("USD", order.getCurrency());
        assertEquals(0, new BigDecimal("99.99").compareTo(order.getTotalAmount()));
        assertEquals(0, new BigDecimal("2499750").compareTo(order.getPaidAmount())); // 99.99 * 25000 = 2499750 VND

        // 4. Simulate VNPAY IPN Webhook GET request
        String tmnCode = "2QXUIB0A";
        String hashSecret = "MSBUNHXVQPHFLNNEUJWTLKJZPEXXWJCT";
        String amount = "249975000"; // VND * 100 for VNPAY
        String orderCode = order.getOrderCode();
        String transId = "test-trans-" + System.currentTimeMillis();
        String responseCode = "00"; // Success

        // parameters ordered alphabetically:
        // vnp_Amount, vnp_Command, vnp_CreateDate, vnp_CurrCode, vnp_IpAddr, vnp_Locale, vnp_OrderInfo, vnp_OrderType, vnp_ResponseCode, vnp_ReturnUrl, vnp_TmnCode, vnp_TransactionNo, vnp_TxnRef, vnp_Version
        // For simulation, we only need to pass the ones we sign
        Map<String, String> ipnParams = new java.util.TreeMap<>();
        ipnParams.put("vnp_TmnCode", tmnCode);
        ipnParams.put("vnp_Amount", amount);
        ipnParams.put("vnp_TxnRef", orderCode);
        ipnParams.put("vnp_ResponseCode", responseCode);
        ipnParams.put("vnp_TransactionNo", transId);
        ipnParams.put("vnp_OrderInfo", "Purchase course: " + course2.getTitle());
        ipnParams.put("vnp_Command", "pay");
        ipnParams.put("vnp_Version", "2.1.0");
        ipnParams.put("vnp_CurrCode", "VND");

        StringBuilder hashData = new StringBuilder();
        java.util.Iterator<String> itr = ipnParams.keySet().iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = ipnParams.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(java.net.URLEncoder.encode(fieldValue, "UTF-8").replace("+", "%20"));
                if (itr.hasNext()) {
                    hashData.append('&');
                }
            }
        }
        String rawSig = hashData.toString();
        String signature = calculateHmacSha512(rawSig, hashSecret);

        // 5. Trigger the GET vnpay-ipn webhook endpoint
        mockMvc.perform(get("/api/payment/vnpay-ipn")
                        .param("vnp_TmnCode", tmnCode)
                        .param("vnp_Amount", amount)
                        .param("vnp_TxnRef", orderCode)
                        .param("vnp_ResponseCode", responseCode)
                        .param("vnp_TransactionNo", transId)
                        .param("vnp_OrderInfo", "Purchase course: " + course2.getTitle())
                        .param("vnp_Command", "pay")
                        .param("vnp_Version", "2.1.0")
                        .param("vnp_CurrCode", "VND")
                        .param("vnp_SecureHash", signature))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("00"));

        // 6. Assert Order and Enrollment are correctly activated in database
        Order updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.PAID, updatedOrder.getStatus());
        assertTrue(courseEnrollmentRepository.existsByStudentIdAndCourseId(student.getId(), course2.getId()));
    }

    private String calculateHmacSha512(String data, String key) {
        try {
            javax.crypto.Mac sha512HMAC = javax.crypto.Mac.getInstance("HmacSHA512");
            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA512");
            sha512HMAC.init(secretKey);
            byte[] rawHmac = sha512HMAC.doFinal(data.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : rawHmac) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
