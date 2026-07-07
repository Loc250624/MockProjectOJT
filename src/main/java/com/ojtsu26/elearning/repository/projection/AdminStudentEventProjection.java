package com.ojtsu26.elearning.repository.projection;

import java.time.LocalDateTime;

public interface AdminStudentEventProjection {
    Integer getStudentId();
    LocalDateTime getOccurredAt();
}
