package com.ojtsu26.elearning.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Quiz_Answers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "attempt_id")
    private QuizAttempt attempt;

    @ManyToOne
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(columnDefinition = "TEXT")
    private String selectedOptionsJson;

    @Column(columnDefinition = "TEXT")
    private String answerText;
}
