package com.ojtsu26.elearning.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StudentQuizQuestionDTO {
    private Integer id;
    private String questionText;
    private String optionsJson;
}
