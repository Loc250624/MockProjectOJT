package com.ojtsu26.elearning.dto.request;

import lombok.Data;
import com.ojtsu26.elearning.model.enums.*;

@Data
public class SubmissionRequestDTO {
    @jakarta.validation.constraints.NotNull
    private java.math.BigDecimal score;

    @jakarta.validation.constraints.NotNull
    private SubmissionStatus status;

    @jakarta.validation.constraints.NotBlank
    private String submittedContent;

    @jakarta.validation.constraints.NotBlank
    private String teacherFeedback;

    @jakarta.validation.constraints.NotNull
    private Integer studentId;

    @jakarta.validation.constraints.NotNull
    private Integer lessonId;
}
