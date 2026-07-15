package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.dto.response.TeacherDashboardStatsDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.exception.ErrorCode;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.OrderStatus;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.repository.CourseEnrollmentRepository;
import com.ojtsu26.elearning.repository.LessonProgressRepository;
import com.ojtsu26.elearning.repository.OrderItemRepository;
import com.ojtsu26.elearning.service.CurrentUserService;
import com.ojtsu26.elearning.service.TeacherDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeacherDashboardServiceImpl implements TeacherDashboardService {

    private static final String CHANGE_UP = "stat-change-up";
    private static final String CHANGE_DOWN = "stat-change-down";
    private static final String CHANGE_NEUTRAL = "stat-change-neutral";

    private final CurrentUserService currentUserService;
    private final LessonProgressRepository lessonProgressRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    @Transactional(readOnly = true)
    public TeacherDashboardStatsDTO getCurrentTeacherStats() {
        User teacher = currentUserService.getCurrentUser();
        if (teacher.getRole() != Role.TEACHER) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        MonthWindow currentMonth = MonthWindow.current();
        MonthWindow previousMonth = currentMonth.previous();

        long currentActiveStudents = lessonProgressRepository.countTeacherActiveStudentsForAnalytics(
                teacher.getId(), currentMonth.from(), currentMonth.to());
        long previousActiveStudents = lessonProgressRepository.countTeacherActiveStudentsForAnalytics(
                teacher.getId(), previousMonth.from(), previousMonth.to());

        BigDecimal averageCompletionRate = percent(enrollmentRepository.averageProgressByTeacherId(teacher.getId()));
        BigDecimal currentCompletionRate = enrollmentRepository.averageProgressByTeacherIdAndEnrolledAtBetween(
                teacher.getId(), currentMonth.from(), currentMonth.to());
        BigDecimal previousCompletionRate = enrollmentRepository.averageProgressByTeacherIdAndEnrolledAtBetween(
                teacher.getId(), previousMonth.from(), previousMonth.to());

        BigDecimal totalRevenueMtd = money(orderItemRepository.sumTeacherRevenue(
                teacher.getId(), OrderStatus.PAID, currentMonth.from(), currentMonth.to(), null));
        BigDecimal previousRevenue = money(orderItemRepository.sumTeacherRevenue(
                teacher.getId(), OrderStatus.PAID, previousMonth.from(), previousMonth.to(), null));
        List<String> revenueCurrencies = orderItemRepository.findTeacherPaidRevenueCurrencies(
                teacher.getId(), OrderStatus.PAID, currentMonth.from(), currentMonth.to());

        Trend activeTrend = trendPercent(BigDecimal.valueOf(currentActiveStudents), BigDecimal.valueOf(previousActiveStudents),
                "No active students last month");
        Trend completionTrend = completionTrend(currentCompletionRate, previousCompletionRate);
        Trend revenueTrend = trendPercent(totalRevenueMtd, previousRevenue, "No paid revenue last month");

        return TeacherDashboardStatsDTO.builder()
                .activeStudents(currentActiveStudents)
                .activeStudentsChangeText(activeTrend.text())
                .activeStudentsChangeClass(activeTrend.cssClass())
                .averageCompletionRate(averageCompletionRate)
                .completionRateChangeText(completionTrend.text())
                .completionRateChangeClass(completionTrend.cssClass())
                .totalRevenueMtd(totalRevenueMtd)
                .revenueCurrencyCode(revenueCurrencyCode(revenueCurrencies))
                .revenueChangeText(revenueTrend.text())
                .revenueChangeClass(revenueTrend.cssClass())
                .build();
    }

    private String revenueCurrencyCode(List<String> currencyCodes) {
        if (currencyCodes == null || currencyCodes.isEmpty()) {
            return "";
        }
        if (currencyCodes.size() == 1) {
            return currencyCodes.get(0);
        }
        return "Mixed";
    }

    private Trend completionTrend(BigDecimal currentCompletionRate, BigDecimal previousCompletionRate) {
        if (currentCompletionRate == null || previousCompletionRate == null) {
            return new Trend("No previous-month completion data", CHANGE_NEUTRAL);
        }
        BigDecimal delta = currentCompletionRate.subtract(previousCompletionRate).setScale(1, RoundingMode.HALF_UP);
        int comparison = delta.compareTo(BigDecimal.ZERO);
        if (comparison == 0) {
            return new Trend("No change from last month", CHANGE_NEUTRAL);
        }
        String direction = comparison > 0 ? "up" : "down";
        String css = comparison > 0 ? CHANGE_UP : CHANGE_DOWN;
        return new Trend(delta.abs().stripTrailingZeros().toPlainString() + " pts " + direction + " from last month", css);
    }

    private Trend trendPercent(BigDecimal current, BigDecimal previous, String zeroPreviousText) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            if (current != null && current.compareTo(BigDecimal.ZERO) > 0) {
                return new Trend(zeroPreviousText, CHANGE_NEUTRAL);
            }
            return new Trend("No change from last month", CHANGE_NEUTRAL);
        }

        BigDecimal deltaPercent = current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 1, RoundingMode.HALF_UP);
        int comparison = deltaPercent.compareTo(BigDecimal.ZERO);
        if (comparison == 0) {
            return new Trend("No change from last month", CHANGE_NEUTRAL);
        }
        String direction = comparison > 0 ? "up" : "down";
        String css = comparison > 0 ? CHANGE_UP : CHANGE_DOWN;
        return new Trend(deltaPercent.abs().stripTrailingZeros().toPlainString() + "% " + direction + " from last month", css);
    }

    private BigDecimal percent(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(1, RoundingMode.HALF_UP);
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private record Trend(String text, String cssClass) {
    }

    private record MonthWindow(LocalDateTime from, LocalDateTime to) {
        private static MonthWindow current() {
            LocalDate firstDay = LocalDate.now().withDayOfMonth(1);
            return new MonthWindow(firstDay.atStartOfDay(), LocalDate.now().plusDays(1).atStartOfDay());
        }

        private MonthWindow previous() {
            LocalDateTime previousFrom = from.minusMonths(1);
            return new MonthWindow(previousFrom, from);
        }
    }
}
