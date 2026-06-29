package com.ojtsu26.elearning.mapper;

import com.ojtsu26.elearning.dto.request.TestcaseRequestDTO;
import com.ojtsu26.elearning.dto.response.TestcaseResponseDTO;
import com.ojtsu26.elearning.model.entity.CodingAssignment;
import com.ojtsu26.elearning.model.entity.Testcase;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-29T19:01:29+0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.11 (Oracle Corporation)"
)
@Component
public class TestcaseMapperImpl implements TestcaseMapper {

    @Override
    public TestcaseResponseDTO toDto(Testcase entity) {
        if ( entity == null ) {
            return null;
        }

        TestcaseResponseDTO testcaseResponseDTO = new TestcaseResponseDTO();

        testcaseResponseDTO.setAssignmentId( entityAssignmentId( entity ) );
        testcaseResponseDTO.setId( entity.getId() );
        testcaseResponseDTO.setInputData( entity.getInputData() );
        testcaseResponseDTO.setExpectedOutput( entity.getExpectedOutput() );
        testcaseResponseDTO.setIsHidden( entity.getIsHidden() );

        return testcaseResponseDTO;
    }

    @Override
    public Testcase toEntity(TestcaseRequestDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Testcase.TestcaseBuilder testcase = Testcase.builder();

        testcase.assignment( testcaseRequestDTOToCodingAssignment( dto ) );
        testcase.inputData( dto.getInputData() );
        testcase.expectedOutput( dto.getExpectedOutput() );
        testcase.isHidden( dto.getIsHidden() );

        return testcase.build();
    }

    private Integer entityAssignmentId(Testcase testcase) {
        if ( testcase == null ) {
            return null;
        }
        CodingAssignment assignment = testcase.getAssignment();
        if ( assignment == null ) {
            return null;
        }
        Integer id = assignment.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    protected CodingAssignment testcaseRequestDTOToCodingAssignment(TestcaseRequestDTO testcaseRequestDTO) {
        if ( testcaseRequestDTO == null ) {
            return null;
        }

        CodingAssignment.CodingAssignmentBuilder codingAssignment = CodingAssignment.builder();

        codingAssignment.id( testcaseRequestDTO.getAssignmentId() );

        return codingAssignment.build();
    }
}
