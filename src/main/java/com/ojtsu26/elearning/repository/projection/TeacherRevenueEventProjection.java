package com.ojtsu26.elearning.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface TeacherRevenueEventProjection {
    Integer getOrderId();
    LocalDateTime getCreatedAt();
    Integer getCourseId();
    String getCourseTitle();
    BigDecimal getRevenue();
}
