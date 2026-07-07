package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.dto.response.TeacherRevenueAnalyticsResponseDTO;
import com.ojtsu26.elearning.dto.response.TeacherRevenueByCourseResponseDTO;
import com.ojtsu26.elearning.dto.response.TeacherRevenueCourseDTO;
import com.ojtsu26.elearning.dto.response.TeacherRevenueTrendPointDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.exception.ErrorCode;
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
import com.ojtsu26.elearning.service.CurrentUserService;
import com.ojtsu26.elearning.service.TeacherRevenueAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TeacherRevenueAnalyticsServiceImpl implements TeacherRevenueAnalyticsService {

    private static final String GROUP_DAY = "day";
    private static final String GROUP_MONTH = "month";
    private static final String GROUP_YEAR = "year";

    private final CurrentUserService currentUserService;
    private final CourseRepository courseRepository;
    private final OrderItemRepository orderItemRepository;
    private final CourseEnrollmentRepository enrollmentRepository;

    @Override
    @Transactional(readOnly = true)
    public TeacherRevenueAnalyticsResponseDTO getRevenueAnalytics(LocalDate from,
                                                                  LocalDate to,
                                                                  String groupBy,
                                                                  Integer courseId) {
        User teacher = currentUserService.getCurrentUser();
        requireTeacher(teacher);
        DateFilter filter = normalizeDateFilter(from, to);
        String cleanGroupBy = normalizeGroupBy(groupBy);
        requireOwnedCourseIfPresent(courseId, teacher.getId());

        LocalDateTime fromDateTime = filter.from().atStartOfDay();
        LocalDateTime toDateTime = filter.to().plusDays(1).atStartOfDay();
        BigDecimal totalRevenue = money(orderItemRepository.sumTeacherRevenue(
                teacher.getId(), OrderStatus.PAID, fromDateTime, toDateTime, courseId));
        long paidOrderCount = orderItemRepository.countTeacherPaidOrders(
                teacher.getId(), OrderStatus.PAID, fromDateTime, toDateTime, courseId);
        long enrollmentCount = enrollmentRepository.countTeacherEnrollmentsForAnalytics(
                teacher.getId(), fromDateTime, toDateTime, courseId);
        long studentCount = enrollmentRepository.countTeacherStudentsForAnalytics(
                teacher.getId(), fromDateTime, toDateTime, courseId);

        List<TeacherRevenueEventProjection> events = orderItemRepository.findTeacherRevenueEvents(
                teacher.getId(), OrderStatus.PAID, fromDateTime, toDateTime, courseId);
        List<TeacherRevenueTrendPointDTO> trend = buildTrend(filter, cleanGroupBy, events);
        TeacherRevenueCourseDTO bestSellingCourse = getRevenueCourses(teacher.getId(), filter, courseId).stream()
                .filter(course -> course.getUnitsSold() > 0)
                .max(Comparator.comparingLong(TeacherRevenueCourseDTO::getUnitsSold)
                        .thenComparing(TeacherRevenueCourseDTO::getRevenue))
                .orElse(null);

        return TeacherRevenueAnalyticsResponseDTO.builder()
                .from(filter.from())
                .to(filter.to())
                .groupBy(cleanGroupBy)
                .courseId(courseId)
                .totalRevenue(totalRevenue)
                .paidOrderCount(paidOrderCount)
                .enrollmentCount(enrollmentCount)
                .studentCount(studentCount)
                .bestSellingCourse(bestSellingCourse)
                .trend(trend)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherRevenueByCourseResponseDTO getRevenueByCourse(LocalDate from,
                                                                LocalDate to,
                                                                Integer courseId) {
        User teacher = currentUserService.getCurrentUser();
        requireTeacher(teacher);
        DateFilter filter = normalizeDateFilter(from, to);
        requireOwnedCourseIfPresent(courseId, teacher.getId());

        return TeacherRevenueByCourseResponseDTO.builder()
                .from(filter.from())
                .to(filter.to())
                .courseId(courseId)
                .courses(getRevenueCourses(teacher.getId(), filter, courseId))
                .build();
    }

    private List<TeacherRevenueCourseDTO> getRevenueCourses(Integer teacherId, DateFilter filter, Integer courseId) {
        LocalDateTime fromDateTime = filter.from().atStartOfDay();
        LocalDateTime toDateTime = filter.to().plusDays(1).atStartOfDay();
        Map<Integer, TeacherRevenueCourseProjection> revenueByCourse = new HashMap<>();
        orderItemRepository.findTeacherRevenueByCourse(teacherId, OrderStatus.PAID, fromDateTime, toDateTime, courseId)
                .forEach(row -> revenueByCourse.put(row.getCourseId(), row));

        Map<Integer, TeacherEnrollmentCountProjection> enrollmentsByCourse = new HashMap<>();
        enrollmentRepository.countTeacherEnrollmentsByCourseForAnalytics(teacherId, fromDateTime, toDateTime, courseId)
                .forEach(row -> enrollmentsByCourse.put(row.getCourseId(), row));

        return courseRepository.findByInstructorId(teacherId).stream()
                .filter(course -> courseId == null || Objects.equals(course.getId(), courseId))
                .map(course -> toCourseDto(course, revenueByCourse.get(course.getId()), enrollmentsByCourse.get(course.getId())))
                .sorted(Comparator.comparing(TeacherRevenueCourseDTO::getRevenue).reversed()
                        .thenComparing(TeacherRevenueCourseDTO::getUnitsSold, Comparator.reverseOrder())
                        .thenComparing(TeacherRevenueCourseDTO::getCourseTitle, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private TeacherRevenueCourseDTO toCourseDto(Course course,
                                                TeacherRevenueCourseProjection revenue,
                                                TeacherEnrollmentCountProjection enrollment) {
        return TeacherRevenueCourseDTO.builder()
                .courseId(course.getId())
                .courseTitle(course.getTitle())
                .revenue(money(revenue == null ? null : revenue.getRevenue()))
                .paidOrderCount(revenue == null || revenue.getPaidOrderCount() == null ? 0 : revenue.getPaidOrderCount())
                .unitsSold(revenue == null || revenue.getUnitsSold() == null ? 0 : revenue.getUnitsSold())
                .enrollmentCount(enrollment == null || enrollment.getEnrollmentCount() == null ? 0 : enrollment.getEnrollmentCount())
                .studentCount(enrollment == null || enrollment.getStudentCount() == null ? 0 : enrollment.getStudentCount())
                .build();
    }

    private List<TeacherRevenueTrendPointDTO> buildTrend(DateFilter filter,
                                                         String groupBy,
                                                         List<TeacherRevenueEventProjection> events) {
        Map<String, TrendBucket> buckets = new LinkedHashMap<>();
        initializeBuckets(filter, groupBy, buckets);
        events.forEach(event -> {
            String key = trendKey(event.getCreatedAt().toLocalDate(), groupBy);
            TrendBucket bucket = buckets.computeIfAbsent(key, ignored -> new TrendBucket());
            bucket.revenue = bucket.revenue.add(event.getRevenue() == null ? BigDecimal.ZERO : event.getRevenue());
            bucket.orderIds.put(event.getOrderId(), true);
        });

        return buckets.entrySet().stream()
                .map(entry -> TeacherRevenueTrendPointDTO.builder()
                        .period(entry.getKey())
                        .revenue(money(entry.getValue().revenue))
                        .paidOrderCount(entry.getValue().orderIds.size())
                        .build())
                .toList();
    }

    private void initializeBuckets(DateFilter filter, String groupBy, Map<String, TrendBucket> buckets) {
        if (GROUP_DAY.equals(groupBy) && filter.from().plusDays(370).isAfter(filter.to())) {
            for (LocalDate current = filter.from(); !current.isAfter(filter.to()); current = current.plusDays(1)) {
                buckets.put(trendKey(current, groupBy), new TrendBucket());
            }
            return;
        }
        if (GROUP_MONTH.equals(groupBy) && YearMonth.from(filter.from()).plusMonths(60).isAfter(YearMonth.from(filter.to()))) {
            for (YearMonth current = YearMonth.from(filter.from()); !current.isAfter(YearMonth.from(filter.to())); current = current.plusMonths(1)) {
                buckets.put(current.toString(), new TrendBucket());
            }
            return;
        }
        if (GROUP_YEAR.equals(groupBy) && filter.from().plusYears(20).isAfter(filter.to())) {
            for (int year = filter.from().getYear(); year <= filter.to().getYear(); year++) {
                buckets.put(String.valueOf(year), new TrendBucket());
            }
        }
    }

    private String trendKey(LocalDate date, String groupBy) {
        return switch (groupBy) {
            case GROUP_MONTH -> YearMonth.from(date).toString();
            case GROUP_YEAR -> String.valueOf(date.getYear());
            default -> date.toString();
        };
    }

    private DateFilter normalizeDateFilter(LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now();
        LocalDate cleanTo = to == null ? today : to;
        LocalDate cleanFrom = from == null ? cleanTo.minusDays(29) : from;
        if (cleanFrom.isAfter(cleanTo)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Start date must be before or equal to end date.");
        }
        return new DateFilter(cleanFrom, cleanTo);
    }

    private String normalizeGroupBy(String groupBy) {
        if (groupBy == null || groupBy.isBlank()) {
            return GROUP_DAY;
        }
        String clean = groupBy.trim().toLowerCase(Locale.ROOT);
        if (!GROUP_DAY.equals(clean) && !GROUP_MONTH.equals(clean) && !GROUP_YEAR.equals(clean)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "groupBy must be day, month, or year.");
        }
        return clean;
    }

    private void requireTeacher(User user) {
        if (user.getRole() != Role.TEACHER) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    private void requireOwnedCourseIfPresent(Integer courseId, Integer teacherId) {
        if (courseId == null) {
            return;
        }
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));
        if (course.getInstructor() == null || !Objects.equals(course.getInstructor().getId(), teacherId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "You do not have access to this course.");
        }
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private record DateFilter(LocalDate from, LocalDate to) {
    }

    private static class TrendBucket {
        private BigDecimal revenue = BigDecimal.ZERO;
        private final Map<Integer, Boolean> orderIds = new HashMap<>();
    }
}
