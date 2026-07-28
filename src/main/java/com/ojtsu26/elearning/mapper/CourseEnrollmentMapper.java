package com.ojtsu26.elearning.mapper;

import com.ojtsu26.elearning.model.entity.CourseEnrollment;
import com.ojtsu26.elearning.dto.request.CourseEnrollmentRequestDTO;
import com.ojtsu26.elearning.dto.response.CourseEnrollmentResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CourseEnrollmentMapper {

    @Mapping(source = "student.id", target = "studentId")
    @Mapping(source = "course.id", target = "courseId")
    CourseEnrollmentResponseDTO toDto(CourseEnrollment entity);

    @Mapping(source = "studentId", target = "student.id")
    @Mapping(source = "courseId", target = "course.id")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enrolledAt", ignore = true)
    @Mapping(target = "lessonprogresss", ignore = true)
    @Mapping(target = "certificate", ignore = true)
    CourseEnrollment toEntity(CourseEnrollmentRequestDTO dto);
}
