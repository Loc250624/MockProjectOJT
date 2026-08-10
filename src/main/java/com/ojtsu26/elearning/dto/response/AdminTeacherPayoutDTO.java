package com.ojtsu26.elearning.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class AdminTeacherPayoutDTO {
    private Integer teacherId;
    private String teacherName;
    private String teacherEmail;
    private BigDecimal eligibleRevenue;
    private BigDecimal teacherShare;
    private String currency;
    private String status;
    private Long paidOrderCount;
    private LocalDateTime latestEligiblePaymentAt;
}
