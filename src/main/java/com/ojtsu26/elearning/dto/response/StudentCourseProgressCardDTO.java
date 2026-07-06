package com.ojtsu26.elearning.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class StudentCourseProgressCardDTO {
    private Integer courseId;
    private String courseTitle;
    private String instructorName;
    private String thumbnailUrl;
    private LocalDateTime enrolledAt;
    private Integer resumeLessonId;
    private Integer completedLessons;
    private Integer totalLessons;
    private BigDecimal progressPercentage;
    private Boolean completed;
}
