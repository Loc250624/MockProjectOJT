package com.ojtsu26.elearning.mapper;

import com.ojtsu26.elearning.dto.request.LessonRequestDTO;
import com.ojtsu26.elearning.dto.response.LessonResponseDTO;
import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.entity.Lesson;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-29T15:31:24+0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.11 (Oracle Corporation)"
)
@Component
public class LessonMapperImpl implements LessonMapper {

    @Override
    public LessonResponseDTO toDto(Lesson entity) {
        if ( entity == null ) {
            return null;
        }

        LessonResponseDTO lessonResponseDTO = new LessonResponseDTO();

        lessonResponseDTO.setCourseId( entityCourseId( entity ) );
        lessonResponseDTO.setId( entity.getId() );
        lessonResponseDTO.setTitle( entity.getTitle() );
        lessonResponseDTO.setContent( entity.getContent() );
        lessonResponseDTO.setType( entity.getType() );
        lessonResponseDTO.setOrderIndex( entity.getOrderIndex() );
        lessonResponseDTO.setCreatedAt( entity.getCreatedAt() );

        return lessonResponseDTO;
    }

    @Override
    public Lesson toEntity(LessonRequestDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Lesson.LessonBuilder lesson = Lesson.builder();

        lesson.course( lessonRequestDTOToCourse( dto ) );
        lesson.title( dto.getTitle() );
        lesson.content( dto.getContent() );
        lesson.type( dto.getType() );
        lesson.orderIndex( dto.getOrderIndex() );

        return lesson.build();
    }

    private Integer entityCourseId(Lesson lesson) {
        if ( lesson == null ) {
            return null;
        }
        Course course = lesson.getCourse();
        if ( course == null ) {
            return null;
        }
        Integer id = course.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    protected Course lessonRequestDTOToCourse(LessonRequestDTO lessonRequestDTO) {
        if ( lessonRequestDTO == null ) {
            return null;
        }

        Course.CourseBuilder course = Course.builder();

        course.id( lessonRequestDTO.getCourseId() );

        return course.build();
    }
}
