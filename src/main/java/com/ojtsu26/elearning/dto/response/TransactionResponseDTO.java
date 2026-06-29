package com.ojtsu26.elearning.dto.response;

import lombok.Data;
import com.ojtsu26.elearning.model.enums.*;

@Data
public class TransactionResponseDTO {
    private Integer id;

    private java.math.BigDecimal amount;

    private PaymentMethod paymentMethod;

    private String transactionRef;

    private TransactionStatus status;

    private String webhookResponse;

    private java.time.LocalDateTime createdAt;

    private java.time.LocalDateTime updatedAt;

    private Integer studentId;

    private Integer courseId;
}
