package com.ojtsu26.elearning.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TeacherDashboardStatsDTO {
    private long activeStudents;
    private String activeStudentsChangeText;
    private String activeStudentsChangeClass;
    private BigDecimal averageCompletionRate;
    private String completionRateChangeText;
    private String completionRateChangeClass;
    private BigDecimal totalRevenueMtd;
    private String revenueCurrencyCode;
    private String totalRevenueMtdDisplay;
    private String revenueChangeText;
    private String revenueChangeClass;
}
