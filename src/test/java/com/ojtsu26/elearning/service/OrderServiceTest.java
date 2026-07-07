package com.ojtsu26.elearning.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ojtsu26.elearning.model.entity.*;
import com.ojtsu26.elearning.model.enums.*;
import com.ojtsu26.elearning.repository.*;
import com.ojtsu26.elearning.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseEnrollmentRepository courseEnrollmentRepository;

    @Mock
    private TransactionRepository transactionRepository;

    // CurrencyConversionService replaces the old PaymentGatewayProperties direct injection
    @Mock
    private CurrencyConversionService currencyConversionService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User student;
    private Course course;

    // Exchange rate: 1 USD = 25,000 VND
    private static final BigDecimal EXCHANGE_RATE = new BigDecimal("25000");
    // Course price in USD
    private static final BigDecimal PRICE_USD = new BigDecimal("99.00");
    // Expected paidAmount in VND: 99.00 × 25000 = 2,475,000
    private static final BigDecimal PAID_AMOUNT_VND = new BigDecimal("2475000");

    @BeforeEach
    void setUp() {
        student = User.builder()
                .id(1)
                .email("student@elearning.com")
                .status(UserStatus.ACTIVE)
                .build();

        course = Course.builder()
                .id(101)
                .title("Test Course")
                .price(PRICE_USD)
                .status(CourseStatus.APPROVED)
                .build();

        // Stub currency conversion: 99.00 USD → 2,475,000 VND
        lenient().when(currencyConversionService.convertUsdToVnd(PRICE_USD)).thenReturn(PAID_AMOUNT_VND);
        lenient().when(currencyConversionService.getExchangeRate()).thenReturn(EXCHANGE_RATE);
    }

    @Test
    void createOrder_Success() {
        // Arrange
        when(courseRepository.findById(101)).thenReturn(Optional.of(course));
        when(courseEnrollmentRepository.existsByStudentIdAndCourseId(1, 101)).thenReturn(false);
        when(orderRepository.findByStudentIdAndCourseIdAndStatus(1, 101, OrderStatus.PENDING))
                .thenReturn(Collections.emptyList());
        when(currencyConversionService.convertUsdToVnd(PRICE_USD)).thenReturn(PAID_AMOUNT_VND);
        when(currencyConversionService.getExchangeRate()).thenReturn(EXCHANGE_RATE);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Order order = orderService.createOrder(student, 101, PaymentMethod.MOMO);

        // Assert
        assertNotNull(order);
        assertEquals(student, order.getUser());
        // totalAmount stored in USD (business currency)
        assertEquals(PRICE_USD, order.getTotalAmount());
        assertEquals("USD", order.getCurrency());
        assertEquals(EXCHANGE_RATE, order.getExchangeRate());
        // paidAmount stored in VND (gateway settlement currency) = 99.00 × 25000 = 2,475,000
        assertEquals(PAID_AMOUNT_VND, order.getPaidAmount());
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals(PaymentMethod.MOMO, order.getPaymentMethod());
        assertFalse(order.getItems().isEmpty());
        assertEquals("Test Course", order.getItems().get(0).getCourseName());
        // Unit price in USD
        assertEquals(PRICE_USD, order.getItems().get(0).getUnitPrice());
        assertTrue(order.getOrderCode().startsWith("ORD"));

        verify(orderRepository).save(any(Order.class));
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void createOrder_DecimalPrice_Success() {
        // Arrange
        BigDecimal decimalPrice = new BigDecimal("99.99");
        BigDecimal expectedPaidAmount = new BigDecimal("2499750"); // 99.99 * 25000 = 2499750
        
        course.setPrice(decimalPrice);
        
        when(courseRepository.findById(101)).thenReturn(Optional.of(course));
        when(courseEnrollmentRepository.existsByStudentIdAndCourseId(1, 101)).thenReturn(false);
        when(orderRepository.findByStudentIdAndCourseIdAndStatus(1, 101, OrderStatus.PENDING))
                .thenReturn(Collections.emptyList());
        when(currencyConversionService.convertUsdToVnd(decimalPrice)).thenReturn(expectedPaidAmount);
        when(currencyConversionService.getExchangeRate()).thenReturn(EXCHANGE_RATE);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Order order = orderService.createOrder(student, 101, PaymentMethod.MOMO);

        // Assert
        assertNotNull(order);
        assertEquals(student, order.getUser());
        assertEquals(decimalPrice, order.getTotalAmount()); // Stored in USD
        assertEquals("USD", order.getCurrency());
        assertEquals(EXCHANGE_RATE, order.getExchangeRate());
        assertEquals(expectedPaidAmount, order.getPaidAmount()); // Stored in VND
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals(PaymentMethod.MOMO, order.getPaymentMethod());
        assertFalse(order.getItems().isEmpty());
        assertEquals("Test Course", order.getItems().get(0).getCourseName());
        assertEquals(decimalPrice, order.getItems().get(0).getUnitPrice());
        assertTrue(order.getOrderCode().startsWith("ORD"));

        verify(orderRepository).save(any(Order.class));
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void createOrder_CourseNotApproved_ThrowsException() {
        // Arrange
        course.setStatus(CourseStatus.DRAFT);
        when(courseRepository.findById(101)).thenReturn(Optional.of(course));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> 
            orderService.createOrder(student, 101, PaymentMethod.MOMO)
        );
    }

    @Test
    void createOrder_CourseNotFound_ThrowsException() {
        // Arrange
        when(courseRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            orderService.createOrder(student, 999, PaymentMethod.MOMO)
        );
    }

    @Test
    void createOrder_InactiveUser_ThrowsException() {
        // Arrange
        student.setStatus(UserStatus.BLOCKED);
        when(courseRepository.findById(101)).thenReturn(Optional.of(course));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> 
            orderService.createOrder(student, 101, PaymentMethod.MOMO)
        );
    }

    @Test
    void createOrder_AlreadyEnrolled_ThrowsException() {
        // Arrange
        when(courseRepository.findById(101)).thenReturn(Optional.of(course));
        when(courseEnrollmentRepository.existsByStudentIdAndCourseId(1, 101)).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> 
            orderService.createOrder(student, 101, PaymentMethod.MOMO)
        );
    }

    @Test
    void createOrder_DuplicatePendingOrder_ReturnsExistingPendingOrder() {
        // Arrange
        Order existingOrder = Order.builder().id(77).status(OrderStatus.PENDING).build();
        when(courseRepository.findById(101)).thenReturn(Optional.of(course));
        when(courseEnrollmentRepository.existsByStudentIdAndCourseId(1, 101)).thenReturn(false);
        when(orderRepository.findByStudentIdAndCourseIdAndStatus(1, 101, OrderStatus.PENDING))
                .thenReturn(Collections.singletonList(existingOrder));
        // The reuse path now updates payment method + expiry and saves the existing order
        when(orderRepository.save(existingOrder)).thenReturn(existingOrder);

        // Act
        Order order = orderService.createOrder(student, 101, PaymentMethod.MOMO);

        // Assert — same order returned, save called once to persist updates, no new transaction
        assertEquals(existingOrder, order);
        verify(orderRepository, times(1)).save(existingOrder);
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void createOrder_TransactionSaveFails_PropagatesException() {
        // Arrange
        when(courseRepository.findById(101)).thenReturn(Optional.of(course));
        when(courseEnrollmentRepository.existsByStudentIdAndCourseId(1, 101)).thenReturn(false);
        when(orderRepository.findByStudentIdAndCourseIdAndStatus(1, 101, OrderStatus.PENDING))
                .thenReturn(Collections.emptyList());
        
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Mock transaction repository saving to fail, simulating a rollback trigger
        when(transactionRepository.save(any(Transaction.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            orderService.createOrder(student, 101, PaymentMethod.MOMO)
        );
    }

    @Test
    void expirePendingOrders_ExpiredOrder_TransitionsStatus() {
        // Arrange
        Order expiredOrder = Order.builder()
                .id(10)
                .orderCode("ORD123")
                .status(OrderStatus.PENDING)
                .expiredAt(java.time.LocalDateTime.now().minusMinutes(1))
                .build();

        Transaction linkedTxn = Transaction.builder()
                .id(50)
                .order(expiredOrder)
                .status(TransactionStatus.PENDING)
                .build();

        when(orderRepository.findByStatusAndExpiredAtBefore(eq(OrderStatus.PENDING), any(java.time.LocalDateTime.class)))
                .thenReturn(Collections.singletonList(expiredOrder));
        when(transactionRepository.findByOrderId(10)).thenReturn(Collections.singletonList(linkedTxn));

        // Act
        orderService.expirePendingOrders();

        // Assert
        assertEquals(OrderStatus.EXPIRED, expiredOrder.getStatus());
        assertEquals(TransactionStatus.FAILED, linkedTxn.getStatus());
        verify(orderRepository).save(expiredOrder);
        verify(transactionRepository).save(linkedTxn);
    }

    @Test
    void expirePendingOrders_PaidOrderNotExpired_KeepsPaid() {
        // Arrange
        when(orderRepository.findByStatusAndExpiredAtBefore(eq(OrderStatus.PENDING), any(java.time.LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // Act
        orderService.expirePendingOrders();

        // Assert
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void expirePendingOrders_NoExpiredOrders_DoesNothing() {
        // Arrange
        when(orderRepository.findByStatusAndExpiredAtBefore(eq(OrderStatus.PENDING), any(java.time.LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // Act
        orderService.expirePendingOrders();

        // Assert
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void expirePendingOrders_MultipleExpiredOrders_UpdatesAll() {
        // Arrange
        Order expiredOrder1 = Order.builder()
                .id(10)
                .orderCode("ORD1")
                .status(OrderStatus.PENDING)
                .build();
        Order expiredOrder2 = Order.builder()
                .id(20)
                .orderCode("ORD2")
                .status(OrderStatus.PENDING)
                .build();

        when(orderRepository.findByStatusAndExpiredAtBefore(eq(OrderStatus.PENDING), any(java.time.LocalDateTime.class)))
                .thenReturn(Arrays.asList(expiredOrder1, expiredOrder2));
        when(transactionRepository.findByOrderId(10)).thenReturn(Collections.emptyList());
        when(transactionRepository.findByOrderId(20)).thenReturn(Collections.emptyList());

        // Act
        orderService.expirePendingOrders();

        // Assert
        assertEquals(OrderStatus.EXPIRED, expiredOrder1.getStatus());
        assertEquals(OrderStatus.EXPIRED, expiredOrder2.getStatus());
        verify(orderRepository).save(expiredOrder1);
        verify(orderRepository).save(expiredOrder2);
    }
}
