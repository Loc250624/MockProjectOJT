package com.ojtsu26.elearning.controller;

import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.entity.CourseEnrollment;
import com.ojtsu26.elearning.model.entity.Lesson;
import com.ojtsu26.elearning.model.entity.LessonProgress;
import com.ojtsu26.elearning.model.entity.Order;
import com.ojtsu26.elearning.model.entity.OrderItem;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.AuthProvider;
import com.ojtsu26.elearning.model.enums.CourseStatus;
import com.ojtsu26.elearning.model.enums.OrderStatus;
import com.ojtsu26.elearning.model.enums.PaymentMethod;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.model.enums.UserStatus;
import com.ojtsu26.elearning.repository.CourseEnrollmentRepository;
import com.ojtsu26.elearning.repository.CourseRepository;
import com.ojtsu26.elearning.repository.LessonProgressRepository;
import com.ojtsu26.elearning.repository.LessonRepository;
import com.ojtsu26.elearning.repository.OrderItemRepository;
import com.ojtsu26.elearning.repository.OrderRepository;
import com.ojtsu26.elearning.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin_analytics_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class AdminAnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseEnrollmentRepository enrollmentRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private LessonProgressRepository lessonProgressRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    private LocalDate today;

    @BeforeEach
    void setUp() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        lessonProgressRepository.deleteAll();
        lessonRepository.deleteAll();
        enrollmentRepository.deleteAll();
        courseRepository.deleteAll();
        userRepository.deleteAll();

        today = LocalDate.now();
        User admin = userRepository.save(testUser("Admin", "admin.analytics@example.com", Role.ADMIN));
        User teacher = userRepository.save(testUser("Teacher", "teacher.admin.analytics@example.com", Role.TEACHER));
        User studentOne = userRepository.save(testUser("Student One", "student.one.analytics@example.com", Role.STUDENT));
        User studentTwo = userRepository.save(testUser("Student Two", "student.two.analytics@example.com", Role.STUDENT));

        Course course = courseRepository.save(Course.builder()
                .title("Admin Analytics Course")
                .description("Course")
                .price(new BigDecimal("100.00"))
                .status(CourseStatus.APPROVED)
                .instructor(teacher)
                .build());
        Lesson firstLesson = lessonRepository.save(Lesson.builder()
                .title("Intro")
                .content("Intro")
                .orderIndex(1)
                .course(course)
                .build());
        Lesson secondLesson = lessonRepository.save(Lesson.builder()
                .title("Deep Dive")
                .content("Deep Dive")
                .orderIndex(2)
                .course(course)
                .build());
        CourseEnrollment firstEnrollment = enrollmentRepository.save(CourseEnrollment.builder()
                .student(studentOne)
                .course(course)
                .progressPercentage(BigDecimal.ZERO)
                .isCompleted(false)
                .build());
        enrollmentRepository.save(CourseEnrollment.builder()
                .student(studentTwo)
                .course(course)
                .progressPercentage(BigDecimal.ZERO)
                .isCompleted(false)
                .build());
        lessonProgressRepository.save(LessonProgress.builder()
                .enrollment(firstEnrollment)
                .lesson(firstLesson)
                .isCompleted(false)
                .lastAccessedAt(today.atTime(11, 0))
                .watchedSeconds(30)
                .build());
        lessonProgressRepository.save(LessonProgress.builder()
                .enrollment(firstEnrollment)
                .lesson(secondLesson)
                .isCompleted(true)
                .lastAccessedAt(today.atTime(12, 0))
                .watchedSeconds(60)
                .build());

        saveOrderWithItem(studentOne, course, OrderStatus.PAID, "100.00", "paid-1");
        saveOrderWithItem(studentTwo, course, OrderStatus.PAID, "50.00", "paid-2");
        saveOrderWithItem(studentOne, course, OrderStatus.PENDING, "999.00", "pending");
        saveOrderWithItem(studentOne, course, OrderStatus.FAILED, "222.00", "failed");
        saveOrderWithItem(studentOne, course, OrderStatus.CANCELLED, "333.00", "cancelled");
        saveOrderWithItem(studentOne, course, OrderStatus.REFUNDED, "444.00", "refunded");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void dashboardOverviewReturnsAdminTotals() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/overview")
                        .param("from", today.minusDays(1).toString())
                        .param("to", today.plusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalUsers").value(4))
                .andExpect(jsonPath("$.data.totalStudents").value(2))
                .andExpect(jsonPath("$.data.totalTeachers").value(1))
                .andExpect(jsonPath("$.data.totalCourses").value(1))
                .andExpect(jsonPath("$.data.totalEnrollments").value(2))
                .andExpect(jsonPath("$.data.paidOrderCount").value(2))
                .andExpect(jsonPath("$.data.totalRevenue").value(150.00))
                .andExpect(jsonPath("$.data.newStudents").value(2))
                .andExpect(jsonPath("$.data.activeStudents").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void revenueReportExcludesInvalidOrderStatuses() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/revenue")
                        .param("from", today.minusDays(1).toString())
                        .param("to", today.plusDays(1).toString())
                        .param("groupBy", "day"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRevenue").value(150.00))
                .andExpect(jsonPath("$.data.paidOrderCount").value(2))
                .andExpect(jsonPath("$.data.trend[1].period").value(today.toString()))
                .andExpect(jsonPath("$.data.trend[1].revenue").value(150.00))
                .andExpect(jsonPath("$.data.trend[1].paidOrderCount").value(2));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void studentAnalyticsGroupsNewAndActiveStudents() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/students")
                        .param("from", today.minusDays(1).toString())
                        .param("to", today.plusDays(1).toString())
                        .param("groupBy", "day"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.newStudents").value(2))
                .andExpect(jsonPath("$.data.activeStudents").value(1))
                .andExpect(jsonPath("$.data.trend[1].period").value(today.toString()))
                .andExpect(jsonPath("$.data.trend[1].newStudents").value(2))
                .andExpect(jsonPath("$.data.trend[1].activeStudents").value(1));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherRoleIsForbiddenFromAdminAnalytics() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/revenue"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentRoleIsForbiddenFromAdminAnalytics() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/students"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousUserIsUnauthorizedFromAdminAnalytics() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/overview"))
                .andExpect(status().isUnauthorized());
    }

    private User testUser(String fullName, String email, Role role) {
        return User.builder()
                .fullName(fullName)
                .email(email)
                .passwordHash("secret")
                .avatarUrl(null)
                .role(role)
                .status(UserStatus.ACTIVE)
                .authProvider(AuthProvider.LOCAL)
                .build();
    }

    private void saveOrderWithItem(User buyer, Course course, OrderStatus status, String amount, String suffix) {
        Order order = orderRepository.save(Order.builder()
                .orderCode("ADM-ANL-" + suffix)
                .user(buyer)
                .totalAmount(new BigDecimal(amount))
                .currency("USD")
                .paidAmount(new BigDecimal(amount))
                .status(status)
                .paymentMethod(PaymentMethod.MOMO)
                .build());
        orderItemRepository.save(OrderItem.builder()
                .order(order)
                .course(course)
                .courseName(course.getTitle())
                .unitPrice(new BigDecimal(amount))
                .build());
    }
}
