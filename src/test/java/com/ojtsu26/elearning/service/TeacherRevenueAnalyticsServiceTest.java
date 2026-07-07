package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.response.TeacherRevenueAnalyticsResponseDTO;
import com.ojtsu26.elearning.dto.response.TeacherRevenueByCourseResponseDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.OrderStatus;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.repository.CourseEnrollmentRepository;
import com.ojtsu26.elearning.repository.CourseRepository;
import com.ojtsu26.elearning.repository.OrderItemRepository;
import com.ojtsu26.elearning.repository.projection.TeacherEnrollmentCountProjection;
import com.ojtsu26.elearning.repository.projection.TeacherRevenueCourseProjection;
import com.ojtsu26.elearning.repository.projection.TeacherRevenueEventProjection;
import com.ojtsu26.elearning.service.impl.TeacherRevenueAnalyticsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeacherRevenueAnalyticsServiceTest {

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CourseEnrollmentRepository enrollmentRepository;

    private TeacherRevenueAnalyticsServiceImpl service;
    private User teacher;
    private User otherTeacher;
    private User student;
    private Course course;
    private Course otherCourse;

    @BeforeEach
    void setUp() {
        service = new TeacherRevenueAnalyticsServiceImpl(
                currentUserService,
                courseRepository,
                orderItemRepository,
                enrollmentRepository
        );
        teacher = User.builder().id(7).role(Role.TEACHER).fullName("Teacher").build();
        otherTeacher = User.builder().id(8).role(Role.TEACHER).fullName("Other Teacher").build();
        student = User.builder().id(9).role(Role.STUDENT).fullName("Student").build();
        course = Course.builder().id(101).title("Spring Revenue").instructor(teacher).build();
        otherCourse = Course.builder().id(202).title("Other Revenue").instructor(otherTeacher).build();
    }

    @Test
    void teacherCanViewOwnPaidRevenueAndTrend() {
        when(currentUserService.getCurrentUser()).thenReturn(teacher);
        when(courseRepository.findById(101)).thenReturn(Optional.of(course));
        LocalDateTime from = LocalDateTime.of(2026, 6, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 7, 1, 0, 0);
        when(orderItemRepository.sumTeacherRevenue(7, OrderStatus.PAID, from, to, 101))
                .thenReturn(new BigDecimal("149.995"));
        when(orderItemRepository.countTeacherPaidOrders(7, OrderStatus.PAID, from, to, 101)).thenReturn(2L);
        when(enrollmentRepository.countTeacherEnrollmentsForAnalytics(7, from, to, 101)).thenReturn(3L);
        when(enrollmentRepository.countTeacherStudentsForAnalytics(7, from, to, 101)).thenReturn(3L);
        when(orderItemRepository.findTeacherRevenueEvents(7, OrderStatus.PAID, from, to, 101))
                .thenReturn(List.of(
                        revenueEvent(1, LocalDateTime.of(2026, 6, 3, 9, 0), 101, "Spring Revenue", "50.00"),
                        revenueEvent(2, LocalDateTime.of(2026, 6, 3, 10, 0), 101, "Spring Revenue", "99.995")
                ));
        when(orderItemRepository.findTeacherRevenueByCourse(7, OrderStatus.PAID, from, to, 101))
                .thenReturn(List.of(revenueCourse(101, "Spring Revenue", "149.995", 2L, 2L)));
        when(enrollmentRepository.countTeacherEnrollmentsByCourseForAnalytics(7, from, to, 101))
                .thenReturn(List.of(enrollmentCount(101, 3L, 3L)));
        when(courseRepository.findByInstructorId(7)).thenReturn(List.of(course));

        TeacherRevenueAnalyticsResponseDTO response = service.getRevenueAnalytics(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
                "day",
                101
        );

        assertEquals(new BigDecimal("150.00"), response.getTotalRevenue());
        assertEquals(2, response.getPaidOrderCount());
        assertEquals(3, response.getEnrollmentCount());
        assertEquals("Spring Revenue", response.getBestSellingCourse().getCourseTitle());
        assertEquals(new BigDecimal("150.00"), response.getTrend().get(2).getRevenue());
        assertEquals(2, response.getTrend().get(2).getPaidOrderCount());
    }

    @Test
    void otherTeacherCourseIsRejectedBeforeRevenueQueries() {
        when(currentUserService.getCurrentUser()).thenReturn(teacher);
        when(courseRepository.findById(202)).thenReturn(Optional.of(otherCourse));

        assertThrows(BusinessException.class, () -> service.getRevenueAnalytics(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
                "month",
                202
        ));

        verify(orderItemRepository, never()).sumTeacherRevenue(any(), any(), any(), any(), any());
        verify(orderItemRepository, never()).findTeacherRevenueEvents(any(), any(), any(), any(), any());
    }

    @Test
    void nonTeacherRoleIsRejected() {
        when(currentUserService.getCurrentUser()).thenReturn(student);

        assertThrows(BusinessException.class, () -> service.getRevenueByCourse(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
                null
        ));

        verify(courseRepository, never()).findByInstructorId(any());
    }

    @Test
    void invalidDateRangeAndGroupByAreRejected() {
        when(currentUserService.getCurrentUser()).thenReturn(teacher);

        assertThrows(BusinessException.class, () -> service.getRevenueAnalytics(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 6, 30),
                "day",
                null
        ));

        assertThrows(BusinessException.class, () -> service.getRevenueAnalytics(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
                "week",
                null
        ));
    }

    @Test
    void byCourseIncludesZeroRevenueOwnedCoursesAndUsesPaidStatusOnly() {
        when(currentUserService.getCurrentUser()).thenReturn(teacher);
        LocalDateTime from = LocalDateTime.of(2026, 6, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 7, 1, 0, 0);
        Course quietCourse = Course.builder().id(303).title("Quiet Course").instructor(teacher).build();
        when(courseRepository.findByInstructorId(7)).thenReturn(List.of(course, quietCourse));
        when(orderItemRepository.findTeacherRevenueByCourse(7, OrderStatus.PAID, from, to, null))
                .thenReturn(List.of(revenueCourse(101, "Spring Revenue", "75.00", 1L, 1L)));
        when(enrollmentRepository.countTeacherEnrollmentsByCourseForAnalytics(7, from, to, null))
                .thenReturn(List.of(enrollmentCount(101, 2L, 2L), enrollmentCount(303, 1L, 1L)));

        TeacherRevenueByCourseResponseDTO response = service.getRevenueByCourse(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
                null
        );

        assertEquals(2, response.getCourses().size());
        assertEquals("Spring Revenue", response.getCourses().get(0).getCourseTitle());
        assertEquals("Quiet Course", response.getCourses().get(1).getCourseTitle());
        assertEquals(BigDecimal.ZERO.setScale(2), response.getCourses().get(1).getRevenue());
        verify(orderItemRepository).findTeacherRevenueByCourse(7, OrderStatus.PAID, from, to, null);
    }

    private TeacherRevenueEventProjection revenueEvent(Integer orderId,
                                                       LocalDateTime createdAt,
                                                       Integer courseId,
                                                       String courseTitle,
                                                       String revenue) {
        return new TeacherRevenueEventProjection() {
            @Override
            public Integer getOrderId() {
                return orderId;
            }

            @Override
            public LocalDateTime getCreatedAt() {
                return createdAt;
            }

            @Override
            public Integer getCourseId() {
                return courseId;
            }

            @Override
            public String getCourseTitle() {
                return courseTitle;
            }

            @Override
            public BigDecimal getRevenue() {
                return new BigDecimal(revenue);
            }
        };
    }

    private TeacherRevenueCourseProjection revenueCourse(Integer courseId,
                                                         String courseTitle,
                                                         String revenue,
                                                         Long paidOrderCount,
                                                         Long unitsSold) {
        return new TeacherRevenueCourseProjection() {
            @Override
            public Integer getCourseId() {
                return courseId;
            }

            @Override
            public String getCourseTitle() {
                return courseTitle;
            }

            @Override
            public BigDecimal getRevenue() {
                return new BigDecimal(revenue);
            }

            @Override
            public Long getPaidOrderCount() {
                return paidOrderCount;
            }

            @Override
            public Long getUnitsSold() {
                return unitsSold;
            }
        };
    }

    private TeacherEnrollmentCountProjection enrollmentCount(Integer courseId, Long enrollmentCount, Long studentCount) {
        return new TeacherEnrollmentCountProjection() {
            @Override
            public Integer getCourseId() {
                return courseId;
            }

            @Override
            public Long getEnrollmentCount() {
                return enrollmentCount;
            }

            @Override
            public Long getStudentCount() {
                return studentCount;
            }
        };
    }
}
