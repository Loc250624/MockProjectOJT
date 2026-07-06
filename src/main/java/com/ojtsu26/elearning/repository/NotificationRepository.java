package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Integer recipientId, Pageable pageable);

    long countByRecipientIdAndReadAtIsNull(Integer recipientId);

    Optional<Notification> findByIdAndRecipientId(Integer id, Integer recipientId);

    boolean existsByDedupeKey(String dedupeKey);

    @Modifying
    @Query("update Notification n set n.readAt = :readAt where n.recipient.id = :recipientId and n.readAt is null")
    int markAllReadForRecipient(@Param("recipientId") Integer recipientId, @Param("readAt") LocalDateTime readAt);
}
