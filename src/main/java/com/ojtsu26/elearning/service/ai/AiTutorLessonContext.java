package com.ojtsu26.elearning.service.ai;

public record AiTutorLessonContext(
        Integer courseId,
        Integer lessonId,
        String courseTitle,
        String sectionTitle,
        String lessonTitle,
        String learningObjectives,
        String lessonText
) {
}
