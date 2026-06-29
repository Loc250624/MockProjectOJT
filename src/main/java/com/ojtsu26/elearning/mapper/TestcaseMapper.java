package com.ojtsu26.elearning.mapper;

import com.ojtsu26.elearning.model.entity.Testcase;
import com.ojtsu26.elearning.dto.request.TestcaseRequestDTO;
import com.ojtsu26.elearning.dto.response.TestcaseResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TestcaseMapper {

    @Mapping(source = "assignment.id", target = "assignmentId")
    TestcaseResponseDTO toDto(Testcase entity);

    @Mapping(source = "assignmentId", target = "assignment.id")
    Testcase toEntity(TestcaseRequestDTO dto);
}
