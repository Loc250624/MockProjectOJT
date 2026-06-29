package com.ojtsu26.elearning.mapper;

import com.ojtsu26.elearning.model.entity.Question;
import com.ojtsu26.elearning.dto.request.QuestionRequestDTO;
import com.ojtsu26.elearning.dto.response.QuestionResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface QuestionMapper {

    @Mapping(source = "quiz.id", target = "quizId")
    QuestionResponseDTO toDto(Question entity);

    @Mapping(source = "quizId", target = "quiz.id")
    Question toEntity(QuestionRequestDTO dto);
}
