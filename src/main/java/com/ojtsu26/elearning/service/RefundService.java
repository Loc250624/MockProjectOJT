package com.ojtsu26.elearning.service;

public interface RefundService {
    void processRefund(Integer orderId, String reason);
}
