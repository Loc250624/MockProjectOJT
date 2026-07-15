package com.ojtsu26.elearning.repository.projection;

import java.math.BigDecimal;

public interface AdminRevenueBucketProjection {
    Integer getBucketYear();
    Integer getBucketMonth();
    Integer getBucketDay();
    BigDecimal getRevenue();
    Long getPaidOrderCount();
}
