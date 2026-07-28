package com.ojtsu26.elearning.dto.assessment;

import com.ojtsu26.elearning.model.enums.CodeJudgeStatus;
import com.ojtsu26.elearning.model.enums.AssignmentType;
import com.ojtsu26.elearning.model.enums.QuestionType;
import com.ojtsu26.elearning.model.enums.QuizAttemptStatus;
import com.ojtsu26.elearning.model.enums.QuizStatus;
import com.ojtsu26.elearning.model.enums.SubmissionStatus;
import com.ojtsu26.elearning.model.enums.QuestionDifficulty;
import com.ojtsu26.elearning.model.enums.QuestionGenerationSource;
import com.ojtsu26.elearning.model.enums.QuestionReviewStatus;
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
        private String topicCode = "GENERAL";
        private QuestionDifficulty difficulty = QuestionDifficulty.MEDIUM;
        private QuestionReviewStatus reviewStatus = QuestionReviewStatus.APPROVED;
        private Boolean active = true;
        private QuestionGenerationSource generationSource = QuestionGenerationSource.MANUAL;
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
    public static class AssignmentPayload {
        @NotBlank
        private String title;
        private String problemStatement;
        private String instructions;
        private String starterCode;
        private String allowedLanguages;
        @Positive
        private Integer timeLimitMs = 1000;
        @DecimalMin(value = "0.0", inclusive = false)
        private BigDecimal maxScore = new BigDecimal("100.00");
        private LocalDateTime dueDate;
        private String status = "DRAFT";
        private Integer lessonId;
        private AssignmentType type = AssignmentType.CODING;
        @Valid
        private List<QuestionPayload> questions = new ArrayList<>();
        private List<Integer> assigneeStudentIds = new ArrayList<>();
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
    public static class AssignmentSubmissionPayload {
        private String contentText;
        private String codeLanguage;
        private String codeContent;
        private String filePath;
        @Valid
        private List<AnswerPayload> answers = new ArrayList<>();
    }

    @Data
    public static class TestcasePayload {
        @NotBlank
        private String input;
        @NotBlank
        private String expectedOutput;
        private Boolean hidden = false;
        @NotNull
        @DecimalMin(value = "0.0", inclusive = false)
        private BigDecimal points = BigDecimal.ONE;
        private Integer displayOrder;
    }

    @Data
    public static class GradePayload {
        @NotNull
        @DecimalMin("0.0")
        private BigDecimal score;
        @NotBlank
        private String feedback;
        private Boolean publish = true;
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
        private String topicCode;
        private QuestionDifficulty difficulty;
        private QuestionReviewStatus reviewStatus;
        private Integer version;
        private Boolean active;
        private QuestionGenerationSource generationSource;
        private List<OptionView> options = new ArrayList<>();
        private List<Integer> selectedOptionIds = new ArrayList<>();
    }

    @Data
    public static class BlueprintItemPayload {
        @NotBlank
        private String topicCode;
        @NotNull
        private QuestionDifficulty difficulty;
        @Positive
        private Integer questionCount;
        private Integer displayOrder;
    }

    @Data
    public static class BlueprintBucketView {
        private String topicCode;
        private QuestionDifficulty difficulty;
        private Integer required;
        private Long available;
        private Boolean ready;
    }

    @Data
    public static class QuizReadinessView {
        private Boolean ready;
        private List<BlueprintBucketView> buckets = new ArrayList<>();
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
    public static class SubmissionView {
        private Integer id;
        private Integer assignmentId;
        private Integer lessonId;
        private Integer courseId;
        private Integer attemptNo;
        private String courseTitle;
        private String assignmentTitle;
        private String assignmentType;
        private String studentName;
        private String contentText;
        private String codeLanguage;
        private String codeContent;
        private String filePath;
        private List<QuestionView> answers = new ArrayList<>();
        private SubmissionStatus status;
        private BigDecimal score;
        private BigDecimal maxScore;
        private String feedback;
        private LocalDateTime submittedAt;
        private LocalDateTime updatedAt;
        private LocalDateTime gradedAt;
        private String gradedByName;
        private Boolean released;
        private CodeJudgeStatus judgeStatus;
        private Integer totalTests;
        private Integer passedTests;
        private String outputLog;
        private List<SubmissionView> history = new ArrayList<>();
    }

    @Data
    public static class AssignmentView {
        private Integer id;
        private Integer lessonId;
        private Integer courseId;
        private String courseTitle;
        private String lessonTitle;
        private String title;
        private String problemStatement;
        private String instructions;
        private String starterCode;
        private String allowedLanguages;
        private Integer timeLimitMs;
        private String type;
        private LocalDateTime dueDate;
        private String status;
        private BigDecimal maxScore;
        private List<Integer> assigneeStudentIds = new ArrayList<>();
        private long assigneeCount;
        private long submissionCount;
        private long pendingCount;
        private List<QuestionView> questions = new ArrayList<>();
    }

    @Data
    public static class GradingSummaryView {
        private long pendingCount;
        private long gradedCount;
        private long failedCount;
    }

    @Data
    public static class TestcaseView {
        private Integer id;
        private String input;
        private String expectedOutput;
        private Boolean hidden;
        private BigDecimal points;
        private Integer displayOrder;
    }

    @Data
    public static class ResultSummaryView {
        private List<QuizAttemptView> quizAttempts = new ArrayList<>();
        private List<SubmissionView> submissions = new ArrayList<>();
    }
}
