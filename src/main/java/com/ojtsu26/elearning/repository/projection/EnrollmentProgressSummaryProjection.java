package com.ojtsu26.elearning.repository.projection;

import java.time.LocalDateTime;

public interface EnrollmentProgressSummaryProjection {
    Integer getEnrollmentId();
    Long getCompletedLessons();
    LocalDateTime getLastActivityAt();
}
