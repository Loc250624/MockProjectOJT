package com.ojtsu26.elearning.mapper;

import com.ojtsu26.elearning.dto.request.VideoRequestDTO;
import com.ojtsu26.elearning.dto.response.VideoResponseDTO;
import com.ojtsu26.elearning.model.entity.Lesson;
import com.ojtsu26.elearning.model.entity.Video;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-29T19:01:29+0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.11 (Oracle Corporation)"
)
@Component
public class VideoMapperImpl implements VideoMapper {

    @Override
    public VideoResponseDTO toDto(Video entity) {
        if ( entity == null ) {
            return null;
        }

        VideoResponseDTO videoResponseDTO = new VideoResponseDTO();

        videoResponseDTO.setLessonId( entityLessonId( entity ) );
        videoResponseDTO.setId( entity.getId() );
        videoResponseDTO.setVideoUrl( entity.getVideoUrl() );
        videoResponseDTO.setDurationSeconds( entity.getDurationSeconds() );

        return videoResponseDTO;
    }

    @Override
    public Video toEntity(VideoRequestDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Video.VideoBuilder video = Video.builder();

        video.lesson( videoRequestDTOToLesson( dto ) );
        video.videoUrl( dto.getVideoUrl() );
        video.durationSeconds( dto.getDurationSeconds() );

        return video.build();
    }

    private Integer entityLessonId(Video video) {
        if ( video == null ) {
            return null;
        }
        Lesson lesson = video.getLesson();
        if ( lesson == null ) {
            return null;
        }
        Integer id = lesson.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    protected Lesson videoRequestDTOToLesson(VideoRequestDTO videoRequestDTO) {
        if ( videoRequestDTO == null ) {
            return null;
        }

        Lesson.LessonBuilder lesson = Lesson.builder();

        lesson.id( videoRequestDTO.getLessonId() );

        return lesson.build();
    }
}
