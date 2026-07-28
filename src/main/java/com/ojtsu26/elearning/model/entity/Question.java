package com.ojtsu26.elearning.model.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.ojtsu26.elearning.model.enums.*;
import java.util.List;

@Entity
@Table(name = "Questions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    
    private String questionText;

    
    private String optionsJson;

    
    private String correctAnswer;

    @Convert(converter = QuestionTypeConverter.class)
    private QuestionType questionType;

    private java.math.BigDecimal points;

    private Integer displayOrder;

    @Builder.Default
    @Column(name = "topic_code", nullable = false, length = 100)
    private String topicCode = "GENERAL";

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionDifficulty difficulty = QuestionDifficulty.MEDIUM;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 20)
    private QuestionReviewStatus reviewStatus = QuestionReviewStatus.APPROVED;

    @Builder.Default
    @Column(nullable = false)
    private Integer version = 1;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "generation_source", nullable = false, length = 20)
    private QuestionGenerationSource generationSource = QuestionGenerationSource.MANUAL;

    @ManyToOne
    @JoinColumn(name = "quiz_id")
    @JsonBackReference("question-quiz")
    private Quiz quiz;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuizAnswer> answers;
}
