package com.ojtsu26.elearning.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TeacherCourseStudentDTO {
    private Integer enrollmentId;
    private Integer studentId;
    private String studentName;
    private String studentEmail;
    private String avatarUrl;
    private String enrollmentStatus;
    private LocalDateTime enrolledAt;
    private BigDecimal progressPercentage;
    private Integer completedLessons;
    private Integer totalLessons;
    private String progressState;
    private LocalDateTime lastActivityAt;
    private Boolean courseCompleted;
}
