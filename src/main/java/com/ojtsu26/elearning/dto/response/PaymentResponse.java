package com.ojtsu26.elearning.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentResponse {
    private boolean success;
    private String paymentUrl;
    private String rawResponse;
    private String errorMessage;
}
