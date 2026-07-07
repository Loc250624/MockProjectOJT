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
    private Course teacherCourse;
    private Course otherTeacherCourse;

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
        User secondStudent = userRepository.save(testUser("Student Two", "student.two.analytics@example.com", Role.STUDENT));

        teacherCourse = courseRepository.save(course("Teacher Course", teacher, "100.00"));
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
        saveOrderWithItem(student, otherTeacherCourse, OrderStatus.PAID, "500.00");
    }

    @Test
    void teacherRevenueEndpointReturnsOnlyOwnPaidOrders() throws Exception {
        mockMvc.perform(get("/api/teacher/analytics/revenue")
                        .param("from", LocalDate.now().minusDays(1).toString())
                        .param("to", LocalDate.now().plusDays(1).toString())
                        .param("groupBy", "day")
                        .with(user(new CustomUserDetails(teacher))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRevenue").value(100.00))
                .andExpect(jsonPath("$.data.paidOrderCount").value(1))
                .andExpect(jsonPath("$.data.enrollmentCount").value(2))
                .andExpect(jsonPath("$.data.studentCount").value(2))
                .andExpect(jsonPath("$.data.bestSellingCourse.courseTitle").value("Teacher Course"));
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
    @WithMockUser(roles = "STUDENT")
    void studentRoleIsForbiddenFromTeacherAnalyticsApi() throws Exception {
        mockMvc.perform(get("/api/teacher/analytics/revenue"))
                .andExpect(status().isForbidden());
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
        Order order = orderRepository.save(Order.builder()
                .orderCode("ORD-" + status + "-" + course.getId() + "-" + amount)
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
