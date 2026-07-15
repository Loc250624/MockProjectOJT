package com.ojtsu26.elearning.controller;

import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.entity.CourseEnrollment;
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
import com.ojtsu26.elearning.repository.OrderItemRepository;
import com.ojtsu26.elearning.repository.OrderRepository;
import com.ojtsu26.elearning.repository.UserRepository;
import com.ojtsu26.elearning.security.CustomUserDetails;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:teacher_analytics_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class TeacherAnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseEnrollmentRepository enrollmentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    private User teacher;
    private User otherTeacher;
    private User student;
    private User secondStudent;
    private Course teacherCourse;
    private Course teacherSecondCourse;
    private Course otherTeacherCourse;
    private int orderSequence;

    @BeforeEach
    void setUp() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        enrollmentRepository.deleteAll();
        courseRepository.deleteAll();
        userRepository.deleteAll();

        teacher = userRepository.save(testUser("Teacher One", "teacher.analytics@example.com", Role.TEACHER));
        otherTeacher = userRepository.save(testUser("Teacher Two", "other.analytics@example.com", Role.TEACHER));
        student = userRepository.save(testUser("Student One", "student.analytics@example.com", Role.STUDENT));
        secondStudent = userRepository.save(testUser("Student Two", "student.two.analytics@example.com", Role.STUDENT));

        teacherCourse = courseRepository.save(course("Teacher Course", teacher, "100.00"));
        teacherSecondCourse = courseRepository.save(course("Teacher Second Course", teacher, "80.00"));
        otherTeacherCourse = courseRepository.save(course("Other Course", otherTeacher, "500.00"));

        enrollmentRepository.save(CourseEnrollment.builder()
                .student(student)
                .course(teacherCourse)
                .progressPercentage(BigDecimal.ZERO)
                .isCompleted(false)
                .build());
        enrollmentRepository.save(CourseEnrollment.builder()
                .student(secondStudent)
                .course(teacherCourse)
                .progressPercentage(BigDecimal.ZERO)
                .isCompleted(false)
                .build());

        saveOrderWithItem(student, teacherCourse, OrderStatus.PAID, "100.00");
        saveOrderWithItem(student, teacherCourse, OrderStatus.PENDING, "999.00");
        saveOrderWithItem(student, teacherCourse, OrderStatus.FAILED, "333.00");
        saveOrderWithItem(student, teacherCourse, OrderStatus.CANCELLED, "222.00");
        saveOrderWithItem(student, teacherCourse, OrderStatus.REFUNDED, "111.00");
        saveOrderWithItem(student, otherTeacherCourse, OrderStatus.PAID, "500.00");
    }

    @Test
    void paymentSuccessOrderIsCountedForCurrentTeacherOnly() throws Exception {
        mockMvc.perform(get("/api/teacher/analytics/revenue")
                        .param("from", LocalDate.now().minusDays(1).toString())
                        .param("to", LocalDate.now().plusDays(1).toString())
                        .param("groupBy", "day")
                        .with(user(new CustomUserDetails(teacher))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currency").value("USD"))
                .andExpect(jsonPath("$.data.totalRevenue").value(100.00))
                .andExpect(jsonPath("$.data.paidOrderCount").value(1))
                .andExpect(jsonPath("$.data.paidStudentCount").value(1))
                .andExpect(jsonPath("$.data.enrollmentCount").value(2))
                .andExpect(jsonPath("$.data.studentCount").value(2))
                .andExpect(jsonPath("$.data.bestSellingCourse.courseTitle").value("Teacher Course"));
    }

    @Test
    void teacherWithoutRevenueGetsZeroKpisAndOwnedCourses() throws Exception {
        User quietTeacher = userRepository.save(testUser("Quiet Teacher", "quiet.teacher@example.com", Role.TEACHER));
        Course quietCourse = courseRepository.save(course("Quiet Course", quietTeacher, "70.00"));

        mockMvc.perform(get("/api/teacher/analytics/revenue")
                        .param("from", LocalDate.now().minusDays(1).toString())
                        .param("to", LocalDate.now().plusDays(1).toString())
                        .with(user(new CustomUserDetails(quietTeacher))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRevenue").value(0))
                .andExpect(jsonPath("$.data.paidOrderCount").value(0))
                .andExpect(jsonPath("$.data.paidStudentCount").value(0))
                .andExpect(jsonPath("$.data.bestSellingCourse").doesNotExist());

        mockMvc.perform(get("/api/teacher/analytics/revenue/by-course")
                        .param("from", LocalDate.now().minusDays(1).toString())
                        .param("to", LocalDate.now().plusDays(1).toString())
                        .with(user(new CustomUserDetails(quietTeacher))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courses[0].courseId").value(quietCourse.getId()))
                .andExpect(jsonPath("$.data.courses[0].revenue").value(0));
    }

    @Test
    void pendingFailedCancelledAndRefundedOrdersAreNotCounted() throws Exception {
        mockMvc.perform(get("/api/teacher/analytics/revenue")
                        .param("from", LocalDate.now().minusDays(1).toString())
                        .param("to", LocalDate.now().plusDays(1).toString())
                        .with(user(new CustomUserDetails(teacher))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currency").value("USD"))
                .andExpect(jsonPath("$.data.totalRevenue").value(100.00))
                .andExpect(jsonPath("$.data.paidOrderCount").value(1))
                .andExpect(jsonPath("$.data.paidStudentCount").value(1));
    }

    @Test
    void twoTeachersHaveSeparateRevenue() throws Exception {
        mockMvc.perform(get("/api/teacher/analytics/revenue")
                        .param("from", LocalDate.now().minusDays(1).toString())
                        .param("to", LocalDate.now().plusDays(1).toString())
                        .with(user(new CustomUserDetails(teacher))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRevenue").value(100.00));

        mockMvc.perform(get("/api/teacher/analytics/revenue")
                        .param("from", LocalDate.now().minusDays(1).toString())
                        .param("to", LocalDate.now().plusDays(1).toString())
                        .with(user(new CustomUserDetails(otherTeacher))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRevenue").value(500.00));
    }

    @Test
    void multiItemOrderCountsItemRevenueWithoutDoubleCountingOrderOrStudent() throws Exception {
        saveOrderWithItems(secondStudent, OrderStatus.PAID, List.of(
                item(teacherCourse, "60.00"),
                item(teacherSecondCourse, "40.00")
        ));

        mockMvc.perform(get("/api/teacher/analytics/revenue")
                        .param("from", LocalDate.now().minusDays(1).toString())
                        .param("to", LocalDate.now().plusDays(1).toString())
                        .with(user(new CustomUserDetails(teacher))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRevenue").value(200.00))
                .andExpect(jsonPath("$.data.paidOrderCount").value(2))
                .andExpect(jsonPath("$.data.paidStudentCount").value(2));
    }

    @Test
    void multiTeacherOrderDoesNotExposeOtherTeachersItemRevenue() throws Exception {
        saveOrderWithItems(secondStudent, OrderStatus.PAID, List.of(
                item(teacherCourse, "75.00"),
                item(otherTeacherCourse, "125.00")
        ));

        mockMvc.perform(get("/api/teacher/analytics/revenue")
                        .param("from", LocalDate.now().minusDays(1).toString())
                        .param("to", LocalDate.now().plusDays(1).toString())
                        .with(user(new CustomUserDetails(teacher))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRevenue").value(175.00))
                .andExpect(jsonPath("$.data.paidOrderCount").value(2));

        mockMvc.perform(get("/api/teacher/analytics/revenue")
                        .param("from", LocalDate.now().minusDays(1).toString())
                        .param("to", LocalDate.now().plusDays(1).toString())
                        .with(user(new CustomUserDetails(otherTeacher))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRevenue").value(625.00))
                .andExpect(jsonPath("$.data.paidOrderCount").value(2));
    }

    @Test
    void dateRangeIsAppliedOnBackend() throws Exception {
        mockMvc.perform(get("/api/teacher/analytics/revenue")
                        .param("from", LocalDate.now().plusDays(2).toString())
                        .param("to", LocalDate.now().plusDays(3).toString())
                        .with(user(new CustomUserDetails(teacher))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRevenue").value(0))
                .andExpect(jsonPath("$.data.paidOrderCount").value(0))
                .andExpect(jsonPath("$.data.paidStudentCount").value(0));
    }

    @Test
    void monthBoundaryDatesAreInclusiveForFromAndTo() throws Exception {
        LocalDateTime juneStart = LocalDateTime.of(2026, 6, 1, 0, 0);
        LocalDateTime juneEnd = LocalDateTime.of(2026, 6, 30, 23, 59, 59);
        LocalDateTime julyStart = LocalDateTime.of(2026, 7, 1, 0, 0);

        saveOrderWithItemsAt(student, OrderStatus.PAID, juneStart, List.of(item(teacherCourse, "10.00")));
        saveOrderWithItemsAt(secondStudent, OrderStatus.PAID, juneEnd, List.of(item(teacherCourse, "20.00")));
        saveOrderWithItemsAt(secondStudent, OrderStatus.PAID, julyStart, List.of(item(teacherCourse, "30.00")));

        mockMvc.perform(get("/api/teacher/analytics/revenue")
                        .param("from", "2026-06-01")
                        .param("to", "2026-06-30")
                        .param("groupBy", "month")
                        .with(user(new CustomUserDetails(teacher))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRevenue").value(30.00))
                .andExpect(jsonPath("$.data.paidOrderCount").value(2))
                .andExpect(jsonPath("$.data.paidStudentCount").value(2))
                .andExpect(jsonPath("$.data.trend[0].period").value("2026-06"))
                .andExpect(jsonPath("$.data.trend[0].revenue").value(30.00))
                .andExpect(jsonPath("$.data.trend[0].paidOrderCount").value(2));
    }

    @Test
    void summaryTrendByCourseAndSourceQueryStayConsistent() throws Exception {
        saveOrderWithItems(secondStudent, OrderStatus.PAID, List.of(
                item(teacherCourse, "60.00"),
                item(teacherSecondCourse, "40.00")
        ));

        BigDecimal sourceRevenue = orderItemRepository.sumTeacherRevenue(
                teacher.getId(),
                OrderStatus.PAID,
                LocalDate.now().minusDays(1).atStartOfDay(),
                LocalDate.now().plusDays(2).atStartOfDay(),
                null
        );

        mockMvc.perform(get("/api/teacher/analytics/revenue")
                        .param("from", LocalDate.now().minusDays(1).toString())
                        .param("to", LocalDate.now().plusDays(1).toString())
                        .param("groupBy", "day")
                        .with(user(new CustomUserDetails(teacher))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRevenue").value(sourceRevenue.doubleValue()))
                .andExpect(jsonPath("$.data.trend[1].revenue").value(sourceRevenue.doubleValue()))
                .andExpect(jsonPath("$.data.paidOrderCount").value(2));

        mockMvc.perform(get("/api/teacher/analytics/revenue/by-course")
                        .param("from", LocalDate.now().minusDays(1).toString())
                        .param("to", LocalDate.now().plusDays(1).toString())
                        .with(user(new CustomUserDetails(teacher))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courses[0].revenue").value(160.00))
                .andExpect(jsonPath("$.data.courses[1].revenue").value(40.00));
    }

    @Test
    void teacherCannotFilterByAnotherTeachersCourse() throws Exception {
        mockMvc.perform(get("/api/teacher/analytics/revenue")
                        .param("from", LocalDate.now().minusDays(1).toString())
                        .param("to", LocalDate.now().plusDays(1).toString())
                        .param("courseId", otherTeacherCourse.getId().toString())
                        .with(user(new CustomUserDetails(teacher))))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousIsUnauthorizedFromTeacherAnalyticsApi() throws Exception {
        mockMvc.perform(get("/api/teacher/analytics/revenue"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousIsRedirectedFromTeacherAnalyticsPage() throws Exception {
        mockMvc.perform(get("/teacher/analytics"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentRoleIsForbiddenFromTeacherAnalyticsApi() throws Exception {
        mockMvc.perform(get("/api/teacher/analytics/revenue"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentRoleIsForbiddenFromTeacherAnalyticsPage() throws Exception {
        mockMvc.perform(get("/teacher/analytics"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void teacherAnalyticsRejectsMalformedDateHtmlAndIdorFiltersWithoutServerError() throws Exception {
        mockMvc.perform(get("/api/teacher/analytics/revenue")
                        .param("from", "<script>alert(1)</script>")
                        .param("to", LocalDate.now().toString())
                        .with(user(new CustomUserDetails(teacher))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(get("/api/teacher/analytics/revenue")
                        .param("from", LocalDate.now().minusDays(1).toString())
                        .param("to", LocalDate.now().plusDays(1).toString())
                        .param("teacherId", otherTeacher.getId().toString())
                        .param("userId", student.getId().toString())
                        .param("orderId", "1")
                        .param("groupBy", "day")
                        .with(user(new CustomUserDetails(teacher))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRevenue").value(100.00));
    }

    private User testUser(String fullName, String email, Role role) {
        return User.builder()
                .fullName(fullName)
                .email(email)
                .passwordHash("secret")
                .role(role)
                .status(UserStatus.ACTIVE)
                .authProvider(AuthProvider.LOCAL)
                .build();
    }

    private Course course(String title, User instructor, String price) {
        return Course.builder()
                .title(title)
                .description(title)
                .thumbnailUrl(null)
                .price(new BigDecimal(price))
                .status(CourseStatus.APPROVED)
                .instructor(instructor)
                .build();
    }

    private void saveOrderWithItem(User buyer, Course course, OrderStatus status, String amount) {
        saveOrderWithItems(buyer, status, List.of(item(course, amount)));
    }

    private void saveOrderWithItems(User buyer, OrderStatus status, List<OrderLine> lines) {
        saveOrderWithItemsAt(buyer, status, null, lines);
    }

    private void saveOrderWithItemsAt(User buyer, OrderStatus status, LocalDateTime createdAt, List<OrderLine> lines) {
        BigDecimal total = lines.stream()
                .map(OrderLine::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Order order = orderRepository.save(Order.builder()
                .orderCode("ORD-" + (++orderSequence) + "-" + status)
                .user(buyer)
                .totalAmount(total)
                .currency("USD")
                .paidAmount(total)
                .status(status)
                .paymentMethod(PaymentMethod.MOMO)
                .items(new ArrayList<>())
                .build());
        if (createdAt != null) {
            order.setCreatedAt(createdAt);
            order = orderRepository.saveAndFlush(order);
        }
        Order savedOrder = order;
        lines.forEach(line -> orderItemRepository.save(OrderItem.builder()
                .order(savedOrder)
                .course(line.course())
                .courseName(line.course().getTitle())
                .unitPrice(line.amount())
                .build()));
    }

    private OrderLine item(Course course, String amount) {
        return new OrderLine(course, new BigDecimal(amount));
    }

    private record OrderLine(Course course, BigDecimal amount) {
    }
}
