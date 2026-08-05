package com.ojtsu26.elearning.dto.response;

import com.ojtsu26.elearning.model.enums.LessonType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
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
    private String videoSourceType;
    private Boolean videoPlayable;
    private String videoUnavailableReason;
    private Integer videoDurationSeconds;
    private String videoDurationDisplay;
    private Integer watchedSeconds;
    private Integer lastPositionSeconds;
    private Integer maxReachedSeconds;
    private BigDecimal videoProgressPercentage;
    private Integer previousLessonId;
    private Integer nextLessonId;
    private Boolean nextLessonAccessible;
    private String nextLessonLockReason;
    private Boolean required;
    private Boolean accessible;
    private Boolean locked;
    private String lockReason;
    private Boolean completed;
    private LearningProgressDTO courseProgress;
    private List<StudentLearningResourceDTO> resources;
}
