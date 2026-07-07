package com.ojtsu26.elearning.dto.response;

import com.ojtsu26.elearning.model.enums.LessonType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class StudentLearningLessonDTO {
    private Integer id;
    private Integer courseId;
    private String title;
    private LessonType type;
    private Integer orderIndex;
    private String sanitizedContent;
    private String videoUrl;
    private String embedUrl;
    private Integer videoDurationSeconds;
    private Integer watchedSeconds;
    private Integer previousLessonId;
    private Integer nextLessonId;
    private Boolean completed;
    private LearningProgressDTO courseProgress;
    private List<StudentLearningResourceDTO> resources;
}
