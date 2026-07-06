package com.ojtsu26.elearning.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class LearningProgressDTO {
    private Integer courseId;
    private Integer lessonId;
    private Integer enrollmentId;
    private Integer completedLessons;
    private Integer totalLessons;
    private BigDecimal progressPercentage;
    private Boolean completed;
    private Integer watchedSeconds;
    private Integer videoDurationSeconds;
    private LocalDateTime completedAt;
    private LocalDateTime lastAccessedAt;
}
