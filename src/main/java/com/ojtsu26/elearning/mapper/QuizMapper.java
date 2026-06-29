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
    Quiz toEntity(QuizRequestDTO dto);
}
