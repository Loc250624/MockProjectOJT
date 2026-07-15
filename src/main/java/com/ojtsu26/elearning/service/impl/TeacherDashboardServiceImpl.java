package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.dto.response.TeacherCourseDashboardDTO;
import com.ojtsu26.elearning.dto.response.TeacherDashboardDTO;
import com.ojtsu26.elearning.dto.response.TeacherRecentSubmissionDashboardDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.exception.ErrorCode;
import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.CourseStatus;
import com.ojtsu26.elearning.model.enums.OrderStatus;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.model.enums.SubmissionStatus;
import com.ojtsu26.elearning.repository.CourseEnrollmentRepository;
import com.ojtsu26.elearning.repository.CourseRepository;
import com.ojtsu26.elearning.repository.OrderItemRepository;
import com.ojtsu26.elearning.repository.SubmissionRepository;
import com.ojtsu26.elearning.repository.projection.TeacherCourseMetricProjection;
import com.ojtsu26.elearning.repository.projection.TeacherRecentSubmissionProjection;
import com.ojtsu26.elearning.repository.projection.TeacherRevenueCourseProjection;
import com.ojtsu26.elearning.service.CurrentUserService;
import com.ojtsu26.elearning.service.TeacherDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherDashboardServiceImpl implements TeacherDashboardService {

    private static final int COURSE_LIMIT = 5;
    private static final int SUBMISSION_LIMIT = 5;
    private static final String BUSINESS_CURRENCY = "USD";
    private static final Set<SubmissionStatus> REVIEW_STATUSES = Set.of(
            SubmissionStatus.SUBMITTED,
            SubmissionStatus.PENDING_REVIEW
    );

    private final CurrentUserService currentUserService;
    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final OrderItemRepository orderItemRepository;
    private final SubmissionRepository submissionRepository;

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

        List<TeacherRecentSubmissionDashboardDTO> recentSubmissions = submissionRepository
                .findRecentDashboardSubmissionsByTeacherId(teacher.getId(), REVIEW_STATUSES, PageRequest.of(0, SUBMISSION_LIMIT))
                .stream()
                .map(this::toSubmissionDashboardDto)
                .toList();

        BigDecimal totalRevenue = money(orderItemRepository.sumTeacherRevenue(
                teacher.getId(), OrderStatus.PAID, monthStart, nextMonthStart, null));
        BigDecimal averageProgress = enrollmentRepository.averageProgressByTeacherId(teacher.getId());

        return TeacherDashboardDTO.builder()
                .totalActiveStudents(enrollmentRepository.countDistinctStudentsByTeacherId(teacher.getId()))
                .averageCompletionPercent(clampPercent(averageProgress))
                .revenueMonthToDate(totalRevenue)
                .revenueMonthToDateDisplay(formatMoney(totalRevenue))
                .activeCourses(activeCourses)
                .recentSubmissions(recentSubmissions)
                .gradeAllUrl(recentSubmissions.stream()
                        .map(TeacherRecentSubmissionDashboardDTO::getReviewUrl)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse("/teacher/grading"))
                .build();
    }

    private TeacherCourseDashboardDTO toCourseDashboardDto(Course course,
                                                           TeacherCourseMetricProjection metric,
                                                           TeacherRevenueCourseProjection revenue) {
        int averageProgress = clampPercent(metric == null ? null : metric.getAverageProgress());
        BigDecimal revenueValue = money(revenue == null ? null : revenue.getRevenue());
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

    private TeacherRecentSubmissionDashboardDTO toSubmissionDashboardDto(TeacherRecentSubmissionProjection row) {
        return TeacherRecentSubmissionDashboardDTO.builder()
                .submissionId(row.getSubmissionId())
                .assignmentId(row.getAssignmentId())
                .assessmentTitle(blankToFallback(row.getAssessmentTitle(), "Untitled assignment"))
                .studentName(blankToFallback(row.getStudentName(), "Unknown student"))
                .submittedAt(row.getSubmittedAt())
                .submittedAgoLabel(formatSubmittedAgo(row.getSubmittedAt()))
                .reviewStatus(formatSubmissionStatus(row.getStatus()))
                .reviewStatusClass(submissionStatusClass(row.getStatus()))
                .reviewUrl("/teacher/assignments/" + row.getAssignmentId() + "/submissions")
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

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private String formatMoney(BigDecimal value) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
        formatter.setCurrency(java.util.Currency.getInstance(BUSINESS_CURRENCY));
        return formatter.format(money(value));
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

    private String formatSubmissionStatus(SubmissionStatus status) {
        if (status == null) {
            return "Unknown";
        }
        return switch (status) {
            case SUBMITTED, PENDING_REVIEW -> "Needs Review";
            case GRADED, AUTO_GRADED -> "Graded";
            case PASSED -> "Passed";
            case FAILED -> "Failed";
            case RETURNED -> "Returned";
            case DRAFT -> "Draft";
        };
    }

    private String submissionStatusClass(SubmissionStatus status) {
        if (status == SubmissionStatus.SUBMITTED || status == SubmissionStatus.PENDING_REVIEW) {
            return "badge-warning";
        }
        if (status == SubmissionStatus.FAILED || status == SubmissionStatus.RETURNED) {
            return "badge-danger";
        }
        return "badge-success";
    }

    private String formatSubmittedAgo(LocalDateTime submittedAt) {
        if (submittedAt == null) {
            return "Submission time unavailable";
        }
        Duration elapsed = Duration.between(submittedAt, LocalDateTime.now());
        if (elapsed.isNegative() || elapsed.toMinutes() < 1) {
            return "Submitted just now";
        }
        if (elapsed.toHours() < 1) {
            return "Submitted " + elapsed.toMinutes() + " min ago";
        }
        if (elapsed.toDays() < 1) {
            return "Submitted " + elapsed.toHours() + " hrs ago";
        }
        return "Submitted " + elapsed.toDays() + " days ago";
    }

    private String blankToFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
