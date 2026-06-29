package com.ojtsu26.elearning.mapper;

import com.ojtsu26.elearning.model.entity.CodingAssignment;
import com.ojtsu26.elearning.dto.request.CodingAssignmentRequestDTO;
import com.ojtsu26.elearning.dto.response.CodingAssignmentResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CodingAssignmentMapper {

    @Mapping(source = "lesson.id", target = "lessonId")
    CodingAssignmentResponseDTO toDto(CodingAssignment entity);

    @Mapping(source = "lessonId", target = "lesson.id")
    CodingAssignment toEntity(CodingAssignmentRequestDTO dto);
}
