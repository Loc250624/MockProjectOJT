package com.ojtsu26.elearning.dto.response;

import com.ojtsu26.elearning.model.enums.FeedbackCategory;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class StudentFeedbackResponseDTO {
    private Integer id;
    private FeedbackCategory category;
    private String categoryLabel;
    private String subject;
    private String content;
    private Integer courseContentRating;
    private Integer instructorSupportRating;
    private Integer learningExperienceRating;
    private Integer platformUsabilityRating;
    private Integer assessmentExperienceRating;
    private Integer overallSatisfactionRating;
    private Integer studentId;
    private String studentName;
    private String studentEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
