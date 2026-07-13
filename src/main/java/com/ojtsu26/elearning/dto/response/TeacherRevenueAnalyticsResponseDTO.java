package com.ojtsu26.elearning.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherRevenueAnalyticsResponseDTO {
    private LocalDate from;
    private LocalDate to;
    private String groupBy;
    private Integer courseId;
    private BigDecimal totalRevenue;
    private long paidOrderCount;
    private long enrollmentCount;
    private long studentCount;
    private TeacherRevenueCourseDTO bestSellingCourse;
    private List<TeacherRevenueTrendPointDTO> trend;
}
