package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.model.entity.CourseEnrollment;
import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.entity.Order;
import com.ojtsu26.elearning.model.entity.OrderItem;
import com.ojtsu26.elearning.model.entity.Transaction;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.dto.request.CourseEnrollmentRequestDTO;
import com.ojtsu26.elearning.dto.response.CourseEnrollmentResponseDTO;
import com.ojtsu26.elearning.dto.response.CourseEnrollmentStateResponseDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.exception.ErrorCode;
import com.ojtsu26.elearning.mapper.CourseEnrollmentMapper;
import com.ojtsu26.elearning.repository.CourseEnrollmentRepository;
import com.ojtsu26.elearning.repository.CourseRepository;
import com.ojtsu26.elearning.repository.OrderRepository;
import com.ojtsu26.elearning.service.CourseEnrollmentService;
import com.ojtsu26.elearning.service.CurrentUserService;
import com.ojtsu26.elearning.model.enums.CourseStatus;
import com.ojtsu26.elearning.model.enums.NotificationType;
import com.ojtsu26.elearning.model.enums.OrderStatus;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.model.enums.TransactionStatus;
import com.ojtsu26.elearning.model.enums.UserStatus;
import com.ojtsu26.elearning.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseEnrollmentServiceImpl implements CourseEnrollmentService {

    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final CourseEnrollmentMapper courseEnrollmentMapper;
    private final CourseRepository courseRepository;
    private final OrderRepository orderRepository;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;

    @Override
    public List<CourseEnrollmentResponseDTO> findAll() {
        return courseEnrollmentRepository.findAll().stream()
                .map(courseEnrollmentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public CourseEnrollmentResponseDTO findById(Integer id) {
        CourseEnrollment entity = courseEnrollmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("CourseEnrollment not found"));
        return courseEnrollmentMapper.toDto(entity);
    }

    @Override
    public CourseEnrollmentResponseDTO create(CourseEnrollmentRequestDTO requestDTO) {
        CourseEnrollment entity = courseEnrollmentMapper.toEntity(requestDTO);
        CourseEnrollment saved = courseEnrollmentRepository.save(entity);
        return courseEnrollmentMapper.toDto(saved);
    }

    @Override
    public CourseEnrollmentResponseDTO update(Integer id, CourseEnrollmentRequestDTO requestDTO) {
        if (!courseEnrollmentRepository.existsById(id)) {
            throw new RuntimeException("CourseEnrollment not found");
        }
        CourseEnrollment entity = courseEnrollmentMapper.toEntity(requestDTO);
        entity.setId(id);
        CourseEnrollment updated = courseEnrollmentRepository.save(entity);
        return courseEnrollmentMapper.toDto(updated);
    }

    @Override
    public void delete(Integer id) {
        courseEnrollmentRepository.deleteById(id);
    }

    @Override
    @Transactional
    public CourseEnrollmentResponseDTO enrollCurrentStudentInFreeCourse(Integer courseId) {
        User student = currentUserService.getCurrentUser();
        validateStudent(student);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));
        validateAvailableCourse(course);

        if (!isFree(course)) {
            throw new BusinessException(ErrorCode.PAYMENT_REQUIRED, "Paid courses must be purchased through checkout.");
        }

        CourseEnrollment enrollment = createEnrollmentIfAbsent(student, course);
        notificationService.createCourseEnrollmentNotifications(enrollment, NotificationType.FREE_ENROLLMENT_CONFIRMED);
        return courseEnrollmentMapper.toDto(enrollment);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseEnrollmentStateResponseDTO getCurrentStudentCourseState(Integer courseId) {
        User student = currentUserService.getCurrentUser();
        validateStudent(student);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));
        boolean available = isAvailableCourse(course);
        boolean freeCourse = isFree(course);

        CourseEnrollment existing = courseEnrollmentRepository
                .findByStudentIdAndCourseId(student.getId(), courseId)
                .orElse(null);
        if (existing != null) {
            return buildState(courseId, available, freeCourse, true, false,
                    "CONTINUE_LEARNING", "Continue learning", existing.getId(), "You are enrolled in this course.");
        }

        boolean paymentPending = !orderRepository
                .findByStudentIdAndCourseIdAndStatus(student.getId(), courseId, OrderStatus.PENDING)
                .isEmpty();
        if (!available) {
            return buildState(courseId, false, freeCourse, false, paymentPending,
                    "UNAVAILABLE", "Course unavailable", null, "This course is not available for enrollment.");
        }
        if (paymentPending) {
            return buildState(courseId, true, freeCourse, false, true,
                    "PAYMENT_PENDING", "Payment pending", null, "Complete your pending payment to start learning.");
        }
        if (freeCourse) {
            return buildState(courseId, true, true, false, false,
                    "ENROLL_FREE", "Enroll for free", null, "This course is free to enroll.");
        }
        return buildState(courseId, true, false, false, false,
                "BUY_COURSE", "Buy course", null, "Purchase this course to start learning.");
    }

    @Override
    @Transactional
    public CourseEnrollmentResponseDTO activateEnrollmentAfterVerifiedPayment(Order order, Transaction transaction) {
        validateVerifiedPayment(order, transaction);
        OrderItem item = order.getItems().get(0);
        Course course = item.getCourse();
        User student = order.getUser();
        CourseEnrollment enrollment = createEnrollmentIfAbsent(student, course);
        notificationService.createCourseEnrollmentNotifications(enrollment, NotificationType.PAID_ENROLLMENT_ACTIVATED);
        return courseEnrollmentMapper.toDto(enrollment);
    }

    private CourseEnrollment createEnrollmentIfAbsent(User student, Course course) {
        return courseEnrollmentRepository.findByStudentIdAndCourseIdForUpdate(student.getId(), course.getId())
                .orElseGet(() -> saveNewEnrollment(student, course));
    }

    private CourseEnrollment saveNewEnrollment(User student, Course course) {
        try {
            CourseEnrollment enrollment = CourseEnrollment.builder()
                    .student(student)
                    .course(course)
                    .progressPercentage(BigDecimal.ZERO)
                    .isCompleted(false)
                    .build();
            return courseEnrollmentRepository.save(enrollment);
        } catch (DataIntegrityViolationException ex) {
            return courseEnrollmentRepository.findByStudentIdAndCourseId(student.getId(), course.getId())
                    .orElseThrow(() -> ex);
        }
    }

    private void validateStudent(User student) {
        if (student.getRole() != Role.STUDENT) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        if (student.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ENROLLMENT_NOT_ALLOWED, "Only active student accounts can enroll.");
        }
    }

    private void validateAvailableCourse(Course course) {
        if (!isAvailableCourse(course)) {
            throw new BusinessException(ErrorCode.COURSE_UNAVAILABLE);
        }
    }

    private boolean isAvailableCourse(Course course) {
        return course.getStatus() == CourseStatus.APPROVED;
    }

    private boolean isFree(Course course) {
        return course.getPrice() == null || course.getPrice().compareTo(BigDecimal.ZERO) <= 0;
    }

    private void validateVerifiedPayment(Order order, Transaction transaction) {
        if (order == null || transaction == null) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_VERIFIED);
        }
        if (order.getStatus() != OrderStatus.PAID || transaction.getStatus() != TransactionStatus.SUCCESS) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_VERIFIED);
        }
        if (order.getUser() == null || transaction.getStudent() == null
                || !Objects.equals(order.getUser().getId(), transaction.getStudent().getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Payment does not belong to the order owner.");
        }
        if (order.getItems() == null || order.getItems().isEmpty() || order.getItems().get(0).getCourse() == null) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_VERIFIED, "Order does not contain a course.");
        }
        Course course = order.getItems().get(0).getCourse();
        if (transaction.getCourse() == null || !Objects.equals(course.getId(), transaction.getCourse().getId())) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_VERIFIED, "Payment course does not match the order course.");
        }
        if (transaction.getAmount() != null && order.getPaidAmount() != null
                && transaction.getAmount().compareTo(order.getPaidAmount()) != 0) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_VERIFIED, "Payment amount does not match the order amount.");
        }
        validateStudent(order.getUser());
        validateAvailableCourse(course);
    }

    private CourseEnrollmentStateResponseDTO buildState(Integer courseId, boolean available, boolean freeCourse,
                                                        boolean enrolled, boolean paymentPending, String action,
                                                        String actionLabel, Integer enrollmentId, String message) {
        return CourseEnrollmentStateResponseDTO.builder()
                .courseId(courseId)
                .available(available)
                .freeCourse(freeCourse)
                .enrolled(enrolled)
                .paymentPending(paymentPending)
                .action(action)
                .actionLabel(actionLabel)
                .enrollmentId(enrollmentId)
                .message(message)
                .build();
    }
}
