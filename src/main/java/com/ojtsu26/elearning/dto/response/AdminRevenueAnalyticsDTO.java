package com.ojtsu26.elearning.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminRevenueAnalyticsDTO {
    private LocalDate from;
    private LocalDate to;
    private String groupBy;
    private BigDecimal totalRevenue;
    private long paidOrderCount;
    private List<AdminRevenueTrendPointDTO> trend;
}
