package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.model.entity.*;
import com.ojtsu26.elearning.model.enums.*;
import com.ojtsu26.elearning.repository.*;
import com.ojtsu26.elearning.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final TransactionRepository transactionRepository;
    private final com.ojtsu26.elearning.config.PaymentGatewayProperties paymentGatewayProperties;

    @Override
    @Transactional
    public Order createOrder(User student, Integer courseId, PaymentMethod paymentMethod) {
        // Fetch course
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));

        // Validations
        if (course.getStatus() != CourseStatus.APPROVED) {
            throw new IllegalStateException("Course is not approved or available for purchase");
        }

        if (student.getStatus() == UserStatus.BLOCKED) {
            throw new IllegalStateException("Student account is blocked");
        }

        // Check ownership
        if (courseEnrollmentRepository.existsByStudentIdAndCourseId(student.getId(), courseId)) {
            throw new IllegalStateException("You are already enrolled in this course");
        }

        // Check duplicate pending order
        boolean hasPending = !orderRepository.findByStudentIdAndCourseIdAndStatus(student.getId(), courseId, OrderStatus.PENDING).isEmpty();
        if (hasPending) {
            throw new IllegalStateException("You already have a pending order for this course");
        }

        // Generate unique order code
        String orderCode = "ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        BigDecimal price = course.getPrice() != null ? course.getPrice() : BigDecimal.ZERO;
        BigDecimal exchangeRate = paymentGatewayProperties.getExchangeRate() != null 
                ? paymentGatewayProperties.getExchangeRate() 
                : new BigDecimal("25000");
        BigDecimal paidAmount = price.multiply(exchangeRate);

        Order order = Order.builder()
                .orderCode(orderCode)
                .user(student)
                .totalAmount(price)
                .currency("USD")
                .exchangeRate(exchangeRate)
                .paidAmount(paidAmount)
                .status(OrderStatus.PENDING)
                .paymentMethod(paymentMethod)
                .createdAt(LocalDateTime.now())
                .expiredAt(LocalDateTime.now().plusMinutes(15))
                .items(new ArrayList<>())
                .build();

        OrderItem item = OrderItem.builder()
                .order(order)
                .course(course)
                .courseName(course.getTitle())
                .unitPrice(price)
                .build();

        order.getItems().add(item);
        Order savedOrder = orderRepository.save(order);

        // Create initial transaction record
        Transaction transaction = Transaction.builder()
                .amount(paidAmount)
                .paymentMethod(paymentMethod)
                .transactionRef(orderCode)
                .status(TransactionStatus.PENDING)
                .webhookResponse("{}")
                .student(student)
                .course(course)
                .order(savedOrder)
                .build();

        transactionRepository.save(transaction);

        return savedOrder;
    }

    @Override
    @Transactional
    public void expirePendingOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<Order> expiredOrders = orderRepository.findByStatusAndExpiredAtBefore(OrderStatus.PENDING, now);

        if (expiredOrders.isEmpty()) {
            log.info("No expired pending orders found.");
            return;
        }

        log.info("Found {} expired pending orders to process.", expiredOrders.size());
        for (Order order : expiredOrders) {
            order.setStatus(OrderStatus.EXPIRED);
            orderRepository.save(order);

            List<Transaction> txns = transactionRepository.findByOrderId(order.getId());
            for (Transaction txn : txns) {
                if (txn.getStatus() == TransactionStatus.PENDING) {
                    txn.setStatus(TransactionStatus.FAILED);
                    transactionRepository.save(txn);
                }
            }
            log.info("Expired order code: {}", order.getOrderCode());
        }
        log.info("Successfully expired {} pending orders.", expiredOrders.size());
    }
}
