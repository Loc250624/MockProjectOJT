package com.ojtsu26.elearning.mapper;

import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.dto.request.CourseRequestDTO;
import com.ojtsu26.elearning.dto.response.CourseResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CourseMapper {

    @Mapping(source = "instructor.id", target = "instructorId")
    @Mapping(source = "instructor.fullName", target = "instructorName")
    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "category.name", target = "categoryName")
    @Mapping(source = "roadmap.id", target = "roadmapId")
    @Mapping(source = "roadmap.title", target = "roadmapTitle")
    @Mapping(target = "enrollmentStatus", ignore = true)
    CourseResponseDTO toDto(Course entity);

    @Mapping(source = "instructorId", target = "instructor.id")
    @Mapping(source = "categoryId", target = "category.id")
    @Mapping(source = "roadmapId", target = "roadmap.id")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "rejectReason", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "lessons", ignore = true)
    @Mapping(target = "courseenrollments", ignore = true)
    @Mapping(target = "certificates", ignore = true)
    @Mapping(target = "transactions", ignore = true)
    Course toEntity(CourseRequestDTO dto);
}
