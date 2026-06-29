package com.ojtsu26.elearning.dto.request;

import lombok.Data;
import com.ojtsu26.elearning.model.enums.*;

@Data
public class QuizRequestDTO {
    @jakarta.validation.constraints.NotBlank
    private String title;

    @jakarta.validation.constraints.NotNull
    private java.math.BigDecimal passingScore;

    @jakarta.validation.constraints.NotNull
    private Integer lessonId;
}
