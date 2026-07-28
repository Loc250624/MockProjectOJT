package com.ojtsu26.elearning.dto.assessment;

import com.ojtsu26.elearning.model.enums.QuestionType;
import com.ojtsu26.elearning.model.enums.QuizAttemptStatus;
import com.ojtsu26.elearning.model.enums.QuizStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class AssessmentDtos {
    private AssessmentDtos() {
    }

    @Data
    public static class OptionPayload {
        @NotBlank
        private String content;
        private Boolean correct = false;
    }

    @Data
    public static class QuestionPayload {
        private Integer id;
        @NotBlank
        private String content;
        private QuestionType questionType = QuestionType.SINGLE_CHOICE;
        @DecimalMin("0.0")
        private BigDecimal points = BigDecimal.ONE;
        private Integer displayOrder;
        @Valid
        @NotEmpty
        private List<OptionPayload> options = new ArrayList<>();
    }

    @Data
    public static class QuizPayload {
        @NotBlank
        private String title;
        private String description;
        @NotNull
        private Integer lessonId;
        private Integer durationMinutes = 30;
        @Positive
        private Integer maxAttempts = 1;
        private BigDecimal passingScore = new BigDecimal("70.00");
        private QuizStatus status = QuizStatus.DRAFT;
    }

    @Data
    public static class AnswerPayload {
        @NotNull
        private Integer questionId;
        private List<Integer> selectedOptionIds = new ArrayList<>();
        private String answerText;
    }

    @Data
    public static class QuizDraftPayload {
        @Valid
        private List<AnswerPayload> answers = new ArrayList<>();
    }

    @Data
    public static class OptionView {
        private Integer id;
        private String content;
        private Boolean correct;
    }

    @Data
    public static class QuestionView {
        private Integer id;
        private String content;
        private QuestionType questionType;
        private BigDecimal points;
        private Integer displayOrder;
        private List<OptionView> options = new ArrayList<>();
        private List<Integer> selectedOptionIds = new ArrayList<>();
    }

    @Data
    public static class QuizView {
        private Integer id;
        private Integer courseId;
        private Integer lessonId;
        private String courseTitle;
        private String lessonTitle;
        private String title;
        private String description;
        private Integer durationMinutes;
        private Integer maxAttempts;
        private QuizStatus status;
        private BigDecimal passingScore;
        private BigDecimal totalPoints;
        private List<QuestionView> questions = new ArrayList<>();
    }

    @Data
    public static class QuizAttemptView {
        private Integer id;
        private QuizAttemptStatus status;
        private LocalDateTime startedAt;
        private LocalDateTime submittedAt;
        private BigDecimal score;
        private BigDecimal totalPoints;
        private QuizView quiz;
    }

    @Data
    public static class ResultSummaryView {
        private List<QuizAttemptView> quizAttempts = new ArrayList<>();
    }
}
