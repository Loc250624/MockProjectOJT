package com.ojtsu26.elearning.mapper;

import com.ojtsu26.elearning.dto.request.LessonProgressRequestDTO;
import com.ojtsu26.elearning.dto.response.LessonProgressResponseDTO;
import com.ojtsu26.elearning.model.entity.CourseEnrollment;
import com.ojtsu26.elearning.model.entity.Lesson;
import com.ojtsu26.elearning.model.entity.LessonProgress;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-29T22:09:51+0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.11 (Oracle Corporation)"
)
@Component
public class LessonProgressMapperImpl implements LessonProgressMapper {

    @Override
    public LessonProgressResponseDTO toDto(LessonProgress entity) {
        if ( entity == null ) {
            return null;
        }

        LessonProgressResponseDTO lessonProgressResponseDTO = new LessonProgressResponseDTO();

        lessonProgressResponseDTO.setEnrollmentId( entityEnrollmentId( entity ) );
        lessonProgressResponseDTO.setLessonId( entityLessonId( entity ) );
        lessonProgressResponseDTO.setId( entity.getId() );
        lessonProgressResponseDTO.setIsCompleted( entity.getIsCompleted() );
        lessonProgressResponseDTO.setCompletedAt( entity.getCompletedAt() );

        return lessonProgressResponseDTO;
    }

    @Override
    public LessonProgress toEntity(LessonProgressRequestDTO dto) {
        if ( dto == null ) {
            return null;
        }

        LessonProgress.LessonProgressBuilder lessonProgress = LessonProgress.builder();

        lessonProgress.enrollment( lessonProgressRequestDTOToCourseEnrollment( dto ) );
        lessonProgress.lesson( lessonProgressRequestDTOToLesson( dto ) );
        lessonProgress.isCompleted( dto.getIsCompleted() );
        lessonProgress.completedAt( dto.getCompletedAt() );

        return lessonProgress.build();
    }

    private Integer entityEnrollmentId(LessonProgress lessonProgress) {
        if ( lessonProgress == null ) {
            return null;
        }
        CourseEnrollment enrollment = lessonProgress.getEnrollment();
        if ( enrollment == null ) {
            return null;
        }
        Integer id = enrollment.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private Integer entityLessonId(LessonProgress lessonProgress) {
        if ( lessonProgress == null ) {
            return null;
        }
        Lesson lesson = lessonProgress.getLesson();
        if ( lesson == null ) {
            return null;
        }
        Integer id = lesson.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    protected CourseEnrollment lessonProgressRequestDTOToCourseEnrollment(LessonProgressRequestDTO lessonProgressRequestDTO) {
        if ( lessonProgressRequestDTO == null ) {
            return null;
        }

        CourseEnrollment.CourseEnrollmentBuilder courseEnrollment = CourseEnrollment.builder();

        courseEnrollment.id( lessonProgressRequestDTO.getEnrollmentId() );

        return courseEnrollment.build();
    }

    protected Lesson lessonProgressRequestDTOToLesson(LessonProgressRequestDTO lessonProgressRequestDTO) {
        if ( lessonProgressRequestDTO == null ) {
            return null;
        }

        Lesson.LessonBuilder lesson = Lesson.builder();

        lesson.id( lessonProgressRequestDTO.getLessonId() );

        return lesson.build();
    }
}
