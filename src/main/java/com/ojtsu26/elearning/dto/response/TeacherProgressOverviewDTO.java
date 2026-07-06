package com.ojtsu26.elearning.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TeacherProgressOverviewDTO {
    private Integer courseId;
    private String courseTitle;
    private long totalStudents;
    private long activeStudents;
    private long notStartedStudents;
    private long inProgressStudents;
    private long completedStudents;
    private long totalRequiredLessons;
    private BigDecimal averageProgress;
    private BigDecimal completionRate;
    private LocalDateTime latestActivityAt;
    private boolean assessmentSummaryAvailable;
    private long assessmentLessons;
}
