package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.model.entity.*;
import com.ojtsu26.elearning.model.enums.*;
import com.ojtsu26.elearning.repository.*;
import com.ojtsu26.elearning.service.CurrencyConversionService;
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
    private final CurrencyConversionService currencyConversionService;

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

        // Check duplicate pending order — reuse it but update payment method and extend expiry
        List<Order> pendingOrders = orderRepository.findByStudentIdAndCourseIdAndStatus(student.getId(), courseId, OrderStatus.PENDING);
        if (!pendingOrders.isEmpty()) {
            Order existing = pendingOrders.get(0);
            existing.setPaymentMethod(paymentMethod);
            existing.setExpiredAt(LocalDateTime.now().plusMinutes(15));
            Order savedExisting = orderRepository.save(existing);
            if (savedExisting.getItems() != null) {
                savedExisting.getItems().size(); // Force initialization
            }
            return savedExisting;
        }

        // Generate unique order code
        String orderCode = "ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        // Invariant: Course.price is stored in USD.
        // paidAmount is the VND equivalent sent to payment gateways (e.g. MoMo).
        // Conversion: paidAmount = totalAmount(USD) × exchangeRate(VND/USD), rounded to integer VND.
        BigDecimal price = course.getPrice() != null ? course.getPrice() : BigDecimal.ZERO;
        BigDecimal exchangeRate = currencyConversionService.getExchangeRate();
        BigDecimal paidAmount = currencyConversionService.convertUsdToVnd(price);

        log.info("Creating order: courseId={}, priceUSD={}, exchangeRate={}, paidAmountVND={}",
                courseId, price, exchangeRate, paidAmount);

        Order order = Order.builder()
                .orderCode(orderCode)
                .user(student)
                .totalAmount(price)       // stored in USD (business currency)
                .currency("USD")
                .exchangeRate(exchangeRate)
                .paidAmount(paidAmount)   // stored in VND (gateway settlement currency)
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
                .unitPrice(price)         // unit price in USD
                .build();

        order.getItems().add(item);
        Order savedOrder = orderRepository.save(order);

        // Create initial transaction record (amount in VND — what the gateway actually charges)
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

        if (savedOrder.getItems() != null) {
            savedOrder.getItems().size(); // Force initialization
        }
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
