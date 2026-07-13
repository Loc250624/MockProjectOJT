package com.ojtsu26.elearning.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface AdminRevenueEventProjection {
    Integer getOrderId();
    LocalDateTime getCreatedAt();
    BigDecimal getRevenue();
}
