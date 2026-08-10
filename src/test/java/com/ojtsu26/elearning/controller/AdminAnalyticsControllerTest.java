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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
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
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "payment.gateway.exchange-rate=25000"
})
class AdminAnalyticsControllerTest {

    private static final BigDecimal TEST_EXCHANGE_RATE = new BigDecimal("25000");

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    void adminDashboardPageRendersAdminLayoutAndNavigationLinks() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"nav-admin-dashboard\"")))
                .andExpect(content().string(containsString("href=\"/admin/users\"")))
                .andExpect(content().string(containsString("href=\"/admin/courses\"")))
                .andExpect(content().string(containsString("href=\"/admin/transactions\"")))
                .andExpect(content().string(containsString("href=\"/admin/analytics\"")))
                .andExpect(content().string(containsString("href=\"/admin/analytics/revenue\"")))
                .andExpect(content().string(containsString("data-overview=\"totalUsers\"")))
                .andExpect(content().string(containsString("data-overview-money=\"totalRevenue\"")));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherRoleIsForbiddenFromAdminDashboardPage() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentRoleIsForbiddenFromAdminDashboardPage() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
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
                .andExpect(jsonPath("$.data.currency").value("VND"))
                .andExpect(jsonPath("$.data.totalRevenue").value(vnd("150.00")))
                .andExpect(jsonPath("$.data.newStudents").value(2))
                .andExpect(jsonPath("$.data.activeStudents").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void dashboardOverviewReturnsZeroesForEmptyDatabase() throws Exception {
        clearDatabase();

        mockMvc.perform(get("/api/admin/dashboard/overview")
                        .param("from", today.minusDays(1).toString())
                        .param("to", today.plusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalUsers").value(0))
                .andExpect(jsonPath("$.data.totalStudents").value(0))
                .andExpect(jsonPath("$.data.totalTeachers").value(0))
                .andExpect(jsonPath("$.data.totalCourses").value(0))
                .andExpect(jsonPath("$.data.totalEnrollments").value(0))
                .andExpect(jsonPath("$.data.paidOrderCount").value(0))
                .andExpect(jsonPath("$.data.currency").value("VND"))
                .andExpect(jsonPath("$.data.totalRevenue").value(0.00))
                .andExpect(jsonPath("$.data.newStudents").value(0))
                .andExpect(jsonPath("$.data.activeStudents").value(0));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void dashboardOverviewKpisMatchRepositories() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/overview")
                        .param("from", today.minusDays(1).toString())
                        .param("to", today.plusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalUsers").value((int) userRepository.count()))
                .andExpect(jsonPath("$.data.totalStudents").value((int) userRepository.countByRole(Role.STUDENT)))
                .andExpect(jsonPath("$.data.totalTeachers").value((int) userRepository.countByRole(Role.TEACHER)))
                .andExpect(jsonPath("$.data.totalCourses").value((int) courseRepository.count()))
                .andExpect(jsonPath("$.data.totalEnrollments").value((int) enrollmentRepository.count()))
                .andExpect(jsonPath("$.data.paidOrderCount").value((int) orderRepository.countByStatus(OrderStatus.PAID)))
                .andExpect(jsonPath("$.data.currency").value("VND"))
                .andExpect(jsonPath("$.data.totalRevenue").value(vnd(orderItemRepository.sumPaidRevenue(OrderStatus.PAID))))
                .andExpect(jsonPath("$.data.activeStudents").value((int) lessonProgressRepository.countActiveStudentsForAnalytics(
                        today.minusDays(1).atStartOfDay(),
                        today.plusDays(2).atStartOfDay(),
                        Role.STUDENT)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void revenueReportExcludesInvalidOrderStatuses() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/revenue")
                        .param("from", today.minusDays(1).toString())
                        .param("to", today.plusDays(1).toString())
                        .param("groupBy", "day"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currency").value("VND"))
                .andExpect(jsonPath("$.data.totalRevenue").value(vnd("150.00")))
                .andExpect(jsonPath("$.data.paidOrderCount").value(2))
                .andExpect(jsonPath("$.data.trend[1].period").value(today.toString()))
                .andExpect(jsonPath("$.data.trend[1].revenue").value(vnd("150.00")))
                .andExpect(jsonPath("$.data.trend[1].paidOrderCount").value(2));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void revenueAnalyticsGroupsByDayAndKeepsKpiChartAndTableTotalsConsistent() throws Exception {
        clearDatabase();
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 3);
        User teacher = saveUserAt("Revenue Teacher", "revenue.teacher.day@example.com", Role.TEACHER, from.minusDays(10).atStartOfDay());
        User student = saveUserAt("Revenue Student", "revenue.student.day@example.com", Role.STUDENT, from.minusDays(10).atStartOfDay());
        Course course = saveCourse(teacher, "Daily Revenue Course");

        saveOrderWithItemsAt(student, course, OrderStatus.PAID, LocalDateTime.of(2026, 1, 1, 10, 0), "day-paid-1", "10.00");
        saveOrderWithItemsAt(student, course, OrderStatus.PAID, LocalDateTime.of(2026, 1, 2, 11, 0), "day-paid-2", "20.00", "30.00");
        saveOrderWithItemsAt(student, course, OrderStatus.PENDING, LocalDateTime.of(2026, 1, 2, 12, 0), "day-pending", "999.00");
        saveOrderWithItemsAt(student, course, OrderStatus.FAILED, LocalDateTime.of(2026, 1, 2, 13, 0), "day-failed", "888.00");
        saveOrderWithItemsAt(student, course, OrderStatus.REFUNDED, LocalDateTime.of(2026, 1, 2, 14, 0), "day-refunded", "777.00");

        mockMvc.perform(get("/api/admin/analytics/revenue")
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .param("groupBy", "day"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groupBy").value("day"))
                .andExpect(jsonPath("$.data.totalRevenue").value(vnd("60.00")))
                .andExpect(jsonPath("$.data.paidOrderCount").value(2))
                .andExpect(jsonPath("$.data.timeZone").isNotEmpty())
                .andExpect(jsonPath("$.data.revenueRecognitionMethod").isNotEmpty())
                .andExpect(jsonPath("$.data.trend[0].period").value("2026-01-01"))
                .andExpect(jsonPath("$.data.trend[0].revenue").value(vnd("10.00")))
                .andExpect(jsonPath("$.data.trend[0].paidOrderCount").value(1))
                .andExpect(jsonPath("$.data.trend[1].period").value("2026-01-02"))
                .andExpect(jsonPath("$.data.trend[1].revenue").value(vnd("50.00")))
                .andExpect(jsonPath("$.data.trend[1].paidOrderCount").value(1))
                .andExpect(jsonPath("$.data.trend[2].period").value("2026-01-03"))
                .andExpect(jsonPath("$.data.trend[2].revenue").value(0.00))
                .andExpect(jsonPath("$.data.trend[2].paidOrderCount").value(0));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void revenueAnalyticsGroupsByMonthWithYearAndHonorsEndOfMonth() throws Exception {
        clearDatabase();
        LocalDate from = LocalDate.of(2025, 12, 1);
        LocalDate to = LocalDate.of(2026, 1, 31);
        User teacher = saveUserAt("Revenue Teacher", "revenue.teacher.month@example.com", Role.TEACHER, from.minusDays(10).atStartOfDay());
        User student = saveUserAt("Revenue Student", "revenue.student.month@example.com", Role.STUDENT, from.minusDays(10).atStartOfDay());
        Course course = saveCourse(teacher, "Monthly Revenue Course");

        saveOrderWithItemsAt(student, course, OrderStatus.PAID, LocalDateTime.of(2025, 12, 31, 23, 59, 59), "month-dec", "40.00");
        saveOrderWithItemsAt(student, course, OrderStatus.PAID, LocalDateTime.of(2026, 1, 31, 23, 59, 59), "month-jan", "60.00");
        saveOrderWithItemsAt(student, course, OrderStatus.PAID, LocalDateTime.of(2026, 2, 1, 0, 0), "month-feb-excluded", "500.00");

        mockMvc.perform(get("/api/admin/analytics/revenue")
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .param("groupBy", "month"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groupBy").value("month"))
                .andExpect(jsonPath("$.data.currency").value("VND"))
                .andExpect(jsonPath("$.data.totalRevenue").value(vnd("100.00")))
                .andExpect(jsonPath("$.data.paidOrderCount").value(2))
                .andExpect(jsonPath("$.data.trend[0].period").value("2025-12"))
                .andExpect(jsonPath("$.data.trend[0].revenue").value(vnd("40.00")))
                .andExpect(jsonPath("$.data.trend[0].paidOrderCount").value(1))
                .andExpect(jsonPath("$.data.trend[1].period").value("2026-01"))
                .andExpect(jsonPath("$.data.trend[1].revenue").value(vnd("60.00")))
                .andExpect(jsonPath("$.data.trend[1].paidOrderCount").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void revenueAnalyticsGroupsByYearAndHonorsEndOfYear() throws Exception {
        clearDatabase();
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2026, 12, 31);
        User teacher = saveUserAt("Revenue Teacher", "revenue.teacher.year@example.com", Role.TEACHER, from.minusDays(10).atStartOfDay());
        User student = saveUserAt("Revenue Student", "revenue.student.year@example.com", Role.STUDENT, from.minusDays(10).atStartOfDay());
        Course course = saveCourse(teacher, "Yearly Revenue Course");

        saveOrderWithItemsAt(student, course, OrderStatus.PAID, LocalDateTime.of(2025, 12, 31, 23, 59, 59), "year-2025", "10.00");
        saveOrderWithItemsAt(student, course, OrderStatus.PAID, LocalDateTime.of(2026, 1, 1, 0, 0), "year-2026-a", "20.00");
        saveOrderWithItemsAt(student, course, OrderStatus.PAID, LocalDateTime.of(2026, 12, 31, 23, 59, 59), "year-2026-b", "30.00");
        saveOrderWithItemsAt(student, course, OrderStatus.PAID, LocalDateTime.of(2027, 1, 1, 0, 0), "year-2027-excluded", "999.00");

        mockMvc.perform(get("/api/admin/analytics/revenue")
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .param("groupBy", "year"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groupBy").value("year"))
                .andExpect(jsonPath("$.data.totalRevenue").value(vnd("60.00")))
                .andExpect(jsonPath("$.data.paidOrderCount").value(3))
                .andExpect(jsonPath("$.data.trend[0].period").value("2025"))
                .andExpect(jsonPath("$.data.trend[0].revenue").value(vnd("10.00")))
                .andExpect(jsonPath("$.data.trend[0].paidOrderCount").value(1))
                .andExpect(jsonPath("$.data.trend[1].period").value("2026"))
                .andExpect(jsonPath("$.data.trend[1].revenue").value(vnd("50.00")))
                .andExpect(jsonPath("$.data.trend[1].paidOrderCount").value(2));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void revenueAnalyticsIncludesEndOfDayAndExcludesNextDayBoundary() throws Exception {
        clearDatabase();
        LocalDate day = LocalDate.of(2026, 6, 1);
        User teacher = saveUserAt("Revenue Teacher", "revenue.teacher.boundary@example.com", Role.TEACHER, day.minusDays(10).atStartOfDay());
        User student = saveUserAt("Revenue Student", "revenue.student.boundary@example.com", Role.STUDENT, day.minusDays(10).atStartOfDay());
        Course course = saveCourse(teacher, "Boundary Revenue Course");

        saveOrderWithItemsAt(student, course, OrderStatus.PAID, LocalDateTime.of(2026, 6, 1, 23, 59, 59), "day-boundary-in", "25.00");
        saveOrderWithItemsAt(student, course, OrderStatus.PAID, LocalDateTime.of(2026, 6, 2, 0, 0), "day-boundary-out", "75.00");

        mockMvc.perform(get("/api/admin/analytics/revenue")
                        .param("from", day.toString())
                        .param("to", day.toString())
                        .param("groupBy", "day"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRevenue").value(vnd("25.00")))
                .andExpect(jsonPath("$.data.paidOrderCount").value(1))
                .andExpect(jsonPath("$.data.trend[0].period").value(day.toString()))
                .andExpect(jsonPath("$.data.trend[0].revenue").value(vnd("25.00")))
                .andExpect(jsonPath("$.data.trend[0].paidOrderCount").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void revenueAnalyticsReturnsZeroesForEmptyDateRange() throws Exception {
        clearDatabase();
        LocalDate day = LocalDate.of(2026, 7, 1);
        User teacher = saveUserAt("Revenue Teacher", "revenue.teacher.empty@example.com", Role.TEACHER, day.minusDays(10).atStartOfDay());
        User student = saveUserAt("Revenue Student", "revenue.student.empty@example.com", Role.STUDENT, day.minusDays(10).atStartOfDay());
        Course course = saveCourse(teacher, "Empty Revenue Course");
        saveOrderWithItemsAt(student, course, OrderStatus.PAID, day.minusDays(1).atTime(12, 0), "empty-outside", "100.00");

        mockMvc.perform(get("/api/admin/analytics/revenue")
                        .param("from", day.toString())
                        .param("to", day.toString())
                        .param("groupBy", "day"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRevenue").value(0.00))
                .andExpect(jsonPath("$.data.paidOrderCount").value(0))
                .andExpect(jsonPath("$.data.trend[0].period").value(day.toString()))
                .andExpect(jsonPath("$.data.trend[0].revenue").value(0.00))
                .andExpect(jsonPath("$.data.trend[0].paidOrderCount").value(0));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void revenueAnalyticsRejectsFromDateAfterToDateWithoutServerError() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/revenue")
                        .param("from", "2026-08-10")
                        .param("to", "2026-08-01")
                        .param("groupBy", "day"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Start date must be before or equal to end date."));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void revenueAnalyticsRejectsInvalidGranularityWithoutServerError() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/revenue")
                        .param("from", "2026-08-01")
                        .param("to", "2026-08-10")
                        .param("groupBy", "week"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("groupBy must be day, month, or year."));
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
                .andExpect(jsonPath("$.data.timeZone").isNotEmpty())
                .andExpect(jsonPath("$.data.trend[1].period").value(today.toString()))
                .andExpect(jsonPath("$.data.trend[1].newStudents").value(2))
                .andExpect(jsonPath("$.data.trend[1].activeStudents").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void studentAnalyticsCountsOnlyStudentRoleCreatedInsideRange() throws Exception {
        clearDatabase();
        LocalDate from = LocalDate.of(2026, 2, 10);
        LocalDate to = LocalDate.of(2026, 2, 12);

        saveUserAt("Teacher In Range", "teacher.in.range@example.com", Role.TEACHER, from.atTime(9, 0));
        saveUserAt("Admin In Range", "admin.in.range@example.com", Role.ADMIN, from.atTime(10, 0));
        saveUserAt("Student Before", "student.before@example.com", Role.STUDENT, from.minusDays(1).atTime(23, 59));
        saveUserAt("Student Start", "student.start@example.com", Role.STUDENT, from.atStartOfDay());
        saveUserAt("Student Middle", "student.middle@example.com", Role.STUDENT, from.plusDays(1).atTime(12, 0));
        saveUserAt("Student After", "student.after@example.com", Role.STUDENT, to.plusDays(1).atStartOfDay());

        mockMvc.perform(get("/api/admin/analytics/students")
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .param("groupBy", "day"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.newStudents").value(2))
                .andExpect(jsonPath("$.data.activeStudents").value(0))
                .andExpect(jsonPath("$.data.trend[0].period").value("2026-02-10"))
                .andExpect(jsonPath("$.data.trend[0].newStudents").value(1))
                .andExpect(jsonPath("$.data.trend[1].period").value("2026-02-11"))
                .andExpect(jsonPath("$.data.trend[1].newStudents").value(1))
                .andExpect(jsonPath("$.data.trend[2].period").value("2026-02-12"))
                .andExpect(jsonPath("$.data.trend[2].newStudents").value(0));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void studentAnalyticsCountsDistinctActiveStudentsAndExcludesNonStudentActivity() throws Exception {
        clearDatabase();
        LocalDate day = LocalDate.of(2026, 3, 5);
        User teacher = saveUserAt("Teacher Active", "teacher.active@example.com", Role.TEACHER, day.minusDays(10).atStartOfDay());
        User student = saveUserAt("Student Active", "student.active@example.com", Role.STUDENT, day.minusDays(10).atStartOfDay());
        Course course = saveCourse(teacher, "Active Source Course");
        Lesson firstLesson = saveLesson(course, "Lesson One", 1);
        Lesson secondLesson = saveLesson(course, "Lesson Two", 2);
        CourseEnrollment studentEnrollment = saveEnrollment(student, course);
        CourseEnrollment teacherEnrollment = saveEnrollment(teacher, course);

        saveProgress(studentEnrollment, firstLesson, day.atTime(9, 0));
        saveProgress(studentEnrollment, secondLesson, day.atTime(11, 0));
        saveProgress(teacherEnrollment, firstLesson, day.atTime(10, 0));

        mockMvc.perform(get("/api/admin/analytics/students")
                        .param("from", day.toString())
                        .param("to", day.toString())
                        .param("groupBy", "day"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.newStudents").value(0))
                .andExpect(jsonPath("$.data.activeStudents").value(1))
                .andExpect(jsonPath("$.data.trend[0].activeStudents").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void studentAnalyticsReturnsZeroesForEmptyDateRange() throws Exception {
        clearDatabase();
        LocalDate day = LocalDate.of(2026, 4, 10);
        saveUserAt("Student Outside", "student.outside@example.com", Role.STUDENT, day.minusDays(1).atTime(12, 0));

        mockMvc.perform(get("/api/admin/analytics/students")
                        .param("from", day.toString())
                        .param("to", day.toString())
                        .param("groupBy", "day"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.newStudents").value(0))
                .andExpect(jsonPath("$.data.activeStudents").value(0))
                .andExpect(jsonPath("$.data.trend[0].period").value(day.toString()))
                .andExpect(jsonPath("$.data.trend[0].newStudents").value(0))
                .andExpect(jsonPath("$.data.trend[0].activeStudents").value(0));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void studentAnalyticsRejectsFromDateAfterToDateWithoutServerError() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/students")
                        .param("from", "2026-05-10")
                        .param("to", "2026-05-01")
                        .param("groupBy", "day"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Start date must be before or equal to end date."));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void studentAnalyticsUsesInclusiveStartAndExclusiveNextDayBoundary() throws Exception {
        clearDatabase();
        LocalDate day = LocalDate.of(2026, 6, 1);
        User teacher = saveUserAt("Boundary Teacher", "boundary.teacher@example.com", Role.TEACHER, day.minusDays(5).atStartOfDay());
        User includedStudent = saveUserAt("Boundary Included", "boundary.included@example.com", Role.STUDENT, day.atStartOfDay());
        User excludedStudent = saveUserAt("Boundary Excluded", "boundary.excluded@example.com", Role.STUDENT, day.plusDays(1).atStartOfDay());
        Course course = saveCourse(teacher, "Boundary Course");
        Lesson lesson = saveLesson(course, "Boundary Lesson", 1);
        saveProgress(saveEnrollment(includedStudent, course), lesson, day.atStartOfDay());
        saveProgress(saveEnrollment(excludedStudent, course), lesson, day.plusDays(1).atStartOfDay());

        mockMvc.perform(get("/api/admin/analytics/students")
                        .param("from", day.toString())
                        .param("to", day.toString())
                        .param("groupBy", "day"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.newStudents").value(1))
                .andExpect(jsonPath("$.data.activeStudents").value(1))
                .andExpect(jsonPath("$.data.trend[0].newStudents").value(1))
                .andExpect(jsonPath("$.data.trend[0].activeStudents").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void studentAnalyticsMonthGroupingKeepsYearInPeriod() throws Exception {
        clearDatabase();
        LocalDate from = LocalDate.of(2025, 12, 1);
        LocalDate to = LocalDate.of(2026, 1, 31);
        saveUserAt("December Student", "december.student@example.com", Role.STUDENT, LocalDateTime.of(2025, 12, 15, 8, 0));
        saveUserAt("January Student", "january.student@example.com", Role.STUDENT, LocalDateTime.of(2026, 1, 2, 8, 0));

        mockMvc.perform(get("/api/admin/analytics/students")
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .param("groupBy", "month"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trend[0].period").value("2025-12"))
                .andExpect(jsonPath("$.data.trend[0].newStudents").value(1))
                .andExpect(jsonPath("$.data.trend[1].period").value("2026-01"))
                .andExpect(jsonPath("$.data.trend[1].newStudents").value(1));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherRoleIsForbiddenFromAdminAnalytics() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/students"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherRoleIsForbiddenFromAdminRevenueAnalytics() throws Exception {
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

    @Test
    void anonymousUserIsRedirectedFromAdminPages() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"));

        mockMvc.perform(get("/admin/analytics"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"));

        mockMvc.perform(get("/admin/analytics/revenue"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentRoleIsForbiddenFromAllAdminAnalyticsApis() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/overview"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/analytics/students"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/analytics/revenue"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherRoleIsForbiddenFromAllAdminAnalyticsPages() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
        mockMvc.perform(get("/admin/analytics"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
        mockMvc.perform(get("/admin/analytics/revenue"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminAnalyticsRejectsMalformedDateAndSqlLikeFilterWithoutServerError() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/students")
                        .param("from", "2026-99-99")
                        .param("to", "2026-01-01")
                        .param("groupBy", "day"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(get("/api/admin/analytics/revenue")
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-02")
                        .param("groupBy", "day;DROP TABLE users"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("groupBy must be day, month, or year."));
    }

    private void clearDatabase() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        lessonProgressRepository.deleteAll();
        lessonRepository.deleteAll();
        enrollmentRepository.deleteAll();
        courseRepository.deleteAll();
        userRepository.deleteAll();
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

    private User saveUserAt(String fullName, String email, Role role, LocalDateTime createdAt) {
        User user = userRepository.save(testUser(fullName, email, role));
        userRepository.flush();
        jdbcTemplate.update("update users set created_at = ? where id = ?", Timestamp.valueOf(createdAt), user.getId());
        user.setCreatedAt(createdAt);
        return user;
    }

    private Course saveCourse(User teacher, String title) {
        return courseRepository.saveAndFlush(Course.builder()
                .title(title)
                .description(title)
                .price(BigDecimal.ZERO)
                .status(CourseStatus.APPROVED)
                .instructor(teacher)
                .build());
    }

    private Lesson saveLesson(Course course, String title, int orderIndex) {
        return lessonRepository.saveAndFlush(Lesson.builder()
                .title(title)
                .content(title)
                .orderIndex(orderIndex)
                .course(course)
                .build());
    }

    private CourseEnrollment saveEnrollment(User user, Course course) {
        return enrollmentRepository.saveAndFlush(CourseEnrollment.builder()
                .student(user)
                .course(course)
                .progressPercentage(BigDecimal.ZERO)
                .isCompleted(false)
                .build());
    }

    private LessonProgress saveProgress(CourseEnrollment enrollment, Lesson lesson, LocalDateTime accessedAt) {
        return lessonProgressRepository.saveAndFlush(LessonProgress.builder()
                .enrollment(enrollment)
                .lesson(lesson)
                .isCompleted(false)
                .lastAccessedAt(accessedAt)
                .watchedSeconds(30)
                .build());
    }

    private Order saveOrderWithItemsAt(User buyer,
                                       Course course,
                                       OrderStatus status,
                                       LocalDateTime createdAt,
                                       String suffix,
                                       String... amounts) {
        BigDecimal total = BigDecimal.ZERO;
        for (String amount : amounts) {
            total = total.add(new BigDecimal(amount));
        }
        Order order = orderRepository.save(Order.builder()
                .orderCode("ADM-ANL-" + suffix)
                .user(buyer)
                .totalAmount(total)
                .currency("USD")
                .paidAmount(total)
                .status(status)
                .paymentMethod(PaymentMethod.MOMO)
                .build());
        orderRepository.flush();
        jdbcTemplate.update("update orders set created_at = ? where id = ?", Timestamp.valueOf(createdAt), order.getId());
        order.setCreatedAt(createdAt);
        for (String amount : amounts) {
            orderItemRepository.save(OrderItem.builder()
                    .order(order)
                    .course(course)
                    .courseName(course.getTitle())
                    .unitPrice(new BigDecimal(amount))
                    .build());
        }
        orderItemRepository.flush();
        return order;
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

    private static int vnd(String usdAmount) {
        return vnd(new BigDecimal(usdAmount));
    }

    private static int vnd(BigDecimal usdAmount) {
        return usdAmount.multiply(TEST_EXCHANGE_RATE).intValueExact();
    }
}
