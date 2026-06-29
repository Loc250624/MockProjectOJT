package com.ojtsu26.elearning.dto.request;

import lombok.Data;
import com.ojtsu26.elearning.model.enums.*;

@Data
public class VideoRequestDTO {
    @jakarta.validation.constraints.NotBlank
    private String videoUrl;

    @jakarta.validation.constraints.NotNull
    private Integer durationSeconds;

    @jakarta.validation.constraints.NotNull
    private Integer lessonId;
}
