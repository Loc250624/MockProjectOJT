package com.ojtsu26.elearning.dto.response;

import com.ojtsu26.elearning.model.enums.SubmissionStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class StudentQuizAttemptDTO {
    private Integer courseId;
    private Integer lessonId;
    private Integer quizId;
    private String quizTitle;
    private Integer attemptId;
    private SubmissionStatus status;
    private String attemptState;
    private BigDecimal passingScore;
    private BigDecimal score;
    private Boolean passed;
    private Boolean submitted;
    private Boolean unavailable;
    private String unavailableMessage;
    private List<StudentQuizQuestionDTO> questions;
    private Map<Integer, String> answers;
    private LocalDateTime submittedAt;
    private LearningProgressDTO learningProgress;
}
