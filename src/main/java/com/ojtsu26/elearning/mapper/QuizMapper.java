package com.ojtsu26.elearning.mapper;

import com.ojtsu26.elearning.model.entity.Quiz;
import com.ojtsu26.elearning.dto.request.QuizRequestDTO;
import com.ojtsu26.elearning.dto.response.QuizResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface QuizMapper {

    @Mapping(source = "lesson.id", target = "lessonId")
    QuizResponseDTO toDto(Quiz entity);

    @Mapping(source = "lessonId", target = "lesson.id")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "durationMinutes", ignore = true)
    @Mapping(target = "maxAttempts", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "questions", ignore = true)
    @Mapping(target = "attempts", ignore = true)
    Quiz toEntity(QuizRequestDTO dto);
}
