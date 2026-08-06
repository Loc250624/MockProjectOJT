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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:student_view_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "payment.gateway.vnpay.tmnCode=2QXUIB0A",
        "payment.gateway.vnpay.hashSecret=MSBUNHXVQPHFLNNEUJWTLKJZPEXXWJCT"
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
    void dashboardShowsAtMostSixCoursesAndMyCoursesPaginatesAllEnrollmentsBySix() throws Exception {
        for (int index = 1; index <= 6; index++) {
            Course additionalCourse = courseRepository.save(Course.builder()
                    .title("Additional course " + index)
                    .description("Pagination test course")
                    .price(BigDecimal.ZERO)
                    .status(CourseStatus.APPROVED)
                    .category(course.getCategory())
                    .instructor(course.getInstructor())
                    .build());
            courseEnrollmentRepository.save(CourseEnrollment.builder()
                    .student(student)
                    .course(additionalCourse)
                    .isCompleted(false)
                    .enrolledAt(LocalDateTime.now().plusMinutes(index))
                    .progressPercentage(BigDecimal.ZERO)
                    .build());
        }

        mockMvc.perform(get("/student/dashboard")
                        .cookie(new Cookie("jwt_token", studentToken)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("courseCards", org.hamcrest.Matchers.hasSize(6)))
                .andExpect(model().attribute("enrolledCourseCount", 7));

        mockMvc.perform(get("/student/my-courses")
                        .param("page", "0")
                        .cookie(new Cookie("jwt_token", studentToken)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("courseCards", org.hamcrest.Matchers.hasSize(6)))
                .andExpect(model().attribute("currentPage", 0))
                .andExpect(model().attribute("totalPages", 2))
                .andExpect(model().attribute("enrolledCourseCount", 7));

        mockMvc.perform(get("/student/my-courses")
                        .param("page", "1")
                        .cookie(new Cookie("jwt_token", studentToken)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("courseCards", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(model().attribute("currentPage", 1))
                .andExpect(model().attribute("totalPages", 2));
    }

    @Test
    void dashboardDoesNotRollbackWhenAutoCertificateIsNotYetEligible() throws Exception {
        CourseEnrollment enrollment = courseEnrollmentRepository
                .findByStudentIdAndCourseId(student.getId(), course.getId())
                .orElseThrow();
        enrollment.setIsCompleted(true);
        enrollment.setProgressPercentage(new BigDecimal("100.00"));
        enrollment = courseEnrollmentRepository.save(enrollment);

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
                        .with(csrf())
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
        ipnParams.put("vnp_TransactionStatus", "00");
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
                hashData.append(java.net.URLEncoder.encode(fieldValue, "UTF-8"));
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
                        .param("vnp_TransactionStatus", "00")
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

    @Test
    void vnpayReturnMarksSuccessfulPaymentAsPaidWhenIpnCannotReachLocalhost() throws Exception {
        Course purchasableCourse = createPurchasableCourse("VNPay return success");
        Order order = checkoutWithVnpay(purchasableCourse);
        Map<String, String> returnParams = signedVnpayParams(order, purchasableCourse, "00");
        removeCurrencyAndResign(returnParams);

        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request =
                get("/student/payment-result")
                        .with(mockRequest -> {
                            mockRequest.setServerPort(8080);
                            return mockRequest;
                        });
        returnParams.forEach(request::param);

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(view().name("student/payment-result"))
                .andExpect(model().attribute("paymentSuccess", true))
                .andExpect(model().attribute("redirectToMyCourses", true));

        Order updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.PAID, updatedOrder.getStatus());
        assertTrue(courseEnrollmentRepository.existsByStudentIdAndCourseId(
                student.getId(), purchasableCourse.getId()));
    }

    @Test
    void vnpayReturnMarksFailedPaymentAsFailedWhenIpnCannotReachLocalhost() throws Exception {
        Course purchasableCourse = createPurchasableCourse("VNPay return failure");
        Order order = checkoutWithVnpay(purchasableCourse);
        Map<String, String> returnParams = signedVnpayParams(order, purchasableCourse, "24");
        removeCurrencyAndResign(returnParams);

        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request =
                get("/student/payment-result")
                        .secure(true)
                        .with(mockRequest -> {
                            mockRequest.setServerName("sandbox-return.trycloudflare.com");
                            mockRequest.setServerPort(443);
                            return mockRequest;
                        });
        returnParams.forEach(request::param);

        mockMvc.perform(request)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("http://localhost:8080/student/payment-result?*"));

        Order updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.FAILED, updatedOrder.getStatus());
        assertFalse(courseEnrollmentRepository.existsByStudentIdAndCourseId(
                student.getId(), purchasableCourse.getId()));
    }

    @Test
    void vnpayReturnDoesNotPayWhenResponseIsSuccessfulButTransactionStatusFailed() throws Exception {
        Course purchasableCourse = createPurchasableCourse("VNPay transaction status failure");
        Order order = checkoutWithVnpay(purchasableCourse);
        Map<String, String> returnParams = signedVnpayParams(
                order, purchasableCourse, "00", "02");

        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request =
                get("/student/payment-result")
                        .with(mockRequest -> {
                            mockRequest.setServerPort(8080);
                            return mockRequest;
                        })
                        .cookie(new Cookie("jwt_token", studentToken));
        returnParams.forEach(request::param);

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(view().name("student/payment-result"))
                .andExpect(model().attribute("paymentSuccess", false))
                .andExpect(model().attribute("paymentStatus", "Failed"));

        Order updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.FAILED, updatedOrder.getStatus());
        assertFalse(courseEnrollmentRepository.existsByStudentIdAndCourseId(
                student.getId(), purchasableCourse.getId()));
    }

    @Test
    void vnpayReturnRejectsAValidSignatureForTheWrongMerchantCode() throws Exception {
        Course purchasableCourse = createPurchasableCourse("VNPay wrong merchant callback");
        Order order = checkoutWithVnpay(purchasableCourse);
        Map<String, String> returnParams = signedVnpayParams(order, purchasableCourse, "00");
        returnParams.remove("vnp_SecureHash");
        returnParams.put("vnp_TmnCode", "WRONGCODE");
        signVnpayParams(returnParams);

        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request =
            get("/student/payment-result")
                .with(mockRequest -> {
                    mockRequest.setServerPort(8080);
                    return mockRequest;
                })
                .cookie(new Cookie("jwt_token", studentToken));
        returnParams.forEach(request::param);

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(model().attribute("paymentSuccess", false));

        Order updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.FAILED, updatedOrder.getStatus());
        assertFalse(courseEnrollmentRepository.existsByStudentIdAndCourseId(
                student.getId(), purchasableCourse.getId()));
    }

    @Test
    void vnpayReturnRejectsAnExplicitNonVndCurrency() throws Exception {
        Course purchasableCourse = createPurchasableCourse("VNPay wrong currency callback");
        Order order = checkoutWithVnpay(purchasableCourse);
        Map<String, String> returnParams = signedVnpayParams(order, purchasableCourse, "00");
        returnParams.remove("vnp_SecureHash");
        returnParams.put("vnp_CurrCode", "USD");
        signVnpayParams(returnParams);

        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request =
            get("/student/payment-result")
                .with(mockRequest -> {
                    mockRequest.setServerPort(8080);
                    return mockRequest;
                })
                .cookie(new Cookie("jwt_token", studentToken));
        returnParams.forEach(request::param);

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(model().attribute("paymentSuccess", false));

        Order updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.FAILED, updatedOrder.getStatus());
        assertFalse(courseEnrollmentRepository.existsByStudentIdAndCourseId(
                student.getId(), purchasableCourse.getId()));
    }

    private Course createPurchasableCourse(String title) {
        return courseRepository.save(Course.builder()
                .title(title)
                .description("Course used to verify the VNPay return callback")
                .price(new BigDecimal("99.99"))
                .status(CourseStatus.APPROVED)
                .category(category)
                .instructor(instructor)
                .build());
    }

    private Order checkoutWithVnpay(Course purchasableCourse) throws Exception {
        org.springframework.test.web.servlet.MvcResult checkoutResult = mockMvc.perform(post("/student/checkout")
                        .param("courseId", purchasableCourse.getId().toString())
                        .param("pay", "vnpay")
                        .with(csrf())
                        .with(mockRequest -> {
                            mockRequest.setServerPort(8080);
                            return mockRequest;
                        })
                        .cookie(new Cookie("jwt_token", studentToken)))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        assertTrue(checkoutResult.getResponse().getRedirectedUrl()
                .contains("vnp_ReturnUrl=http%3A%2F%2Flocalhost%3A8080%2Fstudent%2Fpayment-result"));

        return orderRepository.findByStudentIdAndCourseIdAndStatus(
                        student.getId(), purchasableCourse.getId(), OrderStatus.PENDING)
                .stream()
                .findFirst()
                .orElseThrow();
    }

    private Map<String, String> signedVnpayParams(Order order, Course purchasableCourse,
                                                   String responseCode) throws Exception {
        return signedVnpayParams(order, purchasableCourse, responseCode,
                "00".equals(responseCode) ? "00" : "02");
    }

    private Map<String, String> signedVnpayParams(Order order, Course purchasableCourse,
                                                   String responseCode,
                                                   String transactionStatus) throws Exception {
        Map<String, String> params = new java.util.TreeMap<>();
        params.put("vnp_TmnCode", "2QXUIB0A");
        params.put("vnp_Amount", order.getPaidAmount().multiply(new BigDecimal("100")).toPlainString());
        params.put("vnp_TxnRef", order.getOrderCode());
        params.put("vnp_ResponseCode", responseCode);
        params.put("vnp_TransactionStatus", transactionStatus);
        params.put("vnp_TransactionNo", "return-test-" + System.nanoTime());
        params.put("vnp_OrderInfo", "Purchase course: " + purchasableCourse.getTitle());
        params.put("vnp_Command", "pay");
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_CurrCode", "VND");

        signVnpayParams(params);
        return params;
    }

    private void signVnpayParams(Map<String, String> params) throws Exception {
        StringBuilder hashData = new StringBuilder();
        java.util.Iterator<String> iterator = params.keySet().iterator();
        while (iterator.hasNext()) {
            String fieldName = iterator.next();
            hashData.append(fieldName)
                    .append('=')
                    .append(java.net.URLEncoder.encode(params.get(fieldName), "UTF-8"));
            if (iterator.hasNext()) {
                hashData.append('&');
            }
        }
        params.put("vnp_SecureHash", calculateHmacSha512(hashData.toString(),
                "MSBUNHXVQPHFLNNEUJWTLKJZPEXXWJCT"));
    }

    private void removeCurrencyAndResign(Map<String, String> params) throws Exception {
        params.remove("vnp_CurrCode");
        params.remove("vnp_SecureHash");
        signVnpayParams(params);
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
