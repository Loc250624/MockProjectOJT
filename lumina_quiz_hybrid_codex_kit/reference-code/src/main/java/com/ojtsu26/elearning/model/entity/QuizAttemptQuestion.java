package com.ojtsu26.elearning.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "Quiz_Attempt_Questions",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_attempt_question",
            columnNames = {"attempt_id", "question_id"}),
        @UniqueConstraint(
            name = "uq_attempt_display_order",
            columnNames = {"attempt_id", "display_order"})
    },
    indexes = {
        @Index(
            name = "idx_attempt_question_order",
            columnList = "attempt_id,display_order"),
        @Index(
            name = "idx_attempt_question_question",
            columnList = "question_id")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizAttemptQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private QuizAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(
        name = "points_snapshot",
        nullable = false,
        precision = 10,
        scale = 2)
    private BigDecimal pointsSnapshot;

    @Column(name = "question_version", nullable = false)
    private Integer questionVersion;

    @Lob
    @Column(
        name = "question_text_snapshot",
        nullable = false,
        columnDefinition = "TEXT")
    private String questionTextSnapshot;

    @Lob
    @Column(name = "options_json_snapshot", columnDefinition = "LONGTEXT")
    private String optionsJsonSnapshot;

    @Lob
    @Column(name = "correct_answer_snapshot", columnDefinition = "TEXT")
    private String correctAnswerSnapshot;

    @Column(name = "question_type_snapshot", length = 50)
    private String questionTypeSnapshot;

    @Column(name = "topic_snapshot", length = 100)
    private String topicSnapshot;

    @Column(name = "difficulty_snapshot", length = 20)
    private String difficultySnapshot;

    @CreationTimestamp
    @Column(name = "assigned_at", nullable = false, updatable = false)
    private LocalDateTime assignedAt;
}
