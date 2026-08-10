package com.ojtsu26.elearning.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface AdminTeacherPayoutProjection {
    Integer getTeacherId();
    String getTeacherName();
    String getTeacherEmail();
    BigDecimal getEligibleRevenue();
    Long getPaidOrderCount();
    LocalDateTime getLatestEligiblePaymentAt();
}
