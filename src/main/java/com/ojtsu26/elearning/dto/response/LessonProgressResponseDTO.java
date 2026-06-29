package com.ojtsu26.elearning.dto.response;

import lombok.Data;
import com.ojtsu26.elearning.model.enums.*;

@Data
public class LessonProgressResponseDTO {
    private Integer id;

    private Boolean isCompleted;

    private java.time.LocalDateTime completedAt;

    private Integer enrollmentId;

    private Integer lessonId;
}
