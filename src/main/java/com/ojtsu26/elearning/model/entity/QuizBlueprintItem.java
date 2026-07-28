package com.ojtsu26.elearning.model.entity;

import com.ojtsu26.elearning.model.enums.QuestionDifficulty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "Quiz_Blueprint_Items",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_quiz_blueprint_bucket",
                columnNames = {"quiz_id", "topic_code", "difficulty"})
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizBlueprintItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "topic_code", nullable = false, length = 100)
    private String topicCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionDifficulty difficulty;

    @Column(name = "question_count", nullable = false)
    private Integer questionCount;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
