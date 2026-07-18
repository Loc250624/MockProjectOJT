package com.ojtsu26.elearning.mapper;

import com.ojtsu26.elearning.model.entity.Lesson;
import com.ojtsu26.elearning.dto.request.LessonRequestDTO;
import com.ojtsu26.elearning.dto.response.LessonResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LessonMapper {

    @Mapping(source = "course.id", target = "courseId")
    @Mapping(source = "quiz.id", target = "quizId")
    @Mapping(source = "codingassignment.id", target = "codingAssignmentId")
    @Mapping(expression = "java(entity.getQuiz() != null)", target = "hasQuiz")
    @Mapping(expression = "java(entity.getCodingassignment() != null)", target = "hasCodingAssignment")
    LessonResponseDTO toDto(Lesson entity);

    @Mapping(source = "courseId", target = "course.id")
    Lesson toEntity(LessonRequestDTO dto);
}
