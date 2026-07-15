package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.response.TeacherDashboardStatsDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.OrderStatus;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.repository.CourseEnrollmentRepository;
import com.ojtsu26.elearning.repository.LessonProgressRepository;
import com.ojtsu26.elearning.repository.OrderItemRepository;
import com.ojtsu26.elearning.service.impl.TeacherDashboardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class TeacherDashboardServiceTest {

    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private LessonProgressRepository lessonProgressRepository;
    @Mock
    private CourseEnrollmentRepository enrollmentRepository;
    @Mock
    private OrderItemRepository orderItemRepository;

    private TeacherDashboardServiceImpl service;
    private User teacher;

    @BeforeEach
    void setUp() {
        service = new TeacherDashboardServiceImpl(
                currentUserService,
                lessonProgressRepository,
                enrollmentRepository,
                orderItemRepository
        );
        teacher = User.builder().id(7).role(Role.TEACHER).build();
    }

    @Test
    void teacherStatsUseCurrentMonthBoundaryOwnDataAndPaidRevenueOnly() {
        when(currentUserService.getCurrentUser()).thenReturn(teacher);
        LocalDate firstDay = LocalDate.now().withDayOfMonth(1);
        LocalDateTime currentFrom = firstDay.atStartOfDay();
        LocalDateTime currentTo = LocalDate.now().plusDays(1).atStartOfDay();
        LocalDateTime previousFrom = currentFrom.minusMonths(1);

        when(lessonProgressRepository.countTeacherActiveStudentsForAnalytics(7, currentFrom, currentTo)).thenReturn(15L);
        when(lessonProgressRepository.countTeacherActiveStudentsForAnalytics(7, previousFrom, currentFrom)).thenReturn(10L);
        when(enrollmentRepository.averageProgressByTeacherId(7)).thenReturn(new BigDecimal("82.34"));
        when(enrollmentRepository.averageProgressByTeacherIdAndEnrolledAtBetween(7, currentFrom, currentTo))
                .thenReturn(new BigDecimal("80.00"));
        when(enrollmentRepository.averageProgressByTeacherIdAndEnrolledAtBetween(7, previousFrom, currentFrom))
                .thenReturn(new BigDecimal("70.00"));
        when(orderItemRepository.sumTeacherRevenue(7, OrderStatus.PAID, currentFrom, currentTo, null))
                .thenReturn(new BigDecimal("200.00"));
        when(orderItemRepository.sumTeacherRevenue(7, OrderStatus.PAID, previousFrom, currentFrom, null))
                .thenReturn(new BigDecimal("100.00"));
        when(orderItemRepository.findTeacherPaidRevenueCurrencies(7, OrderStatus.PAID, currentFrom, currentTo))
                .thenReturn(List.of("USD"));

        TeacherDashboardStatsDTO stats = service.getCurrentTeacherStats();

        assertEquals(15, stats.getActiveStudents());
        assertEquals(new BigDecimal("82.3"), stats.getAverageCompletionRate());
        assertEquals(new BigDecimal("200.00"), stats.getTotalRevenueMtd());
        assertEquals("USD", stats.getRevenueCurrencyCode());
        assertTrue(stats.getActiveStudentsChangeText().contains("50%"));
        assertTrue(stats.getCompletionRateChangeText().contains("10 pts"));
        assertTrue(stats.getRevenueChangeText().contains("100%"));

        verify(orderItemRepository).sumTeacherRevenue(7, OrderStatus.PAID, currentFrom, currentTo, null);
        verify(orderItemRepository).sumTeacherRevenue(7, OrderStatus.PAID, previousFrom, currentFrom, null);
        verify(orderItemRepository, never()).sumTeacherRevenue(7, OrderStatus.PENDING, currentFrom, currentTo, null);
        verify(orderItemRepository, never()).sumTeacherRevenue(7, OrderStatus.FAILED, currentFrom, currentTo, null);
    }

    @Test
    void zeroRecordsAndZeroPreviousDenominatorRenderNeutralValues() {
        when(currentUserService.getCurrentUser()).thenReturn(teacher);
        LocalDate firstDay = LocalDate.now().withDayOfMonth(1);
        LocalDateTime currentFrom = firstDay.atStartOfDay();
        LocalDateTime currentTo = LocalDate.now().plusDays(1).atStartOfDay();
        LocalDateTime previousFrom = currentFrom.minusMonths(1);

        when(lessonProgressRepository.countTeacherActiveStudentsForAnalytics(7, currentFrom, currentTo)).thenReturn(0L);
        when(lessonProgressRepository.countTeacherActiveStudentsForAnalytics(7, previousFrom, currentFrom)).thenReturn(0L);
        when(enrollmentRepository.averageProgressByTeacherId(7)).thenReturn(null);
        when(enrollmentRepository.averageProgressByTeacherIdAndEnrolledAtBetween(7, currentFrom, currentTo)).thenReturn(null);
        when(enrollmentRepository.averageProgressByTeacherIdAndEnrolledAtBetween(7, previousFrom, currentFrom)).thenReturn(null);
        when(orderItemRepository.sumTeacherRevenue(7, OrderStatus.PAID, currentFrom, currentTo, null)).thenReturn(null);
        when(orderItemRepository.sumTeacherRevenue(7, OrderStatus.PAID, previousFrom, currentFrom, null)).thenReturn(BigDecimal.ZERO);
        when(orderItemRepository.findTeacherPaidRevenueCurrencies(7, OrderStatus.PAID, currentFrom, currentTo)).thenReturn(List.of());

        TeacherDashboardStatsDTO stats = service.getCurrentTeacherStats();

        assertEquals(0, stats.getActiveStudents());
        assertEquals(new BigDecimal("0.0"), stats.getAverageCompletionRate());
        assertEquals(new BigDecimal("0.00"), stats.getTotalRevenueMtd());
        assertEquals("", stats.getRevenueCurrencyCode());
        assertEquals("No change from last month", stats.getActiveStudentsChangeText());
        assertEquals("No previous-month completion data", stats.getCompletionRateChangeText());
        assertEquals("No change from last month", stats.getRevenueChangeText());
    }

    @Test
    void wrongRoleIsRejectedBeforeDashboardQueries() {
        when(currentUserService.getCurrentUser()).thenReturn(User.builder().id(9).role(Role.STUDENT).build());

        assertThrows(BusinessException.class, () -> service.getCurrentTeacherStats());

        verify(lessonProgressRepository, never()).countTeacherActiveStudentsForAnalytics(any(), any(), any());
        verify(orderItemRepository, never()).sumTeacherRevenue(any(), any(), any(), any(), any());
    }
}
