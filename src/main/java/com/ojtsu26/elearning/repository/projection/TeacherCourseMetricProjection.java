package com.ojtsu26.elearning.repository.projection;

import java.math.BigDecimal;

public interface TeacherCourseMetricProjection {
    Integer getCourseId();
    Long getEnrollmentCount();
    BigDecimal getAverageProgress();
}
