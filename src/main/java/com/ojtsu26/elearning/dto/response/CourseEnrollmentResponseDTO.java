package com.ojtsu26.elearning.dto.response;

import lombok.Data;
import com.ojtsu26.elearning.model.enums.*;

@Data
public class CourseEnrollmentResponseDTO {
    private Integer id;

    private java.math.BigDecimal progressPercentage;

    private Boolean isCompleted;

    private java.time.LocalDateTime enrolledAt;

    private Integer studentId;

    private Integer courseId;
}
