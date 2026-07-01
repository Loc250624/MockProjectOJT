package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.response.PaymentResponse;
import com.ojtsu26.elearning.model.entity.Order;
import com.ojtsu26.elearning.model.enums.PaymentMethod;
import java.util.Map;

public interface PaymentService {
    PaymentResponse initiatePayment(Order order);
    boolean verifyWebhookSignature(PaymentMethod method, Map<String, String> params);
    void processWebhook(PaymentMethod method, Map<String, String> params);
    void refundOrder(Integer orderId, String reason);
}
