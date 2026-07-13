package com.ojtsu26.elearning.dto.response;

import com.ojtsu26.elearning.model.enums.LessonType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StudentLearningLessonSummaryDTO {
    private Integer id;
    private String title;
    private LessonType type;
    private Integer orderIndex;
    private Boolean completed;
    private Boolean current;
    private Boolean required;
    private Boolean accessible;
    private Boolean locked;
    private String lockReason;
    private Integer watchedSeconds;
    private Integer videoDurationSeconds;
}
