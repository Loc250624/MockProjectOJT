package com.ojtsu26.elearning.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ojtsu26.elearning.config.PaymentGatewayProperties;
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

    @Mock
    private PaymentGatewayProperties paymentGatewayProperties;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User student;
    private Course course;

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
                .price(new BigDecimal("99.00"))
                .status(CourseStatus.APPROVED)
                .build();

        lenient().when(paymentGatewayProperties.getExchangeRate()).thenReturn(new BigDecimal("25000"));
    }

    @Test
    void createOrder_Success() {
        // Arrange
        when(courseRepository.findById(101)).thenReturn(Optional.of(course));
        when(courseEnrollmentRepository.existsByStudentIdAndCourseId(1, 101)).thenReturn(false);
        when(orderRepository.findByStudentIdAndCourseIdAndStatus(1, 101, OrderStatus.PENDING))
                .thenReturn(Collections.emptyList());
        
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Order order = orderService.createOrder(student, 101, PaymentMethod.MOMO);

        // Assert
        assertNotNull(order);
        assertEquals(student, order.getUser());
        assertEquals(new BigDecimal("99.00"), order.getTotalAmount());
        assertEquals("USD", order.getCurrency());
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals(PaymentMethod.MOMO, order.getPaymentMethod());
        assertFalse(order.getItems().isEmpty());
        assertEquals("Test Course", order.getItems().get(0).getCourseName());
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
    void createOrder_DuplicatePendingOrder_ThrowsException() {
        // Arrange
        when(courseRepository.findById(101)).thenReturn(Optional.of(course));
        when(courseEnrollmentRepository.existsByStudentIdAndCourseId(1, 101)).thenReturn(false);
        when(orderRepository.findByStudentIdAndCourseIdAndStatus(1, 101, OrderStatus.PENDING))
                .thenReturn(Collections.singletonList(new Order()));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> 
            orderService.createOrder(student, 101, PaymentMethod.MOMO)
        );
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
