package com.ojtsu26.elearning.repository.projection;

import java.time.LocalDateTime;

public interface StudentDeadlineProjection {
    Integer getAssignmentId();
    String getTitle();
    Integer getCourseId();
    String getCourseTitle();
    LocalDateTime getDueAt();
}
