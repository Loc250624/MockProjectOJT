package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.request.PaymentRequest;
import com.ojtsu26.elearning.dto.response.PaymentResponse;
import com.ojtsu26.elearning.model.enums.PaymentMethod;
import java.util.Map;

public interface PaymentProvider {
    PaymentMethod getMethod();
    PaymentResponse initiatePayment(PaymentRequest request);
    boolean verifyWebhookSignature(Map<String, String> params);
    PaymentResponse refundPayment(String refundCode, long amount, String originalTransId, String reason);
}
