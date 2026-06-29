package com.ojtsu26.elearning.dto.request;

import lombok.Data;
import com.ojtsu26.elearning.model.enums.*;

@Data
public class QuestionRequestDTO {
    @jakarta.validation.constraints.NotBlank
    private String questionText;

    @jakarta.validation.constraints.NotBlank
    private String optionsJson;

    @jakarta.validation.constraints.NotBlank
    private String correctAnswer;

    @jakarta.validation.constraints.NotNull
    private Integer quizId;
}
