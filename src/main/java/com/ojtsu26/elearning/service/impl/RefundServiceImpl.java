package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.service.PaymentService;
import com.ojtsu26.elearning.service.RefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    private final PaymentService paymentService;

    @Override
    public void processRefund(Integer orderId, String reason) {
        paymentService.refundOrder(orderId, reason);
    }
}
