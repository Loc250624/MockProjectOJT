package com.ojtsu26.elearning.mapper;

import com.ojtsu26.elearning.dto.request.CourseEnrollmentRequestDTO;
import com.ojtsu26.elearning.dto.response.CourseEnrollmentResponseDTO;
import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.entity.CourseEnrollment;
import com.ojtsu26.elearning.model.entity.User;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-29T22:09:51+0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.11 (Oracle Corporation)"
)
@Component
public class CourseEnrollmentMapperImpl implements CourseEnrollmentMapper {

    @Override
    public CourseEnrollmentResponseDTO toDto(CourseEnrollment entity) {
        if ( entity == null ) {
            return null;
        }

        CourseEnrollmentResponseDTO courseEnrollmentResponseDTO = new CourseEnrollmentResponseDTO();

        courseEnrollmentResponseDTO.setStudentId( entityStudentId( entity ) );
        courseEnrollmentResponseDTO.setCourseId( entityCourseId( entity ) );
        courseEnrollmentResponseDTO.setId( entity.getId() );
        courseEnrollmentResponseDTO.setProgressPercentage( entity.getProgressPercentage() );
        courseEnrollmentResponseDTO.setIsCompleted( entity.getIsCompleted() );
        courseEnrollmentResponseDTO.setEnrolledAt( entity.getEnrolledAt() );

        return courseEnrollmentResponseDTO;
    }

    @Override
    public CourseEnrollment toEntity(CourseEnrollmentRequestDTO dto) {
        if ( dto == null ) {
            return null;
        }

        CourseEnrollment.CourseEnrollmentBuilder courseEnrollment = CourseEnrollment.builder();

        courseEnrollment.student( courseEnrollmentRequestDTOToUser( dto ) );
        courseEnrollment.course( courseEnrollmentRequestDTOToCourse( dto ) );
        courseEnrollment.progressPercentage( dto.getProgressPercentage() );
        courseEnrollment.isCompleted( dto.getIsCompleted() );

        return courseEnrollment.build();
    }

    private Integer entityStudentId(CourseEnrollment courseEnrollment) {
        if ( courseEnrollment == null ) {
            return null;
        }
        User student = courseEnrollment.getStudent();
        if ( student == null ) {
            return null;
        }
        Integer id = student.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private Integer entityCourseId(CourseEnrollment courseEnrollment) {
        if ( courseEnrollment == null ) {
            return null;
        }
        Course course = courseEnrollment.getCourse();
        if ( course == null ) {
            return null;
        }
        Integer id = course.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    protected User courseEnrollmentRequestDTOToUser(CourseEnrollmentRequestDTO courseEnrollmentRequestDTO) {
        if ( courseEnrollmentRequestDTO == null ) {
            return null;
        }

        User.UserBuilder user = User.builder();

        user.id( courseEnrollmentRequestDTO.getStudentId() );

        return user.build();
    }

    protected Course courseEnrollmentRequestDTOToCourse(CourseEnrollmentRequestDTO courseEnrollmentRequestDTO) {
        if ( courseEnrollmentRequestDTO == null ) {
            return null;
        }

        Course.CourseBuilder course = Course.builder();

        course.id( courseEnrollmentRequestDTO.getCourseId() );

        return course.build();
    }
}
