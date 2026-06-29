package com.ojtsu26.elearning.dto.response;

import lombok.Data;
import com.ojtsu26.elearning.model.enums.*;

@Data
public class QuizResponseDTO {
    private Integer id;

    private String title;

    private java.math.BigDecimal passingScore;

    private Integer lessonId;
}
