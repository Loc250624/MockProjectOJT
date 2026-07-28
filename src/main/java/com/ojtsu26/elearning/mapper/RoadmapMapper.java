package com.ojtsu26.elearning.mapper;

import com.ojtsu26.elearning.model.entity.Roadmap;
import com.ojtsu26.elearning.dto.request.RoadmapRequestDTO;
import com.ojtsu26.elearning.dto.response.RoadmapResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoadmapMapper {

    @Mapping(source = "instructor.id", target = "instructorId")
    RoadmapResponseDTO toDto(Roadmap entity);

    @Mapping(source = "instructorId", target = "instructor.id")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "courses", ignore = true)
    Roadmap toEntity(RoadmapRequestDTO dto);
}
