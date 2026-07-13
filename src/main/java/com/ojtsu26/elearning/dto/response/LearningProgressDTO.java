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
    private Boolean lessonCompleted;
    private Boolean courseCompleted;
    private String courseStatus;
    private BigDecimal lessonProgressPercentage;
    private Integer watchedSeconds;
    private Integer lastPositionSeconds;
    private Integer maxReachedSeconds;
    private Integer videoDurationSeconds;
    private Integer durationSeconds;
    private Integer nextLessonId;
    private Boolean nextLessonAccessible;
    private String nextLessonLockReason;
    private LocalDateTime completedAt;
    private LocalDateTime lastAccessedAt;
}
