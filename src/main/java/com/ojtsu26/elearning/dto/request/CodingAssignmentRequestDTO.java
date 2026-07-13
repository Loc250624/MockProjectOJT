package com.ojtsu26.elearning.dto.request;

import lombok.Data;
import com.ojtsu26.elearning.model.enums.*;

@Data
public class CodingAssignmentRequestDTO {
    @jakarta.validation.constraints.NotBlank
    private String title;

    @jakarta.validation.constraints.NotBlank
    private String problemStatement;

    private String starterCode;

    @jakarta.validation.constraints.NotBlank
    private String allowedLanguages;

    @jakarta.validation.constraints.NotNull
    private Integer timeLimitMs;

    @jakarta.validation.constraints.NotNull
    private Integer lessonId;
}
