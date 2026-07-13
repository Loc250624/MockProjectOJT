package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.request.VideoProgressRequestDTO;
import com.ojtsu26.elearning.dto.response.LearningProgressDTO;
import com.ojtsu26.elearning.dto.response.StudentCourseProgressCardDTO;
import com.ojtsu26.elearning.dto.response.StudentLearningCourseDTO;
import com.ojtsu26.elearning.dto.response.StudentLearningLessonDTO;

import java.util.List;

public interface StudentLearningService {
    StudentLearningCourseDTO getLearningCourse(Integer courseId, Integer lessonId);
    StudentLearningLessonDTO openLesson(Integer courseId, Integer lessonId);
    LearningProgressDTO recordVideoProgress(Integer courseId, Integer lessonId, VideoProgressRequestDTO request);
    LearningProgressDTO completeLesson(Integer courseId, Integer lessonId);
    LearningProgressDTO getCourseProgress(Integer courseId);
    List<StudentCourseProgressCardDTO> getCurrentStudentCourseCards();
}
