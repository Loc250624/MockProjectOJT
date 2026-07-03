package com.ojtsu26.elearning.controller.api;

import com.ojtsu26.elearning.common.ApiResponse;
import com.ojtsu26.elearning.dto.response.NotificationPageResponseDTO;
import com.ojtsu26.elearning.dto.response.NotificationResponseDTO;
import com.ojtsu26.elearning.dto.response.UnreadNotificationCountDTO;
import com.ojtsu26.elearning.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationRestController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<NotificationPageResponseDTO>> list(@RequestParam(defaultValue = "0") int page,
                                                                         @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getCurrentUserNotifications(page, size)));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<UnreadNotificationCountDTO>> unreadCount() {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getCurrentUserUnreadCount()));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponseDTO>> markRead(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.markCurrentUserNotificationRead(id)));
    }

    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<UnreadNotificationCountDTO>> markAllRead() {
        return ResponseEntity.ok(ApiResponse.success(notificationService.markAllCurrentUserNotificationsRead()));
    }
}
