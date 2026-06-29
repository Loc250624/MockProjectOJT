package com.ojtsu26.elearning.mapper;

import com.ojtsu26.elearning.model.entity.Video;
import com.ojtsu26.elearning.dto.request.VideoRequestDTO;
import com.ojtsu26.elearning.dto.response.VideoResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VideoMapper {

    @Mapping(source = "lesson.id", target = "lessonId")
    VideoResponseDTO toDto(Video entity);

    @Mapping(source = "lessonId", target = "lesson.id")
    Video toEntity(VideoRequestDTO dto);
}
