package com.ojtsu26.elearning.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TeacherStudentProgressDetailDTO {
    private Integer courseId;
    private Integer enrollmentId;
    private Integer studentId;
    private String studentName;
    private String studentEmail;
    private String avatarUrl;
    private BigDecimal progressPercentage;
    private String progressState;
    private Integer completedLessons;
    private Integer totalLessons;
    private LocalDateTime enrolledAt;
    private LocalDateTime lastActivityAt;
    private LocalDateTime completedAt;
    private long assessmentLessons;
    private long passedAssessments;
    private boolean assessmentSummaryAvailable;
    private List<TeacherProgressLessonDTO> lessons;
}
