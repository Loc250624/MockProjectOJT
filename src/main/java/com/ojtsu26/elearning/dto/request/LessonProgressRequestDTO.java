package com.ojtsu26.elearning.dto.request;

import lombok.Data;
import com.ojtsu26.elearning.model.enums.*;

@Data
public class LessonProgressRequestDTO {
    @jakarta.validation.constraints.NotNull
    private Boolean isCompleted;

    @jakarta.validation.constraints.NotNull
    private java.time.LocalDateTime completedAt;

    @jakarta.validation.constraints.NotNull
    private Integer enrollmentId;

    @jakarta.validation.constraints.NotNull
    private Integer lessonId;
}
