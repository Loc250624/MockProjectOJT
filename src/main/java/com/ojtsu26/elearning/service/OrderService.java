package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.model.entity.Order;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.PaymentMethod;

public interface OrderService {
    Order createOrder(User student, Integer courseId, PaymentMethod paymentMethod);
    void expirePendingOrders();
}
