package com.ojtsu26.elearning.dto.request;

import com.ojtsu26.elearning.model.enums.FeedbackCategory;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StudentFeedbackRequestDTO {

    private FeedbackCategory category;

    private String subject;

    @NotBlank(message = "Content is required")
    @Size(max = 5000, message = "Content must not exceed 5000 characters")
    private String content;

    @NotNull(message = "Course content rating is required")
    @Min(value = 1, message = "Course content rating must be at least 1")
    @Max(value = 5, message = "Course content rating must not exceed 5")
    private Integer courseContentRating;

    @NotNull(message = "Instructor support rating is required")
    @Min(value = 1, message = "Instructor support rating must be at least 1")
    @Max(value = 5, message = "Instructor support rating must not exceed 5")
    private Integer instructorSupportRating;

    @NotNull(message = "Learning experience rating is required")
    @Min(value = 1, message = "Learning experience rating must be at least 1")
    @Max(value = 5, message = "Learning experience rating must not exceed 5")
    private Integer learningExperienceRating;

    @NotNull(message = "Platform usability rating is required")
    @Min(value = 1, message = "Platform usability rating must be at least 1")
    @Max(value = 5, message = "Platform usability rating must not exceed 5")
    private Integer platformUsabilityRating;

    @NotNull(message = "Assessment experience rating is required")
    @Min(value = 1, message = "Assessment experience rating must be at least 1")
    @Max(value = 5, message = "Assessment experience rating must not exceed 5")
    private Integer assessmentExperienceRating;

    @NotNull(message = "Overall satisfaction rating is required")
    @Min(value = 1, message = "Overall satisfaction rating must be at least 1")
    @Max(value = 5, message = "Overall satisfaction rating must not exceed 5")
    private Integer overallSatisfactionRating;
}
