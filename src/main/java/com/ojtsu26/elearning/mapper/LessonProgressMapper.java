package com.ojtsu26.elearning.mapper;

import com.ojtsu26.elearning.model.entity.LessonProgress;
import com.ojtsu26.elearning.dto.request.LessonProgressRequestDTO;
import com.ojtsu26.elearning.dto.response.LessonProgressResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LessonProgressMapper {

    @Mapping(source = "enrollment.id", target = "enrollmentId")
    @Mapping(source = "lesson.id", target = "lessonId")
    LessonProgressResponseDTO toDto(LessonProgress entity);

    @Mapping(source = "enrollmentId", target = "enrollment.id")
    @Mapping(source = "lessonId", target = "lesson.id")
    LessonProgress toEntity(LessonProgressRequestDTO dto);
}
