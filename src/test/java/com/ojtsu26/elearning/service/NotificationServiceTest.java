package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.response.NotificationPageResponseDTO;
import com.ojtsu26.elearning.dto.response.NotificationResponseDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.entity.CourseEnrollment;
import com.ojtsu26.elearning.model.entity.Notification;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.NotificationType;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.repository.NotificationRepository;
import com.ojtsu26.elearning.repository.UserRepository;
import com.ojtsu26.elearning.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrentUserService currentUserService;

    private NotificationServiceImpl service;
    private User student;
    private User teacher;
    private Course course;
    private CourseEnrollment enrollment;

    @BeforeEach
    void setUp() {
        service = new NotificationServiceImpl(notificationRepository, userRepository, currentUserService);
        student = User.builder().id(1).fullName("<b>Ada</b>").role(Role.STUDENT).build();
        teacher = User.builder().id(2).fullName("Teacher").role(Role.TEACHER).build();
        course = Course.builder().id(10).title("<script>x</script>Java").instructor(teacher).build();
        enrollment = CourseEnrollment.builder().id(20).student(student).course(course).build();
    }

    @Test
    void enrollmentEventCreatesStudentAndTeacherNotificationsWithSafeText() {
        when(notificationRepository.existsByDedupeKey(any())).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createCourseEnrollmentNotifications(enrollment, NotificationType.FREE_ENROLLMENT_CONFIRMED);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(captor.capture());
        List<Notification> saved = captor.getAllValues();

        assertEquals(student, saved.get(0).getRecipient());
        assertEquals(NotificationType.FREE_ENROLLMENT_CONFIRMED, saved.get(0).getType());
        assertEquals("/student/learning?courseId=10", saved.get(0).getTargetPath());
        assertFalse(saved.get(0).getMessage().contains("<script>"));

        assertEquals(teacher, saved.get(1).getRecipient());
        assertEquals(NotificationType.TEACHER_NEW_ENROLLMENT, saved.get(1).getType());
        assertEquals("/teacher/courses/edit/10", saved.get(1).getTargetPath());
        assertFalse(saved.get(1).getMessage().contains("<b>"));
    }

    @Test
    void duplicateDedupeKeyDoesNotCreateAnotherNotification() {
        when(notificationRepository.existsByDedupeKey("same-key")).thenReturn(true);

        service.createNotification(student, NotificationType.COURSE_COMPLETED, "Done", "Message",
                "/student/learning?courseId=10", "same-key");

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void dangerousOrCrossRoleTargetIsRejected() {
        assertThrows(BusinessException.class, () -> service.createNotification(student,
                NotificationType.COURSE_COMPLETED, "Done", "Message", "javascript:alert(1)", "bad-1"));
        assertThrows(BusinessException.class, () -> service.createNotification(student,
                NotificationType.COURSE_COMPLETED, "Done", "Message", "/admin/users", "bad-2"));
    }

    @Test
    void currentUserListsOnlyOwnedNotificationsWithPagination() {
        Notification notification = Notification.builder()
                .id(101)
                .recipient(student)
                .type(NotificationType.COURSE_COMPLETED)
                .title("Done")
                .message("Finished")
                .targetPath("/student/learning?courseId=10")
                .createdAt(LocalDateTime.now())
                .build();
        when(currentUserService.getCurrentUser()).thenReturn(student);
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(eq(1), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(notification), PageRequest.of(0, 10), 1));

        NotificationPageResponseDTO page = service.getCurrentUserNotifications(0, 10);

        assertEquals(1, page.getItems().size());
        assertEquals(101, page.getItems().get(0).getId());
        verify(notificationRepository).findByRecipientIdOrderByCreatedAtDesc(eq(1), any(Pageable.class));
    }

    @Test
    void pageSizeLimitIsEnforced() {
        when(currentUserService.getCurrentUser()).thenReturn(student);

        assertThrows(BusinessException.class, () -> service.getCurrentUserNotifications(0, 99));
    }

    @Test
    void markOneOwnedNotificationReadIsIdempotent() {
        Notification notification = Notification.builder()
                .id(101)
                .recipient(student)
                .type(NotificationType.COURSE_COMPLETED)
                .title("Done")
                .message("Finished")
                .createdAt(LocalDateTime.now())
                .build();
        when(currentUserService.getCurrentUser()).thenReturn(student);
        when(notificationRepository.findByIdAndRecipientId(101, 1)).thenReturn(Optional.of(notification));

        NotificationResponseDTO first = service.markCurrentUserNotificationRead(101);
        LocalDateTime readAt = notification.getReadAt();
        NotificationResponseDTO second = service.markCurrentUserNotificationRead(101);

        assertTrue(first.getRead());
        assertTrue(second.getRead());
        assertEquals(readAt, notification.getReadAt());
        verify(notificationRepository, times(1)).save(notification);
    }

    @Test
    void userCannotMarkAnotherUsersNotificationRead() {
        when(currentUserService.getCurrentUser()).thenReturn(student);
        when(notificationRepository.findByIdAndRecipientId(202, 1)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.markCurrentUserNotificationRead(202));
    }

    @Test
    void markAllUpdatesOnlyCurrentUserNotifications() {
        when(currentUserService.getCurrentUser()).thenReturn(student);
        when(notificationRepository.countByRecipientIdAndReadAtIsNull(1)).thenReturn(0L);

        assertEquals(0L, service.markAllCurrentUserNotificationsRead().getUnreadCount());

        verify(notificationRepository).markAllReadForRecipient(eq(1), any(LocalDateTime.class));
        verify(notificationRepository, never()).markAllReadForRecipient(eq(2), any(LocalDateTime.class));
    }

    @Test
    void legacyAssignmentGradedNotificationTypeRemainsReadable() {
        assertEquals(NotificationType.ASSIGNMENT_GRADED, NotificationType.valueOf("ASSIGNMENT_GRADED"));
    }
}
