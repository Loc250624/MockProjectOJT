package com.ojtsu26.elearning.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ojtsu26.elearning.model.entity.*;
import com.ojtsu26.elearning.model.enums.*;
import com.ojtsu26.elearning.repository.*;
import com.ojtsu26.elearning.dto.response.PaymentResponse;
import com.ojtsu26.elearning.config.PaymentGatewayProperties;
import com.ojtsu26.elearning.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    private List<PaymentProvider> providers;

    @Mock
    private PaymentProvider paymentProvider;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private RefundTransactionRepository refundTransactionRepository;

    @Mock
    private CourseEnrollmentService courseEnrollmentService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private PaymentGatewayProperties properties;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Order order;
    private Transaction transaction;
    private User student;
    private Course course;

    @BeforeEach
    void setUp() {
        student = User.builder().id(1).email("student@elearning.com").status(UserStatus.ACTIVE).build();
        course = Course.builder().id(101).title("Test Course").price(new BigDecimal("99.00")).build();
        
        OrderItem item = OrderItem.builder().course(course).courseName("Test Course").unitPrice(new BigDecimal("99.00")).build();
        List<OrderItem> items = new ArrayList<>();
        items.add(item);

        order = Order.builder()
                .id(10)
                .orderCode("ORD123")
                .user(student)
                .status(OrderStatus.PENDING)
                .paidAmount(new BigDecimal("2475000"))
                .paymentMethod(PaymentMethod.MOMO) // Explicitly set to avoid lookup failures
                .items(items)
                .build();

        transaction = Transaction.builder()
                .id(50)
                .order(order)
                .transactionRef("MOMO_REF_1")
                .status(TransactionStatus.PENDING)
                .amount(new BigDecimal("2475000"))
                .student(student)
                .course(course)
                .build();
                
        lenient().when(paymentProvider.getMethod()).thenReturn(PaymentMethod.MOMO);
        paymentService = new PaymentServiceImpl(
                Collections.singletonList(paymentProvider),
                orderRepository,
                transactionRepository,
                refundTransactionRepository,
                courseEnrollmentService,
                notificationService,
                properties
        );
    }

    @Test
    void processWebhook_SignatureVerificationFails_ThrowsException() {
        // Arrange
        Map<String, String> params = new HashMap<>();
        params.put("orderId", "ORD123");
        when(orderRepository.findByOrderCode("ORD123")).thenReturn(Optional.of(order));
        when(paymentProvider.verifyWebhookSignature(params)).thenReturn(false);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            paymentService.processWebhook(PaymentMethod.MOMO, params)
        );
        assertEquals(OrderStatus.FAILED, order.getStatus());
    }

    @Test
    void processWebhook_Success() {
        // Arrange
        Map<String, String> params = new HashMap<>();
        params.put("orderId", "ORD123");
        params.put("transId", "MOMO9999");
        params.put("resultCode", "0");
        params.put("message", "Success");

        when(paymentProvider.verifyWebhookSignature(params)).thenReturn(true);
        when(orderRepository.findByOrderCode("ORD123")).thenReturn(Optional.of(order));
        when(transactionRepository.findByOrderId(10)).thenReturn(Collections.singletonList(transaction));
        // Act
        paymentService.processWebhook(PaymentMethod.MOMO, params);

        // Assert
        assertEquals(OrderStatus.PAID, order.getStatus());
        assertEquals(TransactionStatus.SUCCESS, transaction.getStatus());
        assertEquals("MOMO9999", transaction.getTransactionRef());

        verify(orderRepository).save(order);
        verify(transactionRepository).save(transaction);
        verify(courseEnrollmentService).activateEnrollmentAfterVerifiedPayment(order, transaction);
    }

    @Test
    void processWebhook_IdempotentCheck_SkipPaidOrder() {
        // Arrange
        order.setStatus(OrderStatus.PAID);
        Map<String, String> params = new HashMap<>();
        params.put("orderId", "ORD123");

        when(orderRepository.findByOrderCode("ORD123")).thenReturn(Optional.of(order));

        // Act
        paymentService.processWebhook(PaymentMethod.MOMO, params);

        // Assert: no repository saves should happen
        verify(orderRepository, never()).save(any(Order.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(courseEnrollmentService, never()).activateEnrollmentAfterVerifiedPayment(any(Order.class), any(Transaction.class));
    }

    @Test
    void processWebhook_DuplicateEnrollment_DelegatesToIdempotentEnrollmentService() {
        // Arrange
        Map<String, String> params = new HashMap<>();
        params.put("orderId", "ORD123");
        params.put("transId", "MOMO9999");
        params.put("resultCode", "0");

        when(paymentProvider.verifyWebhookSignature(params)).thenReturn(true);
        when(orderRepository.findByOrderCode("ORD123")).thenReturn(Optional.of(order));
        when(transactionRepository.findByOrderId(10)).thenReturn(Collections.singletonList(transaction));
        // Act
        paymentService.processWebhook(PaymentMethod.MOMO, params);

        // Assert
        assertEquals(OrderStatus.PAID, order.getStatus());
        assertEquals(TransactionStatus.SUCCESS, transaction.getStatus());
        
        verify(orderRepository).save(order);
        verify(transactionRepository).save(transaction);
        verify(courseEnrollmentService).activateEnrollmentAfterVerifiedPayment(order, transaction);
    }

    @Test
    void processWebhook_InvalidOrder_ThrowsException() {
        // Arrange
        Map<String, String> params = new HashMap<>();
        params.put("orderId", "ORD_UNKNOWN");
        when(orderRepository.findByOrderCode("ORD_UNKNOWN")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            paymentService.processWebhook(PaymentMethod.MOMO, params)
        );
    }

    @Test
    void processWebhook_EnrollmentSaveFails_PropagatesException() {
        // Arrange
        Map<String, String> params = new HashMap<>();
        params.put("orderId", "ORD123");
        params.put("resultCode", "0");

        when(paymentProvider.verifyWebhookSignature(params)).thenReturn(true);
        when(orderRepository.findByOrderCode("ORD123")).thenReturn(Optional.of(order));
        when(transactionRepository.findByOrderId(10)).thenReturn(Collections.singletonList(transaction));
        doThrow(new RuntimeException("DB error"))
                .when(courseEnrollmentService).activateEnrollmentAfterVerifiedPayment(order, transaction);

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
            paymentService.processWebhook(PaymentMethod.MOMO, params)
        );
    }

    @Test
    void refundOrder_Success() {
        // Arrange
        order.setStatus(OrderStatus.PAID);
        transaction.setStatus(TransactionStatus.SUCCESS);

        when(orderRepository.findById(10)).thenReturn(Optional.of(order));
        when(refundTransactionRepository.findByOrderId(10)).thenReturn(Collections.emptyList());
        when(refundTransactionRepository.save(any(RefundTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.findByOrderId(10)).thenReturn(Collections.singletonList(transaction));
        
        PaymentResponse refundResponse = PaymentResponse.builder()
                .success(true)
                .errorMessage("PROVIDER_REF_999")
                .rawResponse("{\"resultCode\":0}")
                .build();
        when(paymentProvider.refundPayment(anyString(), anyLong(), anyString(), anyString())).thenReturn(refundResponse);

        // Act
        paymentService.refundOrder(10, "Reason");

        // Assert
        assertEquals(OrderStatus.REFUNDED, order.getStatus());
        verify(orderRepository).save(order);
        verify(refundTransactionRepository, times(2)).save(any(RefundTransaction.class));
    }

    @Test
    void refundOrder_ProviderFailure_UpdatesRefundToFailed() {
        // Arrange
        order.setStatus(OrderStatus.PAID);
        transaction.setStatus(TransactionStatus.SUCCESS);

        when(orderRepository.findById(10)).thenReturn(Optional.of(order));
        when(refundTransactionRepository.findByOrderId(10)).thenReturn(Collections.emptyList());
        when(refundTransactionRepository.save(any(RefundTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.findByOrderId(10)).thenReturn(Collections.singletonList(transaction));

        PaymentResponse refundResponse = PaymentResponse.builder()
                .success(false)
                .errorMessage("Out of balance")
                .rawResponse("{\"resultCode\":1001}")
                .build();
        when(paymentProvider.refundPayment(anyString(), anyLong(), anyString(), anyString())).thenReturn(refundResponse);

        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
            paymentService.refundOrder(10, "Reason")
        );

        // Order remains PAID
        assertEquals(OrderStatus.PAID, order.getStatus());
        verify(refundTransactionRepository, times(2)).save(any(RefundTransaction.class));
    }

    @Test
    void refundOrder_DuplicateRefund_ThrowsException() {
        // Arrange
        order.setStatus(OrderStatus.PAID);
        RefundTransaction existingSuccess = RefundTransaction.builder().status(RefundStatus.SUCCESS).build();

        when(orderRepository.findById(10)).thenReturn(Optional.of(order));
        when(refundTransactionRepository.findByOrderId(10)).thenReturn(Collections.singletonList(existingSuccess));

        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
            paymentService.refundOrder(10, "Reason")
        );
    }

    @Test
    void refundOrder_UnpaidOrder_ThrowsException() {
        // Arrange
        order.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById(10)).thenReturn(Optional.of(order));

        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
            paymentService.refundOrder(10, "Reason")
        );
    }

    @Test
    void refundOrder_UnknownOrder_ThrowsException() {
        // Arrange
        when(orderRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            paymentService.refundOrder(999, "Reason")
        );
    }
}
