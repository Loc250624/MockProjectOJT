package com.ojtsu26.elearning.dto.response;

import lombok.Data;
import com.ojtsu26.elearning.model.enums.*;

@Data
public class CodingAssignmentResponseDTO {
    private Integer id;

    private String title;

    private String problemStatement;

    private String starterCode;

    private String allowedLanguages;

    private Integer timeLimitMs;

    private Integer lessonId;
}
