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
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lastAccessedAt", ignore = true)
    @Mapping(target = "watchedSeconds", ignore = true)
    @Mapping(target = "lastPositionSeconds", ignore = true)
    @Mapping(target = "maxReachedSeconds", ignore = true)
    @Mapping(target = "durationSeconds", ignore = true)
    @Mapping(target = "lastUpdatedAt", ignore = true)
    LessonProgress toEntity(LessonProgressRequestDTO dto);
}
