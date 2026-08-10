package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.response.TeacherDashboardDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.CourseStatus;
import com.ojtsu26.elearning.model.enums.OrderStatus;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.repository.CourseEnrollmentRepository;
import com.ojtsu26.elearning.repository.CourseRepository;
import com.ojtsu26.elearning.repository.LessonProgressRepository;
import com.ojtsu26.elearning.repository.OrderItemRepository;
import com.ojtsu26.elearning.repository.projection.TeacherCourseMetricProjection;
import com.ojtsu26.elearning.service.impl.TeacherDashboardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherDashboardServiceTest {

    @Mock private CurrentUserService currentUserService;
    @Mock private CourseRepository courseRepository;
    @Mock private CourseEnrollmentRepository enrollmentRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private LessonProgressRepository lessonProgressRepository;
    @Mock private CurrencyDisplayService currencyDisplayService;

    private TeacherDashboardServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TeacherDashboardServiceImpl(
                currentUserService,
                courseRepository,
                enrollmentRepository,
                orderItemRepository,
                lessonProgressRepository,
                currencyDisplayService);
        lenient().when(currencyDisplayService.convertUsdToDisplay(any()))
                .thenAnswer(invocation -> {
                    BigDecimal value = invocation.getArgument(0);
                    return (value == null ? BigDecimal.ZERO : value).setScale(2, java.math.RoundingMode.HALF_UP);
                });
        lenient().when(currencyDisplayService.formatDisplayMoney(any()))
                .thenAnswer(invocation -> "$" + invocation.getArgument(0).toString());
        lenient().when(currencyDisplayService.getDisplayCurrency()).thenReturn("USD");
    }

    @Test
    void buildsDashboardWithoutAssignmentGradingData() {
        User teacher = User.builder().id(7).role(Role.TEACHER).build();
        Course course = Course.builder()
                .id(11)
                .title("Java")
                .status(CourseStatus.DRAFT)
                .updatedAt(LocalDateTime.now())
                .build();
        when(currentUserService.getCurrentUser()).thenReturn(teacher);
        when(courseRepository.findByInstructorId(7)).thenReturn(List.of(course));
        when(enrollmentRepository.findDashboardCourseMetricsByTeacherId(7))
                .thenReturn(List.of(metric(11, 4L, "37.6")));
        when(orderItemRepository.findTeacherRevenueByCourse(
                eq(7), eq(OrderStatus.PAID), any(), any(), isNull()))
                .thenReturn(List.of());
        when(orderItemRepository.sumTeacherRevenue(
                eq(7), eq(OrderStatus.PAID), any(), any(), isNull()))
                .thenReturn(new BigDecimal("59.995"));
        when(enrollmentRepository.averageProgressByTeacherId(7))
                .thenReturn(new BigDecimal("88.4"));
        when(enrollmentRepository.countDistinctStudentsByTeacherId(7)).thenReturn(5L);

        TeacherDashboardDTO result = service.getCurrentTeacherDashboard();

        assertEquals(5, result.getTotalActiveStudents());
        assertEquals(88, result.getAverageCompletionPercent());
        assertEquals("$60.00", result.getRevenueMonthToDateDisplay());
        assertEquals(1, result.getActiveCourses().size());
        assertEquals("Resume", result.getActiveCourses().get(0).getActionLabel());
    }

    @Test
    void nonTeacherCannotReadTeacherDashboardData() {
        when(currentUserService.getCurrentUser())
                .thenReturn(User.builder().id(8).role(Role.STUDENT).build());

        assertThrows(BusinessException.class,
                service::getCurrentTeacherDashboard);
        verifyNoInteractions(
                courseRepository, enrollmentRepository, orderItemRepository);
    }

    private TeacherCourseMetricProjection metric(
            Integer courseId, Long enrollmentCount, String averageProgress) {
        return new TeacherCourseMetricProjection() {
            public Integer getCourseId() { return courseId; }
            public Long getEnrollmentCount() { return enrollmentCount; }
            public BigDecimal getAverageProgress() {
                return new BigDecimal(averageProgress);
            }
        };
    }
}
