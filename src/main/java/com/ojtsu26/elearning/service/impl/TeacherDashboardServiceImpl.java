package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.dto.response.TeacherDashboardStatsDTO;
import com.ojtsu26.elearning.repository.LessonProgressRepository;
import com.ojtsu26.elearning.dto.response.TeacherCourseDashboardDTO;
import com.ojtsu26.elearning.dto.response.TeacherDashboardDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.exception.ErrorCode;
import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.CourseStatus;
import com.ojtsu26.elearning.model.enums.OrderStatus;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.repository.CourseEnrollmentRepository;
import com.ojtsu26.elearning.repository.CourseRepository;
import com.ojtsu26.elearning.repository.OrderItemRepository;
import com.ojtsu26.elearning.repository.projection.TeacherCourseMetricProjection;
import com.ojtsu26.elearning.repository.projection.TeacherRevenueCourseProjection;
import com.ojtsu26.elearning.service.CurrentUserService;
import com.ojtsu26.elearning.service.CurrencyDisplayService;
import com.ojtsu26.elearning.service.TeacherDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherDashboardServiceImpl implements TeacherDashboardService {

    private static final int COURSE_LIMIT = 5;
    private static final String CHANGE_UP = "stat-change-up";
    private static final String CHANGE_DOWN = "stat-change-down";
    private static final String CHANGE_NEUTRAL = "stat-change-neutral";

    private final CurrentUserService currentUserService;
    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final OrderItemRepository orderItemRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final CurrencyDisplayService currencyDisplayService;

    @Override
    @Transactional(readOnly = true)
    public TeacherDashboardDTO getCurrentTeacherDashboard() {
        User teacher = currentUserService.getCurrentUser();
        requireTeacher(teacher);

        YearMonth currentMonth = YearMonth.now();
        LocalDateTime monthStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime nextMonthStart = currentMonth.plusMonths(1).atDay(1).atStartOfDay();

        Map<Integer, TeacherCourseMetricProjection> metricsByCourse = enrollmentRepository
                .findDashboardCourseMetricsByTeacherId(teacher.getId())
                .stream()
                .collect(Collectors.toMap(TeacherCourseMetricProjection::getCourseId, Function.identity()));

        Map<Integer, TeacherRevenueCourseProjection> revenueByCourse = orderItemRepository
                .findTeacherRevenueByCourse(teacher.getId(), OrderStatus.PAID, monthStart, nextMonthStart, null)
                .stream()
                .collect(Collectors.toMap(TeacherRevenueCourseProjection::getCourseId, Function.identity()));

        List<TeacherCourseDashboardDTO> activeCourses = courseRepository.findByInstructorId(teacher.getId())
                .stream()
                .sorted(Comparator.comparing(Course::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Course::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(COURSE_LIMIT)
                .map(course -> toCourseDashboardDto(course, metricsByCourse.get(course.getId()), revenueByCourse.get(course.getId())))
                .toList();

        BigDecimal totalRevenue = displayMoney(orderItemRepository.sumTeacherRevenue(
                teacher.getId(), OrderStatus.PAID, monthStart, nextMonthStart, null));
        BigDecimal averageProgress = enrollmentRepository.averageProgressByTeacherId(teacher.getId());

        return TeacherDashboardDTO.builder()
                .totalActiveStudents(enrollmentRepository.countDistinctStudentsByTeacherId(teacher.getId()))
                .averageCompletionPercent(clampPercent(averageProgress))
                .revenueMonthToDate(totalRevenue)
                .revenueMonthToDateDisplay(formatMoney(totalRevenue))
                .activeCourses(activeCourses)
                .build();
    }

    private TeacherCourseDashboardDTO toCourseDashboardDto(Course course,
                                                           TeacherCourseMetricProjection metric,
                                                           TeacherRevenueCourseProjection revenue) {
        int averageProgress = clampPercent(metric == null ? null : metric.getAverageProgress());
        BigDecimal revenueValue = displayMoney(revenue == null ? null : revenue.getRevenue());
        CourseStatus status = course.getStatus();
        String actionLabel = status == CourseStatus.DRAFT ? "Resume" : "Edit";

        return TeacherCourseDashboardDTO.builder()
                .courseId(course.getId())
                .title(blankToFallback(course.getTitle(), "Untitled course"))
                .courseLabel(course.getId() == null ? "No course ID" : "Course ID " + course.getId())
                .enrolledCount(metric == null || metric.getEnrollmentCount() == null ? 0 : metric.getEnrollmentCount())
                .averageCompletionPercent(averageProgress)
                .revenueMonthToDate(revenueValue)
                .revenueMonthToDateDisplay(formatMoney(revenueValue))
                .status(formatStatus(status))
                .statusClass(statusClass(status))
                .actionLabel(actionLabel)
                .actionUrl("/teacher/courses/edit/" + course.getId())
                .build();
    }

    private void requireTeacher(User user) {
        if (user == null || user.getRole() != Role.TEACHER) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    private int clampPercent(BigDecimal value) {
        if (value == null) {
            return 0;
        }
        int rounded = value.setScale(0, RoundingMode.HALF_UP).intValue();
        return Math.max(0, Math.min(100, rounded));
    }

    private String formatMoney(BigDecimal value) {
        return currencyDisplayService.formatDisplayMoney(value);
    }

    private String formatStatus(CourseStatus status) {
        if (status == null) {
            return "Unknown";
        }
        return switch (status) {
            case APPROVED -> "Approved";
            case DRAFT -> "Draft";
            case PENDING_APPROVAL -> "Pending Approval";
            case HIDDEN -> "Hidden";
        };
    }

    private String statusClass(CourseStatus status) {
        if (status == CourseStatus.APPROVED) {
            return "badge-success";
        }
        if (status == CourseStatus.PENDING_APPROVAL) {
            return "badge-warning";
        }
        return "badge-secondary";
    }

    private String blankToFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherDashboardStatsDTO getCurrentTeacherStats() {
        User teacher = currentUserService.getCurrentUser();
        requireTeacher(teacher);

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

        BigDecimal totalRevenueMtd = displayMoney(orderItemRepository.sumTeacherRevenue(
                teacher.getId(), OrderStatus.PAID, currentMonth.from(), currentMonth.to(), null));
        BigDecimal previousRevenue = displayMoney(orderItemRepository.sumTeacherRevenue(
                teacher.getId(), OrderStatus.PAID, previousMonth.from(), previousMonth.to(), null));

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
                .revenueCurrencyCode(currencyDisplayService.getDisplayCurrency())
                .totalRevenueMtdDisplay(formatMoney(totalRevenueMtd))
                .revenueChangeText(revenueTrend.text())
                .revenueChangeClass(revenueTrend.cssClass())
                .build();
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

    private BigDecimal displayMoney(BigDecimal usdValue) {
        return currencyDisplayService.convertUsdToDisplay(usdValue);
    }

    private record Trend(String text, String cssClass) {
    }

    private record MonthWindow(LocalDateTime from, LocalDateTime to) {
        private static MonthWindow current() {
            java.time.LocalDate firstDay = java.time.LocalDate.now().withDayOfMonth(1);
            return new MonthWindow(firstDay.atStartOfDay(), java.time.LocalDate.now().plusDays(1).atStartOfDay());
        }

        private MonthWindow previous() {
            LocalDateTime previousFrom = from.minusMonths(1);
            return new MonthWindow(previousFrom, from);
        }
    }
}

