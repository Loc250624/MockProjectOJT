package com.ojtsu26.elearning.model.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.ojtsu26.elearning.model.enums.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "Notifications", uniqueConstraints = {
        @UniqueConstraint(name = "uk_notification_dedupe_key", columnNames = "dedupe_key")
}, indexes = {
        @Index(name = "idx_notification_recipient_created", columnList = "recipient_id, created_at"),
        @Index(name = "idx_notification_recipient_read", columnList = "recipient_id, read_at"),
        @Index(name = "idx_notification_type", columnList = "type")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    @JsonBackReference("notification-recipient")
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private NotificationType type;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 800)
    private String message;

    @Column(name = "target_path", length = 240)
    private String targetPath;

    @Column(name = "dedupe_key", length = 160)
    private String dedupeKey;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;
}
