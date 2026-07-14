package com.ojtsu26.elearning.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ojtsu26.elearning.model.enums.PaymentMethod;
import com.ojtsu26.elearning.model.enums.TransactionStatus;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminTransactionDTO {
    Integer id;
    BigDecimal amount;
    String currency;
    PaymentMethod paymentMethod;
    String transactionRef;
    TransactionStatus status;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    Integer studentId;
    String studentName;
    String studentEmail;
    Integer courseId;
    String courseTitle;
    Integer orderId;
    String orderCode;
}
