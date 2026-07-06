package com.ojtsu26.elearning.dto.response;

import com.ojtsu26.elearning.model.enums.LessonType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TeacherProgressLessonDTO {
    private Integer lessonId;
    private String title;
    private LessonType type;
    private Integer orderIndex;
    private boolean completed;
    private LocalDateTime completedAt;
    private LocalDateTime lastAccessedAt;
    private Integer watchedSeconds;
}
