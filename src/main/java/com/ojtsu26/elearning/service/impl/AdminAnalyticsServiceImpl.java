package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.dto.response.AdminDashboardOverviewDTO;
import com.ojtsu26.elearning.dto.response.AdminRevenueAnalyticsDTO;
import com.ojtsu26.elearning.dto.response.AdminRevenueTrendPointDTO;
import com.ojtsu26.elearning.dto.response.AdminStudentAnalyticsDTO;
import com.ojtsu26.elearning.dto.response.AdminStudentAnalyticsPointDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.exception.ErrorCode;
import com.ojtsu26.elearning.model.enums.OrderStatus;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.repository.CourseEnrollmentRepository;
import com.ojtsu26.elearning.repository.CourseRepository;
import com.ojtsu26.elearning.repository.LessonProgressRepository;
import com.ojtsu26.elearning.repository.OrderItemRepository;
import com.ojtsu26.elearning.repository.OrderRepository;
import com.ojtsu26.elearning.repository.UserRepository;
import com.ojtsu26.elearning.repository.projection.AdminRevenueBucketProjection;
import com.ojtsu26.elearning.repository.projection.AdminStudentEventProjection;
import com.ojtsu26.elearning.service.AdminAnalyticsService;
import com.ojtsu26.elearning.service.CurrencyDisplayService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminAnalyticsServiceImpl implements AdminAnalyticsService {

    private static final String GROUP_DAY = "day";
    private static final String GROUP_MONTH = "month";
    private static final String GROUP_YEAR = "year";
    private static final ZoneId ANALYTICS_ZONE = ZoneId.systemDefault();
    private static final String ACTIVE_STUDENT_METHOD = "Users with role STUDENT who have lesson progress activity in the selected date range, using LessonProgress.lastAccessedAt with lastUpdatedAt fallback. Dates are interpreted in the application time zone.";
    private static final String REVENUE_RECOGNITION_METHOD = "Gross order-item revenue from orders with status PAID, recognized by Order.createdAt in the application time zone. Refunded orders are excluded by status.";

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final CurrencyDisplayService currencyDisplayService;

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardOverviewDTO getDashboardOverview(LocalDate from, LocalDate to) {
        DateFilter filter = normalizeDateFilter(from, to);
        LocalDateTime fromDateTime = filter.from().atStartOfDay();
        LocalDateTime toDateTime = filter.to().plusDays(1).atStartOfDay();

        return AdminDashboardOverviewDTO.builder()
                .from(filter.from())
                .to(filter.to())
                .totalUsers(userRepository.count())
                .totalStudents(userRepository.countByRole(Role.STUDENT))
                .totalTeachers(userRepository.countByRole(Role.TEACHER))
                .totalCourses(courseRepository.count())
                .totalEnrollments(enrollmentRepository.count())
                .paidOrderCount(orderRepository.countByStatus(OrderStatus.PAID))
                .currency(currencyDisplayService.getDisplayCurrency())
                .totalRevenue(displayMoney(orderItemRepository.sumPaidRevenue(OrderStatus.PAID)))
                .newStudents(userRepository.countByRoleAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(Role.STUDENT, fromDateTime, toDateTime))
                .activeStudents(lessonProgressRepository.countActiveStudentsForAnalytics(fromDateTime, toDateTime, Role.STUDENT))
                .activeStudentMethod(ACTIVE_STUDENT_METHOD)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminStudentAnalyticsDTO getStudentAnalytics(LocalDate from, LocalDate to, String groupBy) {
        DateFilter filter = normalizeDateFilter(from, to);
        String cleanGroupBy = normalizeGroupBy(groupBy);
        LocalDateTime fromDateTime = filter.from().atStartOfDay();
        LocalDateTime toDateTime = filter.to().plusDays(1).atStartOfDay();
        List<AdminStudentEventProjection> newStudentEvents = userRepository.findNewStudentEvents(Role.STUDENT, fromDateTime, toDateTime);
        List<AdminStudentEventProjection> activeStudentEvents = lessonProgressRepository.findActiveStudentEventsForAnalytics(fromDateTime, toDateTime, Role.STUDENT);

        List<AdminStudentAnalyticsPointDTO> trend = buildStudentTrend(filter, cleanGroupBy, newStudentEvents, activeStudentEvents);

        return AdminStudentAnalyticsDTO.builder()
                .from(filter.from())
                .to(filter.to())
                .timeZone(ANALYTICS_ZONE.getId())
                .groupBy(cleanGroupBy)
                .newStudents(countDistinctStudents(newStudentEvents))
                .activeStudents(countDistinctStudents(activeStudentEvents))
                .activeStudentMethod(ACTIVE_STUDENT_METHOD)
                .trend(trend)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminRevenueAnalyticsDTO getRevenueAnalytics(LocalDate from, LocalDate to, String groupBy) {
        DateFilter filter = normalizeDateFilter(from, to);
        String cleanGroupBy = normalizeGroupBy(groupBy);
        LocalDateTime fromDateTime = filter.from().atStartOfDay();
        LocalDateTime toDateTime = filter.to().plusDays(1).atStartOfDay();
        List<AdminRevenueTrendPointDTO> trend = buildRevenueTrend(filter, cleanGroupBy,
                findRevenueBuckets(cleanGroupBy, fromDateTime, toDateTime));

        return AdminRevenueAnalyticsDTO.builder()
                .from(filter.from())
                .to(filter.to())
                .timeZone(ANALYTICS_ZONE.getId())
                .groupBy(cleanGroupBy)
                .revenueRecognitionMethod(REVENUE_RECOGNITION_METHOD)
                .currency(currencyDisplayService.getDisplayCurrency())
                .totalRevenue(sumTrendRevenue(trend))
                .paidOrderCount(sumTrendPaidOrders(trend))
                .trend(trend)
                .build();
    }

    private List<AdminRevenueBucketProjection> findRevenueBuckets(String groupBy,
                                                                  LocalDateTime fromDateTime,
                                                                  LocalDateTime toDateTime) {
        return switch (groupBy) {
            case GROUP_MONTH -> orderItemRepository.findPaidRevenueBucketsByMonth(OrderStatus.PAID, fromDateTime, toDateTime);
            case GROUP_YEAR -> orderItemRepository.findPaidRevenueBucketsByYear(OrderStatus.PAID, fromDateTime, toDateTime);
            default -> orderItemRepository.findPaidRevenueBucketsByDay(OrderStatus.PAID, fromDateTime, toDateTime);
        };
    }

    private List<AdminRevenueTrendPointDTO> buildRevenueTrend(DateFilter filter,
                                                              String groupBy,
                                                              List<AdminRevenueBucketProjection> aggregates) {
        Map<String, AdminRevenueTrendPointDTO> buckets = new LinkedHashMap<>();
        initializeBucketKeys(filter, groupBy).forEach(key -> buckets.put(key, AdminRevenueTrendPointDTO.builder()
                .period(key)
                .revenue(currencyDisplayService.money(BigDecimal.ZERO, currencyDisplayService.getDisplayCurrency()))
                .paidOrderCount(0)
                .build()));
        aggregates.forEach(bucket -> {
            String key = revenueBucketKey(bucket, groupBy);
            buckets.put(key, AdminRevenueTrendPointDTO.builder()
                    .period(key)
                    .revenue(displayMoney(bucket.getRevenue()))
                    .paidOrderCount(bucket.getPaidOrderCount() == null ? 0L : bucket.getPaidOrderCount())
                    .build());
        });

        return List.copyOf(buckets.values());
    }

    private String revenueBucketKey(AdminRevenueBucketProjection bucket, String groupBy) {
        int year = bucket.getBucketYear() == null ? 0 : bucket.getBucketYear();
        if (GROUP_YEAR.equals(groupBy)) {
            return String.valueOf(year);
        }
        int month = bucket.getBucketMonth() == null ? 1 : bucket.getBucketMonth();
        if (GROUP_MONTH.equals(groupBy)) {
            return YearMonth.of(year, month).toString();
        }
        int day = bucket.getBucketDay() == null ? 1 : bucket.getBucketDay();
        return LocalDate.of(year, month, day).toString();
    }

    private BigDecimal sumTrendRevenue(List<AdminRevenueTrendPointDTO> trend) {
        return currencyDisplayService.money(trend.stream()
                .map(AdminRevenueTrendPointDTO::getRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add), currencyDisplayService.getDisplayCurrency());
    }

    private long sumTrendPaidOrders(List<AdminRevenueTrendPointDTO> trend) {
        return trend.stream()
                .mapToLong(AdminRevenueTrendPointDTO::getPaidOrderCount)
                .sum();
    }

    private List<AdminStudentAnalyticsPointDTO> buildStudentTrend(DateFilter filter,
                                                                  String groupBy,
                                                                  List<AdminStudentEventProjection> newStudentEvents,
                                                                  List<AdminStudentEventProjection> activeStudentEvents) {
        Map<String, StudentBucket> buckets = new LinkedHashMap<>();
        initializeStudentBuckets(filter, groupBy, buckets);
        newStudentEvents.forEach(event -> {
            String key = trendKey(event.getOccurredAt().toLocalDate(), groupBy);
            buckets.computeIfAbsent(key, ignored -> new StudentBucket()).newStudentIds.add(event.getStudentId());
        });
        activeStudentEvents.forEach(event -> {
            String key = trendKey(event.getOccurredAt().toLocalDate(), groupBy);
            buckets.computeIfAbsent(key, ignored -> new StudentBucket()).activeStudentIds.add(event.getStudentId());
        });

        return buckets.entrySet().stream()
                .map(entry -> AdminStudentAnalyticsPointDTO.builder()
                        .period(entry.getKey())
                        .newStudents(entry.getValue().newStudentIds.size())
                        .activeStudents(entry.getValue().activeStudentIds.size())
                        .build())
                .toList();
    }

    private void initializeStudentBuckets(DateFilter filter, String groupBy, Map<String, StudentBucket> buckets) {
        initializeBucketKeys(filter, groupBy).forEach(key -> buckets.put(key, new StudentBucket()));
    }

    private List<String> initializeBucketKeys(DateFilter filter, String groupBy) {
        if (GROUP_DAY.equals(groupBy) && filter.from().plusDays(370).isAfter(filter.to())) {
            Map<String, Boolean> keys = new LinkedHashMap<>();
            for (LocalDate current = filter.from(); !current.isAfter(filter.to()); current = current.plusDays(1)) {
                keys.put(current.toString(), true);
            }
            return List.copyOf(keys.keySet());
        }
        if (GROUP_MONTH.equals(groupBy) && YearMonth.from(filter.from()).plusMonths(60).isAfter(YearMonth.from(filter.to()))) {
            Map<String, Boolean> keys = new LinkedHashMap<>();
            for (YearMonth current = YearMonth.from(filter.from()); !current.isAfter(YearMonth.from(filter.to())); current = current.plusMonths(1)) {
                keys.put(current.toString(), true);
            }
            return List.copyOf(keys.keySet());
        }
        if (GROUP_YEAR.equals(groupBy) && filter.from().plusYears(20).isAfter(filter.to())) {
            Map<String, Boolean> keys = new LinkedHashMap<>();
            for (int year = filter.from().getYear(); year <= filter.to().getYear(); year++) {
                keys.put(String.valueOf(year), true);
            }
            return List.copyOf(keys.keySet());
        }
        return List.of();
    }

    private String trendKey(LocalDate date, String groupBy) {
        return switch (groupBy) {
            case GROUP_MONTH -> YearMonth.from(date).toString();
            case GROUP_YEAR -> String.valueOf(date.getYear());
            default -> date.toString();
        };
    }

    private long countDistinctStudents(List<AdminStudentEventProjection> events) {
        return events.stream()
                .map(AdminStudentEventProjection::getStudentId)
                .distinct()
                .count();
    }

    private DateFilter normalizeDateFilter(LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now(ANALYTICS_ZONE);
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

    private BigDecimal displayMoney(BigDecimal usdValue) {
        return currencyDisplayService.convertUsdToDisplay(usdValue);
    }

    private record DateFilter(LocalDate from, LocalDate to) {
    }

    private static class StudentBucket {
        private final Set<Integer> newStudentIds = new HashSet<>();
        private final Set<Integer> activeStudentIds = new HashSet<>();
    }
}
