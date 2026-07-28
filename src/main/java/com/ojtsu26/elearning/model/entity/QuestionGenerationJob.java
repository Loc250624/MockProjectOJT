package com.ojtsu26.elearning.model.entity;

import com.ojtsu26.elearning.model.enums.QuestionDifficulty;
import com.ojtsu26.elearning.model.enums.QuestionGenerationJobStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "Question_Generation_Jobs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionGenerationJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionGenerationJobStatus status;

    @Column(name = "requested_count", nullable = false)
    private Integer requestedCount;

    @Column(name = "generated_count", nullable = false)
    private Integer generatedCount;

    @Column(name = "topic_code", nullable = false, length = 100)
    private String topicCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionDifficulty difficulty;

    @Lob
    @Column(name = "lesson_content_snapshot", nullable = false, columnDefinition = "LONGTEXT")
    private String lessonContentSnapshot;

    @Column(name = "provider_name", length = 50)
    private String providerName;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Lob
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
