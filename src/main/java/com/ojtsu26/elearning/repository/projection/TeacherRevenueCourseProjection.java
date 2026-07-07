package com.ojtsu26.elearning.repository.projection;

import java.math.BigDecimal;

public interface TeacherRevenueCourseProjection {
    Integer getCourseId();
    String getCourseTitle();
    BigDecimal getRevenue();
    Long getPaidOrderCount();
    Long getUnitsSold();
}
