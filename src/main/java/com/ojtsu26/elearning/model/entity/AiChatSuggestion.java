package com.ojtsu26.elearning.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "Ai_Chat_Suggestions", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_ai_chat_suggestion_fingerprint",
                columnNames = {"conversation_id", "normalized_text"})
}, indexes = {
        @Index(name = "idx_ai_chat_suggestion_message", columnList = "assistant_message_id, id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private AiChatConversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assistant_message_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private AiChatMessage assistantMessage;

    @Column(name = "normalized_text", nullable = false, length = 500)
    private String normalizedText;

    @Column(name = "display_text", nullable = false, length = 500)
    private String displayText;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
