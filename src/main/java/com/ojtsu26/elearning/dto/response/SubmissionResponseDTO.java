package com.ojtsu26.elearning.dto.response;

import lombok.Data;
import com.ojtsu26.elearning.model.enums.*;

@Data
public class SubmissionResponseDTO {
    private Integer id;

    private java.math.BigDecimal score;

    private SubmissionStatus status;

    private String submittedContent;

    private String teacherFeedback;

    private java.time.LocalDateTime submittedAt;

    private Integer studentId;

    private Integer lessonId;
}
