package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.response.NotificationPageResponseDTO;
import com.ojtsu26.elearning.dto.response.NotificationResponseDTO;
import com.ojtsu26.elearning.dto.response.UnreadNotificationCountDTO;
import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.entity.CourseEnrollment;
import com.ojtsu26.elearning.model.entity.Order;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.NotificationType;

public interface NotificationService {
    NotificationPageResponseDTO getCurrentUserNotifications(int page, int size);
    UnreadNotificationCountDTO getCurrentUserUnreadCount();
    NotificationResponseDTO markCurrentUserNotificationRead(Integer notificationId);
    UnreadNotificationCountDTO markAllCurrentUserNotificationsRead();
    void createCourseEnrollmentNotifications(CourseEnrollment enrollment, NotificationType studentType);
    void createCourseCompletedNotification(CourseEnrollment enrollment);
    void createPaymentFailedNotification(Order order);
    void createCourseSubmittedForReviewNotification(Course course);
    void createCourseModerationNotification(Course course, NotificationType type);
    void createNotification(User recipient, NotificationType type, String title, String message, String targetPath, String dedupeKey);
}
