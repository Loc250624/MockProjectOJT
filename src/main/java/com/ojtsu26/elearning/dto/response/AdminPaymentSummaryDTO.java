package com.ojtsu26.elearning.dto.response;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class AdminPaymentSummaryDTO {
    long totalTransactions;
    long successfulTransactions;
    long pendingTransactions;
    long failedTransactions;
    long refundedTransactions;
    BigDecimal successfulAmount;
}
