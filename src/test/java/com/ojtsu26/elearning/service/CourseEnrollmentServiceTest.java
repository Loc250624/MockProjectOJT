package com.ojtsu26.elearning.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ojtsu26.elearning.dto.response.CourseEnrollmentResponseDTO;
import com.ojtsu26.elearning.dto.response.CourseEnrollmentStateResponseDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.mapper.CourseEnrollmentMapper;
import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.entity.CourseEnrollment;
import com.ojtsu26.elearning.model.entity.Order;
import com.ojtsu26.elearning.model.entity.OrderItem;
import com.ojtsu26.elearning.model.entity.Transaction;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.CourseStatus;
import com.ojtsu26.elearning.model.enums.OrderStatus;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.model.enums.TransactionStatus;
import com.ojtsu26.elearning.model.enums.UserStatus;
import com.ojtsu26.elearning.repository.CourseEnrollmentRepository;
import com.ojtsu26.elearning.repository.CourseRepository;
import com.ojtsu26.elearning.repository.OrderRepository;
import com.ojtsu26.elearning.service.impl.CourseEnrollmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class CourseEnrollmentServiceTest {

    @Mock
    private CourseEnrollmentRepository enrollmentRepository;

    @Mock
    private CourseEnrollmentMapper enrollmentMapper;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private NotificationService notificationService;

    private CourseEnrollmentServiceImpl service;
    private User student;
    private User otherStudent;
    private Course freeCourse;
    private Course paidCourse;
    private CourseEnrollment enrollment;
    private CourseEnrollmentResponseDTO enrollmentDto;

    @BeforeEach
    void setUp() {
        service = new CourseEnrollmentServiceImpl(
                enrollmentRepository,
                enrollmentMapper,
                courseRepository,
                orderRepository,
                currentUserService,
                notificationService
        );
        student = User.builder().id(1).email("student@example.com").role(Role.STUDENT).status(UserStatus.ACTIVE).build();
        otherStudent = User.builder().id(2).email("other@example.com").role(Role.STUDENT).status(UserStatus.ACTIVE).build();
        freeCourse = Course.builder().id(101).title("Free Course").price(BigDecimal.ZERO).status(CourseStatus.APPROVED).build();
        paidCourse = Course.builder().id(102).title("Paid Course").price(new BigDecimal("49.00")).status(CourseStatus.APPROVED).build();
        enrollment = CourseEnrollment.builder().id(501).student(student).course(freeCourse).progressPercentage(BigDecimal.ZERO).isCompleted(false).build();
        enrollmentDto = new CourseEnrollmentResponseDTO();
        enrollmentDto.setId(501);
        enrollmentDto.setStudentId(1);
        enrollmentDto.setCourseId(101);
    }

    @Test
    void enrollCurrentStudentInFreeCourse_Success() {
        when(currentUserService.getCurrentUser()).thenReturn(student);
        when(courseRepository.findById(101)).thenReturn(Optional.of(freeCourse));
        when(enrollmentRepository.findByStudentIdAndCourseIdForUpdate(1, 101)).thenReturn(Optional.empty());
        when(enrollmentRepository.save(any(CourseEnrollment.class))).thenReturn(enrollment);
        when(enrollmentMapper.toDto(enrollment)).thenReturn(enrollmentDto);

        CourseEnrollmentResponseDTO response = service.enrollCurrentStudentInFreeCourse(101);

        assertEquals(501, response.getId());
        verify(enrollmentRepository).save(any(CourseEnrollment.class));
        verify(notificationService).createCourseEnrollmentNotifications(enrollment, com.ojtsu26.elearning.model.enums.NotificationType.FREE_ENROLLMENT_CONFIRMED);
    }

    @Test
    void enrollCurrentStudentInFreeCourse_RepeatedRequestReturnsExistingEnrollment() {
        when(currentUserService.getCurrentUser()).thenReturn(student);
        when(courseRepository.findById(101)).thenReturn(Optional.of(freeCourse));
        when(enrollmentRepository.findByStudentIdAndCourseIdForUpdate(1, 101)).thenReturn(Optional.of(enrollment));
        when(enrollmentMapper.toDto(enrollment)).thenReturn(enrollmentDto);

        CourseEnrollmentResponseDTO response = service.enrollCurrentStudentInFreeCourse(101);

        assertEquals(501, response.getId());
        verify(enrollmentRepository, never()).save(any(CourseEnrollment.class));
        verify(notificationService).createCourseEnrollmentNotifications(enrollment, com.ojtsu26.elearning.model.enums.NotificationType.FREE_ENROLLMENT_CONFIRMED);
    }

    @Test
    void enrollCurrentStudentInFreeCourse_DuplicateKeyFallbackReturnsExistingEnrollment() {
        when(currentUserService.getCurrentUser()).thenReturn(student);
        when(courseRepository.findById(101)).thenReturn(Optional.of(freeCourse));
        when(enrollmentRepository.findByStudentIdAndCourseIdForUpdate(1, 101)).thenReturn(Optional.empty());
        when(enrollmentRepository.save(any(CourseEnrollment.class))).thenThrow(new DataIntegrityViolationException("duplicate"));
        when(enrollmentRepository.findByStudentIdAndCourseId(1, 101)).thenReturn(Optional.of(enrollment));
        when(enrollmentMapper.toDto(enrollment)).thenReturn(enrollmentDto);

        CourseEnrollmentResponseDTO response = service.enrollCurrentStudentInFreeCourse(101);

        assertEquals(501, response.getId());
        verify(enrollmentRepository).findByStudentIdAndCourseId(1, 101);
    }

    @Test
    void enrollCurrentStudentInFreeCourse_UnavailableCourseRejected() {
        freeCourse.setStatus(CourseStatus.HIDDEN);
        when(currentUserService.getCurrentUser()).thenReturn(student);
        when(courseRepository.findById(101)).thenReturn(Optional.of(freeCourse));

        assertThrows(BusinessException.class, () -> service.enrollCurrentStudentInFreeCourse(101));
        verify(enrollmentRepository, never()).save(any(CourseEnrollment.class));
    }

    @Test
    void enrollCurrentStudentInFreeCourse_PaidCourseRejectedBeforePayment() {
        when(currentUserService.getCurrentUser()).thenReturn(student);
        when(courseRepository.findById(102)).thenReturn(Optional.of(paidCourse));

        assertThrows(BusinessException.class, () -> service.enrollCurrentStudentInFreeCourse(102));
        verify(enrollmentRepository, never()).save(any(CourseEnrollment.class));
    }

    @Test
    void getCurrentStudentCourseState_ReturnsPendingPayment() {
        when(currentUserService.getCurrentUser()).thenReturn(student);
        when(courseRepository.findById(102)).thenReturn(Optional.of(paidCourse));
        when(enrollmentRepository.findByStudentIdAndCourseId(1, 102)).thenReturn(Optional.empty());

        CourseEnrollmentStateResponseDTO state = service.getCurrentStudentCourseState(102);

        assertEquals("BUY_COURSE", state.getAction());
        assertFalse(state.isPaymentPending());
    }

    @Test
    void activateEnrollmentAfterVerifiedPayment_UnpaidPaymentRejected() {
        Order order = paidOrder(student, paidCourse, OrderStatus.PENDING);
        Transaction transaction = paidTransaction(student, paidCourse, TransactionStatus.PENDING);

        assertThrows(BusinessException.class, () -> service.activateEnrollmentAfterVerifiedPayment(order, transaction));
        verify(enrollmentRepository, never()).save(any(CourseEnrollment.class));
    }

    @Test
    void activateEnrollmentAfterVerifiedPayment_OtherUserRejected() {
        Order order = paidOrder(student, paidCourse, OrderStatus.PAID);
        Transaction transaction = paidTransaction(otherStudent, paidCourse, TransactionStatus.SUCCESS);

        assertThrows(BusinessException.class, () -> service.activateEnrollmentAfterVerifiedPayment(order, transaction));
    }

    @Test
    void activateEnrollmentAfterVerifiedPayment_OtherCourseRejected() {
        Course otherCourse = Course.builder().id(999).title("Other").price(new BigDecimal("49.00")).status(CourseStatus.APPROVED).build();
        Order order = paidOrder(student, paidCourse, OrderStatus.PAID);
        Transaction transaction = paidTransaction(student, otherCourse, TransactionStatus.SUCCESS);

        assertThrows(BusinessException.class, () -> service.activateEnrollmentAfterVerifiedPayment(order, transaction));
    }

    @Test
    void activateEnrollmentAfterVerifiedPayment_ValidPaymentCreatesEnrollmentOnce() {
        CourseEnrollment paidEnrollment = CourseEnrollment.builder().id(777).student(student).course(paidCourse).build();
        CourseEnrollmentResponseDTO dto = new CourseEnrollmentResponseDTO();
        dto.setId(777);
        dto.setStudentId(1);
        dto.setCourseId(102);
        Order order = paidOrder(student, paidCourse, OrderStatus.PAID);
        Transaction transaction = paidTransaction(student, paidCourse, TransactionStatus.SUCCESS);

        when(enrollmentRepository.findByStudentIdAndCourseIdForUpdate(1, 102)).thenReturn(Optional.empty(), Optional.of(paidEnrollment));
        when(enrollmentRepository.save(any(CourseEnrollment.class))).thenReturn(paidEnrollment);
        when(enrollmentMapper.toDto(paidEnrollment)).thenReturn(dto);

        CourseEnrollmentResponseDTO first = service.activateEnrollmentAfterVerifiedPayment(order, transaction);
        CourseEnrollmentResponseDTO second = service.activateEnrollmentAfterVerifiedPayment(order, transaction);

        assertEquals(777, first.getId());
        assertEquals(777, second.getId());
        verify(enrollmentRepository, times(1)).save(any(CourseEnrollment.class));
        verify(notificationService, times(2)).createCourseEnrollmentNotifications(paidEnrollment, com.ojtsu26.elearning.model.enums.NotificationType.PAID_ENROLLMENT_ACTIVATED);
    }

    private Order paidOrder(User user, Course course, OrderStatus status) {
        Order order = Order.builder()
                .id(10)
                .user(user)
                .paidAmount(new BigDecimal("1225000"))
                .status(status)
                .items(Collections.emptyList())
                .build();
        OrderItem item = OrderItem.builder().order(order).course(course).unitPrice(course.getPrice()).build();
        order.setItems(List.of(item));
        return order;
    }

    private Transaction paidTransaction(User user, Course course, TransactionStatus status) {
        return Transaction.builder()
                .id(20)
                .student(user)
                .course(course)
                .amount(new BigDecimal("1225000"))
                .status(status)
                .build();
    }
}
