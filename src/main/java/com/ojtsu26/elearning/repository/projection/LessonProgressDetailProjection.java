package com.ojtsu26.elearning.repository.projection;

import com.ojtsu26.elearning.model.enums.LessonType;

import java.time.LocalDateTime;

public interface LessonProgressDetailProjection {
    Integer getLessonId();
    String getTitle();
    LessonType getType();
    Integer getOrderIndex();
    Boolean getCompleted();
    LocalDateTime getCompletedAt();
    LocalDateTime getLastAccessedAt();
    Integer getWatchedSeconds();
}
