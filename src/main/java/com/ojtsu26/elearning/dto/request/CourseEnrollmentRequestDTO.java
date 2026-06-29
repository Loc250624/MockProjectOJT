package com.ojtsu26.elearning.dto.request;

import lombok.Data;
import com.ojtsu26.elearning.model.enums.*;

@Data
public class CourseEnrollmentRequestDTO {
    @jakarta.validation.constraints.NotNull
    private java.math.BigDecimal progressPercentage;

    @jakarta.validation.constraints.NotNull
    private Boolean isCompleted;

    @jakarta.validation.constraints.NotNull
    private Integer studentId;

    @jakarta.validation.constraints.NotNull
    private Integer courseId;
}
