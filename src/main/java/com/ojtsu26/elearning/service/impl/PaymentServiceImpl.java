package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.dto.request.PaymentRequest;
import com.ojtsu26.elearning.dto.response.PaymentResponse;
import com.ojtsu26.elearning.model.entity.*;
import com.ojtsu26.elearning.model.enums.OrderStatus;
import com.ojtsu26.elearning.model.enums.PaymentMethod;
import com.ojtsu26.elearning.model.enums.TransactionStatus;
import com.ojtsu26.elearning.model.enums.RefundStatus;
import com.ojtsu26.elearning.repository.CourseEnrollmentRepository;
import com.ojtsu26.elearning.repository.OrderRepository;
import com.ojtsu26.elearning.repository.TransactionRepository;
import com.ojtsu26.elearning.repository.RefundTransactionRepository;
import com.ojtsu26.elearning.service.PaymentProvider;
import com.ojtsu26.elearning.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final List<PaymentProvider> providers;
    private final OrderRepository orderRepository;
    private final TransactionRepository transactionRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final RefundTransactionRepository refundTransactionRepository;

    @Override
    public PaymentResponse initiatePayment(Order order) {
        PaymentProvider provider = providers.stream()
                .filter(p -> p.getMethod() == order.getPaymentMethod())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No provider found for payment method: " + order.getPaymentMethod()));

        PaymentRequest request = PaymentRequest.builder()
                .orderCode(order.getOrderCode())
                .amount(order.getPaidAmount())
                .orderInfo("Purchase course: " + order.getItems().get(0).getCourseName())
                .returnUrl("http://localhost:8080/student/payment-result?orderId=" + order.getOrderCode())
                .notifyUrl("http://localhost:8080/api/payment/webhook")
                .build();

        return provider.initiatePayment(request);
    }

    @Override
    public boolean verifyWebhookSignature(PaymentMethod method, Map<String, String> params) {
        PaymentProvider provider = providers.stream()
                .filter(p -> p.getMethod() == method)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No provider found for payment method: " + method));

        return provider.verifyWebhookSignature(params);
    }

    @Override
    @Transactional
    public void processWebhook(PaymentMethod method, Map<String, String> params) {
        // 1. Signature verification
        if (!verifyWebhookSignature(method, params)) {
            log.error("Webhook signature verification failed for method={}", method);
            throw new IllegalArgumentException("Invalid signature");
        }

        String orderCode = params.get("orderId");
        String transId = params.get("transId");
        String message = params.get("message");
        Number resultCodeNum = null;
        try {
            resultCodeNum = Double.parseDouble(params.get("resultCode"));
        } catch (Exception e) {
            log.warn("Failed to parse resultCode from webhook", e);
        }
        int resultCode = resultCodeNum != null ? resultCodeNum.intValue() : -1;

        // Fetch Order
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with code: " + orderCode));

        // 2. Idempotency validation: check if already processed
        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn("Order {} is already processed (status={}). Skipping.", orderCode, order.getStatus());
            return;
        }

        // Fetch Transaction
        Transaction transaction = transactionRepository.findByOrderId(order.getId())
                .stream().findFirst()
                .orElse(null);

        if (transaction == null) {
            transaction = transactionRepository.findByTransactionRef(orderCode)
                    .orElse(null);
        }

        if (transaction != null && transaction.getStatus() == TransactionStatus.SUCCESS) {
            log.info("Transaction for order {} already completed. Skipping.", orderCode);
            return;
        }

        // 3. Update Transaction & Order status
        if (resultCode == 0) {
            order.setStatus(OrderStatus.PAID);
            orderRepository.save(order);

            if (transaction != null) {
                transaction.setStatus(TransactionStatus.SUCCESS);
                transaction.setTransactionRef(transId);
                transaction.setWebhookResponse(params.toString());
                transactionRepository.save(transaction);
            }

            // 5. Enrollment creation (Inside transactional service)
            User student = order.getUser();
            if (!order.getItems().isEmpty()) {
                Course course = order.getItems().get(0).getCourse();
                if (!courseEnrollmentRepository.existsByStudentIdAndCourseId(student.getId(), course.getId())) {
                    CourseEnrollment enrollment = CourseEnrollment.builder()
                            .student(student)
                            .course(course)
                            .progressPercentage(java.math.BigDecimal.ZERO)
                            .isCompleted(false)
                            .build();
                    courseEnrollmentRepository.save(enrollment);
                    log.info("Enrollment created successfully for student {} and course {}", student.getEmail(), course.getTitle());
                }
            }
        } else {
            order.setStatus(OrderStatus.FAILED);
            orderRepository.save(order);

            if (transaction != null) {
                transaction.setStatus(TransactionStatus.FAILED);
                transaction.setWebhookResponse(params.toString());
                transactionRepository.save(transaction);
            }
        }
    }

    @Override
    @Transactional
    public void refundOrder(Integer orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (order.getStatus() != OrderStatus.PAID) {
            throw new IllegalStateException("Only paid orders can be refunded");
        }

        List<RefundTransaction> existingRefunds = refundTransactionRepository.findByOrderId(orderId);
        boolean alreadyRefunded = existingRefunds.stream()
                .anyMatch(r -> r.getStatus() == RefundStatus.SUCCESS);
        if (alreadyRefunded) {
            throw new IllegalStateException("This order has already been refunded");
        }

        String refundCode = "REF" + System.currentTimeMillis() + java.util.UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        RefundTransaction refundTxn = RefundTransaction.builder()
                .order(order)
                .amount(order.getPaidAmount())
                .status(RefundStatus.PENDING)
                .refundRef(refundCode)
                .reason(reason)
                .createdAt(java.time.LocalDateTime.now())
                .rawResponse("{}")
                .build();

        RefundTransaction savedRefund = refundTransactionRepository.save(refundTxn);

        Transaction successTxn = transactionRepository.findByOrderId(order.getId()).stream()
                .filter(t -> t.getStatus() == TransactionStatus.SUCCESS)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No successful transaction found for order"));

        PaymentProvider provider = providers.stream()
                .filter(p -> p.getMethod() == order.getPaymentMethod())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No provider found for order payment method"));

        PaymentResponse response = provider.refundPayment(refundCode, order.getPaidAmount().longValue(), successTxn.getTransactionRef(), reason);

        if (response.isSuccess()) {
            savedRefund.setStatus(RefundStatus.SUCCESS);
            savedRefund.setProviderRefundId(response.getErrorMessage());
            savedRefund.setRawResponse(response.getRawResponse());
            refundTransactionRepository.save(savedRefund);

            order.setStatus(OrderStatus.REFUNDED);
            orderRepository.save(order);
            log.info("Order {} successfully refunded via gateway. Local reference: {}", order.getOrderCode(), refundCode);
        } else {
            savedRefund.setStatus(RefundStatus.FAILED);
            savedRefund.setRawResponse(response.getRawResponse());
            refundTransactionRepository.save(savedRefund);

            log.error("Gateway refund failed for order {}: {}", order.getOrderCode(), response.getErrorMessage());
            throw new IllegalStateException("Refund failed at payment gateway: " + response.getErrorMessage());
        }
    }
}
