package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.dto.response.NotificationPageResponseDTO;
import com.ojtsu26.elearning.dto.response.NotificationResponseDTO;
import com.ojtsu26.elearning.dto.response.UnreadNotificationCountDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.exception.ErrorCode;
import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.entity.CourseEnrollment;
import com.ojtsu26.elearning.model.entity.Notification;
import com.ojtsu26.elearning.model.entity.Order;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.NotificationType;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.repository.NotificationRepository;
import com.ojtsu26.elearning.repository.UserRepository;
import com.ojtsu26.elearning.service.CurrentUserService;
import com.ojtsu26.elearning.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final int MAX_PAGE_SIZE = 50;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional(readOnly = true)
    public NotificationPageResponseDTO getCurrentUserNotifications(int page, int size) {
        User user = currentUserService.getCurrentUser();
        PageRequest pageRequest = PageRequest.of(validatePage(page), validateSize(size),
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));
        Page<Notification> notifications = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(user.getId(), pageRequest);
        return NotificationPageResponseDTO.builder()
                .items(notifications.getContent().stream().map(this::toDto).toList())
                .page(notifications.getNumber())
                .size(notifications.getSize())
                .totalElements(notifications.getTotalElements())
                .totalPages(notifications.getTotalPages())
                .hasNext(notifications.hasNext())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadNotificationCountDTO getCurrentUserUnreadCount() {
        User user = currentUserService.getCurrentUser();
        return UnreadNotificationCountDTO.builder()
                .unreadCount(notificationRepository.countByRecipientIdAndReadAtIsNull(user.getId()))
                .build();
    }

    @Override
    @Transactional
    public NotificationResponseDTO markCurrentUserNotificationRead(Integer notificationId) {
        User user = currentUserService.getCurrentUser();
        Notification notification = notificationRepository.findByIdAndRecipientId(notificationId, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Notification not found."));
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
        return toDto(notification);
    }

    @Override
    @Transactional
    public UnreadNotificationCountDTO markAllCurrentUserNotificationsRead() {
        User user = currentUserService.getCurrentUser();
        notificationRepository.markAllReadForRecipient(user.getId(), LocalDateTime.now());
        return getCurrentUserUnreadCount();
    }

    @Override
    @Transactional
    public void createCourseEnrollmentNotifications(CourseEnrollment enrollment, NotificationType studentType) {
        if (enrollment == null || enrollment.getStudent() == null || enrollment.getCourse() == null) {
            return;
        }
        Course course = enrollment.getCourse();
        User student = enrollment.getStudent();
        String courseTitle = safeValue(course.getTitle(), "course");
        createNotification(student, studentType, studentEnrollmentTitle(studentType),
                "Your enrollment in " + courseTitle + " is ready.",
                studentCoursePath(course.getId()),
                studentType.name() + ":student:" + student.getId() + ":course:" + course.getId());

        if (course.getInstructor() != null) {
            createNotification(course.getInstructor(), NotificationType.TEACHER_NEW_ENROLLMENT,
                    "New student enrollment",
                    safeValue(student.getFullName(), "A student") + " enrolled in " + courseTitle + ".",
                    teacherCoursePath(course.getId()),
                    NotificationType.TEACHER_NEW_ENROLLMENT.name() + ":teacher:" + course.getInstructor().getId()
                            + ":student:" + student.getId() + ":course:" + course.getId());
        }
    }

    @Override
    @Transactional
    public void createCourseCompletedNotification(CourseEnrollment enrollment) {
        if (enrollment == null || enrollment.getStudent() == null || enrollment.getCourse() == null) {
            return;
        }
        Course course = enrollment.getCourse();
        createNotification(enrollment.getStudent(), NotificationType.COURSE_COMPLETED,
                "Course completed",
                "You completed " + safeValue(course.getTitle(), "this course") + ".",
                studentCoursePath(course.getId()),
                NotificationType.COURSE_COMPLETED.name() + ":enrollment:" + enrollment.getId());
    }

    @Override
    @Transactional
    public void createPaymentFailedNotification(Order order) {
        if (order == null || order.getUser() == null || order.getItems() == null || order.getItems().isEmpty()
                || order.getItems().get(0).getCourse() == null) {
            return;
        }
        Course course = order.getItems().get(0).getCourse();
        createNotification(order.getUser(), NotificationType.PAYMENT_FAILED,
                "Payment failed",
                "Your payment for " + safeValue(course.getTitle(), "this course") + " could not be completed.",
                "/student/checkout?courseId=" + course.getId(),
                NotificationType.PAYMENT_FAILED.name() + ":order:" + order.getId());
    }

    @Override
    @Transactional
    public void createCourseSubmittedForReviewNotification(Course course) {
        if (course == null || course.getId() == null) {
            return;
        }
        List<User> admins = userRepository.findByRole(Role.ADMIN);
        for (User admin : admins) {
            createNotification(admin, NotificationType.COURSE_SUBMITTED_FOR_REVIEW,
                    "Course waiting for review",
                    safeValue(course.getTitle(), "A course") + " was submitted for approval.",
                    "/admin/courses/approval?status=PENDING_APPROVAL",
                    NotificationType.COURSE_SUBMITTED_FOR_REVIEW.name() + ":admin:" + admin.getId() + ":course:" + course.getId());
        }
    }

    @Override
    @Transactional
    public void createCourseModerationNotification(Course course, NotificationType type) {
        if (course == null || course.getInstructor() == null || course.getId() == null) {
            return;
        }
        createNotification(course.getInstructor(), type, moderationTitle(type),
                moderationMessage(type, course),
                teacherCoursePath(course.getId()),
                type.name() + ":course:" + course.getId());
    }

    @Override
    @Transactional
    public void createNotification(User recipient, NotificationType type, String title, String message, String targetPath, String dedupeKey) {
        if (recipient == null || recipient.getId() == null || type == null || dedupeKey == null || dedupeKey.isBlank()) {
            return;
        }
        if (!isAllowedTargetForRole(recipient.getRole(), targetPath)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid notification target.");
        }
        if (notificationRepository.existsByDedupeKey(dedupeKey)) {
            return;
        }
        try {
            notificationRepository.save(Notification.builder()
                    .recipient(recipient)
                    .type(type)
                    .title(truncate(safeValue(title, "Notification"), 160))
                    .message(truncate(safeValue(message, "You have a new notification."), 800))
                    .targetPath(targetPath)
                    .dedupeKey(dedupeKey)
                    .build());
        } catch (DataIntegrityViolationException ignored) {
            // A concurrent retry can win the same dedupe key. The operation remains idempotent.
        }
    }

    private int validatePage(int page) {
        if (page < 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Page index must not be negative.");
        }
        return page;
    }

    private int validateSize(int size) {
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Page size must be between 1 and " + MAX_PAGE_SIZE + ".");
        }
        return size;
    }

    private NotificationResponseDTO toDto(Notification notification) {
        return NotificationResponseDTO.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .targetPath(notification.getTargetPath())
                .read(notification.getReadAt() != null)
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .build();
    }

    private boolean isAllowedTargetForRole(Role role, String targetPath) {
        if (targetPath == null || targetPath.isBlank()) {
            return true;
        }
        if (!targetPath.startsWith("/") || targetPath.startsWith("//") || targetPath.contains("://")
                || targetPath.toLowerCase().startsWith("javascript:")) {
            return false;
        }
        if (role == Role.STUDENT) {
            return targetPath.startsWith("/student/");
        }
        if (role == Role.TEACHER) {
            return targetPath.startsWith("/teacher/") || targetPath.startsWith("/instructor/");
        }
        return role == Role.ADMIN && targetPath.startsWith("/admin/");
    }

    private String studentEnrollmentTitle(NotificationType type) {
        return type == NotificationType.PAID_ENROLLMENT_ACTIVATED
                ? "Course purchase activated"
                : "Enrollment confirmed";
    }

    private String moderationTitle(NotificationType type) {
        return switch (type) {
            case COURSE_APPROVED -> "Course approved";
            case COURSE_REJECTED -> "Course needs changes";
            case COURSE_HIDDEN -> "Course hidden";
            case COURSE_UNHIDDEN -> "Course visible again";
            default -> "Course status updated";
        };
    }

    private String moderationMessage(NotificationType type, Course course) {
        String title = safeValue(course.getTitle(), "Your course");
        return switch (type) {
            case COURSE_APPROVED -> title + " was approved and is available to students.";
            case COURSE_REJECTED -> title + " was returned for changes.";
            case COURSE_HIDDEN -> title + " was hidden from the catalog.";
            case COURSE_UNHIDDEN -> title + " was restored to the catalog.";
            default -> title + " has a status update.";
        };
    }

    private String studentCoursePath(Integer courseId) {
        return "/student/learning?courseId=" + courseId;
    }

    private String teacherCoursePath(Integer courseId) {
        return "/teacher/courses/edit/" + courseId;
    }

    private String safeValue(String value, String fallback) {
        String clean = value == null || value.isBlank() ? fallback : value;
        return clean.replaceAll("<[^>]*>", "").replaceAll("[\\r\\n\\t]+", " ").trim();
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength - 1).trim();
    }
}
