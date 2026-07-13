package com.ojtsu26.elearning.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class StudentLearningCourseDTO {
    private Integer courseId;
    private String courseTitle;
    private String instructorName;
    private Integer enrollmentId;
    private Integer activeLessonId;
    private Integer lastAccessedLessonId;
    private Integer completedLessons;
    private Integer totalLessons;
    private BigDecimal progressPercentage;
    private Boolean completed;
    private String courseStatus;
    private StudentLearningLessonDTO activeLesson;
    private List<StudentLearningLessonSummaryDTO> lessons;
    private List<StudentLearningResourceDTO> courseResources;
}
