package com.ojtsu26.elearning.mapper;

import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.dto.request.CourseRequestDTO;
import com.ojtsu26.elearning.dto.response.CourseResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CourseMapper {

    @Mapping(source = "instructor.id", target = "instructorId")
    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "roadmap.id", target = "roadmapId")
    CourseResponseDTO toDto(Course entity);

    @Mapping(source = "instructorId", target = "instructor.id")
    @Mapping(source = "categoryId", target = "category.id")
    @Mapping(source = "roadmapId", target = "roadmap.id")
    Course toEntity(CourseRequestDTO dto);
}
