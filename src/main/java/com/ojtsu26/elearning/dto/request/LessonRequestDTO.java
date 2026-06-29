package com.ojtsu26.elearning.dto.request;

import lombok.Data;
import com.ojtsu26.elearning.model.enums.*;

@Data
public class LessonRequestDTO {
    @jakarta.validation.constraints.NotBlank
    private String title;

    @jakarta.validation.constraints.NotBlank
    private String content;

    @jakarta.validation.constraints.NotNull
    private LessonType type;

    @jakarta.validation.constraints.NotNull
    private Integer orderIndex;

    @jakarta.validation.constraints.NotNull
    private Integer courseId;
}
