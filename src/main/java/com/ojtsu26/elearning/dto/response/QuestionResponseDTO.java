package com.ojtsu26.elearning.dto.response;

import lombok.Data;
import com.ojtsu26.elearning.model.enums.*;

@Data
public class QuestionResponseDTO {
    private Integer id;

    private String questionText;

    private String optionsJson;

    private String correctAnswer;

    private Integer quizId;
}
