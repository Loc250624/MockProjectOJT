package com.ojtsu26.elearning.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherCourseDashboardDTO {
    private Integer courseId;
    private String title;
    private String courseLabel;
    private long enrolledCount;
    private int averageCompletionPercent;
    private BigDecimal revenueMonthToDate;
    private String revenueMonthToDateDisplay;
    private String status;
    private String statusClass;
    private String actionLabel;
    private String actionUrl;
}
