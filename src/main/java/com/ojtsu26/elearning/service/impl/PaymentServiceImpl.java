package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.dto.request.PaymentRequest;
import com.ojtsu26.elearning.dto.response.PaymentResponse;
import com.ojtsu26.elearning.model.entity.*;
import com.ojtsu26.elearning.model.enums.OrderStatus;
import com.ojtsu26.elearning.model.enums.PaymentMethod;
import com.ojtsu26.elearning.model.enums.TransactionStatus;
import com.ojtsu26.elearning.model.enums.RefundStatus;
import com.ojtsu26.elearning.repository.OrderRepository;
import com.ojtsu26.elearning.repository.TransactionRepository;
import com.ojtsu26.elearning.repository.RefundTransactionRepository;
import com.ojtsu26.elearning.service.CourseEnrollmentService;
import com.ojtsu26.elearning.service.NotificationService;
import com.ojtsu26.elearning.service.PaymentProvider;
import com.ojtsu26.elearning.service.PaymentService;
import com.ojtsu26.elearning.config.PaymentGatewayProperties;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final List<PaymentProvider> providers;
    private final OrderRepository orderRepository;
    private final TransactionRepository transactionRepository;
    private final RefundTransactionRepository refundTransactionRepository;
    private final CourseEnrollmentService courseEnrollmentService;
    private final NotificationService notificationService;
    private final PaymentGatewayProperties properties;

    @Override
    public PaymentResponse initiatePayment(Order order) {
        PaymentProvider provider = providers.stream()
                .filter(p -> p.getMethod() == order.getPaymentMethod())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No provider found for payment method: " + order.getPaymentMethod()));

        String returnUrl = "";
        String notifyUrl = "";
        
        if (order.getPaymentMethod() == PaymentMethod.MOMO) {
            returnUrl = properties.getMomo().getReturnUrl();
            notifyUrl = properties.getMomo().getIpnUrl();
        } else if (order.getPaymentMethod() == PaymentMethod.VNPAY) {
            returnUrl = properties.getVnpay().getReturnUrl();
            notifyUrl = properties.getVnpay().getIpnUrl();
        }

        PaymentRequest request = PaymentRequest.builder()
                .orderCode(order.getOrderCode())
                .amount(order.getPaidAmount())
                .orderInfo("Purchase course: " + order.getItems().get(0).getCourseName())
                .returnUrl(returnUrl)
                .notifyUrl(notifyUrl)
                .ipAddress(getClientIp())
                .build();

        return provider.initiatePayment(request);
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getRemoteAddr();
                }
                if (ip != null && ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip != null ? ip : "127.0.0.1";
            }
        } catch (Exception e) {
            log.warn("Failed to get request IP, fallback to 127.0.0.1", e);
        }
        return "127.0.0.1";
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
        String resultCodeValue = params.get("resultCode");
        int resultCode = -1;
        if (resultCodeValue != null && !resultCodeValue.isBlank()) {
            try {
                resultCode = Double.valueOf(resultCodeValue).intValue();
            } catch (NumberFormatException e) {
                log.warn("Failed to parse resultCode from webhook: {}", resultCodeValue);
            }
        }

        // Fetch Order
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with code: " + orderCode));

        // Security Audit Validations for VNPAY
        if (method == PaymentMethod.VNPAY) {
            // 1. Verify Payment Method is VNPAY
            if (order.getPaymentMethod() != PaymentMethod.VNPAY) {
                log.error("Payment method mismatch: expected VNPAY, but order has {}", order.getPaymentMethod());
                throw new IllegalArgumentException("Payment method mismatch");
            }
            
            // 2. Verify currency is VND
            String currCode = params.get("vnp_CurrCode");
            if (!"VND".equalsIgnoreCase(currCode)) {
                log.error("Currency code mismatch: expected VND, but got {}", currCode);
                throw new IllegalArgumentException("Currency code mismatch");
            }

            // 3. Verify amount: vnp_Amount equals Order.paidAmount (multiplied by 100)
            String vnpAmountStr = params.get("vnp_Amount");
            if (vnpAmountStr == null || vnpAmountStr.isEmpty()) {
                log.error("Missing vnp_Amount parameter in callback");
                throw new IllegalArgumentException("Missing amount parameter");
            }
            BigDecimal expectedAmount = order.getPaidAmount().multiply(new BigDecimal("100"));
            BigDecimal receivedAmount = new BigDecimal(vnpAmountStr);
            if (expectedAmount.compareTo(receivedAmount) != 0) {
                log.error("Amount mismatch: expected {}, but received {}", expectedAmount, receivedAmount);
                throw new IllegalArgumentException("Amount mismatch");
            }
        }

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

            if (transaction != null) {
                courseEnrollmentService.activateEnrollmentAfterVerifiedPayment(order, transaction);
                log.info("Enrollment activation completed for verified order {}", order.getOrderCode());
            }
        } else {
            order.setStatus(OrderStatus.FAILED);
            orderRepository.save(order);

            if (transaction != null) {
                transaction.setStatus(TransactionStatus.FAILED);
                transaction.setWebhookResponse(params.toString());
                transactionRepository.save(transaction);
            }
            notificationService.createPaymentFailedNotification(order);
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
