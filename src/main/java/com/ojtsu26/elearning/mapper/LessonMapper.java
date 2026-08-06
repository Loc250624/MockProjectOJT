package com.ojtsu26.elearning.mapper;

import com.ojtsu26.elearning.common.CourseDurationFormatter;
import com.ojtsu26.elearning.model.entity.Lesson;
import com.ojtsu26.elearning.dto.request.LessonRequestDTO;
import com.ojtsu26.elearning.dto.response.LessonResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.AfterMapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface LessonMapper {

    @Mapping(source = "course.id", target = "courseId")
    @Mapping(source = "quiz.id", target = "quizId")
    @Mapping(expression = "java(entity.getQuiz() != null)", target = "hasQuiz")
    @Mapping(source = "video.durationSeconds", target = "videoDurationSeconds")
    @Mapping(target = "videoDurationDisplay", ignore = true)
    LessonResponseDTO toDto(Lesson entity);

    @AfterMapping
    default void addVideoDurationDisplay(Lesson entity, @MappingTarget LessonResponseDTO dto) {
        dto.setVideoDurationDisplay(CourseDurationFormatter.formatVideoSeconds(dto.getVideoDurationSeconds()));
    }

    @Mapping(source = "courseId", target = "course.id")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "video", ignore = true)
    @Mapping(target = "quiz", ignore = true)
    @Mapping(target = "lessonprogresss", ignore = true)
    @Mapping(target = "submissions", ignore = true)
    Lesson toEntity(LessonRequestDTO dto);
}
