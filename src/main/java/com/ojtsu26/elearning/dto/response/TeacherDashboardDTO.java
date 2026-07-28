package com.ojtsu26.elearning.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherDashboardDTO {
    private long totalActiveStudents;
    private int averageCompletionPercent;
    private BigDecimal revenueMonthToDate;
    private String revenueMonthToDateDisplay;
    private List<TeacherCourseDashboardDTO> activeCourses;
}
