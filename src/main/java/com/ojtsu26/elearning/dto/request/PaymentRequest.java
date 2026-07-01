package com.ojtsu26.elearning.dto.request;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class PaymentRequest {
    private String orderCode;
    private BigDecimal amount;
    private String orderInfo;
    private String returnUrl;
    private String notifyUrl;
}
