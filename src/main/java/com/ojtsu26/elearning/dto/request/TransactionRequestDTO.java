package com.ojtsu26.elearning.dto.request;

import lombok.Data;
import com.ojtsu26.elearning.model.enums.*;

@Data
public class TransactionRequestDTO {
    @jakarta.validation.constraints.NotNull
    private java.math.BigDecimal amount;

    @jakarta.validation.constraints.NotNull
    private PaymentMethod paymentMethod;

    @jakarta.validation.constraints.NotBlank
    private String transactionRef;

    @jakarta.validation.constraints.NotNull
    private TransactionStatus status;

    @jakarta.validation.constraints.NotBlank
    private String webhookResponse;

    @jakarta.validation.constraints.NotNull
    private Integer studentId;

    @jakarta.validation.constraints.NotNull
    private Integer courseId;
}
